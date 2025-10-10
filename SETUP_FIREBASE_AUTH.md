# Firebase Authentication Setup for Migration Script

The migration script requires Firebase Admin SDK authentication. Choose one of the following methods:

## 🔐 Option 1: Service Account Key (Recommended)

This is the most reliable method for automation.

### Steps:
1. **Go to Firebase Console**
   - Navigate to: https://console.firebase.google.com/
   - Select your project: `performancetracker-da4c1`

2. **Generate Service Account Key**
   - Go to: Project Settings → Service Accounts
   - Click: "Generate New Private Key"
   - Save the JSON file

3. **Place the Key File**
   - Rename the file to: `serviceAccountKey.json`
   - Place it in the project root directory: `D:\SmartAcademicPerformanceTracker\serviceAccountKey.json`

4. **Run the Migration Script**
   ```bash
   npm run migrate-firestore
   ```

⚠️ **Security Note**: Add `serviceAccountKey.json` to `.gitignore` to prevent committing it to version control.

---

## 🔐 Option 2: Google Cloud SDK (gcloud)

If you have Google Cloud SDK installed:

### Steps:
1. **Install Google Cloud SDK** (if not installed)
   - Download: https://cloud.google.com/sdk/docs/install
   - Or use: `winget install Google.CloudSDK` (Windows)

2. **Authenticate**
   ```bash
   gcloud auth application-default login
   ```

3. **Set Project** (optional)
   ```bash
   gcloud config set project performancetracker-da4c1
   ```

4. **Run the Migration Script**
   ```bash
   npm run migrate-firestore
   ```

---

## 🔐 Option 3: Environment Variable

Set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable to point to your service account key.

### Windows PowerShell:
```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="D:\path\to\serviceAccountKey.json"
node migrate_firestore_data.js
```

### Windows Command Prompt:
```cmd
set GOOGLE_APPLICATION_CREDENTIALS=D:\path\to\serviceAccountKey.json
node migrate_firestore_data.js
```

### Linux/Mac:
```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccountKey.json"
node migrate_firestore_data.js
```

---

## ✅ Verify Authentication

After setting up authentication, test it:

```bash
node migrate_firestore_data.js
```

You should see:
```
✅ Firebase Admin SDK initialized with [method] (project: performancetracker-da4c1)
```

---

## 🚀 Quick Start (Recommended)

1. **Get Service Account Key**:
   - Firebase Console → Project Settings → Service Accounts → Generate New Private Key
   - Save as `serviceAccountKey.json` in project root

2. **Add to .gitignore**:
   ```bash
   echo "serviceAccountKey.json" >> .gitignore
   ```

3. **Run Migration**:
   ```bash
   npm run migrate-firestore
   ```

---

## 🐛 Troubleshooting

### Error: "Could not load the default credentials"
- **Solution**: Use Option 1 (Service Account Key) - it's the most reliable

### Error: "Permission denied"
- **Solution**: Make sure the service account has "Firestore Admin" or "Owner" role in Firebase Console

### Error: "Project not found"
- **Solution**: Verify project ID in `app/google-services.json` matches your Firebase project

---

**Need Help?** Check the `MIGRATION_GUIDE.md` for more details.

