package com.smartacademictracker.data.model

enum class Semester(val displayName: String, val isSummer: Boolean = false) {
    FIRST_SEMESTER("1st Semester", false),
    SECOND_SEMESTER("2nd Semester", false),
    SUMMER_CLASS("Summer Class", true)
}
