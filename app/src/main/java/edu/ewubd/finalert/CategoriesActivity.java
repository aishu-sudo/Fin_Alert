package edu.ewubd.finalert;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoriesActivity extends AppCompatActivity {

    private static final double TOTAL_BUDGET = 20000.0;

    // Category model
    static class CategoryItem {
        String name;       // lowercase key used for matching
        String label;      // display label shown in UI
        int    iconRes;
        double budget;
        double spent;
        CategoryItem(String n, String lbl, int i, double b) {
            name=n; label=lbl; iconRes=i; budget=b; spent=0;
        }
        float remaining() { return budget<=0 ? 0f : (float)Math.max(0,(budget-spent)/budget); }
        int percentLeft() { return (int)(remaining()*100); }
    }

    private final List<CategoryItem> items = new ArrayList<>();
    private CategoryAdapter adapter;
    private TextView tvBalance, tvExpense, tvPercent, tvBudget, tvStatus;
    private View progressFill, progressEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        tvBalance     = findViewById(R.id.tvTotalBalance);
        tvExpense     = findViewById(R.id.tvTotalExpense);
        tvPercent     = findViewById(R.id.tvProgressPercent);
        tvBudget      = findViewById(R.id.tvBudget);
        tvStatus      = findViewById(R.id.tvStatus);
        progressFill  = findViewById(R.id.progressFill);
        progressEmpty = findViewById(R.id.progressEmpty);

        tvBudget.setText(String.format("$%,.0f", TOTAL_BUDGET));

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Bottom nav
        setupBottomNav();

        // Setup categories list
        buildCategories();

        // RecyclerView grid
        RecyclerView rv = findViewById(R.id.rvCategories);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new CategoryAdapter(items, this::onAddClicked);
        rv.setAdapter(adapter);

        // Load from Firestore
        loadExpenses();
    }

    private void buildCategories() {
        items.clear();
        items.add(new CategoryItem("food",      "Food",      R.drawable.ic_groceries, 3000));
        items.add(new CategoryItem("transport", "Transport", R.drawable.ic_transfer,  2000));
        items.add(new CategoryItem("grocery",   "Grocery",   R.drawable.ic_wallet,    4000));
        items.add(new CategoryItem("shopping",  "Shopping",  R.drawable.ic_expense,   2000));
        items.add(new CategoryItem("savings",   "Savings",   R.drawable.ic_goal,      5000));
        items.add(new CategoryItem("rent",      "Rent",      R.drawable.ic_home,      8000));
        items.add(new CategoryItem("gift",      "Gift",      R.drawable.ic_salary,    1000));
        items.add(new CategoryItem("health",    "Health",    R.drawable.ic_battery,   2000));
    }

    private void loadExpenses() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("expenses")
                .get()
                .addOnSuccessListener(snapshots -> {
                    // Reset spent
                    for (CategoryItem c : items) c.spent = 0;
                    double totalSpent = 0;

                    // Map category name → spent
                    Map<String, Double> spentMap = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String rawCat = doc.getString("category");
                        String rawAmt = doc.getString("amount");
                        if (rawAmt == null || rawCat == null) continue;
                        try {
                            double amt = Double.parseDouble(rawAmt);
                            totalSpent += amt;
                            // Match to our category items
                            String lower = rawCat.toLowerCase();
                            String mapped = mapCategory(lower);
                            spentMap.merge(mapped, amt, Double::sum);
                        } catch (NumberFormatException ignored) {}
                    }

                    // Update spent in items
                    for (CategoryItem c : items) {
                        if (spentMap.containsKey(c.name)) {
                            c.spent = spentMap.get(c.name);
                        }
                    }

                    // Update UI
                    double balance = TOTAL_BUDGET - totalSpent;
                    int pct = (int)Math.min(100, totalSpent / TOTAL_BUDGET * 100);

                    tvBalance.setText(String.format("$%,.2f", balance));
                    tvExpense.setText(String.format("-$%,.2f", totalSpent));
                    tvPercent.setText(pct + "%");

                    float fill = pct / 100f;
                    LinearLayout.LayoutParams fillParams =
                            (LinearLayout.LayoutParams) progressFill.getLayoutParams();
                    fillParams.weight = fill;
                    progressFill.setLayoutParams(fillParams);
                    LinearLayout.LayoutParams emptyParams =
                            (LinearLayout.LayoutParams) progressEmpty.getLayoutParams();
                    emptyParams.weight = 1f - fill;
                    progressEmpty.setLayoutParams(emptyParams);

                    if (pct <= 50) {
                        tvStatus.setText(pct + "% Of Your Expenses, Looks Good.");
                    } else if (pct <= 80) {
                        tvStatus.setText(pct + "% Of Your Expenses, Be Careful.");
                    } else {
                        tvStatus.setText(pct + "% Of Your Expenses, Overspending!");
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private String mapCategory(String lower) {
        if (lower.contains("food") || lower.contains("dining") || lower.contains("restaurant")) return "food";
        if (lower.contains("transport") || lower.contains("bus") || lower.contains("rickshaw")) return "transport";
        if (lower.contains("grocery") || lower.contains("bazar")) return "grocery";
        if (lower.contains("shopping") || lower.contains("cloth")) return "shopping";
        if (lower.contains("saving") || lower.contains("saving")) return "savings";
        if (lower.contains("rent") || lower.contains("house") || lower.contains("home")) return "rent";
        if (lower.contains("gift") || lower.contains("present")) return "gift";
        if (lower.contains("health") || lower.contains("medicine") || lower.contains("doctor")) return "health";
        return "other";
    }

    private void onAddClicked() {
        // Open Add Expense dialog
        finish(); // go back to dashboard to add
    }

    // ── Bottom Nav ────────────────────────────────────────────────
    private void setupBottomNav() {
        int grey = Color.parseColor("#666666");
        int teal = Color.parseColor("#33D49C");

        ImageView navHome     = findViewById(R.id.navHome);
        ImageView navAnalytics= findViewById(R.id.navAnalytics);
        ImageView navTransfer = findViewById(R.id.navTransfer);
        ImageView navLayers   = findViewById(R.id.navLayers);
        ImageView navProfile  = findViewById(R.id.navProfile);

        navHome.setColorFilter(grey);
        navAnalytics.setColorFilter(grey);
        navTransfer.setColorFilter(grey);
        navLayers.setColorFilter(teal); // active
        navProfile.setColorFilter(grey);

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, Main_dashboard.class));
            finish();
        });
        navAnalytics.setOnClickListener(v -> {});
        navTransfer.setOnClickListener(v -> {});
        navLayers.setOnClickListener(v -> {});
        navProfile.setOnClickListener(v -> {});
    }

    // ── Adapter ───────────────────────────────────────────────────
    static class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_ITEM = 0;
        private static final int TYPE_ADD  = 1;

        private final List<CategoryItem> items;
        private final Runnable onAdd;

        CategoryAdapter(List<CategoryItem> items, Runnable onAdd) {
            this.items = items;
            this.onAdd = onAdd;
        }

        @Override public int getItemCount() { return items.size() + 1; } // +1 for Add button
        @Override public int getItemViewType(int pos) { return pos < items.size() ? TYPE_ITEM : TYPE_ADD; }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_ADD) {
                View v = inf.inflate(R.layout.item_category_add, parent, false);
                return new AddViewHolder(v);
            }
            View v = inf.inflate(R.layout.item_category, parent, false);
            return new ItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int pos) {
            if (holder instanceof ItemViewHolder) {
                CategoryItem item = items.get(pos);
                ItemViewHolder vh = (ItemViewHolder) holder;
                vh.circularProgress.setProgress(item.remaining());
                vh.ivIcon.setImageResource(item.iconRes);

                // Icon tint: white on dark circle
                vh.ivIcon.setColorFilter(Color.WHITE);

                vh.tvPercent.setText(item.percentLeft() + "% Left");
                // Dark text for 0% left
                vh.tvPercent.setTextColor(item.percentLeft() == 0
                        ? Color.parseColor("#EF4444")
                        : Color.parseColor("#1A1A1A"));
            } else if (holder instanceof AddViewHolder) {
                ((AddViewHolder) holder).itemView.setOnClickListener(v -> onAdd.run());
            }
        }

        static class ItemViewHolder extends RecyclerView.ViewHolder {
            CircularProgressView circularProgress;
            ImageView ivIcon;
            TextView tvPercent;
            ItemViewHolder(View v) {
                super(v);
                circularProgress = v.findViewById(R.id.circularProgress);
                ivIcon           = v.findViewById(R.id.ivCategoryIcon);
                tvPercent        = v.findViewById(R.id.tvPercentLeft);
            }
        }

        static class AddViewHolder extends RecyclerView.ViewHolder {
            AddViewHolder(View v) { super(v); }
        }
    }
}

