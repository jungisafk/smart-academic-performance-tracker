package com.smartacademictracker.data.security

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityConfiguration @Inject constructor() {
    
    // Password security settings
    val passwordMinLength = 8
    val passwordMaxLength = 128
    val passwordRequireUppercase = true
    val passwordRequireLowercase = true
    val passwordRequireNumbers = true
    val passwordRequireSpecialChars = true
    val passwordMaxAttempts = 5
    val passwordLockoutDuration = 30 * 60 * 1000L // 30 minutes in milliseconds
    
    // Session security settings
    val sessionTimeout = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
    val maxConcurrentSessions = 3
    val sessionInactivityTimeout = 2 * 60 * 60 * 1000L // 2 hours in milliseconds
    
    // Grade security settings
    val maxGradeValue = 100.0
    val minGradeValue = 0.0
    val gradeDecimalPlaces = 2
    val gradeUpdateCooldown = 5 * 60 * 1000L // 5 minutes in milliseconds
    
    // Rate limiting settings
    val maxRequestsPerMinute = 60
    val maxRequestsPerHour = 1000
    val maxRequestsPerDay = 10000
    
    // Data access settings
    val maxBulkOperationSize = 100
    val maxQueryResults = 1000
    val queryTimeout = 30 * 1000L // 30 seconds
    
    // Audit settings
    val auditRetentionDays = 365
    val auditLogLevel = AuditLogLevel.INFO
    val auditSensitiveData = false
    
    // Encryption settings
    val encryptSensitiveData = true
    val encryptionAlgorithm = "AES-256-GCM"
    
    // Network security settings
    val requireHTTPS = true
    val allowInsecureConnections = false
    val certificatePinning = true
    
    // Input validation settings
    val maxStringLength = 1000
    val maxArraySize = 100
    val maxObjectDepth = 10
    
    // File upload settings
    val maxFileSize = 10 * 1024 * 1024L // 10MB
    val allowedFileTypes = listOf("pdf", "doc", "docx", "txt", "jpg", "jpeg", "png")
    val scanUploadsForMalware = true
    
    // API security settings
    val requireAPIKey = true
    val apiKeyRotationDays = 90
    val maxAPIKeyUsage = 10000
    
    // Database security settings
    val enableQueryLogging = false
    val enableSlowQueryLogging = true
    val slowQueryThreshold = 1000L // 1 second
    val maxConnectionPoolSize = 20
    
    // Monitoring settings
    val enableSecurityMonitoring = true
    val alertOnSuspiciousActivity = true
    val alertOnFailedLogins = true
    val alertOnDataBreaches = true
    
    // Backup security settings
    val encryptBackups = true
    val backupRetentionDays = 30
    val backupVerification = true
    
    // Compliance settings
    val enableGDPRCompliance = true
    val enableDataAnonymization = true
    val enableRightToErasure = true
    val enableDataPortability = true
    
    // Multi-factor authentication settings
    val enableMFA = true
    val mfaRequiredForAdmins = true
    val mfaRequiredForTeachers = false
    val mfaRequiredForStudents = false
    val mfaBackupCodes = 10
    
    // Account security settings
    val accountLockoutThreshold = 5
    val accountLockoutDuration = 15 * 60 * 1000L // 15 minutes
    val passwordHistoryCount = 5
    val passwordExpirationDays = 90
    
    // Data classification settings
    val enableDataClassification = true
    val sensitiveDataFields = listOf(
        "ssn", "socialSecurityNumber", "taxId", "bankAccount", 
        "creditCard", "passport", "driverLicense"
    )
    
    // Privacy settings
    val enableDataMinimization = true
    val enablePurposeLimitation = true
    val enableStorageLimitation = true
    val enableAccuracy = true
    
    // Incident response settings
    val enableIncidentResponse = true
    val incidentResponseTeam = listOf("admin@school.edu", "security@school.edu")
    val incidentEscalationMinutes = 15
    val incidentNotificationChannels = listOf("email", "sms", "slack")
    
    // Threat detection settings
    val enableThreatDetection = true
    val threatDetectionSensitivity = ThreatDetectionSensitivity.MEDIUM
    val enableBehavioralAnalysis = true
    val enableAnomalyDetection = true
    
    // Compliance reporting settings
    val enableComplianceReporting = true
    val complianceReportFrequency = ComplianceReportFrequency.MONTHLY
    val complianceReportRecipients = listOf("compliance@school.edu")
    
    // Security training settings
    val enableSecurityTraining = true
    val trainingRequiredForAdmins = true
    val trainingRequiredForTeachers = true
    val trainingRequiredForStudents = false
    val trainingCompletionDeadline = 30 // days
    
    // Vulnerability management settings
    val enableVulnerabilityScanning = true
    val vulnerabilityScanFrequency = VulnerabilityScanFrequency.WEEKLY
    val vulnerabilitySeverityThreshold = VulnerabilitySeverity.MEDIUM
    
    // Data loss prevention settings
    val enableDLP = true
    val dlpSensitivityLevel = DLPSensitivityLevel.MEDIUM
    val dlpActionOnViolation = DLPAction.BLOCK
    
    // Security awareness settings
    val enableSecurityAwareness = true
    val awarenessTrainingFrequency = AwarenessTrainingFrequency.QUARTERLY
    val phishingSimulationFrequency = PhishingSimulationFrequency.MONTHLY
}

enum class AuditLogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

enum class ThreatDetectionSensitivity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class ComplianceReportFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    ANNUALLY
}

enum class VulnerabilityScanFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY
}

enum class VulnerabilitySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class DLPSensitivityLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class DLPAction {
    LOG,
    WARN,
    BLOCK,
    QUARANTINE
}

enum class AwarenessTrainingFrequency {
    MONTHLY,
    QUARTERLY,
    SEMI_ANNUALLY,
    ANNUALLY
}

enum class PhishingSimulationFrequency {
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    SEMI_ANNUALLY
}
