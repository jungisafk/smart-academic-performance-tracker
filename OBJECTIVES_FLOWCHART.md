# System Objectives Flowchart

## Overview
This document maps the system objectives to their implementation across Admin, Teacher, and Student sides.

---

## Objective 1: Teacher Grade Input with Admin Validation

**Goal:** Teachers input Prelim, Midterm, and Final grades → Admin monitors and validates for accuracy and authenticity.

```mermaid
flowchart TD
    A[Teacher: Select Subject] --> B[Load Students & Grades]
    B --> C{Grade Locked?}
    C -->|No| D[Input Grades]
    C -->|Yes| E[Request Edit Permission]
    
    D --> D1[Enter Prelim Grade]
    D --> D2[Enter Midterm Grade]
    D --> D3[Enter Final Grade]
    
    D1 --> F[Save to Firestore]
    D2 --> F
    D3 --> F
    
    E --> E1[Set editRequested = true]
    E1 --> E2[Admin Notification]
    
    E2 --> G[Admin: Grade Edit Requests]
    G --> G1{Review Request}
    G1 -->|Approve| H[Unlock Grade]
    G1 -->|Reject| I[Keep Locked]
    
    H --> D
    F --> J[Real-time Update]
    J --> K[Admin: Grade Status Monitoring]
    
    K --> K1[View Completion Status]
    K1 --> K2[Course → Subject → Section]
    K2 --> K3[Monitor Grade Accuracy]
    K3 --> K4{Validation Needed?}
    K4 -->|Yes| L[Admin Review]
    K4 -->|No| M[Status: Validated]
    
    L --> L1[Check Grade Authenticity]
    L1 --> L2[Verify Calculations]
    L2 --> M
    
    style D fill:#c8e6c9
    style G fill:#fff9c4
    style H fill:#b2ebf2
    style K fill:#ffccbc
    style M fill:#c8e6c9
```

**Key Features:**
- **Teacher Side:** Grade input with lock/unlock mechanism
- **Admin Side:** Real-time grade edit request monitoring
- **Admin Side:** Grade status tracking (Course → Subject → Section)
- **Validation:** Admin can review and validate grade accuracy
- **Real-time:** Live updates when grades are saved

---

## Objective 2: Real-time Student Performance Tracking

**Goal:** Students view and track their academic performance in real-time.

```mermaid
flowchart TD
    A[Student: Login] --> B[Load Dashboard Data]
    B --> C{Cache Valid?}
    C -->|Yes| D[Display Cached Data]
    C -->|No| E[Fetch from Firestore]
    E --> F[Update Cache]
    F --> D
    
    D --> G[Home Tab: Overview]
    D --> H[Grades Tab: View Grades]
    D --> I[Grades Tab: Analytics]
    
    G --> G1[Enrolled Subjects Count]
    G --> G2[Total Subjects Passed]
    G --> G3[Performance Snapshot]
    
    H --> H1[Real-time Listener]
    H1 --> H2[Load Grade Aggregates]
    H2 --> H3[Display by Subject]
    H3 --> H4[Show Period Grades]
    H4 --> H5[Show Final Average]
    
    I --> I1[Load Analytics Data]
    I1 --> I2[Calculate Statistics]
    I2 --> I3[Generate Charts]
    I3 --> I4[Display Performance]
    
    J[Teacher: Save Grade] --> K[Firestore Update]
    K --> L[Real-time Listener Trigger]
    L --> M[Student UI Update]
    M --> H2
    M --> I1
    
    style D fill:#c8e6c9
    style H1 fill:#b2ebf2
    style I3 fill:#fff9c4
    style M fill:#e1bee7
```

**Key Features:**
- **Real-time Updates:** Firestore listeners update student UI immediately
- **Performance Tracking:** Overview cards show current status
- **Grade Viewing:** All period grades (Prelim, Midterm, Final) visible
- **Analytics:** Visual insights updated in real-time
- **Cache Strategy:** Fast loading with background refresh

---

## Objective 3: Automated Final Grade Computation

**Goal:** System automatically computes final grade based on teacher-inputted grades.

```mermaid
flowchart TD
    A[Teacher: Input Prelim] --> D[GradeCalculationEngine]
    B[Teacher: Input Midterm] --> D
    C[Teacher: Input Final] --> D
    
    D --> E[Calculate Weighted Average]
    E --> E1[Prelim × 0.30]
    E --> E2[Midterm × 0.30]
    E --> E3[Final × 0.40]
    
    E1 --> F[Sum: Total Average]
    E2 --> F
    E3 --> F
    
    F --> G{Decimal = .5?}
    G -->|Yes| H[Round Up]
    G -->|No| I[Keep As Is]
    
    H --> J[Final Average]
    I --> J
    
    J --> K[Save to StudentGradeAggregate]
    K --> L[Update Firestore]
    L --> M[Real-time Update]
    M --> N[Student Sees Final Grade]
    
    O[Grade Status Determination] --> P{Check Final Average}
    P -->|≥ 90| Q[Status: PASSING]
    P -->|≥ 80| Q
    P -->|≥ 70| R[Status: AT_RISK]
    P -->|< 70| S[Status: FAILING]
    
    J --> O
    Q --> T[Update Student UI]
    R --> T
    S --> T
    
    style D fill:#fff9c4
    style F fill:#c8e6c9
    style H fill:#b2ebf2
    style J fill:#e1bee7
    style T fill:#ffccbc
```

