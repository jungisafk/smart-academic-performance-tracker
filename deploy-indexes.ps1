# PowerShell script to deploy Firestore indexes using Firebase CLI
# This script will deploy the indexes defined in firestore.indexes.json

Write-Host "Deploying Firestore indexes..." -ForegroundColor Cyan

# Check if Firebase CLI is installed
try {
    $firebaseVersion = firebase --version 2>&1
    Write-Host "Firebase CLI version: $firebaseVersion" -ForegroundColor Green
} catch {
    Write-Host "Error: Firebase CLI is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Firebase CLI: npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

# Check if user is logged in
try {
    $user = firebase login:list 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Not logged in to Firebase. Please run: firebase login" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "Error checking Firebase login status" -ForegroundColor Red
    exit 1
}

# Deploy indexes
Write-Host "`nDeploying indexes from firestore.indexes.json..." -ForegroundColor Cyan
firebase deploy --only firestore:indexes

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✓ Indexes deployed successfully!" -ForegroundColor Green
    Write-Host "Note: Index creation may take a few minutes. Check Firebase Console for status." -ForegroundColor Yellow
} else {
    Write-Host "`n✗ Failed to deploy indexes. Check the error above." -ForegroundColor Red
    exit 1
}

