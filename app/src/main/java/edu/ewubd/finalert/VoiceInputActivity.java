package edu.ewubd.finalert;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VoiceInputActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_PERMISSION = 200;

    private SpeechRecognizer speechRecognizer;

    private TextView     tvStatus;
    private Button       btnStartStop, btnSave, btnCancel;
    private EditText     etResultAmount;
    private Spinner      spinnerCategory;
    private ImageView    ivMicIcon;
    private LinearLayout micCircle;
    private List<String> categories;
    private int currentState = STATE_IDLE;

    private static final int STATE_IDLE   = 0;
    private static final int STATE_LISTEN = 1;
    private static final int STATE_RESULT = 2;

    // Simple containsAny — same pattern as ReceiptScanActivity
    private int autoDetectCategory(String text) {
        String t = text.toLowerCase(Locale.getDefault());

        // Food & Dining — index 1
        if (containsAny(t,
                "food", "restaurant", "cafe", "coffee", "pizza", "burger", "kfc", "bfc",
                "biryani", "biriyani", "meal", "lunch", "dinner", "breakfast", "iftar",
                "nasta", "snack", "khabar", "khawa", "khaoa", "roti", "bhat", "rice",
                "curry", "chicken", "mutton", "fry", "khabo", "khai", "kheye",
                "খাবার", "রেস্তোরাঁ",
                "ভাত", "রুটি"))
            return 1;

        // Grocery — index 2
        if (containsAny(t,
                "grocery", "agora", "shwapno", "meena", "unimart", "chaldal",
                "supermarket", "supershop", "bazar", "bazaar", "kacha bazar",
                "shobji", "sabzi", "vegetable", "maach", "fish market",
                "chaal", "dal", "oil", "tel", "chal kena",
                "বাজার", "চাল",
                "মাছ", "সবজি", "তেল"))
            return 2;

        // Transport — index 3
        if (containsAny(t,
                "transport", "uber", "pathao", "shohoz", "rickshaw", "ricksha", "riksha",
                "cng", "bus", "taxi", "auto", "fuel", "petrol", "train", "launch",
                "ferry", "ride", "gari", "auto rickshaw",
                "রিকশা", "বাস",
                "ভাড়া", "গাড়ি"))
            return 3;

        // Health — index 4
        if (containsAny(t,
                "medicine", "doctor", "hospital", "pharmacy", "clinic",
                "health", "drug", "test", "checkup", "osud", "oshud", "dawai",
                "ওষুধ", "ডাক্তার",
                "ফার্মেসি"))
            return 4;

        // Shopping — index 5
        if (containsAny(t,
                "shopping", "shop", "cloth", "clothes", "dress", "shirt", "pant",
                "shoe", "bag", "wallet", "daraz", "fashion", "brand", "store",
                "mall", "showroom", "kapor", "jamai", "jama", "juta", "kena",
                "জামা", "জুতা",
                "কাপড়", "শপিং",
                "কেনাকাটা"))
            return 5;

        // Education — index 6
        if (containsAny(t,
                "tuition", "school", "college", "university", "book", "boi",
                "stationery", "course", "class", "exam", "fees",
                "বই", "স্কুল",
                "টিউশন"))
            return 6;

        // Utilities — index 7
        if (containsAny(t,
                "electricity", "gas", "water", "internet", "wifi",
                "recharge", "mobile", "sim", "bijli", "pani",
                "বিদ্যুৎ",
                "পানি", "রিচার্জ"))
            return 7;

        // Entertainment — index 8
        if (containsAny(t,
                "movie", "cinema", "netflix", "game", "concert",
                "ticket", "fun", "entertainment",
                "সিনেমা"))
            return 8;

        // Rent — index 9
        if (containsAny(t,
                "rent", "house rent", "basha", "flat", "room", "bari", "basa",
                "ভাড়া", "বাসা",
                "বাড়ি"))
            return 9;

        // Bills — index 10
        if (containsAny(t,
                "bill", "utility bill", "due", "charge",
                "বিল"))
            return 10;

        // Gift — index 11
        if (containsAny(t,
                "gift", "present", "birthday", "uphar",
                "উপহার", "জন্মদিন"))
            return 11;

        // Travel — index 12
        if (containsAny(t,
                "tour", "trip", "hotel", "flight", "travel", "cox", "sundarban",
                "ভ্রমণ", "ট্যার"))
            return 12;

        return 13; // Other
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_input);

        tvStatus = findViewById(R.id.tvStatus);
        btnStartStop = findViewById(R.id.btnStartStop);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        etResultAmount = findViewById(R.id.etResultAmount);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        ivMicIcon = findViewById(R.id.ivMicIcon);
        micCircle = findViewById(R.id.micCircle);

        categories = new ArrayList<>();
        categories.add("");               // index 0 unused
        categories.add("Food & Dining");  // 1
        categories.add("Grocery");        // 2
        categories.add("Transport");      // 3
        categories.add("Health");         // 4
        categories.add("Shopping");       // 5
        categories.add("Education");      // 6
        categories.add("Utilities");      // 7
        categories.add("Entertainment");  // 8
        categories.add("Rent");           // 9
        categories.add("Bills");          // 10
        categories.add("Gift");           // 11
        categories.add("Travel");         // 12
        categories.add("Other");          // 13

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        applyState(STATE_IDLE);

        btnStartStop.setOnClickListener(v -> handleStartStop());
        micCircle.setOnClickListener(v -> handleStartStop());

        btnSave.setOnClickListener(v -> {
            String amt = etResultAmount.getText().toString().trim();
            String cat = categories.get(spinnerCategory.getSelectedItemPosition());
            if (amt.isEmpty()) {
                Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            saveExpenseToFirestore(amt, cat);
        });

        btnCancel.setOnClickListener(v -> applyState(STATE_IDLE));
    }

    private void handleStartStop() {
        if (currentState == STATE_IDLE) {
            checkPermissionAndStart();
        } else if (currentState == STATE_LISTEN) {
            stopListening();
            applyState(STATE_RESULT);
        } else if (currentState == STATE_RESULT) {
            applyState(STATE_IDLE);
            checkPermissionAndStart();
        }
    }

    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_PERMISSION);
        } else {
            startListening();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_PERMISSION &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show();
            applyState(STATE_RESULT);
            return;
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) { applyState(STATE_LISTEN); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                applyState(STATE_IDLE);
                Toast.makeText(VoiceInputActivity.this, "Could not hear. Try again.", Toast.LENGTH_SHORT).show();
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    parseVoiceInput(matches.get(0));
                } else {
                    applyState(STATE_RESULT);
                }
            }
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int t, Bundle b) {}
        });
        speechRecognizer.startListening(intent);
        applyState(STATE_LISTEN);
    }

    private void stopListening() {
        if (speechRecognizer != null) speechRecognizer.stopListening();
    }

    private void parseVoiceInput(String text) {
        String lower = text.toLowerCase(Locale.getDefault()).trim();

        // Extract amount
        String amount = "";
        for (String w : lower.split("\\s+")) {
            String cleaned = w.replaceAll("[^0-9.]", "");
            if (!cleaned.isEmpty() && cleaned.matches("[0-9]+\\.?[0-9]*")) {
                amount = cleaned;
                break;
            }
        }

        int detectedIndex = autoDetectCategory(lower);

        etResultAmount.setText(amount);
        spinnerCategory.setSelection(detectedIndex);
        applyState(STATE_RESULT);

        String detected = categories.get(detectedIndex);
        Toast.makeText(this,
                "Heard: \"" + text + "\"\nCategory: " + detected,
                Toast.LENGTH_LONG).show();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private void saveExpenseToFirestore(String amount, String category) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(new java.util.Date());

        Map<String, Object> expense = new HashMap<>();
        expense.put("title",        category);
        expense.put("amount",       amount);
        expense.put("category",     category);
        expense.put("category_key", mapLabelToKey(category));
        expense.put("source",       "voice_input");
        expense.put("date",         today);
        expense.put("timestamp",    com.google.firebase.firestore.FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("expenses")
                .add(expense);

        Toast.makeText(this, "Saved: " + category + " — " + amount + " tk", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(VoiceInputActivity.this, Main_dashboard.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String mapLabelToKey(String label) {
        if (label == null) return "other";
        String s = label.toLowerCase(Locale.getDefault());
        if (s.contains("food") || s.contains("dining")) return "food";
        if (s.contains("grocery"))                       return "grocery";
        if (s.contains("transport"))                     return "transport";
        if (s.contains("health"))                        return "health";
        if (s.contains("shopping"))                      return "shopping";
        if (s.contains("education"))                     return "education";
        if (s.contains("utilities"))                     return "utilities";
        if (s.contains("entertainment"))                 return "entertainment";
        if (s.contains("rent"))                          return "rent";
        if (s.contains("bills") || s.contains("bill"))  return "utilities";
        if (s.contains("gift"))                          return "gift";
        if (s.contains("travel"))                        return "travel";
        return "other";
    }

    private void applyState(int state) {
        currentState = state;
        switch (state) {
            case STATE_IDLE:
                tvStatus.setText("");
                ivMicIcon.setImageResource(R.drawable.ic_mic);
                ivMicIcon.setColorFilter(0xFFFFFFFF);
                btnStartStop.setText("Start");
                btnStartStop.setBackgroundResource(R.drawable.btn_green2);
                btnStartStop.setTextColor(0xFFFFFFFF);
                btnStartStop.setVisibility(android.view.View.VISIBLE);
                etResultAmount.setVisibility(android.view.View.GONE);
                spinnerCategory.setVisibility(android.view.View.GONE);
                btnSave.setVisibility(android.view.View.GONE);
                btnCancel.setVisibility(android.view.View.GONE);
                break;
            case STATE_LISTEN:
                tvStatus.setText("Speak Now...");
                ivMicIcon.setColorFilter(0xFF33D49C);
                btnStartStop.setText("Stop");
                btnStartStop.setBackgroundResource(R.drawable.btn_light2);
                btnStartStop.setTextColor(0xFF1A1A1A);
                btnStartStop.setVisibility(android.view.View.VISIBLE);
                etResultAmount.setVisibility(android.view.View.GONE);
                spinnerCategory.setVisibility(android.view.View.GONE);
                btnSave.setVisibility(android.view.View.GONE);
                btnCancel.setVisibility(android.view.View.GONE);
                break;
            case STATE_RESULT:
                tvStatus.setText("Select Category & Save");
                ivMicIcon.setImageResource(R.drawable.ic_redo);
                btnStartStop.setVisibility(android.view.View.GONE);
                etResultAmount.setVisibility(android.view.View.VISIBLE);
                spinnerCategory.setVisibility(android.view.View.VISIBLE);
                btnSave.setVisibility(android.view.View.VISIBLE);
                btnCancel.setVisibility(android.view.View.VISIBLE);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}
