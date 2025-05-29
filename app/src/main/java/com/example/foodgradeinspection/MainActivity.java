package com.example.foodgradeinspection;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView welcomeText;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        welcomeText = findViewById(R.id.welcomeText);
        logoutButton = findViewById(R.id.logoutButton);

        // Set up logout button click listener
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            navigateToLogin();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null) {
            // User is not signed in, navigate back to login
            navigateToLogin();
        } else {
            // Update UI with user info
            welcomeText.setText("Welcome, " + currentUser.getEmail());
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}