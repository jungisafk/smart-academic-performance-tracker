package com.smartacademictracker.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.local.database.AppDatabase
import com.smartacademictracker.data.network.NetworkMonitor
import com.smartacademictracker.data.sync.GradeSyncManager
import com.smartacademictracker.data.sync.SyncScheduler
import com.smartacademictracker.data.repository.OfflineGradeRepository
import com.smartacademictracker.data.validation.GradeValidationService
import com.smartacademictracker.data.audit.SecurityAuditLogger
import com.smartacademictracker.data.integrity.DataIntegrityChecker
import com.smartacademictracker.data.security.SecurityConfiguration
import com.smartacademictracker.data.migration.DatabaseMigrationService
import com.smartacademictracker.data.notification.LocalNotificationService
import com.smartacademictracker.data.notification.NotificationTemplateService
import com.smartacademictracker.data.notification.NotificationSenderService
import com.smartacademictracker.data.repository.NotificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }
    
    @Provides
    @Singleton
    fun provideGradeSyncManager(
        database: AppDatabase,
        gradeRepository: com.smartacademictracker.data.repository.GradeRepository,
        networkMonitor: NetworkMonitor
    ): GradeSyncManager {
        return GradeSyncManager(database, gradeRepository, networkMonitor)
    }
    
    @Provides
    @Singleton
    fun provideSyncScheduler(
        @ApplicationContext context: Context,
        networkMonitor: NetworkMonitor
    ): SyncScheduler {
        return SyncScheduler(context, networkMonitor)
    }
    
    @Provides
    @Singleton
    fun provideOfflineGradeRepository(
        database: AppDatabase,
        gradeRepository: com.smartacademictracker.data.repository.GradeRepository,
        networkMonitor: NetworkMonitor,
        gradeSyncManager: GradeSyncManager
    ): OfflineGradeRepository {
        return OfflineGradeRepository(database, gradeRepository, networkMonitor, gradeSyncManager)
    }
    
    @Provides
    @Singleton
    fun provideGradeValidationService(): GradeValidationService {
        return GradeValidationService()
    }
    
    @Provides
    @Singleton
    fun provideSecurityAuditLogger(firestore: FirebaseFirestore): SecurityAuditLogger {
        return SecurityAuditLogger(firestore)
    }
    
    @Provides
    @Singleton
    fun provideDataIntegrityChecker(firestore: FirebaseFirestore): DataIntegrityChecker {
        return DataIntegrityChecker(firestore)
    }
    
    @Provides
    @Singleton
    fun provideSecurityConfiguration(): SecurityConfiguration {
        return SecurityConfiguration()
    }
    
    @Provides
    @Singleton
    fun provideDatabaseMigrationService(firestore: FirebaseFirestore): DatabaseMigrationService {
        return DatabaseMigrationService(firestore)
    }
    
    @Provides
    @Singleton
    fun provideLocalNotificationService(@ApplicationContext context: Context): LocalNotificationService {
        return LocalNotificationService(context)
    }
    
    @Provides
    @Singleton
    fun provideNotificationTemplateService(): NotificationTemplateService {
        return NotificationTemplateService()
    }
    
    @Provides
    @Singleton
    fun provideNotificationRepository(firestore: FirebaseFirestore): NotificationRepository {
        return NotificationRepository(firestore)
    }
    
    @Provides
    @Singleton
    fun provideNotificationSenderService(
        notificationRepository: NotificationRepository,
        localNotificationService: LocalNotificationService,
        templateService: NotificationTemplateService,
        userRepository: com.smartacademictracker.data.repository.UserRepository
    ): NotificationSenderService {
        return NotificationSenderService(notificationRepository, localNotificationService, templateService, userRepository)
    }
}
