package edu.ewubd.finalert;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_scan);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        ImageView    btnBack    = findViewById(R.id.btnBack);
        LinearLayout btnScan    = findViewById(R.id.btnScan);
        ImageView    btnRetry   = findViewById(R.id.btnRetry);
        ImageView    btnGallery = findViewById(R.id.btnGallery);

        btnBack.setOnClickListener(v -> finish());
        btnScan.setOnClickListener(v -> openCamera());
        btnRetry.setOnClickListener(v -> openCamera());
        btnGallery.setOnClickListener(v -> openGallery());
    }

    // ── Camera ────────────────────────────────────────────────────
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

    // ── Gallery ───────────────────────────────────────────────────
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY_PICK);
    }

    // ── Result ────────────────────────────────────────────────────
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

    // ── ML Kit OCR ────────────────────────────────────────────────
    private void processImage(Uri imageUri) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Scanning receipt...");
        pd.setCancelable(false);
        pd.show();

        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        pd.dismiss();
                        String rawText = visionText.getText();
                        if (rawText.isEmpty()) {
                            Toast.makeText(this, "No text found on receipt", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        parseAndSave(rawText);
                    })
                    .addOnFailureListener(e -> {
                        pd.dismiss();
                        Toast.makeText(this, "Scan failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (IOException e) {
            pd.dismiss();
            Toast.makeText(this, "Could not read image", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Parse OCR text → amount + category ───────────────────────
    private void parseAndSave(String text) {
        String lower = text.toLowerCase();

        // ── Find TOTAL amount ──────────────────────────────────────
        String amount = "";
        Pattern totalPattern = Pattern.compile(
                "(?i)(total|grand total|amount|due|payable|মোট)[^\\d]*(\\d+\\.?\\d*)");
        Matcher totalMatcher = totalPattern.matcher(text);
        if (totalMatcher.find()) {
            amount = totalMatcher.group(2);
        }
        // Fallback: largest number on receipt
        if (amount.isEmpty()) {
            Pattern pricePattern = Pattern.compile("\\d+\\.?\\d*");
            Matcher m = pricePattern.matcher(text);
            double maxVal = 0;
            while (m.find()) {
                try {
                    double val = Double.parseDouble(m.group());
                    if (val > maxVal) { maxVal = val; amount = m.group(); }
                } catch (NumberFormatException ignored) {}
            }
        }
        if (amount.isEmpty()) amount = "0.00";

        // ── Smart category detection (BD + English) ───────────────
        String category = "📦 Other";

        if (lower.matches(".*(restaurant|cafe|coffee|pizza|burger|kfc|bfc|biryani|" +
                "food|meal|dine|canteen|খাবার|রেস্তোরাঁ|বিরিয়ানি).*"))
            category = "🍔 Food & Dining";
        else if (lower.matches(".*(grocery|agora|shwapno|meena bazaar|unimart|chaldal|" +
                "supermarket|supershop|rice|fish|meat|vegetable|bazar|market|" +
                "বাজার|চাল|মাছ|মাংস|সবজি|তেল).*"))
            category = "🛒 Grocery";
        else if (lower.matches(".*(uber|pathao|shohoz|rickshaw|cng|bus|taxi|fuel|petrol|" +
                "transport|fare|যাতায়াত|রিকশা|বাস|ভাড়া|সিএনজি).*"))
            category = "🚌 Transport";
        else if (lower.matches(".*(pharmacy|medicine|doctor|hospital|clinic|drug|health|" +
                "ওষুধ|ডাক্তার|হাসপাতাল|ফার্মেসি|মেডিকেল).*"))
            category = "💊 Healthcare";
        else if (lower.matches(".*(shop|store|mall|daraz|cloth|dress|fashion|brand|" +
                "পোশাক|কাপড়|শপিং|কেনাকাটা).*"))
            category = "🛍️ Shopping";
        else if (lower.matches(".*(school|college|university|tuition|book|stationery|" +
                "পড়া|বই|স্কুল|কলেজ|টিউশন).*"))
            category = "🎓 Education";
        else if (lower.matches(".*(electricity|gas|water|internet|wifi|bill|recharge|" +
                "বিদ্যুৎ|পানি|গ্যাস|ইন্টারনেট|রিচার্জ|বিল).*"))
            category = "💡 Utilities";

        Toast.makeText(this,
                "✅ Detected: " + category + " — " + amount,
                Toast.LENGTH_LONG).show();
        saveToFirestore(amount, category);
    }

    // ── Save to Firestore → go to Dashboard ───────────────────────
    private void saveToFirestore(String amount, String category) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> expense = new HashMap<>();
        expense.put("amount",   amount);
        expense.put("category", category);
        expense.put("source",   "receipt_scan");
        expense.put("date",     new Date());

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("expenses").add(expense)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "✅ Saved: " + category + " $" + amount, Toast.LENGTH_SHORT).show();
                    // Go directly to Main Dashboard
                    Intent intent = new Intent(ReceiptScanActivity.this, Main_dashboard.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
