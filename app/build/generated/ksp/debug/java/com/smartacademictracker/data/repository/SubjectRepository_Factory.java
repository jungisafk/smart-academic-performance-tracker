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
public final class SubjectRepository_Factory implements Factory<SubjectRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  public SubjectRepository_Factory(Provider<FirebaseFirestore> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public SubjectRepository get() {
    return newInstance(firestoreProvider.get());
  }

  public static SubjectRepository_Factory create(Provider<FirebaseFirestore> firestoreProvider) {
    return new SubjectRepository_Factory(firestoreProvider);
  }

  public static SubjectRepository newInstance(FirebaseFirestore firestore) {
    return new SubjectRepository(firestore);
  }
}
