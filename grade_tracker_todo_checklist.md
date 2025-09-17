# 📌 Grade Tracker App - To-Do Checklist

## 1. Setup
- [x] Install **Android Studio Ladybug+ (latest)**
- [x] Create new project with **Kotlin + Jetpack Compose template**
- [x] Configure **Gradle JVM 17, AGP 8.5.2, SDK 35**
- [x] Setup **libs.versions.toml** for dependency management

## 2. Firebase Setup
- [ ] Create Firebase project
- [ ] Enable **Authentication** (Email/Password, Google sign-in)
- [ ] Enable **Firestore**
- [ ] Enable **Crashlytics + Analytics**
- [ ] Download **google-services.json** and add to project
- [ ] Add `com.google.gms.google-services` plugin

## 3. Authentication
- [x] Build **login/signup screens** (Compose)
- [x] Implement **role-based user creation** (student/teacher/admin)
- [x] Persist session with Firebase Auth

## 4. Navigation & Roles
- [x] Setup **Navigation-Compose**
- [x] Role-based navigation graph:
  - Student → Dashboard + Grades
  - Teacher → Subject management + Grade input
  - Admin → Approve applications + Manage subjects

## 5. Firestore Integration
- [x] Create **User Repository** (CRUD)
- [x] Create **Subjects Repository**
- [x] Create **Grades Repository**
- [x] Link repositories to ViewModels

## 6. Features
### Student
- [ ] View enrolled subjects
- [ ] View grades per subject

### Teacher
- [ ] Apply for subjects
- [ ] Input grades (quiz, exam, activity)

### Admin
- [ ] Add subjects
- [ ] Approve/reject teacher applications

### Analytics
- [ ] Integrate Firebase Analytics events (logins, grades added)
- [ ] Teacher performance dashboard (optional)

## 7. UI & UX
- [x] Material3 theming
- [x] Light/Dark mode
- [x] Responsive layouts (phones, tablets)

## 8. Security
- [ ] Firestore Security Rules (role-based)
- [ ] Admin-only write for subjects
- [ ] Teachers can only update their own subjects

## 9. Testing
- [ ] Unit tests with JUnit & Coroutine Test
- [ ] Instrumentation tests with Espresso
- [ ] Firebase Test Lab for device testing

## 10. Deployment
- [ ] Generate signed APK/AAB
- [ ] Setup Firebase App Distribution (for testing)
- [ ] Publish to Google Play Store
