package edu.ewubd.finalert;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Log_in extends AppCompatActivity {

    private boolean passwordVisible = false;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        mAuth = FirebaseAuth.getInstance();

        // If user already logged in, skip straight to dashboard
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToDashboard();
            return;
        }

        EditText  etEmail    = findViewById(R.id.etEmail);
        EditText  etPassword = findViewById(R.id.etPassword);
        Button    btnLogin   = findViewById(R.id.btnLogin);
        TextView  tvFooter   = findViewById(R.id.tvFooter);

        // Password eye toggle
        ImageView eyeIcon = (ImageView) ((android.view.ViewGroup)
                etPassword.getParent()).getChildAt(1);
        if (eyeIcon != null) {
            eyeIcon.setOnClickListener(v -> {
                passwordVisible = !passwordVisible;
                etPassword.setTransformationMethod(passwordVisible
                        ? HideReturnsTransformationMethod.getInstance()
                        : PasswordTransformationMethod.getInstance());
                eyeIcon.setAlpha(passwordVisible ? 1f : 0.5f);
                etPassword.setSelection(etPassword.getText().length());
            });
        }

        btnLogin.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) { etEmail.setError("Enter your email"); return; }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email"); return;
            }
            if (password.isEmpty()) { etPassword.setError("Enter your password"); return; }
            if (password.length() < 6) { etPassword.setError("Min 6 characters"); return; }

            btnLogin.setEnabled(false);
            btnLogin.setText("Logging in…");

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            goToDashboard();
                        } else {
                            btnLogin.setEnabled(true);
                            btnLogin.setText("Log In");
                            Toast.makeText(this, "Login failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Only "Sign Up" word is clickable in the footer
        if (tvFooter != null) {
            String full = "Don't have an account? Sign Up";
            SpannableString span = new SpannableString(full);
            int start = full.indexOf("Sign Up");
            int end   = start + "Sign Up".length();

            span.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    startActivity(new Intent(Log_in.this, SignUp.class));
                }
                @Override
                public void updateDrawState(android.text.TextPaint ds) {
                    ds.setColor(0xFF33D49C);
                    ds.setUnderlineText(true);
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            span.setSpan(new ForegroundColorSpan(0xFF6B7280), 0, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            tvFooter.setText(span);
            tvFooter.setMovementMethod(LinkMovementMethod.getInstance());
            tvFooter.setHighlightColor(android.graphics.Color.TRANSPARENT);
        }
    }

    private void goToDashboard() {
        Intent intent = new Intent(Log_in.this, Main_dashboard.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
