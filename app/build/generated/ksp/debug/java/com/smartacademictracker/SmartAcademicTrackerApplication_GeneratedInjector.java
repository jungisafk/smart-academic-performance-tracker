package com.smartacademictracker;

import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.internal.GeneratedEntryPoint;
import javax.annotation.processing.Generated;

@OriginatingElement(
    topLevelClass = SmartAcademicTrackerApplication.class
)
@GeneratedEntryPoint
@InstallIn(SingletonComponent.class)
@Generated("dagger.hilt.android.processor.internal.androidentrypoint.InjectorEntryPointGenerator")
public interface SmartAcademicTrackerApplication_GeneratedInjector {
  void injectSmartAcademicTrackerApplication(
      SmartAcademicTrackerApplication smartAcademicTrackerApplication);
}
