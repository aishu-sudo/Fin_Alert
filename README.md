  # Fin Alert 💰                                                                                                            
                                                                                                                            
  A smart personal finance management Android application designed for Bangladeshi users. Fin Alert helps you track daily   
  expenses through manual entry, receipt scanning (OCR), and voice input — with real-time budget monitoring and           
  category-wise analytics.                                                                                                  

  ---                                                                                                                       
                                                                                                                            
  ## Features

  - **Manual Expense Entry** — Log expenses with title, amount, category, date, and notes
  - **Receipt Scanning** — Capture or upload receipts; ML Kit OCR extracts amount and category automatically
  - **Voice Input** — Speak your expense; speech-to-text detects amount and category in real time
  - **Category Budget Tracking** — 10 predefined categories with circular progress indicators and spending alerts
  - **Real-Time Dashboard** — Live balance, total expense, and transaction list powered by Firestore
  - **Analytics Dashboard** — Category-wise spending breakdown with percentage contribution and progress bars
  - **Bengali Language Support** — Recognizes Bengali keywords for categories (খাবার, রিকশা, বাজার, etc.)
  - **Local Merchant Recognition** — 40+ Bangladeshi brands and services (Aarong, Pathao, Shwapno, KFC, Uber, Chaldal, etc.)
  - **User Authentication** — Secure email/password login and registration via Firebase Auth

  ---

  ## Tech Stack

  | Layer | Technology |
  |-------|-----------|
  | Language | Java |
  | Platform | Android (Min SDK 24 / Target SDK 36) |
  | Authentication | Firebase Authentication |
  | Database | Firebase Firestore (real-time, offline-capable) |
  | OCR | Google ML Kit Text Recognition |
  | Voice | Android SpeechRecognizer |
  | UI Components | RecyclerView, ConstraintLayout, Material Design |
  | Custom Views | CircularProgressView (canvas-drawn budget ring) |
  | Build System | Gradle (Kotlin DSL) |

  ---

  ## Project Structure

  ```
  app/src/main/java/edu/ewubd/finalert/
  ├── MainActivity.java            # Splash screen
  ├── Auth.java                    # Login / Sign Up choice
  ├── On_boardingA.java            # Onboarding screen 1
  ├── On_boardingB.java            # Onboarding screen 2
  ├── SignUp.java                  # Registration with validation
  ├── Log_in.java                  # Firebase email/password login
  ├── Main_dashboard.java          # Home: balance, transactions, actions
  ├── AddExpenseActivity.java      # Manual expense form
  ├── ReceiptScanActivity.java     # Camera/gallery OCR scanning
  ├── VoiceInputActivity.java      # Voice-to-text expense entry
  ├── CategoriesActivity.java      # Budget grid by category
  ├── CategoryDetailActivity.java  # Per-category expense list
  ├── AnalyticsActivity.java       # Spending analytics
  ├── ProfileActivity.java         # User profile & logout
  └── CircularProgressView.java    # Custom ring progress view
  ```

  ---

  ## Firestore Data Structure

  ```
  users/
    {uid}/
      name, email, phone, dob
      expenses/
        {docId}/
          title, amount, category, category_key,
          date, timestamp, message, source
  ```

  `source` is one of: `manual`, `receipt_scan`, `voice_input`

  ---

  ## Categories & Budgets

  | Category | Monthly Budget |
  |----------|---------------|
  | Food & Dining | ৳3,000 |
  | Transport | ৳2,000 |
  | Grocery | ৳4,000 |
  | Shopping | ৳2,000 |
  | Savings | ৳5,000 |
  | Rent | ৳8,000 |
  | Gift | ৳1,000 |
  | Health | ৳2,000 |
  | Education | ৳3,000 |
  | Other | ৳2,000 |

  **Total Budget: ৳20,000/month**

  ---

  ## Setup & Installation

  ### Prerequisites
  - Android Studio Hedgehog or later
  - JDK 11+
  - A Firebase project with Authentication and Firestore enabled

  ### Steps

  1. Clone the repository:
     ```bash
     git clone https://github.com/aishu-sudo/Fin_Alert.git
     ```

  2. Open the project in Android Studio.

  3. Connect Firebase:
     - Go to [Firebase Console](https://console.firebase.google.com/)
     - Create a new project (or use existing)
     - Add an Android app with package name `edu.ewubd.finalert`
     - Download `google-services.json` and place it in `app/`

  4. Enable the following in Firebase Console:
     - **Authentication** → Email/Password sign-in method
     - **Firestore Database** → Start in test mode (configure rules before production)

  5. Build and run on a physical device or emulator (API 24+).

  ---

  ## Permissions

  | Permission | Purpose |
  |-----------|---------|
  | `CAMERA` | Capture receipt photos for OCR scanning |
  | `RECORD_AUDIO` | Voice-based expense entry |
  | `INTERNET` | Firebase data sync and authentication |

  ---

  ## Budget Alert Thresholds

  | Status | Condition |
  |--------|-----------|
  | Looks Good | Spent < 50% of budget |
  | Be Careful | Spent 50–80% of budget |
  | Overspending! | Spent > 80% of budget |

  ---

  ## License

  This project is developed for academic purposes at East West University, Bangladesh.