**Calculation Formula:**
```
Final Average = (Prelim × 0.30) + (Midterm × 0.30) + (Final × 0.40)
```

**Rounding Rule:**
- If decimal = 0.5 → Round up (e.g., 74.5 → 75, 79.5 → 80)

**Automation Points:**
- **Automatic Calculation:** Triggered when any period grade is saved
- **Status Update:** Grade status (PASSING/AT_RISK/FAILING) calculated automatically
- **Real-time Sync:** Final grade appears immediately in student view
- **Aggregate Creation:** System creates/updates StudentGradeAggregate automatically

---

## Objective 4: Visual Insights for Academic Performance

**Goal:** Present graphs and status indicators to help students understand their academic performance trends.

```mermaid
flowchart TD
    A[Student: View Analytics] --> B[Load Grade Aggregates]
    B --> C[Calculate Statistics]
    C --> D[Generate Visual Insights]
    
    D --> E[Performance Distribution Chart]
    D --> F[Grade Trend Chart]
    D --> G[Subject Comparison Chart]
    D --> H[Status Indicators]
    
    E --> E1[Passing Count]
    E --> E2[At-Risk Count]
    E --> E3[Failing Count]
    E1 --> E4[Bar Chart Visualization]
    E2 --> E4
    E3 --> E4
    
    F --> F1[Prelim Grade Point]
    F --> F2[Midterm Grade Point]
    F --> F3[Final Grade Point]
    F1 --> F4[Line Chart: Grade Trend]
    F2 --> F4
    F3 --> F4
    
    G --> G1[Subject 1 Average]
    G --> G2[Subject 2 Average]
    G --> G3[Subject N Average]
    G1 --> G4[Comparison Chart]
    G2 --> G4
    G3 --> G4
    
    H --> H1[Overall Average Badge]
    H --> H2[Total Subjects Passed]
    H --> H3[Status Color Indicators]
    H3 --> H4[Green: Passing]
    H3 --> H5[Orange: At-Risk]
    H3 --> H6[Red: Failing]
    
    I[Home Tab] --> I1[Overview Cards]
    I1 --> I2[Enrolled Subjects Card]
    I1 --> I3[Total Passed Card]
    I1 --> I4[Performance Snapshot]
    
    I4 --> I5[Overall Average Display]
    I4 --> I6[Status Count Summary]
    
    style D fill:#fff9c4
    style E4 fill:#c8e6c9
    style F4 fill:#b2ebf2
    style G4 fill:#e1bee7
    style H3 fill:#ffccbc
```

**Visual Components:**

### Charts & Graphs:
1. **Performance Distribution Chart**
   - Bar chart showing Passing/At-Risk/Failing counts
   - Percentage breakdown per category

2. **Grade Trend Chart**
   - Line chart showing Prelim → Midterm → Final progression
   - Per-subject grade trends

3. **Subject Comparison Chart**
   - Comparison of average grades across subjects
   - Visual ranking of subject performance

### Status Indicators:
1. **Overview Cards**
   - Enrolled Subjects count
   - Total Subjects Passed count
   - Color-coded status badges

2. **Performance Snapshot**
   - Overall average grade
   - Passing/At-Risk/Failing subject counts
   - Quick status overview

3. **Grade Status Colors**
   - 🟢 Green: Passing (≥ 80)
   - 🟠 Orange: At-Risk (70-79)
   - 🔴 Red: Failing (< 70)

**Real-time Updates:**
- All charts and indicators update automatically when grades change
- Visual insights reflect current academic status
- Trend charts show performance progression over time

---

## Complete System Flow

```mermaid
flowchart TD
    A[Teacher Inputs Grades] --> B[Automated Calculation]
    B --> C[Final Grade Computed]
    C --> D[Admin Validation]
    D --> E[Real-time Update]
    E --> F[Student Views Performance]
    F --> G[Visual Insights Displayed]
    
    A --> A1[Prelim: 30%]
    A --> A2[Midterm: 30%]
    A --> A3[Final: 40%]
    
    B --> B1[Weighted Average]
    B --> B2[Round .5 Values]
    
    D --> D1[Grade Status Monitoring]
    D --> D2[Edit Request Approval]
    
    E --> E1[Firestore Listener]
    E --> E2[Cache Update]
    
    F --> F1[Home: Overview]
    F --> F2[Grades: View Details]
    F --> F3[Analytics: Charts]
    
    G --> G1[Distribution Charts]
    G --> G2[Trend Charts]
    G --> G3[Status Indicators]
    
    style A fill:#e1bee7
    style B fill:#fff9c4
    style C fill:#c8e6c9
    style D fill:#ffccbc
    style E fill:#b2ebf2
    style F fill:#c8e6c9
    style G fill:#e1bee7
```

---

## Technology Implementation

### Real-time Updates
- **Firestore Listeners:** Snapshot listeners for live data synchronization
- **StateFlow:** Reactive state management for UI updates
- **Cache Strategy:** 5-minute cache with background refresh

### Grade Calculation
- **GradeCalculationEngine:** Centralized calculation logic
- **Automatic Triggers:** Calculation on grade save
- **Status Determination:** Automatic grade status assignment

### Visual Components
- **MPAndroidChart:** Chart library for graphs
- **Jetpack Compose:** Modern UI framework
- **Material Design:** Consistent visual indicators

---

*Last Updated: November 2025*

