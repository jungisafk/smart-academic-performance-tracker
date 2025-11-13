# 🗑️ Clear Old System Users

This script removes Firebase users that don't use the ID-based authentication system.

## 📋 What It Does

The script identifies and deletes users whose emails don't match the ID system format:

- ✅ **ID System Format (KEPT)**:
  - Students: `s2022-2563@sjp2cd.edu.ph`
  - Teachers: `t-2022-001@sjp2cd.edu.ph` or `temp-12345@sjp2cd.edu.ph`
  - Admins: `a-2024-001@sjp2cd.edu.ph`

- ❌ **Old System Format (DELETED)**:
  - Any email that doesn't match the patterns above
  - Examples: `kid@gmail.com`, `alt@gmail.com`, `lop1@gmail.com`

## 🚀 Usage

### Step 1: Dry Run (Preview What Will Be Deleted)

```bash
node clear-old-users.js --dry-run
```

This will show you:
- Total users found
- Which users use the ID system (will be kept)
- Which users use the old system (will be deleted)
- Details about each old system user

### Step 2: Actually Delete (After Reviewing)

```bash
node clear-old-users.js --confirm
```

**⚠️ WARNING**: This will permanently delete:
- Firebase Auth users
- Firestore user documents
- This action **CANNOT BE UNDONE**

The script will ask you to type "DELETE" to confirm.

## 📊 Current Status

Based on the dry run:
- ✅ **ID System Users**: 4 (will be kept)
- ❌ **Old System Users**: 8 (will be deleted)

## 🔍 What Gets Deleted

For each old system user, the script will:
1. Delete the user document from Firestore (`users/{uid}`)
2. Delete the user from Firebase Authentication

## ⚠️ Important Notes

1. **Backup First**: Consider exporting your Firestore data before running this script
2. **Review Carefully**: Always run `--dry-run` first to see what will be deleted
3. **No Undo**: Once deleted, users cannot be recovered
4. **Related Data**: This only deletes users. Related data (grades, enrollments, etc.) may still exist and need manual cleanup

## 📝 Example Output

```
🔍 Scanning Firebase users...

📊 Scanned 12 users...

✅ Total users found: 12

📊 Summary:
   ✅ ID System Users: 4
   ❌ Old System Users: 8

❌ Old System Users (will be deleted):
────────────────────────────────────────────────────────────────
1. kid@gmail.com
   UID: JZPIfStPUuQLJVbP8GRSqhkYqMo2
   Name: N/A
   Created: Wed, 12 Nov 2025 07:25:12 GMT
   Last Sign In: Wed, 12 Nov 2025 07:25:12 GMT
...
```

## 🛠️ Requirements

- Node.js installed
- Firebase Admin SDK (`npm install firebase-admin`)
- `serviceAccountKey.json` file in the project root
- Proper Firebase Admin permissions

## 🔐 Security

The script requires Firebase Admin SDK credentials. Make sure:
- `serviceAccountKey.json` is in `.gitignore`
- Only authorized personnel run this script
- Review the dry-run output carefully before confirming deletion

