package com.example.chatapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivitySignUpBinding;
import com.example.chatapp.models.User;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding.status.setText("0");
        setListeners();
    }

    private void setListeners(){
        binding.inputPassword.setOnClickListener(v ->
                binding.togglePasswordVisibilityButton.setVisibility(View.VISIBLE));
        binding.inputConfirmPassword.setOnClickListener(v ->
                binding.toggleConfirmPasswordVisibilityButton.setVisibility(View.VISIBLE));
        binding.togglePasswordVisibilityButton.setOnClickListener(v -> togglePasswordVisibility());
        binding.toggleConfirmPasswordVisibilityButton.setOnClickListener(v -> toggleConfirmPasswordVisibility());
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        binding.buttonSignUp.setOnClickListener(v -> {
            checkForSameUsername();
            if (isValidSignUpDetails()) {
                if (binding.status.getText().toString() == "0") {
                    signUp();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Username already exists!", Toast.LENGTH_SHORT).show();
                    binding.inputName.setText("");
                    binding.status.setText("0");
                }
            }
        });

    }

    private void togglePasswordVisibility() {
        if (binding.inputPassword.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
            binding.inputPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            binding.togglePasswordVisibilityButton.setBackgroundDrawable(getDrawable(R.drawable.ic_hide));
        }
        else {
            binding.inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            binding.togglePasswordVisibilityButton.setBackgroundDrawable(getDrawable(R.drawable.ic_show));
        }
    }

    private void toggleConfirmPasswordVisibility() {
        if (binding.inputConfirmPassword.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
            binding.inputConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            binding.toggleConfirmPasswordVisibilityButton.setBackgroundDrawable(getDrawable(R.drawable.ic_hide));
        }
        else {
            binding.inputConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            binding.toggleConfirmPasswordVisibilityButton.setBackgroundDrawable(getDrawable(R.drawable.ic_show));
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.inputName.getText().toString());
        user.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
        user.put(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString());

        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString());
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }



    private void checkForSameUsername() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    String currentUsername = binding.inputName.getText().toString().trim();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUsername.equals(queryDocumentSnapshot.getString(Constants.KEY_NAME).trim())) {
                                binding.status.setText("1");
                            }
                        }
                    } else {
                        showToast("Connection failed!");
                    }
                });
    }

    private boolean isValidSignUpDetails() {

         if (binding.inputName.getText().toString().trim().isEmpty()) {
            showToast("Enter name");
            return false;
        }
        else if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter valid email address");
            return false;
        }
        else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        }
        else if (binding.inputConfirmPassword.getText().toString().trim().isEmpty()) {
            showToast("Confirm your password");
            return false;
        }
        else if (!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())) {
            showToast("Password & confirm password must be same");
            return false;
        }
        else if (!(validatePassword(binding.inputPassword.getText().toString().trim()))) {
            showToast("Choose a stronger password!");
            showToast("The password must contain lower & upper case letters, digits and special characters");
            return false;
        }
        else
            return true;
    }

    private boolean validatePassword(String password) {
        Pattern pattern;
        Matcher matcher;
        String passwordPattern = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[@#$%^&*]).{8,}";
        pattern = Pattern.compile(passwordPattern);
        matcher = pattern.matcher(password);
        return matcher.matches();
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignUp.setVisibility(View.VISIBLE);
        }
    }
}