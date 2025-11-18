#!/bin/bash
# Bash script to deploy Firestore indexes using Firebase CLI
# This script will deploy the indexes defined in firestore.indexes.json

echo "Deploying Firestore indexes..."

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo "Error: Firebase CLI is not installed or not in PATH"
    echo "Please install Firebase CLI: npm install -g firebase-tools"
    exit 1
fi

# Check Firebase CLI version
FIREBASE_VERSION=$(firebase --version)
echo "Firebase CLI version: $FIREBASE_VERSION"

# Check if user is logged in
if ! firebase login:list &> /dev/null; then
    echo "Not logged in to Firebase. Please run: firebase login"
    exit 1
fi

# Deploy indexes
echo ""
echo "Deploying indexes from firestore.indexes.json..."
firebase deploy --only firestore:indexes

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Indexes deployed successfully!"
    echo "Note: Index creation may take a few minutes. Check Firebase Console for status."
else
    echo ""
    echo "✗ Failed to deploy indexes. Check the error above."
    exit 1
fi

