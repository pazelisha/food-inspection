package com.example.foodgradeinspection;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class InspectionFormActivity extends AppCompatActivity {
    private static final String TAG = "InspectionFormActivity";

    private RadioGroup cleanlinessGroup, foodStorageGroup, pestControlGroup;
    private EditText commentsInput;
    private Button submitButton;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String taskId;
    private String locationId;
    private boolean isTaskCompleted = false;

    private int getSelectedRating(RadioGroup group) {
        int selectedId = group.getCheckedRadioButtonId();
        if (selectedId == -1) return -1;
        return Integer.parseInt(((RadioButton) findViewById(selectedId)).getText().toString());
    }

    private void setSelectedRating(RadioGroup group, int rating) {
        for (int i = 0; i < group.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) group.getChildAt(i);
            if (Integer.parseInt(radioButton.getText().toString()) == rating) {
                radioButton.setChecked(true);
                break;
            }
        }
    }

    private String calculateGrade(double avg) {
        if (avg >= 4.5) return "A";
        else if (avg >= 3.5) return "B";
        else if (avg >= 2.5) return "C";
        else return "D";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspection_form);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get task and location IDs from intent
        taskId = getIntent().getStringExtra("taskId");
        locationId = getIntent().getStringExtra("locationId");

        Log.d(TAG, "onCreate: TaskId: " + taskId + ", LocationId: " + locationId);

        // Initialize UI components
        cleanlinessGroup = findViewById(R.id.cleanlinessGroup);
        foodStorageGroup = findViewById(R.id.foodStorageGroup);
        pestControlGroup = findViewById(R.id.pestControlGroup);
        commentsInput = findViewById(R.id.commentsInput);
        submitButton = findViewById(R.id.submitButton);

        // Check task status and load existing report if completed
        checkTaskStatusAndLoadReport();

        submitButton.setOnClickListener(v -> {
            if (isTaskCompleted) {
                // If task is completed, just close the form
                finish();
                return;
            }

            // Validate ratings
            int c = getSelectedRating(cleanlinessGroup);
            int f = getSelectedRating(foodStorageGroup);
            int p = getSelectedRating(pestControlGroup);

            if (c == -1 || f == -1 || p == -1) {
                Toast.makeText(this, "Please rate all categories.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Calculate grade
            double avg = (c + f + p) / 3.0;
            String grade = calculateGrade(avg);

            // Save report and update task/location
            saveInspectionReport(c, f, p, grade, commentsInput.getText().toString().trim());
        });
    }

    private void checkTaskStatusAndLoadReport() {
        if (taskId == null) {
            Log.e(TAG, "checkTaskStatusAndLoadReport: TaskId is null");
            return;
        }

        // First, check the task status
        db.collection("tasks").document(taskId).get()
                .addOnSuccessListener(taskDoc -> {
                    if (taskDoc.exists()) {
                        String status = taskDoc.getString("status");
                        Log.d(TAG, "checkTaskStatusAndLoadReport: Task status: " + status);

                        if ("completed".equalsIgnoreCase(status)) {
                            isTaskCompleted = true;
                            // Task is completed, try to load existing report
                            loadExistingReport();
                            // Change submit button text and disable form editing
                            submitButton.setText("Close");
                            disableFormEditing();
                        } else {
                            isTaskCompleted = false;
                            submitButton.setText("Submit");
                            // Task is open, form should be empty and editable (default state)
                        }
                    } else {
                        Log.w(TAG, "checkTaskStatusAndLoadReport: Task document not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "checkTaskStatusAndLoadReport: Error fetching task", e);
                    Toast.makeText(this, "Error loading task information", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadExistingReport() {
        db.collection("reports")
                .whereEqualTo("taskId", taskId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot reportDoc = querySnapshot.getDocuments().get(0);
                        Log.d(TAG, "loadExistingReport: Found existing report");

                        // Load data into form
                        Long cleanlinessRating = reportDoc.getLong("cleanlinessRating");
                        Long foodStorageRating = reportDoc.getLong("foodStorageRating");
                        Long pestControlRating = reportDoc.getLong("pestControlRating");
                        String comments = reportDoc.getString("comments");
                        String grade = reportDoc.getString("overallGrade");

                        if (cleanlinessRating != null) {
                            setSelectedRating(cleanlinessGroup, cleanlinessRating.intValue());
                        }
                        if (foodStorageRating != null) {
                            setSelectedRating(foodStorageGroup, foodStorageRating.intValue());
                        }
                        if (pestControlRating != null) {
                            setSelectedRating(pestControlGroup, pestControlRating.intValue());
                        }
                        if (comments != null) {
                            commentsInput.setText(comments);
                        }

                        Log.d(TAG, "loadExistingReport: Loaded report with grade: " + grade);
                    } else {
                        Log.w(TAG, "loadExistingReport: No existing report found for completed task");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadExistingReport: Error loading existing report", e);
                    Toast.makeText(this, "Error loading existing report", Toast.LENGTH_SHORT).show();
                });
    }

    private void disableFormEditing() {
        // Disable all radio buttons
        setRadioGroupEnabled(cleanlinessGroup, false);
        setRadioGroupEnabled(foodStorageGroup, false);
        setRadioGroupEnabled(pestControlGroup, false);

        // Disable comments input
        commentsInput.setEnabled(false);
        commentsInput.setFocusable(false);
    }

    private void setRadioGroupEnabled(RadioGroup radioGroup, boolean enabled) {
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setEnabled(enabled);
        }
    }

    private void saveInspectionReport(int cleanlinessRating, int foodStorageRating, int pestControlRating, String grade, String comments) {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create report data
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("taskId", taskId);
        reportData.put("locationId", locationId);
        reportData.put("inspectorId", currentUserId);
        reportData.put("cleanlinessRating", cleanlinessRating);
        reportData.put("foodStorageRating", foodStorageRating);
        reportData.put("pestControlRating", pestControlRating);
        reportData.put("overallGrade", grade);
        reportData.put("comments", comments);
        reportData.put("timestamp", System.currentTimeMillis());

        Log.d(TAG, "saveInspectionReport: Saving report with grade: " + grade);

        // Save report to Firestore
        db.collection("reports").add(reportData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "saveInspectionReport: Report saved successfully");

                    // Update task status to completed
                    updateTaskStatus();

                    // Update location health rating
                    updateLocationRating(grade);

                    Toast.makeText(this, "Inspection Complete. Grade: " + grade, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveInspectionReport: Error saving report", e);
                    Toast.makeText(this, "Error saving inspection report", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTaskStatus() {
        db.collection("tasks").document(taskId)
                .update("status", "completed")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "updateTaskStatus: Task status updated to completed");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateTaskStatus: Error updating task status", e);
                });
    }

    private void updateLocationRating(String grade) {
        if (locationId == null) {
            Log.w(TAG, "updateLocationRating: LocationId is null, cannot update rating");
            return;
        }

        db.collection("locations").document(locationId)
                .update("healthRating", grade)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "updateLocationRating: Location rating updated to: " + grade);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateLocationRating: Error updating location rating", e);
                });
    }
}