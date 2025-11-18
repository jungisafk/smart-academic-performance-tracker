# Admin Side System Flowchart

## Overview
Simplified flowchart of the admin-side system architecture and key processes.

---

## 1. System Entry & Navigation

```mermaid
flowchart TD
    A[Admin Login] --> B{Authentication}
    B -->|Success| C[AdminMainScreen]
    B -->|Failure| A
    C --> D[Bottom Navigation]
    D --> E[Home]
    D --> F[User Management]
    D --> G[Academic Management]
    D --> H[Profile]
    
    style A fill:#e1f5ff
    style C fill:#c8e6c9
    style D fill:#fff9c4
```

---

## 2. Data Flow

```mermaid
flowchart LR
    A[Firestore] --> B[Repository]
    B --> C[AdminDataCache]
    C --> D[ViewModel]
    D --> E[UI Screen]
    
    C -.Cache Hit.-> D
    C -.Cache Miss.-> B
    
    style A fill:#ffccbc
    style C fill:#fff9c4
    style D fill:#c8e6c9
    style E fill:#e1bee7
```

**Cache:** 5 minutes | **Types:** Subjects, Enrollments, Users, Applications, Courses, Year Levels

---

## 3. Home Tab

```mermaid
flowchart TD
    A[AdminHomeScreen] --> B{Cache Valid?}
    B -->|Yes| C[Display Cached Data]
    B -->|No| D[Fetch from Firestore]
    D --> E[Update Cache]
    E --> C
    C --> F[System Overview]
    C --> G[Quick Actions]
    
    style B fill:#fff9c4
    style C fill:#c8e6c9
```

**Overview:** Total Subjects, Students, Teachers, Enrollments, Pending Applications

---

## 4. User Management

```mermaid
flowchart TD
    A[User Management] --> B[Teacher Applications]
    A --> C[Student Applications]
    A --> D[Manage Users]
    A --> E[Pre-Registered Users]
    
    B --> B1{Status}
    B1 -->|Pending| B2[Approve/Reject]
    B1 -->|Approved| B3[Create Account]
    
    C --> C1{Status}
    C1 -->|Pending| C2[Approve/Reject]
    C1 -->|Approved| C3[Create Enrollment]
    
    E --> E1[Bulk Import CSV]
    
    style B2 fill:#c8e6c9
    style C2 fill:#c8e6c9
    style E1 fill:#ffccbc
```

---

## 5. Academic Management

```mermaid
flowchart TD
    A[Academic Management] --> B[Academic Structure]
    A --> C[Grade Status]
    A --> D[Grade Edit Requests]
    A --> E[Academic Periods]
    
    B --> B1[Course/Year Level/Subject CRUD]
    
    C --> C1[View Completion Status]
    C1 --> C2[Course → Subject → Section]
    
    D --> D1{Request Status}
    D1 -->|Pending| D2[Approve/Reject]
    D1 -->|Approved| D3[Unlock Grade]
    
    style B1 fill:#c8e6c9
    style C1 fill:#fff9c4
    style D2 fill:#b2ebf2
```

---

## 6. Key Processes

### Bulk Import
```
CSV File → Parse → Validate → Batch Write → Update Cache → Display Results
```

### Grade Edit Request
```
Teacher Request → Real-time Listener → Admin Review → Approve/Reject → Unlock Grade
```

### Application Approval
```
Application → Admin Review → Approve/Reject → Create Account/Enrollment → Notify
```

---

## 7. Technology Stack

- **UI:** Jetpack Compose
- **Architecture:** MVVM
- **DI:** Hilt
- **Database:** Firebase Firestore
- **Cache:** In-memory (5 min)
- **Real-time:** Firestore Listeners

---

*Last Updated: November 2025*
