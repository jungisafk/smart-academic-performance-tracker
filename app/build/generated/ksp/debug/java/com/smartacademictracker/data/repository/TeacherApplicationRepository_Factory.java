package com.smartacademictracker.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class TeacherApplicationRepository_Factory implements Factory<TeacherApplicationRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  public TeacherApplicationRepository_Factory(Provider<FirebaseFirestore> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public TeacherApplicationRepository get() {
    return newInstance(firestoreProvider.get());
  }

  public static TeacherApplicationRepository_Factory create(
      Provider<FirebaseFirestore> firestoreProvider) {
    return new TeacherApplicationRepository_Factory(firestoreProvider);
  }

  public static TeacherApplicationRepository newInstance(FirebaseFirestore firestore) {
    return new TeacherApplicationRepository(firestore);
  }
}
