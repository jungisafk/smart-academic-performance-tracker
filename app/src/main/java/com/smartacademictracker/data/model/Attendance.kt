package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

data class Attendance(
    @DocumentId
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val date: Long = System.currentTimeMillis(),
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val sessionType: SessionType = SessionType.REGULAR,
    val notes: String = "",
    val recordedBy: String = "",
    val recordedAt: Long = System.currentTimeMillis()
)

enum class AttendanceStatus(val displayName: String, val shortName: String) {
    PRESENT("Present", "P"),
    ABSENT("Absent", "A"),
    LATE("Late", "L"),
    EXCUSED("Excused", "E"),
    TARDY("Tardy", "T")
}

enum class SessionType(val displayName: String) {
    REGULAR("Regular Class"),
    LABORATORY("Laboratory"),
    LECTURE("Lecture"),
    TUTORIAL("Tutorial"),
    EXAM("Examination")
}

data class AttendanceSummary(
    val studentId: String = "",
    val studentName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val totalSessions: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val lateCount: Int = 0,
    val excusedCount: Int = 0,
    val attendanceRate: Double = 0.0,
    val lastAttendance: Long = 0L
)
