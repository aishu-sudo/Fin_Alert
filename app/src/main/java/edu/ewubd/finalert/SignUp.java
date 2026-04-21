package edu.ewubd.finalert;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity {

    private boolean passVisible    = false;
    private boolean confirmVisible = false;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        EditText  etFullName        = findViewById(R.id.etFullName);
        EditText  etEmail           = findViewById(R.id.etEmail);
        EditText  etPhone           = findViewById(R.id.etPhone);
        EditText  etDob             = findViewById(R.id.etDob);
        EditText  etPassword        = findViewById(R.id.etPassword);
        EditText  etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ImageView ivTogglePassword  = findViewById(R.id.ivTogglePassword);
        ImageView ivToggleConfirm   = findViewById(R.id.ivToggleConfirm);
        Button    btnSignUp         = findViewById(R.id.btnLogin);
        TextView  tvLoginLink       = findViewById(R.id.tvLoginLink);

        // DOB DatePicker
        etDob.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            int year  = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day   = cal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog picker = new DatePickerDialog(SignUp.this,
                    (view, y, m, d) -> {
                        String date = String.format("%02d / %02d / %04d", d, m + 1, y);
                        etDob.setText(date);
                    }, year, month, day);

            // Max date = today (can't select future date)
            picker.getDatePicker().setMaxDate(System.currentTimeMillis());
            picker.show();
        });

        ivTogglePassword.setOnClickListener(v -> {
            passVisible = !passVisible;
            etPassword.setTransformationMethod(passVisible
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            ivTogglePassword.setAlpha(passVisible ? 1f : 0.5f);
            etPassword.setSelection(etPassword.getText().length());
        });

        ivToggleConfirm.setOnClickListener(v -> {
            confirmVisible = !confirmVisible;
            etConfirmPassword.setTransformationMethod(confirmVisible
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            ivToggleConfirm.setAlpha(confirmVisible ? 1f : 0.5f);
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });

        btnSignUp.setOnClickListener(v -> {
            String name     = etFullName.getText().toString().trim();
            String email    = etEmail.getText().toString().trim();
            String phone    = etPhone.getText().toString().trim();
            String dob      = etDob.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirm  = etConfirmPassword.getText().toString().trim();

            // ── Validation ──────────────────────────────────────
            if (name.isEmpty()) {
                etFullName.setError("Enter your full name"); return;
            }

            // Email
            if (email.isEmpty()) {
                etEmail.setError("Enter your email"); return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Invalid email (e.g. name@example.com)"); return;
            }

            // BD Phone: 01XXXXXXXXX (11 digits) or +8801XXXXXXXXX
            String phoneDigits = phone.replaceAll("[^0-9]", "");
            boolean validBDPhone = phone.matches("^01[3-9]\\d{8}$") ||
                                   phone.matches("^\\+8801[3-9]\\d{8}$");
            if (phone.isEmpty()) {
                etPhone.setError("Enter your mobile number"); return;
            }
            if (!validBDPhone) {
                etPhone.setError("Enter valid BD number (e.g. 01XXXXXXXXX)"); return;
            }

            if (dob.isEmpty()) {
                etDob.setError("Enter your date of birth"); return;
            }

            // Password conditions
            if (password.length() < 8) {
                etPassword.setError("Min 8 characters"); return;
            }
            if (!password.matches(".*[A-Z].*")) {
                etPassword.setError("Must contain at least 1 uppercase letter"); return;
            }
            if (!password.matches(".*[0-9].*")) {
                etPassword.setError("Must contain at least 1 number"); return;
            }
            if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?].*")) {
                etPassword.setError("Must contain at least 1 special character (!@#$ etc.)"); return;
            }
            if (!password.equals(confirm)) {
                etConfirmPassword.setError("Passwords do not match"); return;
            }
            // ────────────────────────────────────────────────────

            btnSignUp.setEnabled(false);
            btnSignUp.setText("Creating account…");

            // Show progress dialog
            ProgressDialog progress = new ProgressDialog(this);
            progress.setMessage("Creating your account...");
            progress.setCancelable(false);
            progress.show();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            Map<String, Object> user = new HashMap<>();
                            user.put("name",  name);
                            user.put("email", email);
                            user.put("phone", phone);
                            user.put("dob",   dob);

                            // Save to Firestore in background — don't wait for it
                            db.collection("users").document(uid).set(user);

                            // Navigate immediately
                            progress.dismiss();
                            Toast.makeText(this, "Account created! Please log in.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SignUp.this, Log_in.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();

                        } else {
                            progress.dismiss();
                            btnSignUp.setEnabled(true);
                            btnSignUp.setText("Sign Up");
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                etEmail.setError("This email is already registered. Please log in.");
                                etEmail.requestFocus();
                            } else {
                                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignUp.this, Log_in.class));
            finish();
        });
    }
}
