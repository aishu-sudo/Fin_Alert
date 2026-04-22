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
import android.animation.ValueAnimator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoriesActivity extends AppCompatActivity {

    private static final double TOTAL_BUDGET = 20000.0;
    private ListenerRegistration listener;

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

        tvBudget.setText(String.format("৳%,.0f", TOTAL_BUDGET));

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

    private void buildCategories() {
        items.clear();
        // use appropriate icons from drawable resources for each category type
        items.add(new CategoryItem("food",       "Food",        R.drawable.ic_groceries, 3000));
        items.add(new CategoryItem("transport",  "Transport",   R.drawable.ic_transfer,  2000));
        items.add(new CategoryItem("grocery",    "Grocery",     R.drawable.ic_receipt,   4000));
        items.add(new CategoryItem("shopping",   "Shopping",    R.drawable.ic_expense,   2000));
        items.add(new CategoryItem("savings",    "Savings",     R.drawable.ic_goal,      5000));
        items.add(new CategoryItem("rent",       "Rent",        R.drawable.ic_home,      8000));
        items.add(new CategoryItem("gift",       "Gift",        R.drawable.ic_salary,    1000));
        items.add(new CategoryItem("health",     "Health",      R.drawable.ic_battery,   2000));
        items.add(new CategoryItem("education",  "Educational", R.drawable.ic_scan,      3000));
        items.add(new CategoryItem("other",      "Others",      R.drawable.ic_wallet,    2000));
    }

    private void startListening() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;
        if (listener != null) listener.remove();

        listener = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("expenses")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (CategoryItem c : items) c.spent = 0;
                    double totalSpent = 0;
                    Map<String, Double> spentMap = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String catKey = doc.getString("category_key");
                        String rawCat = doc.getString("category");
                        String rawAmt = doc.getString("amount");
                        if (rawAmt == null) continue;
                        try {
                            double amt = Double.parseDouble(rawAmt.replace(",", ""));
                            totalSpent += amt;
                            String key;
                            if (catKey != null && !catKey.isEmpty()) {
                                key = catKey;
                            } else if (rawCat != null) {
                                key = mapCategory(rawCat.toLowerCase());
                            } else {
                                key = "other";
                            }
                            spentMap.merge(key, amt, Double::sum);
                        } catch (NumberFormatException ignored) {}
                    }

                    for (CategoryItem c : items) {
                        Double v = spentMap.get(c.name);
                        c.spent = v != null ? v : 0;
                    }

                    double balance = TOTAL_BUDGET - totalSpent;
                    int pct = (int) Math.min(100, totalSpent / TOTAL_BUDGET * 100);

                    tvBalance.setText(String.format("৳%,.2f", balance));
                    tvExpense.setText(String.format("-৳%,.2f", totalSpent));
                    tvPercent.setText(pct + "%");

                    LinearLayout.LayoutParams currentFillParams = (LinearLayout.LayoutParams) progressFill.getLayoutParams();
                    ValueAnimator anim = ValueAnimator.ofFloat(currentFillParams.weight, pct / 100f);
                    anim.setDuration(400);
                    anim.addUpdateListener(animation -> {
                        float val = (float) animation.getAnimatedValue();
                        LinearLayout.LayoutParams fp = (LinearLayout.LayoutParams) progressFill.getLayoutParams();
                        fp.weight = val;
                        progressFill.setLayoutParams(fp);
                        LinearLayout.LayoutParams ep = (LinearLayout.LayoutParams) progressEmpty.getLayoutParams();
                        ep.weight = 1f - val;
                        progressEmpty.setLayoutParams(ep);
                    });
                    anim.start();

                    if (pct <= 50) tvStatus.setText(pct + "% Of Your Expenses, Looks Good.");
                    else if (pct <= 80) tvStatus.setText(pct + "% Of Your Expenses, Be Careful.");
                    else tvStatus.setText(pct + "% Of Your Expenses, Overspending!");

                    adapter.notifyDataSetChanged();
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

        navHome.setOnClickListener(v -> { startActivity(new Intent(this, Main_dashboard.class)); finish(); });
        navAnalytics.setOnClickListener(v -> { startActivity(new Intent(this, AnalyticsActivity.class)); finish(); });
        navTransfer.setOnClickListener(v -> { startActivity(new Intent(this, AddExpenseActivity.class)); });
        navLayers.setOnClickListener(v -> {});
        navProfile.setOnClickListener(v -> { startActivity(new Intent(this, ProfileActivity.class)); finish(); });
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

                // show label + percent for clarity (helps preview/runtime debugging)
                vh.tvPercent.setText(item.label + "\n" + item.percentLeft() + "% Left");
                // open detail on click
                vh.itemView.setOnClickListener(v -> {
                    Intent i = new Intent(v.getContext(), CategoryDetailActivity.class);
                    i.putExtra("name", item.name);
                    i.putExtra("label", item.label);
                    i.putExtra("budget", item.budget);
                    v.getContext().startActivity(i);
                });
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

