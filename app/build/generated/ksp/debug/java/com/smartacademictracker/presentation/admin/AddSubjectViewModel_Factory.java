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
public final class AddSubjectViewModel_Factory implements Factory<AddSubjectViewModel> {
  private final Provider<SubjectRepository> subjectRepositoryProvider;

  public AddSubjectViewModel_Factory(Provider<SubjectRepository> subjectRepositoryProvider) {
    this.subjectRepositoryProvider = subjectRepositoryProvider;
  }

  @Override
  public AddSubjectViewModel get() {
    return newInstance(subjectRepositoryProvider.get());
  }

  public static AddSubjectViewModel_Factory create(
      Provider<SubjectRepository> subjectRepositoryProvider) {
    return new AddSubjectViewModel_Factory(subjectRepositoryProvider);
  }

  public static AddSubjectViewModel newInstance(SubjectRepository subjectRepository) {
    return new AddSubjectViewModel(subjectRepository);
  }
}
