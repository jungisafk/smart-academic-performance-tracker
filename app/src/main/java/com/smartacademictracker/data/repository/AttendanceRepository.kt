package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.Attendance
import com.smartacademictracker.data.model.AttendanceStatus
import com.smartacademictracker.data.model.AttendanceSummary
import com.smartacademictracker.data.model.SessionType
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val attendanceCollection = firestore.collection("attendance")

    suspend fun recordAttendance(attendance: Attendance): Result<Attendance> {
        return try {
            val docRef = attendanceCollection.add(attendance).await()
            val createdAttendance = attendance.copy(id = docRef.id)
            attendanceCollection.document(docRef.id).set(createdAttendance).await()
            Result.success(createdAttendance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceBySubject(subjectId: String): Result<List<Attendance>> {
        return try {
            val snapshot = attendanceCollection
                .whereEqualTo("subjectId", subjectId)
                .orderBy("date")
                .get()
                .await()
            val attendance = snapshot.toObjects(Attendance::class.java)
            Result.success(attendance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceByStudent(studentId: String, subjectId: String): Result<List<Attendance>> {
        return try {
            val snapshot = attendanceCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subjectId", subjectId)
                .orderBy("date")
                .get()
                .await()
            val attendance = snapshot.toObjects(Attendance::class.java)
            Result.success(attendance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceByDate(subjectId: String, date: Long): Result<List<Attendance>> {
        return try {
            val startOfDay = date
            val endOfDay = date + (24 * 60 * 60 * 1000) - 1
            
            val snapshot = attendanceCollection
                .whereEqualTo("subjectId", subjectId)
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThanOrEqualTo("date", endOfDay)
                .get()
                .await()
            val attendance = snapshot.toObjects(Attendance::class.java)
            Result.success(attendance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAttendanceStatus(
        attendanceId: String,
        status: AttendanceStatus,
        notes: String = ""
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to status.name,
                "notes" to notes,
                "recordedAt" to System.currentTimeMillis()
            )
            attendanceCollection.document(attendanceId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceSummary(subjectId: String): Result<List<AttendanceSummary>> {
        return try {
            val attendance = getAttendanceBySubject(subjectId).getOrThrow()
            
            val summaries = attendance
                .groupBy { it.studentId }
                .map { (studentId, studentAttendance) ->
                    val studentName = studentAttendance.firstOrNull()?.studentName ?: ""
                    val totalSessions = studentAttendance.size
                    val presentCount = studentAttendance.count { it.status == AttendanceStatus.PRESENT }
                    val absentCount = studentAttendance.count { it.status == AttendanceStatus.ABSENT }
                    val lateCount = studentAttendance.count { it.status == AttendanceStatus.LATE }
                    val excusedCount = studentAttendance.count { it.status == AttendanceStatus.EXCUSED }
                    val attendanceRate = if (totalSessions > 0) {
                        (presentCount.toDouble() / totalSessions) * 100
                    } else 0.0
                    val lastAttendance = studentAttendance.maxOfOrNull { it.date } ?: 0L
                    
                    AttendanceSummary(
                        studentId = studentId,
                        studentName = studentName,
                        subjectId = subjectId,
                        subjectName = studentAttendance.firstOrNull()?.subjectName ?: "",
                        totalSessions = totalSessions,
                        presentCount = presentCount,
                        absentCount = absentCount,
                        lateCount = lateCount,
                        excusedCount = excusedCount,
                        attendanceRate = attendanceRate,
                        lastAttendance = lastAttendance
                    )
                }
                .sortedBy { it.studentName }
            
            Result.success(summaries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceSummaryForStudent(studentId: String, subjectId: String): Result<AttendanceSummary> {
        return try {
            val attendance = getAttendanceByStudent(studentId, subjectId).getOrThrow()
            
            val totalSessions = attendance.size
            val presentCount = attendance.count { it.status == AttendanceStatus.PRESENT }
            val absentCount = attendance.count { it.status == AttendanceStatus.ABSENT }
            val lateCount = attendance.count { it.status == AttendanceStatus.LATE }
            val excusedCount = attendance.count { it.status == AttendanceStatus.EXCUSED }
            val attendanceRate = if (totalSessions > 0) {
                (presentCount.toDouble() / totalSessions) * 100
            } else 0.0
            val lastAttendance = attendance.maxOfOrNull { it.date } ?: 0L
            
            val summary = AttendanceSummary(
                studentId = studentId,
                studentName = attendance.firstOrNull()?.studentName ?: "",
                subjectId = subjectId,
                subjectName = attendance.firstOrNull()?.subjectName ?: "",
                totalSessions = totalSessions,
                presentCount = presentCount,
                absentCount = absentCount,
                lateCount = lateCount,
                excusedCount = excusedCount,
                attendanceRate = attendanceRate,
                lastAttendance = lastAttendance
            )
            
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bulkRecordAttendance(
        subjectId: String,
        date: Long,
        attendanceRecords: List<Attendance>
    ): Result<List<Attendance>> {
        return try {
            val recordedAttendance = mutableListOf<Attendance>()
            
            for (record in attendanceRecords) {
                val attendance = record.copy(
                    subjectId = subjectId,
                    date = date
                )
                val result = recordAttendance(attendance)
                if (result.isSuccess) {
                    recordedAttendance.add(result.getOrThrow())
                }
            }
            
            Result.success(recordedAttendance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
