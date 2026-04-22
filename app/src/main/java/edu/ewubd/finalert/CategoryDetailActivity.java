package edu.ewubd.finalert;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.widget.LinearLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class CategoryDetailActivity extends AppCompatActivity {

    public static final int REQ_ADD = 1001;

    private String name, label;
    private double budget;
    private ListenerRegistration listener;
    private final List<Map<String, String>> expenses = new ArrayList<>();
    private ExpenseAdapter adapter;
    private android.widget.TextView tvTotalBalance, tvTotalExpense, tvProgressPercent, tvBudget, tvStatus;
    private View progressFill, progressEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);

        name = getIntent().getStringExtra("name");
        label = getIntent().getStringExtra("label");
        budget = getIntent().getDoubleExtra("budget", 0);

        if (name != null) name = name.toLowerCase();

        TextView tvTitle = findViewById(R.id.tvCategoryTitle);
        // show label if available, otherwise fallback to capitalized name
        if (label != null && !label.isEmpty()) {
            tvTitle.setText(label);
        } else if (name != null && !name.isEmpty()) {
            // capitalize first letter
            tvTitle.setText(name.substring(0,1).toUpperCase() + name.substring(1));
        }

        // back button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // set month title (e.g., "April") if tvMonth exists in layout
        TextView tvMonth = findViewById(R.id.tvMonth);
        if (tvMonth != null) {
            String month = new SimpleDateFormat("MMMM", Locale.getDefault()).format(new Date());
            tvMonth.setText(month);
        }

        // find header views for updating totals
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        tvBudget = findViewById(R.id.tvBudget);
        tvStatus = findViewById(R.id.tvStatus);
        progressFill = findViewById(R.id.progressFill);
        progressEmpty = findViewById(R.id.progressEmpty);

        if (tvBudget != null) tvBudget.setText(String.format("৳%,.0f", budget));

        RecyclerView rv = findViewById(R.id.rvExpenses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpenseAdapter(expenses);
        rv.setAdapter(adapter);

        Button btnAdd = findViewById(R.id.btnAddExpense);
        btnAdd.setOnClickListener(v -> {
            Intent i = new Intent(this, AddExpenseActivity.class);
            i.putExtra("category", label);
            startActivityForResult(i, REQ_ADD);
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
                .addSnapshotListener((snapshots, err) -> {
                    if (err != null || snapshots == null) return;

                    expenses.clear();
                    double spent = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String rawCat    = doc.getString("category");
                        String rawCatKey = doc.getString("category_key");
                        String rawAmt    = doc.getString("amount");
                        if (rawAmt == null) continue;
                        try {
                            Double amt = parseAmount(rawAmt);
                            if (amt == null) continue;
                            boolean match = false;
                            if (rawCatKey != null && name != null && rawCatKey.equalsIgnoreCase(name)) match = true;
                            if (!match && rawCat != null && name != null) {
                                String lower = rawCat.toLowerCase();
                                if (mapCategory(lower).equals(name) || lower.contains(name)) match = true;
                            }
                            if (match) {
                                Map<String, String> e = new HashMap<>();
                                String title = doc.getString("title");
                                String date  = doc.getString("date");
                                e.put("title",  title == null ? "Item" : title);
                                e.put("sub",    date  == null ? ""     : date);
                                e.put("amount", String.format("-৳%,.2f", amt));
                                expenses.add(e);
                                spent += amt;
                            }
                        } catch (Exception ignored) {}
                    }

                    adapter.notifyDataSetChanged();

                    if (tvTotalExpense  != null) tvTotalExpense.setText(String.format("-৳%,.2f", spent));
                    if (tvTotalBalance  != null) tvTotalBalance.setText(String.format("৳%,.2f", Math.max(0, budget - spent)));

                    int pct = budget > 0 ? (int) Math.min(100, (spent / budget) * 100) : 0;
                    if (tvProgressPercent != null) tvProgressPercent.setText(pct + "%");

                    if (progressFill != null && progressEmpty != null) {
                        LinearLayout.LayoutParams fp0 = (LinearLayout.LayoutParams) progressFill.getLayoutParams();
                        ValueAnimator anim = ValueAnimator.ofFloat(fp0.weight, pct / 100f);
                        anim.setDuration(400);
                        anim.addUpdateListener(animation -> {
                            float v = (float) animation.getAnimatedValue();
                            LinearLayout.LayoutParams fp = (LinearLayout.LayoutParams) progressFill.getLayoutParams();
                            fp.weight = v; progressFill.setLayoutParams(fp);
                            LinearLayout.LayoutParams ep = (LinearLayout.LayoutParams) progressEmpty.getLayoutParams();
                            ep.weight = 1f - v; progressEmpty.setLayoutParams(ep);
                        });
                        anim.start();
                    }

                    if (tvStatus != null) {
                        if (pct <= 50) {
                            tvStatus.setText(pct + "% Of Your Expenses, Looks Good.");
                        } else if (pct <= 80) {
                            tvStatus.setText(pct + "% Of Your Expenses, Be Careful.");
                        } else {
                            tvStatus.setText(pct + "% Of Your Expenses, Overspending!");
                        }
                    }
                });
    }

    private String mapCategory(String lower) {
        if (lower.contains("food") || lower.contains("dining") || lower.contains("restaurant")) return "food";
        if (lower.contains("transport") || lower.contains("bus") || lower.contains("rickshaw")) return "transport";
        if (lower.contains("grocery") || lower.contains("bazar")) return "grocery";
        if (lower.contains("shopping") || lower.contains("cloth")) return "shopping";
        if (lower.contains("saving") || lower.contains("savings")) return "savings";
        if (lower.contains("rent") || lower.contains("house") || lower.contains("home")) return "rent";
        if (lower.contains("gift") || lower.contains("present")) return "gift";
        if (lower.contains("health") || lower.contains("medicine") || lower.contains("doctor")) return "health";
        if (lower.contains("education") || lower.contains("school") || lower.contains("tuition") || lower.contains("book")) return "education";
        return "other";
    }

    /**
     * Parse an amount string coming from Firestore which may include currency symbols,
     * thousand separators and either '.' or ',' as decimal separator. Returns null if
     * parsing fails.
     */
    private Double parseAmount(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        // common cases: "$7,783.00", "-26,00", "26.35", "-18.35"
        // If both comma and dot present, assume comma is thousand separator -> remove commas
        if (s.contains(",") && s.contains(".")) {
            s = s.replace(",", "");
        } else if (s.contains(",") && !s.contains(".")) {
            // assume comma is decimal separator -> replace with dot
            s = s.replace(',', '.');
        }
        // remove any currency symbols or spaces except digits, dot, minus
        s = s.replaceAll("[^0-9.\\-]", "");
        if (s.isEmpty() || s.equals("-") ) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // listener auto-updates via snapshot, nothing extra needed
    }

    static class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.VH> {
        private final List<Map<String,String>> data;
        ExpenseAdapter(List<Map<String,String>> d){data=d;}
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense,parent,false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position){
            Map<String,String> m = data.get(position);
            holder.tvTitle.setText(m.getOrDefault("title",""));
            holder.tvSub.setText(m.getOrDefault("sub",""));
            holder.tvAmount.setText(m.getOrDefault("amount",""));
        }
        @Override public int getItemCount(){return data.size();}
        static class VH extends RecyclerView.ViewHolder{
            TextView tvTitle,tvSub,tvAmount;
            VH(View v){super(v);tvTitle=v.findViewById(R.id.tvExpTitle);tvSub=v.findViewById(R.id.tvExpSub);tvAmount=v.findViewById(R.id.tvExpAmount);}        }
    }
}

