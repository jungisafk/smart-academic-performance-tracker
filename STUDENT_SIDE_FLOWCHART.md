# Student Side System Flowchart

## Overview
Simplified flowchart of the student-side system architecture and key processes.

---

## 1. System Entry & Navigation

```mermaid
flowchart TD
    A[Student Login] --> B{Authentication}
    B -->|Success| C[StudentMainScreen]
    B -->|Failure| A
    C --> D[Bottom Navigation]
    D --> E[Home]
    D --> F[Subjects]
    D --> G[Grades]
    D --> H[Profile]
    
    F --> F1[My Subjects / Apply for Subjects]
    G --> G1[View Grades / Analytics]
    
    style A fill:#e1f5ff
    style C fill:#c8e6c9
    style D fill:#fff9c4
```

---

## 2. Data Flow

```mermaid
flowchart LR
    A[Firestore] --> B[Repository]
    B --> C[StudentDataCache]
    C --> D[ViewModel]
    D --> E[UI Screen]
    
    C -.Cache Hit.-> D
    C -.Cache Miss.-> B
    
    style A fill:#ffccbc
    style C fill:#fff9c4
    style D fill:#c8e6c9
    style E fill:#e1bee7
```

**Cache:** 5 minutes | **Types:** Enrollments, Grades, Grade Aggregates, Subjects, Applications

---

## 3. Home Tab

```mermaid
flowchart TD
    A[StudentHomeScreen] --> B{Cache Valid?}
    B -->|Yes| C[Display Cached Data]
    B -->|No| D[Fetch from Firestore]
    D --> E[Update Cache]
    E --> C
    C --> F[Welcome Banner]
    C --> G[Overview Cards]
    C --> H[Performance Snapshot]
    
    G --> G1[Enrolled Subjects]
    G --> G2[Total Subjects Passed]
    
    H --> H1[Overall Average]
    H --> H2[Passing/At-Risk/Failing Count]
    
    style B fill:#fff9c4
    style C fill:#c8e6c9
```

---

## 4. Subjects Tab

```mermaid
flowchart TD
    A[Subjects Tab] --> B[My Subjects]
    A --> C[Apply for Subjects]
    
    B --> B1[View Enrolled Subjects]
    B1 --> B2[Subject Details]
    
    C --> C1[View Available Subjects]
    C1 --> C2[Filter by Year Level]
    C2 --> C3[Apply for Subject]
    C3 --> C4[Submit Application]
    C4 --> C5[Track Application Status]
    
    style B2 fill:#c8e6c9
    style C4 fill:#b2ebf2
```

**Subject Filtering:** Only shows subjects matching student's year level/course

---

## 5. Grades Tab

```mermaid
flowchart TD
    A[Grades Tab] --> B[View Grades]
    A --> C[Analytics]
    
    B --> B1[View All Grades]
    B1 --> B2[Subject Breakdown]
    B2 --> B3[Period Grades: Prelim/Midterm/Final]
    B3 --> B4[Final Average]
    
    C --> C1[Performance Charts]
    C1 --> C2[Grade Trend]
    C1 --> C3[Subject Comparison]
    C1 --> C4[Performance Distribution]
    
    style B4 fill:#c8e6c9
    style C1 fill:#fff9c4
```

**Grade Calculation:** Prelim (30%) + Midterm (30%) + Final (40%) | .5 values round up

---

## 6. Key Processes

### Subject Application
```
View Available → Filter by Year Level → Apply → Submit → Teacher Review → Approval/Rejection
```

### Grade Viewing
```
Load Enrollments → Load Grades → Calculate Aggregates → Display by Subject → Show Period Grades
```

### Analytics
```
Load Grade Aggregates → Calculate Statistics → Generate Charts → Display Performance
```

**Analytics Metrics:**
- Overall average grade
- Total subjects passed
- Passing/At-risk/Failing subjects count
- Grade trends per subject
- Subject performance comparison

---

## 7. Grade Calculation

```mermaid
flowchart TD
    A[Prelim Grade] --> D[Final Average]
    B[Midterm Grade] --> D
    C[Final Grade] --> D
    
    D --> E{Decimal = .5?}
    E -->|Yes| F[Round Up]
    E -->|No| G[Keep As Is]
    
    F --> H[Final Grade]
    G --> H
    
    style D fill:#fff9c4
    style F fill:#c8e6c9
    style H fill:#b2ebf2
```

**Formula:** (Prelim × 0.30) + (Midterm × 0.30) + (Final × 0.40)  
**Rounding:** 74.5 → 75, 79.5 → 80, etc.

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

