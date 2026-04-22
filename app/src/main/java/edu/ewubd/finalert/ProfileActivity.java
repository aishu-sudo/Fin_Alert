package edu.ewubd.finalert;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView tvName   = findViewById(R.id.tvProfileName);
        TextView tvEmail  = findViewById(R.id.tvProfileEmail);
        TextView tvAvatar = findViewById(R.id.tvProfileAvatar);
        Button   btnLogout = findViewById(R.id.btnLogout);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            tvEmail.setText(email != null ? email : "");

            // Load name from Firestore
            FirebaseFirestore.getInstance()
                    .collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            tvName.setText(name);
                            tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                        } else if (email != null) {
                            tvName.setText(email.split("@")[0]);
                            tvAvatar.setText(String.valueOf(email.charAt(0)).toUpperCase());
                        }
                    });
        }

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, Auth.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        setupBottomNav();
    }

    private void setupBottomNav() {
        int grey = Color.parseColor("#666666");
        int teal = Color.parseColor("#33D49C");

        ImageView navHome      = findViewById(R.id.navHome);
        ImageView navAnalytics = findViewById(R.id.navAnalytics);
        ImageView navTransfer  = findViewById(R.id.navTransfer);
        ImageView navLayers    = findViewById(R.id.navLayers);
        ImageView navProfile   = findViewById(R.id.navProfile);

        navHome.setColorFilter(grey);
        navAnalytics.setColorFilter(grey);
        navTransfer.setColorFilter(grey);
        navLayers.setColorFilter(grey);
        navProfile.setColorFilter(teal);

        navHome.setOnClickListener(v -> { startActivity(new Intent(this, Main_dashboard.class)); finish(); });
        navAnalytics.setOnClickListener(v -> { startActivity(new Intent(this, AnalyticsActivity.class)); finish(); });
        navTransfer.setOnClickListener(v -> { startActivity(new Intent(this, AddExpenseActivity.class)); });
        navLayers.setOnClickListener(v -> { startActivity(new Intent(this, CategoriesActivity.class)); finish(); });
        navProfile.setOnClickListener(v -> {});
    }
}
