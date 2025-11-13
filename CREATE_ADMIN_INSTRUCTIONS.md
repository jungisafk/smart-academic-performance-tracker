# 🔐 How to Create Admin Account

## Quick Method: Using the Automated Script

### Option 1: With Service Account Key (Recommended)

1. **Get Service Account Key from Firebase Console:**
   ```
   1. Go to: https://console.firebase.google.com/project/performancetracker-da4c1/settings/serviceaccounts/adminsdk
   2. Click "Generate New Private Key"
   3. Save the JSON file as: serviceAccountKey.json
   4. Place it in the project root: D:\SmartAcademicPerformanceTracker\serviceAccountKey.json
   ```

2. **Grant Required Permissions to Service Account:**
   ```
   ⚠️ IMPORTANT: The service account needs proper permissions!
   
   1. Go to: https://console.cloud.google.com/iam-admin/iam?project=performancetracker-da4c1
   2. Find your service account (usually ends with @performancetracker-da4c1.iam.gserviceaccount.com)
   3. Click the pencil icon to edit
   4. Click "ADD ANOTHER ROLE"
   5. Add these roles:
      - Firebase Authentication Admin
      - Cloud Firestore User
      - Service Usage Consumer (if missing)
   6. Click "SAVE"
   7. Wait 2-3 minutes for permissions to propagate
   ```

3. **Run the script:**
   ```bash
   # Interactive version (prompts for input)
   npm run create-admin
   
   # Automated version (auto-generates password)
   npm run create-admin-auto "Admin" "User"
   ```

3. **Follow the prompts:**
   - Enter Admin First Name (or press Enter for default: "Admin")
   - Enter Admin Last Name (or press Enter for default: "User")
   - Enter a secure password (minimum 8 characters)

### Option 2: With Application Default Credentials

1. **Install Google Cloud SDK** (if not installed):
   ```bash
   # Windows (using winget)
   winget install Google.CloudSDK
   
   # Or download from: https://cloud.google.com/sdk/docs/install
   ```

2. **Authenticate:**
   ```bash
   gcloud auth application-default login
   gcloud config set project performancetracker-da4c1
   ```

3. **Run the script:**
   ```bash
   npm run create-admin
   ```

---

## Manual Method: Using Firebase Console

If you prefer to create the admin account manually:

### Step 1: Create Firebase Auth User

1. Go to: https://console.firebase.google.com/project/performancetracker-da4c1/authentication/users
2. Click "Add user"
3. Enter:
   - **Email:** `a-2024-001@sjp2cd.edu.ph`
   - **Password:** (Choose a secure password)
4. Click "Add user"

### Step 2: Create Firestore User Document

1. Go to: https://console.firebase.google.com/project/performancetracker-da4c1/firestore
2. Navigate to `users` collection
3. Click "Add document"
4. Use the **User UID** from Step 1 as the Document ID
5. Add these fields:

```json
{
  "id": "[Firebase Auth UID from Step 1]",
  "email": "a-2024-001@sjp2cd.edu.ph",
  "adminId": "A-2024-001",
  "firstName": "Admin",
  "lastName": "User",
  "role": "ADMIN",
  "isActive": true,
  "createdAt": [Click "Timestamp" and select "now"],
  "lastLoginAt": null,
  "passwordChangedAt": [Click "Timestamp" and select "now"],
  "mustChangePassword": false,
  "accountSource": "MANUAL"
}
```

6. Click "Save"

---

## Login Credentials

After creating the account, you can login with:

- **Admin ID:** `A-2024-001`
- **Password:** (The password you set)

---

## Troubleshooting

### Error: "Failed to initialize Firebase Admin SDK"
**Solution:** You need either:
- A `serviceAccountKey.json` file in the project root, OR
- Application default credentials set up via `gcloud auth application-default login`

### Error: "Email already exists"
**Solution:** The user already exists. The script will update the password instead.

### Error: "Permission denied"
**Solution:** Make sure your service account has the following roles:
- Firebase Authentication Admin
- Cloud Firestore User

---

## Security Note

⚠️ **Important:** Never commit `serviceAccountKey.json` to version control!
- It's already in `.gitignore`
- Keep it secure and private
- Rotate keys periodically

