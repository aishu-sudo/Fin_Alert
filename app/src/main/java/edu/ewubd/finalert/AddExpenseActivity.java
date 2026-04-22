package edu.ewubd.finalert;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddExpenseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        String category = getIntent().getStringExtra("category");

        EditText etCategory = findViewById(R.id.etCategory);
        EditText etAmount   = findViewById(R.id.etAmount);
        EditText etTitle    = findViewById(R.id.etTitle);
        EditText etMessage  = findViewById(R.id.etMessage);
        TextView etDate     = findViewById(R.id.etDate);
        Button   btnCancel  = findViewById(R.id.btnCancel);
        Button   btnSave    = findViewById(R.id.btnSave);

        if (category != null) etCategory.setText(category);

        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                etDate.setText(String.format("%02d/%02d/%04d", d, m + 1, y));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Don't auto-open keyboard — it covers the date field at the top

        btnCancel.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        btnSave.setOnClickListener(v -> {
            String title  = etTitle.getText().toString().trim();
            String amount = etAmount.getText().toString().trim();
            String date   = etDate.getText().toString().trim();
            String cat    = etCategory.getText().toString().trim();
            String msg    = etMessage.getText().toString().trim();

            if (title.isEmpty())  { etTitle.setError("Enter expense title"); return; }
            if (amount.isEmpty()) { etAmount.setError("Enter amount"); return; }
            if (date.isEmpty())   { Toast.makeText(this, "Select a date", Toast.LENGTH_SHORT).show(); return; }
            if (cat.isEmpty())    { etCategory.setError("Enter category"); return; }

            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            if (uid == null) {
                Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("title",        title);
            data.put("amount",       amount);
            data.put("category",     cat);
            data.put("category_key", mapCategoryKey(cat));
            data.put("date",         date);
            data.put("message",      msg);
            data.put("timestamp",    FieldValue.serverTimestamp());

            // Save to local cache instantly — server sync in background
            FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("expenses")
                    .add(data);

            Intent out = new Intent();
            out.putExtra("title",    title);
            out.putExtra("amount",   amount);
            out.putExtra("date",     date);
            out.putExtra("category", cat);
            setResult(Activity.RESULT_OK, out);
            finish();
        });
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
}
