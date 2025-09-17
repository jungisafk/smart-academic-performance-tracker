package com.smartacademictracker.presentation.admin;

import com.smartacademictracker.data.repository.SubjectRepository;
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
public final class AdminSubjectsViewModel_Factory implements Factory<AdminSubjectsViewModel> {
  private final Provider<SubjectRepository> subjectRepositoryProvider;

  public AdminSubjectsViewModel_Factory(Provider<SubjectRepository> subjectRepositoryProvider) {
    this.subjectRepositoryProvider = subjectRepositoryProvider;
  }

  @Override
  public AdminSubjectsViewModel get() {
    return newInstance(subjectRepositoryProvider.get());
  }

  public static AdminSubjectsViewModel_Factory create(
      Provider<SubjectRepository> subjectRepositoryProvider) {
    return new AdminSubjectsViewModel_Factory(subjectRepositoryProvider);
  }

  public static AdminSubjectsViewModel newInstance(SubjectRepository subjectRepository) {
    return new AdminSubjectsViewModel(subjectRepository);
  }
}
