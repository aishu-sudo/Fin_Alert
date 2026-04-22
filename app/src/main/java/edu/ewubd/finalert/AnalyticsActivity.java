package edu.ewubd.finalert;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;

public class AnalyticsActivity extends AppCompatActivity {

    private LinearLayout categoryContainer;
    private TextView tvTotalSpent, tvTransactionCount;
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        categoryContainer  = findViewById(R.id.categoryContainer);
        tvTotalSpent       = findViewById(R.id.tvTotalSpent);
        tvTransactionCount = findViewById(R.id.tvTransactionCount);

        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    private void startListening() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        // Remove any existing listener first
        if (listener != null) listener.remove();

        listener = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("expenses")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    Map<String, Double> categorySpent = new LinkedHashMap<>();
                    double total = 0;
                    int count = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String rawAmt = doc.getString("amount");
                        String cat    = doc.getString("category_key");
                        if (rawAmt == null) continue;
                        try {
                            double amt = Double.parseDouble(rawAmt.replace(",", ""));
                            if (cat == null || cat.isEmpty()) cat = "other";
                            categorySpent.merge(cat, amt, Double::sum);
                            total += amt;
                            count++;
                        } catch (NumberFormatException ignored) {}
                    }

                    tvTotalSpent.setText(String.format("৳ %.2f", total));
                    tvTransactionCount.setText(count + " transactions");

                    categoryContainer.removeAllViews();
                    for (Map.Entry<String, Double> entry : categorySpent.entrySet()) {
                        addCategoryRow(entry.getKey(), entry.getValue(), total);
                    }
                });
    }

    private void addCategoryRow(String category, double spent, double total) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.transaction_bg);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dpToPx(12));
        row.setLayoutParams(rowParams);
        row.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvName.setText(capitalize(category));
        tvName.setTextColor(Color.parseColor("#1A1A1A"));
        tvName.setTextSize(15);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvAmt = new TextView(this);
        tvAmt.setText(String.format("৳ %.2f", spent));
        tvAmt.setTextColor(Color.parseColor("#F44336"));
        tvAmt.setTextSize(15);
        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);

        topRow.addView(tvName);
        topRow.addView(tvAmt);

        int pct = total > 0 ? (int) Math.min(100, (spent / total) * 100) : 0;
        LinearLayout progressBg = new LinearLayout(this);
        LinearLayout.LayoutParams pgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(6));
        pgParams.setMargins(0, dpToPx(8), 0, dpToPx(4));
        progressBg.setLayoutParams(pgParams);
        progressBg.setBackgroundColor(Color.parseColor("#E0E0E0"));

        View fill = new View(this);
        fill.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, pct));
        fill.setBackgroundColor(Color.parseColor("#33D49C"));
        progressBg.addView(fill);

        View empty = new View(this);
        empty.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 100 - pct));
        progressBg.addView(empty);

        TextView tvPct = new TextView(this);
        tvPct.setText(pct + "% of total");
        tvPct.setTextColor(Color.parseColor("#999999"));
        tvPct.setTextSize(11);

        row.addView(topRow);
        row.addView(progressBg);
        row.addView(tvPct);
        categoryContainer.addView(row);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "Other";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
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
        navAnalytics.setColorFilter(teal);
        navTransfer.setColorFilter(grey);
        navLayers.setColorFilter(grey);
        navProfile.setColorFilter(grey);

        navHome.setOnClickListener(v -> { startActivity(new Intent(this, Main_dashboard.class)); finish(); });
        navAnalytics.setOnClickListener(v -> {});
        navTransfer.setOnClickListener(v -> { startActivity(new Intent(this, AddExpenseActivity.class)); });
        navLayers.setOnClickListener(v -> { startActivity(new Intent(this, CategoriesActivity.class)); finish(); });
        navProfile.setOnClickListener(v -> { startActivity(new Intent(this, ProfileActivity.class)); finish(); });
    }
}
