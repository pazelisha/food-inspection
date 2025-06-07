package com.example.foodgradeinspection;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodgradeinspection.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Set up login button click listener
        binding.loginButton.setOnClickListener(v -> loginUser());

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            // User is already signed in, navigate to main activity
            navigateToMainActivity();
        }
    }

    private void loginUser() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

        // Basic validation
        if (email.isEmpty()) {
            binding.emailInput.setError("Email is required");
            binding.emailInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.passwordInput.setError("Password is required");
            binding.passwordInput.requestFocus();
            return;
        }

        // Show progress bar
        binding.progressBar.setVisibility(View.VISIBLE);

        // Sign in with email and password
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Sign in success
                        Toast.makeText(LoginActivity.this, "Login successful",
                                Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        // If sign in fails, display a message to the user
                        Toast.makeText(LoginActivity.this, "Authentication failed: " +
                                        task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}