package com.example.foodgradeinspection;

import android.content.Intent;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MapActivity";
    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView tasksRecycler;
    private TaskAdapter taskAdapter;
    private ImageButton filterButton;
    private ImageButton refreshButton;
    private String currentFilter = "open"; // Default filter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Log.d(TAG, "onCreate: Activity started");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Log.d(TAG, "onCreate: Firebase initialized");
        Log.d(TAG, "onCreate: Current user: " + (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "null"));

        // Setup Map Fragment
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            Log.d(TAG, "onCreate: Map fragment found, requesting map");
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "onCreate: Map fragment is null!");
        }

        // Setup RecyclerView for tasks
        tasksRecycler = findViewById(R.id.tasks_recycler);
        tasksRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        taskAdapter = new TaskAdapter(new ArrayList<>(), this::onTaskClicked);
        tasksRecycler.setAdapter(taskAdapter);

        // Setup filter and refresh buttons
        setupButtons();

        loadTasks();
    }

    private void setupButtons() {
        filterButton = findViewById(R.id.filter_button);
        refreshButton = findViewById(R.id.refresh_button);

        // Setup filter button click listener
        filterButton.setOnClickListener(v -> showFilterMenu());

        // Setup refresh button click listener
        refreshButton.setOnClickListener(v -> {
            Log.d(TAG, "Refresh button clicked");
            loadTasks();
        });
    }

    private void showFilterMenu() {
        PopupMenu popup = new PopupMenu(this, filterButton);
        popup.getMenuInflater().inflate(R.menu.filter_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.filter_open) {
                currentFilter = "open";
                Log.d(TAG, "Filter changed to: open");
            } else if (itemId == R.id.filter_completed) {
                currentFilter = "completed";
                Log.d(TAG, "Filter changed to: completed");
            } else if (itemId == R.id.filter_all) {
                currentFilter = "all";
                Log.d(TAG, "Filter changed to: all");
            }
            loadTasks();
            return true;
        });

        popup.show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: Map is ready");
        mMap = googleMap;
        loadLocations();
    }

    private void loadLocations() {
        Log.d(TAG, "loadLocations: Starting to load locations from Firestore");

        // Check authentication status
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "loadLocations: User is not authenticated!");
            return;
        } else {
            Log.d(TAG, "loadLocations: User is authenticated: " + mAuth.getCurrentUser().getUid());
        }

        db.collection("locations")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "loadLocations: Error listening to locations", e);
                            return;
                        }

                        if (snapshots == null) {
                            Log.w(TAG, "loadLocations: Snapshots is null");
                            return;
                        }

                        Log.d(TAG, "loadLocations: Received " + snapshots.size() + " documents");

                        if (snapshots.isEmpty()) {
                            Log.w(TAG, "loadLocations: No locations found in database");
                            return;
                        }

                        mMap.clear();
                        int markersAdded = 0;

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Log.d(TAG, "loadLocations: Processing document: " + doc.getId());

                            try {
                                Double latObj = doc.getDouble("lat");
                                Double lngObj = doc.getDouble("lng");
                                String status = doc.getString("status");
                                String ratingObj = doc.getString("healthRating");
                                String name = doc.getString("name");

                                // Check for null values
                                if (latObj == null || lngObj == null) {
                                    Log.w(TAG, "loadLocations: Document " + doc.getId() + " missing lat/lng coordinates");
                                    continue;
                                }

                                double lat = latObj;
                                double lng = lngObj;
                                String rating = ratingObj != null ? ratingObj : "N";

                                Log.d(TAG, "loadLocations: Adding marker - Name: " + name +
                                        ", Lat: " + lat + ", Lng: " + lng +
                                        ", Status: " + status + ", Rating: " + rating);

                                LatLng pos = new LatLng(lat, lng);

                                // Create custom marker with letter grade
                                if (status != null && status.equals("open")) {
                                    // For open status, use azure color without letter
                                    mMap.addMarker(new MarkerOptions()
                                            .position(pos)
                                            .title(name != null ? name : "Unknown Location")
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                                } else {
                                    // For others, create custom marker with letter grade
                                    mMap.addMarker(new MarkerOptions()
                                            .position(pos)
                                            .title(name != null ? name : "Unknown Location")
                                            .icon(createCustomMarkerWithLetter(rating)));
                                }

                                markersAdded++;

                            } catch (Exception ex) {
                                Log.e(TAG, "loadLocations: Error processing document " + doc.getId(), ex);
                            }
                        }

                        Log.d(TAG, "loadLocations: Successfully added " + markersAdded + " markers to map");

                        // Optional: move camera to first location
                        if (!snapshots.isEmpty()) {
                            try {
                                DocumentSnapshot first = snapshots.getDocuments().get(0);
                                Double latObj = first.getDouble("lat");
                                Double lngObj = first.getDouble("lng");

                                if (latObj != null && lngObj != null) {
                                    LatLng pos = new LatLng(latObj, lngObj);
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 12f));
                                    Log.d(TAG, "loadLocations: Camera moved to first location");
                                } else {
                                    Log.w(TAG, "loadLocations: First document missing coordinates for camera move");
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "loadLocations: Error moving camera", ex);
                            }
                        }
                    }
                });
    }

    private BitmapDescriptor createCustomMarkerWithLetter(String rating) {
        // Get the base color for the rating (Your existing call)
        float hue = getColorForRating(rating);

        // Original base size reference
        int size = 100;

        // Pin shape dimensions (derived from 'size')
        float headRadius = size * 0.38f; // Radius of the pin's "head"
        float headCenterX = size / 2f;

        // Position head in the upper part of a taller canvas
        float headTopPadding = size * 0.02f;
        float headCenterY = headTopPadding + headRadius;

        float tailVisibleHeight = size * 0.45f; // Visual height of the tail below the head
        float neckWidthRatio = 0.75f; // Controls how "pinched" the neck is (0.0-1.0)

        // Calculate Y-coordinates for the pin tip and overall bitmap height
        float tipBaseY = headCenterY + headRadius; // Y where head bottom meets tail top
        float tipActualY = tipBaseY + tailVisibleHeight; // Y of the very tip

        int bitmapWidth = size;
        int bitmapHeight = (int) Math.ceil(tipActualY + size * 0.05f); // Add some bottom padding
        // Recalculate actual tipY based on final bitmapHeight to ensure it's at the padded bottom
        tipActualY = bitmapHeight - (size * 0.05f);

        // Create a bitmap to draw on (now taller)
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Create paint for the pin body (using your original circlePaint logic)
        Paint circlePaint = new Paint();
        circlePaint.setAntiAlias(true);
        float[] hsv = {hue, 1.0f, 0.85f}; // Slightly adjusted brightness for better pin appearance
        int color = Color.HSVToColor(hsv);
        circlePaint.setColor(color);
        circlePaint.setStyle(Paint.Style.FILL); // Ensure it's fill for the path

        // Define the pin's path
        Path pinPath = new Path();
        RectF headOval = new RectF(
                headCenterX - headRadius,
                headCenterY - headRadius,
                headCenterX + headRadius,
                headCenterY + headRadius
        );

        // Calculate neck connection points
        float neckXOffset = headRadius * neckWidthRatio;
        // Ensure argument for sqrt is non-negative (can happen if neckWidthRatio > 1, though clamped by logic)
        float neckYOffsetValInsideSqrt = (headRadius * headRadius) - (neckXOffset * neckXOffset);
        float neckYOffset = (float) Math.sqrt(Math.max(0, neckYOffsetValInsideSqrt));


        float leftNeckX = headCenterX - neckXOffset;
        float rightNeckX = headCenterX + neckXOffset;
        float neckConnectionY = headCenterY + neckYOffset;

        pinPath.moveTo(headCenterX, tipActualY); // Start at the tip
        pinPath.lineTo(leftNeckX, neckConnectionY); // Line to left neck

        // Arc for the head
        float startAngleDeg = (float) Math.toDegrees(Math.atan2(neckConnectionY - headCenterY, leftNeckX - headCenterX));
        float endAngleDeg = (float) Math.toDegrees(Math.atan2(neckConnectionY - headCenterY, rightNeckX - headCenterX));
        float sweepAngleDeg = endAngleDeg - startAngleDeg;
        if (sweepAngleDeg <= 0) { // Ensure Counter-Clockwise sweep for the top arc
            sweepAngleDeg += 360;
        }
        pinPath.arcTo(headOval, startAngleDeg, sweepAngleDeg, false);

        pinPath.close(); // Completes path by drawing line from right neck to tip

        // Draw the pin shape (replaces your canvas.drawCircle)
        canvas.drawPath(pinPath, circlePaint);

        // Create paint for the text (Your original text paint setup)
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(size * 0.4f);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Calculate text position to center it in the pin's "head"
        String letter = rating.toUpperCase();
        Rect textBounds = new Rect();
        textPaint.getTextBounds(letter, 0, letter.length(), textBounds);

        float textX = headCenterX; // Text X is center of the head
        // Adjust Y for proper vertical centering in the head
        float textY = headCenterY - (textPaint.ascent() + textPaint.descent()) / 2f;

        // Draw the letter (Your original text drawing)
        canvas.drawText(letter, textX, textY, textPaint);

        // Your original log
        Log.d(TAG, "createCustomMarkerWithLetter: Created marker for rating " + rating);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private float getColorForRating(String rating) {
        float color;

        switch (rating.toUpperCase()) {
            case "A":
                color = 120f; // Green - healthiest
                break;
            case "B":
                color = 90f;  // Yellow-green
                break;
            case "C":
                color = 45f;  // Orange-yellow
                break;
            case "D":
                color = 0f;   // Red - worst
                break;
            case "N":
            default:
                color = 240f; // Blue or greyish tone for "Not ranked"
                break;
        }

        Log.d(TAG, "getColorForLetterRating: Rating " + rating + " -> Color hue " + color);
        return color;
    }

    private void loadTasks() {
        String uid = mAuth.getUid();
        Log.d(TAG, "loadTasks: Loading tasks for user: " + uid + " with filter: " + currentFilter);

        if (uid == null) {
            Log.w(TAG, "loadTasks: User ID is null, cannot load tasks");
            return;
        }

        // Build query based on current filter
        var query = db.collection("tasks")
                .whereEqualTo("inspectorId", uid);

        if (!currentFilter.equals("all")) {
            query = query.whereEqualTo("status", currentFilter);
        }

        query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "loadTasks: Error listening to tasks", e);
                return;
            }

            if (snapshots == null) {
                Log.w(TAG, "loadTasks: Snapshots is null");
                return;
            }

            Log.d(TAG, "loadTasks: Received " + snapshots.size() + " task documents");

            // If no tasks found, clear the list immediately
            if (snapshots.isEmpty()) {
                Log.d(TAG, "loadTasks: No tasks found, clearing adapter");
                taskAdapter.updateTasks(new ArrayList<>());
                return;
            }

            List<Task> list = new ArrayList<>();
            final int totalTasks = snapshots.size();
            final int[] processedTasks = {0}; // Use array to modify in lambda

            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                try {
                    Task t = doc.toObject(Task.class);
                    if (t != null) {
                        t.setId(doc.getId());

                        // Fetch restaurant name from 'locations' collection
                        String locationId = t.getLocationId();
                        if (locationId != null && !locationId.isEmpty()) {
                            db.collection("locations").document(locationId).get()
                                    .addOnSuccessListener(locationDoc -> {
                                        if (locationDoc.exists()) {
                                            t.setLocationName(locationDoc.getString("name"));
                                        } else {
                                            t.setLocationName("Unknown Location");
                                        }

                                        String status = t.getStatus();
                                        if (status != null && !status.isEmpty()) {
                                            status = status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
                                        }
                                        t.setStatus(status);


                                        synchronized (list) {
                                            list.add(t);
                                            processedTasks[0]++;

                                            // Update adapter only when all tasks are processed
                                            if (processedTasks[0] == totalTasks) {
                                                Log.d(TAG, "loadTasks: All tasks processed, updating adapter with " + list.size() + " tasks");
                                                taskAdapter.updateTasks(new ArrayList<>(list));
                                            }
                                        }
                                    })
                                    .addOnFailureListener(ex -> {
                                        Log.e(TAG, "loadTasks: Failed to fetch location for task " + doc.getId(), ex);
                                        t.setLocationName("Unknown Location");

                                        synchronized (list) {
                                            list.add(t);
                                            processedTasks[0]++;

                                            // Update adapter only when all tasks are processed
                                            if (processedTasks[0] == totalTasks) {
                                                Log.d(TAG, "loadTasks: All tasks processed, updating adapter with " + list.size() + " tasks");
                                                taskAdapter.updateTasks(new ArrayList<>(list));
                                            }
                                        }
                                    });
                        } else {
                            // If no locationId, add task immediately
                            t.setLocationName("Unknown Location");
                            synchronized (list) {
                                list.add(t);
                                processedTasks[0]++;

                                if (processedTasks[0] == totalTasks) {
                                    Log.d(TAG, "loadTasks: All tasks processed, updating adapter with " + list.size() + " tasks");
                                    taskAdapter.updateTasks(new ArrayList<>(list));
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "loadTasks: Failed to convert document to Task: " + doc.getId());
                        synchronized (list) {
                            processedTasks[0]++;
                            if (processedTasks[0] == totalTasks) {
                                Log.d(TAG, "loadTasks: All tasks processed, updating adapter with " + list.size() + " tasks");
                                taskAdapter.updateTasks(new ArrayList<>(list));
                            }
                        }
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "loadTasks: Error processing task document " + doc.getId(), ex);
                    synchronized (list) {
                        processedTasks[0]++;
                        if (processedTasks[0] == totalTasks) {
                            Log.d(TAG, "loadTasks: All tasks processed, updating adapter with " + list.size() + " tasks");
                            taskAdapter.updateTasks(new ArrayList<>(list));
                        }
                    }
                }
            }
        });
    }

    private void onTaskClicked(Task task) {
        Log.d(TAG, "onTaskClicked: Task clicked: " + task.getId() + " with status: " + task.getStatus());

        // Check if task is open and map is ready
        if ("Open".equalsIgnoreCase(task.getStatus()) || "Completed".equalsIgnoreCase(task.getStatus())&& mMap != null) {
            // Get location coordinates and animate to them
            String locationId = task.getLocationId();
            if (locationId != null && !locationId.isEmpty()) {
                db.collection("locations").document(locationId).get()
                        .addOnSuccessListener(locationDoc -> {
                            if (locationDoc.exists()) {
                                Double latObj = locationDoc.getDouble("lat");
                                Double lngObj = locationDoc.getDouble("lng");

                                if (latObj != null && lngObj != null) {
                                    LatLng targetLocation = new LatLng(latObj, lngObj);

                                    // Animate camera to location with smooth pan
                                    mMap.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(targetLocation, 16f),
                                            1000, // Animation duration in milliseconds
                                            new GoogleMap.CancelableCallback() {
                                                @Override
                                                public void onFinish() {
                                                    Log.d(TAG, "onTaskClicked: Camera animation completed");
                                                    // Wait 1 second after animation completes, then open form
                                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                        openInspectionForm(task);
                                                    }, 1000);
                                                }

                                                @Override
                                                public void onCancel() {
                                                    Log.d(TAG, "onTaskClicked: Camera animation cancelled");
                                                    // Still open form even if animation is cancelled
                                                    openInspectionForm(task);
                                                }
                                            }
                                    );
                                } else {
                                    Log.w(TAG, "onTaskClicked: Location coordinates not found, opening form directly");
                                    openInspectionForm(task);
                                }
                            } else {
                                Log.w(TAG, "onTaskClicked: Location document not found, opening form directly");
                                openInspectionForm(task);
                            }
                        })
                        .addOnFailureListener(ex -> {
                            Log.e(TAG, "onTaskClicked: Failed to fetch location coordinates", ex);
                            openInspectionForm(task);
                        });
            } else {
                Log.w(TAG, "onTaskClicked: No location ID found, opening form directly");
                openInspectionForm(task);
            }
        } else {
            // For non-open tasks or when map is not ready, open form directly
            Log.d(TAG, "onTaskClicked: Task is not open or map not ready, opening form directly");
            openInspectionForm(task);
        }
    }

    private void openInspectionForm(Task task) {
        Log.d(TAG, "openInspectionForm: Opening form for task: " + task.getId());
        Intent i = new Intent(this, InspectionFormActivity.class);
        i.putExtra("taskId", task.getId());
        i.putExtra("locationId", task.getLocationId());
        startActivity(i);
    }
}