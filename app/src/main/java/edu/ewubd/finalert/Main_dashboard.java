package edu.ewubd.finalert;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Main_dashboard extends AppCompatActivity {

    private TextView chipDaily, chipWeekly, chipMonthly;
    private LinearLayout btnReceiptScan, btnVoiceInput;
    private ImageView navHome, navAnalytics, navTransfer, navLayers, navProfile, notificationIcon;
    private LinearLayout btnCreateGoal;

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
            Intent intent = new Intent(Main_dashboard.this, ReceiptScanActivity.class);
            startActivityForResult(intent, 101);
        });

        btnVoiceInput.setOnClickListener(v -> {
            Intent intent = new Intent(Main_dashboard.this, VoiceInputActivity.class);
            startActivityForResult(intent, 102);
        });
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
        navAnalytics.setOnClickListener(v -> { setNavSelected(navAnalytics); Toast.makeText(this, "Analytics", Toast.LENGTH_SHORT).show(); });
        navTransfer.setOnClickListener(v -> { setNavSelected(navTransfer); Toast.makeText(this, "Transfer", Toast.LENGTH_SHORT).show(); });
        navLayers.setOnClickListener(v -> {
            setNavSelected(navLayers);
            startActivity(new Intent(Main_dashboard.this, CategoriesActivity.class));
        });
        navProfile.setOnClickListener(v -> { setNavSelected(navProfile); Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show(); });
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

        EditText etAmount = dialog.findViewById(R.id.etAmount);
        EditText etCategory = dialog.findViewById(R.id.etCategory);
        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        if (!amount.isEmpty()) etAmount.setText(amount);
        if (!category.isEmpty()) etCategory.setText(category);

        btnSave.setOnClickListener(v -> {
            String amt = etAmount.getText().toString().trim();
            String cat = etCategory.getText().toString().trim();
            if (amt.isEmpty()) { etAmount.setError("Enter amount"); return; }
            if (cat.isEmpty()) { etCategory.setError("Enter category"); return; }
            Toast.makeText(this, "Expense saved: " + cat + " $" + amt, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String amount = data.getStringExtra("amount");
            String category = data.getStringExtra("category");
            if (amount == null) amount = "";
            if (category == null) category = "";
            showAddExpenseDialog(amount, category);
        }
    }
}

