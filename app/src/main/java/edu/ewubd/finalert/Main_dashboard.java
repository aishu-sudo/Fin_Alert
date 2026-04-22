package edu.ewubd.finalert;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Main_dashboard extends AppCompatActivity {

    private static final double TOTAL_BUDGET = 20000.0;

    private TextView chipDaily, chipWeekly, chipMonthly;
    private LinearLayout btnReceiptScan, btnVoiceInput;
    private ImageView navHome, navAnalytics, navTransfer, navLayers, navProfile, notificationIcon;
    private LinearLayout btnCreateGoal;
    private TextView tvTotalBalance, tvTotalExpense, tvTransactionCount;
    private LinearLayout transactionsContainer;
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        chipDaily = findViewById(R.id.chipDaily);
        chipWeekly = findViewById(R.id.chipWeekly);
        chipMonthly = findViewById(R.id.chipMonthly);
        setupFilterChips();

        navHome = findViewById(R.id.navHome);
        navAnalytics = findViewById(R.id.navAnalytics);
        navTransfer = findViewById(R.id.navTransfer);
        navLayers = findViewById(R.id.navLayers);
        navProfile = findViewById(R.id.navProfile);
        setupBottomNav();

        tvTotalBalance = findViewById(R.id.totalBalanceAmount);
        tvTotalExpense = findViewById(R.id.totalExpenseAmount);
        tvTransactionCount = findViewById(R.id.tvTransactionCount);
        transactionsContainer = findViewById(R.id.transactionsContainer);

        notificationIcon = findViewById(R.id.notificationIcon);
        notificationIcon.setOnClickListener(v ->
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show());

        btnCreateGoal = findViewById(R.id.btnCreateGoal);
        btnCreateGoal.setOnClickListener(v ->
                Toast.makeText(this, "Create New Goal", Toast.LENGTH_SHORT).show());

        btnReceiptScan = findViewById(R.id.btnReceiptScan);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);

        EditText etManualExpense = findViewById(R.id.etManualExpense);
        etManualExpense.setOnClickListener(v -> showAddExpenseDialog("", ""));
        etManualExpense.setOnEditorActionListener((v, actionId, event) -> {
            String text = etManualExpense.getText().toString().trim();
            if (!text.isEmpty()) {
                showAddExpenseDialog("", text);
                etManualExpense.setText("");
            }
            return true;
        });

        btnReceiptScan.setOnClickListener(v -> {
            startActivityForResult(new Intent(Main_dashboard.this, ReceiptScanActivity.class), 101);
        });
        btnVoiceInput.setOnClickListener(v -> {
            startActivityForResult(new Intent(Main_dashboard.this, VoiceInputActivity.class), 102);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (listener != null) { listener.remove(); listener = null; }
    }

    private void startListening() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        if (listener != null) listener.remove();

        listener = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("expenses")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    double totalExpense = 0;
                    int count = 0;

                    transactionsContainer.removeAllViews();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String rawAmt = doc.getString("amount");
                        if (rawAmt == null) continue;
                        try {
                            double amt = Double.parseDouble(rawAmt.replace(",", ""));
                            totalExpense += amt;
                            count++;

                            String title = doc.getString("title");
                            String date  = doc.getString("date");
                            String cat   = doc.getString("category");
                            if (title == null || title.isEmpty()) title = cat;
                            if (title == null || title.isEmpty()) title = "Expense";
                            addTransactionRow(title, date != null ? date : "", amt);
                        } catch (NumberFormatException ignored) {}
                    }

                    double balance = TOTAL_BUDGET - totalExpense;
                    tvTotalBalance.setText(String.format("৳%,.2f", balance));
                    tvTotalExpense.setText(String.format("-৳%,.2f", totalExpense));
                    tvTransactionCount.setText(count + " total");
                });
    }

    private void addTransactionRow(String title, String date, double amount) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundResource(R.drawable.transaction_bg);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, dpToPx(10));
        row.setLayoutParams(rp);
        row.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Icon circle
        LinearLayout iconCircle = new LinearLayout(this);
        iconCircle.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(44), dpToPx(44)));
        iconCircle.setBackgroundResource(R.drawable.circle_gray_bg);
        iconCircle.setGravity(android.view.Gravity.CENTER);
        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(22), dpToPx(22)));
        icon.setImageResource(R.drawable.ic_expense);
        icon.setColorFilter(Color.parseColor("#33D49C"));
        iconCircle.addView(icon);

        // Text column
        LinearLayout textCol = new LinearLayout(this);
        LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tcp.setMarginStart(dpToPx(14));
        textCol.setLayoutParams(tcp);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor("#111111"));
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, Typeface.BOLD);

        TextView tvDate = new TextView(this);
        tvDate.setText(date);
        tvDate.setTextColor(Color.parseColor("#999999"));
        tvDate.setTextSize(11);
        tvDate.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        textCol.addView(tvTitle);
        textCol.addView(tvDate);

        // Amount
        TextView tvAmt = new TextView(this);
        tvAmt.setText(String.format("-৳%,.2f", amount));
        tvAmt.setTextColor(Color.parseColor("#F44336"));
        tvAmt.setTextSize(14);
        tvAmt.setTypeface(null, Typeface.BOLD);

        row.addView(iconCircle);
        row.addView(textCol);
        row.addView(tvAmt);

        transactionsContainer.addView(row);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void setupBottomNav() {
        int grey = Color.parseColor("#666666");
        int teal = Color.parseColor("#33D49C");

        navHome.setColorFilter(teal);
        navAnalytics.setColorFilter(grey);
        navTransfer.setColorFilter(grey);
        navLayers.setColorFilter(grey);
        navProfile.setColorFilter(grey);

        navHome.setOnClickListener(v -> setNavSelected(navHome));
        navAnalytics.setOnClickListener(v -> startActivity(new Intent(Main_dashboard.this, AnalyticsActivity.class)));
        navTransfer.setOnClickListener(v -> startActivity(new Intent(Main_dashboard.this, AddExpenseActivity.class)));
        navLayers.setOnClickListener(v -> startActivity(new Intent(Main_dashboard.this, CategoriesActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(Main_dashboard.this, ProfileActivity.class)));
    }

    private void setNavSelected(ImageView selected) {
        int grey = Color.parseColor("#666666");
        int teal = Color.parseColor("#33D49C");
        navHome.setColorFilter(grey);
        navAnalytics.setColorFilter(grey);
        navTransfer.setColorFilter(grey);
        navLayers.setColorFilter(grey);
        navProfile.setColorFilter(grey);
        selected.setColorFilter(teal);
    }

    private void setupFilterChips() {
        View.OnClickListener chipListener = v -> {
            chipDaily.setBackgroundResource(R.drawable.chip_outline_bg);
            chipDaily.setTextColor(Color.parseColor("#1A1A1A"));
            chipWeekly.setBackgroundResource(R.drawable.chip_outline_bg);
            chipWeekly.setTextColor(Color.parseColor("#1A1A1A"));
            chipMonthly.setBackgroundResource(R.drawable.chip_outline_bg);
            chipMonthly.setTextColor(Color.parseColor("#1A1A1A"));
            TextView sel = (TextView) v;
            sel.setBackgroundResource(R.drawable.chip_filled_bg);
            sel.setTextColor(Color.WHITE);
        };
        chipDaily.setOnClickListener(chipListener);
        chipWeekly.setOnClickListener(chipListener);
        chipMonthly.setOnClickListener(chipListener);
    }

    public void showAddExpenseDialog(String amount, String category) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_expense);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );

        EditText etTitle    = dialog.findViewById(R.id.etTitle);
        EditText etAmount   = dialog.findViewById(R.id.etAmount);
        EditText etCategory = dialog.findViewById(R.id.etCategory);
        Button   btnSave    = dialog.findViewById(R.id.btnSave);
        Button   btnCancel  = dialog.findViewById(R.id.btnCancel);

        if (!amount.isEmpty())   etAmount.setText(amount);
        if (!category.isEmpty()) etCategory.setText(category);

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String amt   = etAmount.getText().toString().trim();
            String cat   = etCategory.getText().toString().trim();

            if (title.isEmpty()) { etTitle.setError("Enter expense title"); return; }
            if (amt.isEmpty())   { etAmount.setError("Enter amount"); return; }
            if (cat.isEmpty())   { etCategory.setError("Enter category"); return; }

            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            if (uid == null) {
                Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            Map<String, Object> data = new HashMap<>();
            data.put("title",        title);
            data.put("amount",       amt);
            data.put("category",     cat);
            data.put("category_key", mapCategoryKey(cat));
            data.put("date",         today);
            data.put("timestamp",    FieldValue.serverTimestamp());

            FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("expenses")
                    .add(data);

            Toast.makeText(this, "Saved: " + cat + " — " + amt + " tk", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String mapCategoryKey(String cat) {
        if (cat == null) return "other";
        String lower = cat.toLowerCase();
        if (lower.contains("food") || lower.contains("dining") || lower.contains("restaurant")) return "food";
        if (lower.contains("transport") || lower.contains("bus") || lower.contains("rickshaw")) return "transport";
        if (lower.contains("grocery") || lower.contains("bazar")) return "grocery";
        if (lower.contains("shopping") || lower.contains("cloth")) return "shopping";
        if (lower.contains("saving")) return "savings";
        if (lower.contains("rent") || lower.contains("house") || lower.contains("home")) return "rent";
        if (lower.contains("gift") || lower.contains("present")) return "gift";
        if (lower.contains("health") || lower.contains("medicine") || lower.contains("doctor")) return "health";
        return lower;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String amount   = data.getStringExtra("amount");
            String category = data.getStringExtra("category");
            if (amount == null) amount = "";
            if (category == null) category = "";
            showAddExpenseDialog(amount, category);
        }
    }
}
