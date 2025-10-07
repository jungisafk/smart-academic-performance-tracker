package com.smartacademictracker.data.model

data class GradeCurve(
    val id: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val curveType: CurveType = CurveType.LINEAR,
    val adjustmentFactor: Double = 0.0,
    val targetAverage: Double = 0.0,
    val maxGrade: Double = 100.0,
    val minGrade: Double = 0.0,
    val appliedDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

enum class CurveType(val displayName: String, val description: String) {
    LINEAR("Linear Curve", "Adds a fixed amount to all grades"),
    PERCENTAGE("Percentage Curve", "Multiplies all grades by a percentage"),
    SQUARE_ROOT("Square Root Curve", "Applies square root curve (√(grade * 10))"),
    BELL_CURVE("Bell Curve", "Adjusts grades to fit a normal distribution"),
    TARGET_AVERAGE("Target Average", "Adjusts grades to reach a target average")
}

data class CurveApplication(
    val originalGrade: Double,
    val curvedGrade: Double,
    val adjustment: Double,
    val studentId: String,
    val studentName: String
)

data class CurveStatistics(
    val originalAverage: Double,
    val curvedAverage: Double,
    val originalStandardDeviation: Double,
    val curvedStandardDeviation: Double,
    val gradeDistribution: Map<String, Int>,
    val totalStudents: Int,
    val passingRate: Double
)
