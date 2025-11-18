# Teacher Side System Flowchart

## Overview
Simplified flowchart of the teacher-side system architecture and key processes.

---

## 1. System Entry & Navigation

```mermaid
flowchart TD
    A[Teacher Login] --> B{Authentication}
    B -->|Success| C[TeacherMainScreen]
    B -->|Failure| A
    C --> D[Bottom Navigation]
    D --> E[Home]
    D --> F[My Subjects]
    D --> G[Student Management]
    D --> H[Profile]
    
    F --> F1[My Subjects / Applied / Available]
    G --> G1[Sections / Applications / Analytics]
    
    style A fill:#e1f5ff
    style C fill:#c8e6c9
    style D fill:#fff9c4
```

---

## 2. Data Flow

```mermaid
flowchart LR
    A[Firestore] --> B[Repository]
    B --> C[TeacherDataCache]
    C --> D[ViewModel]
    D --> E[UI Screen]
    
    C -.Cache Hit.-> D
    C -.Cache Miss.-> B
    
    style A fill:#ffccbc
    style C fill:#fff9c4
    style D fill:#c8e6c9
    style E fill:#e1bee7
```

**Cache:** 5 minutes | **Types:** Subjects, Enrollments, Applications, Grades, Section Assignments

---

## 3. Home Tab

```mermaid
flowchart TD
    A[TeacherHomeScreen] --> B{Cache Valid?}
    B -->|Yes| C[Display Cached Data]
    B -->|No| D[Fetch from Firestore]
    D --> E[Update Cache]
    E --> C
    C --> F[Welcome Banner]
    C --> G[Subject Overview]
    
    style B fill:#fff9c4
    style C fill:#c8e6c9
```

---

## 4. My Subjects Tab

```mermaid
flowchart TD
    A[My Subjects] --> B[My Subjects Tab]
    A --> C[Applied Tab]
    A --> D[Available Tab]
    
    B --> B1[View Assigned Subjects]
    B1 --> B2[Grade Input]
    
    C --> C1[View Application Status]
    
    D --> D1[Apply for Subject]
    D1 --> D2[Submit Application]
    
    style B2 fill:#c8e6c9
    style D2 fill:#b2ebf2
```

---

## 5. Student Management Tab

```mermaid
flowchart TD
    A[Student Management] --> B[Sections]
    A --> C[Applications]
    A --> D[Analytics]
    
    B --> B1[View Students]
    B1 --> B2[Remove Student]
    
    C --> C1{Application Status}
    C1 -->|Pending| C2[Approve/Reject]
    C1 -->|Approved| C3[Create Enrollment]
    
    D --> D1[Performance Charts]
    D1 --> D2[Grade Distribution]
    D1 --> D3[Class Statistics]
    
    style C2 fill:#c8e6c9
    style D1 fill:#fff9c4
```

---

## 6. Grade Input Process

```mermaid
flowchart TD
    A[Select Subject] --> B[Load Students & Grades]
    B --> C{Grade Locked?}
    C -->|Yes| D[Request Edit Permission]
    C -->|No| E[Input Grades]
    
    D --> D1[Admin Approval]
    D1 -->|Approve| E
    D1 -->|Reject| F[Remains Locked]
    
    E --> E1[Save to Firestore]
    E1 --> E2[Real-time Update]
    E2 --> E3[Update Cache]
    
    style C fill:#fff9c4
    style E fill:#c8e6c9
    style E2 fill:#b2ebf2
```

**Grade Periods:** Prelim (30%) | Midterm (30%) | Final (40%) | Final Average (calculated)

---

## 7. Key Processes

### Subject Application
```
View Available → Check Sections → Apply → Submit → Admin Review → Approval/Rejection
```

### Student Application Approval
```
Application Received → Review → Approve/Reject → Create Enrollment → Notify
```

### Analytics
```
Load Subjects → Load Grades → Calculate Aggregates → Generate Charts → Display
```

**Analytics Metrics:**
- Average grade per subject
- Passing rate
- Grade distribution (75+, <75)
- Class comparison charts

---

## 8. Technology Stack

- **UI:** Jetpack Compose
- **Architecture:** MVVM
- **DI:** Hilt
- **Database:** Firebase Firestore
- **Cache:** In-memory (5 min)
- **Real-time:** Firestore Listeners

---

*Last Updated: November 2025*
