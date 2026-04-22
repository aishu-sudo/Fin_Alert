package edu.ewubd.finalert;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptScanActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_IMAGE_CAPTURE     = 101;
    private static final int REQUEST_GALLERY_PICK      = 102;

    private Uri photoUri;
    private TextRecognizer recognizer;

    private LinearLayout scanBar, resultPanel;
    private EditText etResultAmount;
    private Spinner spinnerCategory;
    private List<String> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_scan);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        ImageView btnBack    = findViewById(R.id.btnBack);
        LinearLayout btnScan = findViewById(R.id.btnScan);
        ImageView btnRetry   = findViewById(R.id.btnRetry);
        ImageView btnGallery = findViewById(R.id.btnGallery);

        scanBar      = findViewById(R.id.scanBar);
        resultPanel  = findViewById(R.id.resultPanel);
        etResultAmount = findViewById(R.id.etResultAmount);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        Button btnSave      = findViewById(R.id.btnSave);
        Button btnScanAgain = findViewById(R.id.btnScanAgain);

        // Same category list as VoiceInputActivity
        categories = new ArrayList<>();
        categories.add("Food & Dining");
        categories.add("Grocery");
        categories.add("Transport");
        categories.add("Health");
        categories.add("Shopping");
        categories.add("Education");
        categories.add("Utilities");
        categories.add("Rent");
        categories.add("Gift");
        categories.add("Travel");
        categories.add("Savings");
        categories.add("Other");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnScan.setOnClickListener(v -> openCamera());
        btnRetry.setOnClickListener(v -> openCamera());
        btnGallery.setOnClickListener(v -> openGallery());

        btnScanAgain.setOnClickListener(v -> {
            resultPanel.setVisibility(android.view.View.GONE);
            scanBar.setVisibility(android.view.View.VISIBLE);
        });

        btnSave.setOnClickListener(v -> {
            String amount = etResultAmount.getText().toString().trim();
            String category = categories.get(spinnerCategory.getSelectedItemPosition());

            if (amount.isEmpty()) {
                etResultAmount.setError("Enter amount");
                return;
            }

            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            if (uid == null) {
                Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

            Map<String, Object> expense = new HashMap<>();
            expense.put("title",        category);
            expense.put("amount",       amount);
            expense.put("category",     category);
            expense.put("category_key", mapCategoryKey(category));
            expense.put("source",       "receipt_scan");
            expense.put("date",         today);
            expense.put("timestamp",    FieldValue.serverTimestamp());

            // Instant local save — server syncs in background
            FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("expenses")
                    .add(expense);

            Toast.makeText(this, "Saved: " + category + " — " + amount + " tk", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ReceiptScanActivity.this, Main_dashboard.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        launchCamera();
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } catch (IOException e) {
            Toast.makeText(this, "Could not open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return File.createTempFile("RECEIPT_" + ts, ".jpg",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES));
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        Uri imageUri = null;
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            imageUri = photoUri;
        } else if (requestCode == REQUEST_GALLERY_PICK && data != null) {
            imageUri = data.getData();
        }
        if (imageUri != null) processImage(imageUri);
    }

    private void processImage(Uri imageUri) {
        Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show();

        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String rawText = visionText.getText();
                        if (rawText.isEmpty()) {
                            Toast.makeText(this, "No text found on receipt", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        showResult(rawText);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Scan failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } catch (IOException e) {
            Toast.makeText(this, "Could not read image", Toast.LENGTH_SHORT).show();
        }
    }

    private void showResult(String text) {
        String lower = text.toLowerCase();

        // Extract amount — priority order: Payment Total > Net Due > Net Payable > Grand Total > Total
        String amount = extractAmount(text);

        // Detect category index
        int categoryIndex = detectCategory(lower);

        // Fill result panel
        etResultAmount.setText(amount);
        spinnerCategory.setSelection(categoryIndex);

        // Switch to result view
        scanBar.setVisibility(android.view.View.GONE);
        resultPanel.setVisibility(android.view.View.VISIBLE);
    }

    private String extractAmount(String text) {
        Pattern numPat = Pattern.compile("(\\d[\\d,]*\\.?\\d*)");

        // Flatten entire OCR text to one line — fixes column-split issue where
        // "PAYMENT TOTAL" and "575.00" land on separate lines in OCR output
        String flat = text.toLowerCase()
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s+", " ");

        // Priority keywords — most specific first
        String[] keywords = {
            "payment total", "net due", "net payable", "amount due",
            "grand total", "total amount", "payable amount", "মোট"
        };

        // For each keyword: find it in the flat string, then take the first valid
        // number (>= 10, <= 99999) that appears after it
        for (String keyword : keywords) {
            int idx = flat.indexOf(keyword);
            if (idx < 0) continue;
            String after = flat.substring(idx + keyword.length());
            Matcher m = numPat.matcher(after);
            while (m.find()) {
                String candidate = m.group(1).replace(",", "");
                try {
                    double val = Double.parseDouble(candidate);
                    if (val >= 10 && val <= 99999) return candidate;
                } catch (NumberFormatException ignored) {}
            }
        }

        // Fallback: last occurrence of standalone "total" in the receipt
        // (last = bottom of receipt = grand total, not a subtotal)
        int idx = flat.lastIndexOf("total");
        if (idx >= 0) {
            String after = flat.substring(idx + 5);
            Matcher m = numPat.matcher(after);
            while (m.find()) {
                String candidate = m.group(1).replace(",", "");
                try {
                    double val = Double.parseDouble(candidate);
                    if (val >= 10 && val <= 99999) return candidate;
                } catch (NumberFormatException ignored) {}
            }
        }

        // Last resort: largest reasonable number in the whole receipt
        Matcher m = numPat.matcher(flat);
        double maxVal = 0;
        String maxStr = "";
        while (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1).replace(",", ""));
                if (val > maxVal && val >= 10 && val <= 99999) {
                    maxVal = val;
                    maxStr = m.group(1).replace(",", "");
                }
            } catch (NumberFormatException ignored) {}
        }
        return maxStr;
    }

    private int detectCategory(String lower) {
        // Normalize multiline OCR text into single line for reliable contains() checks
        String flat = lower.replaceAll("\\s+", " ");

        // Shopping — checked first (BD brands + cosmetics/lifestyle products)
        if (containsAny(flat, "aarong", "আড়ং", "yellow", "richman", "cats eye", "kay kraft",
                "sailor", "ecstasy", "shajgoj", "outlet", "fashion",
                "scrub", "lotion", "cream", "serum", "moisturizer", "shampoo",
                "conditioner", "face wash", "body wash", "perfume", "lipstick",
                "makeup", "cosmetic", "skincare", "face", "body care",
                "shirt", "pant", "dress", "cloth", "kameez", "salwar", "saree",
                "shoe", "bag", "wallet", "shop", "mall", "showroom",
                "পোশাক", "কাপড়", "শপিং", "জামা", "জুতা"))
            return 4; // Shopping

        if (containsAny(flat, "restaurant", "cafe", "pizza", "burger", "kfc", "bfc",
                "biryani", "dining", "meal", "dine", "canteen", "iftar",
                "খাবার", "রেস্তোরাঁ", "বিরিয়ানি"))
            return 0; // Food & Dining

        if (containsAny(flat, "grocery", "agora", "shwapno", "meena", "unimart",
                "chaldal", "supermarket", "supershop", "rice", "fish", "meat",
                "vegetable", "bazar", "market", "বাজার", "চাল", "মাছ", "সবজি"))
            return 1; // Grocery

        if (containsAny(flat, "uber", "pathao", "shohoz", "rickshaw", "cng", "bus",
                "taxi", "fuel", "petrol", "transport", "fare",
                "রিকশা", "বাস", "ভাড়া", "সিএনজি"))
            return 2; // Transport

        if (containsAny(flat, "pharmacy", "medicine", "doctor", "hospital", "clinic",
                "health", "drug", "ওষুধ", "ডাক্তার", "হাসপাতাল", "ফার্মেসি"))
            return 3; // Health

        if (containsAny(flat, "school", "college", "university", "tuition", "book",
                "stationery", "পড়া", "বই", "স্কুল", "কলেজ", "টিউশন"))
            return 5; // Education

        if (containsAny(flat, "electricity", "gas", "water", "internet", "wifi",
                "bill", "recharge", "বিদ্যুৎ", "পানি", "গ্যাস", "বিল"))
            return 6; // Utilities

        if (containsAny(flat, "rent", "house", "flat", "room", "ভাড়া", "বাসা"))
            return 7; // Rent

        if (containsAny(flat, "gift", "present", "birthday", "উপহার"))
            return 8; // Gift

        if (containsAny(flat, "tour", "trip", "hotel", "flight", "travel", "ভ্রমণ"))
            return 9; // Travel

        return 11; // Other
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String mapCategoryKey(String label) {
        if (label == null) return "other";
        String s = label.toLowerCase(Locale.getDefault());
        if (s.contains("food") || s.contains("dining"))  return "food";
        if (s.contains("grocery"))                        return "grocery";
        if (s.contains("transport"))                      return "transport";
        if (s.contains("health"))                         return "health";
        if (s.contains("shopping"))                       return "shopping";
        if (s.contains("education"))                      return "education";
        if (s.contains("utilities"))                      return "utilities";
        if (s.contains("rent"))                           return "rent";
        if (s.contains("gift"))                           return "gift";
        if (s.contains("travel"))                         return "travel";
        if (s.contains("savings"))                        return "savings";
        return "other";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recognizer.close();
    }
}
