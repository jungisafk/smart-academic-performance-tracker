package com.smartacademictracker.presentation.admin;

import com.smartacademictracker.data.repository.TeacherApplicationRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class AdminApplicationsViewModel_Factory implements Factory<AdminApplicationsViewModel> {
  private final Provider<TeacherApplicationRepository> teacherApplicationRepositoryProvider;

  public AdminApplicationsViewModel_Factory(
      Provider<TeacherApplicationRepository> teacherApplicationRepositoryProvider) {
    this.teacherApplicationRepositoryProvider = teacherApplicationRepositoryProvider;
  }

  @Override
  public AdminApplicationsViewModel get() {
    return newInstance(teacherApplicationRepositoryProvider.get());
  }

  public static AdminApplicationsViewModel_Factory create(
      Provider<TeacherApplicationRepository> teacherApplicationRepositoryProvider) {
    return new AdminApplicationsViewModel_Factory(teacherApplicationRepositoryProvider);
  }

  public static AdminApplicationsViewModel newInstance(
      TeacherApplicationRepository teacherApplicationRepository) {
    return new AdminApplicationsViewModel(teacherApplicationRepository);
  }
}
