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

public class InspectionFormActivity extends AppCompatActivity {

    private RadioGroup cleanlinessGroup, foodStorageGroup, pestControlGroup;
    private EditText commentsInput;
    private Button submitButton;

    private int getSelectedRating(RadioGroup group) {
        int selectedId = group.getCheckedRadioButtonId();
        if (selectedId == -1) return -1;
        return Integer.parseInt(((RadioButton) findViewById(selectedId)).getText().toString());
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

        cleanlinessGroup = findViewById(R.id.cleanlinessGroup);
        foodStorageGroup = findViewById(R.id.foodStorageGroup);
        pestControlGroup = findViewById(R.id.pestControlGroup);
        commentsInput = findViewById(R.id.commentsInput);
        submitButton = findViewById(R.id.submitButton);

        submitButton.setOnClickListener(v -> {
            int c = getSelectedRating(cleanlinessGroup);
            int f = getSelectedRating(foodStorageGroup);
            int p = getSelectedRating(pestControlGroup);

            if (c == -1 || f == -1 || p == -1) {
                Toast.makeText(this, "Please rate all categories.", Toast.LENGTH_SHORT).show();
                return;
            }

            double avg = (c + f + p) / 3.0;
            String grade = calculateGrade(avg);

            Toast.makeText(this, "Inspection Complete. Grade: " + grade, Toast.LENGTH_LONG).show();

            // Here you can pass back results or save them as needed
            finish();
        });
    }
}
