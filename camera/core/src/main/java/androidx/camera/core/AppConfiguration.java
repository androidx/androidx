/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.core;

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;

/**
 * Configuration for adding implementation and user-specific behavior to CameraX.
 *
 * <p>The AppConfiguration
 *
 * @hide
 */
public final class AppConfiguration implements TargetConfiguration<CameraX> {

  private final OptionsBundle config;

  private AppConfiguration(OptionsBundle options) {
    this.config = options;
  }

  /**
   * Returns the {@link CameraFactory} implementation for the application.
   *
   * @hide
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public CameraFactory getCameraFactory(@Nullable CameraFactory valueIfMissing) {
    return getConfiguration().retrieveOption(OPTION_CAMERA_FACTORY, valueIfMissing);
  }

  /**
   * Returns the {@link CameraDeviceSurfaceManager} implementation for the application.
   *
   * @hide
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public CameraDeviceSurfaceManager getDeviceSurfaceManager(
      @Nullable CameraDeviceSurfaceManager valueIfMissing) {
    return getConfiguration().retrieveOption(OPTION_DEVICE_SURFACE_MANAGER, valueIfMissing);
  }

  /**
   * Returns the {@link UseCaseConfigurationFactory} implementation for the application.
   *
   * <p>This factory should produce all default configurations for the application's use cases.
   *
   * @hide
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public UseCaseConfigurationFactory getUseCaseConfigRepository(
      @Nullable UseCaseConfigurationFactory valueIfMissing) {
    return getConfiguration().retrieveOption(OPTION_USECASE_CONFIG_FACTORY, valueIfMissing);
  }

  @Override
  public Configuration getConfiguration() {
    return config;
  }

  /** A builder for generating {@link AppConfiguration} objects. */
  public static final class Builder
      implements TargetConfiguration.Builder<CameraX, AppConfiguration, Builder> {

    private final MutableOptionsBundle mutableConfig;

    /**
     * Sets the {@link CameraFactory} implementation for the application.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public Builder setCameraFactory(CameraFactory cameraFactory) {
      getMutableConfiguration().insertOption(OPTION_CAMERA_FACTORY, cameraFactory);
      return builder();
    }

    /**
     * Sets the {@link CameraDeviceSurfaceManager} implementation for the application.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public Builder setDeviceSurfaceManager(CameraDeviceSurfaceManager repository) {
      getMutableConfiguration().insertOption(OPTION_DEVICE_SURFACE_MANAGER, repository);
      return builder();
    }

    /**
     * Sets the {@link UseCaseConfigurationFactory} implementation for the application.
     *
     * <p>This factory should produce all default configurations for the application's use cases.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public Builder setUseCaseConfigFactory(UseCaseConfigurationFactory repository) {
      getMutableConfiguration().insertOption(OPTION_USECASE_CONFIG_FACTORY, repository);
      return builder();
    }

    /** Creates a new Builder object. */
    public Builder() {
      this(MutableOptionsBundle.create());
    }

    private Builder(MutableOptionsBundle mutableConfig) {
      this.mutableConfig = mutableConfig;

      Class<?> oldConfigClass =
          mutableConfig.retrieveOption(TargetConfiguration.OPTION_TARGET_CLASS, null);
      if (oldConfigClass != null && !oldConfigClass.equals(CameraX.class)) {
        throw new IllegalArgumentException(
            "Invalid target class configuration for "
                + AppConfiguration.Builder.this
                + ": "
                + oldConfigClass);
      }

      setTargetClass(CameraX.class);
    }

    /**
     * Generates a Builder from another Configuration object
     *
     * @param configuration An immutable configuration to pre-populate this builder.
     * @return The new Builder.
     */
    public static Builder fromConfig(Configuration configuration) {
      return new Builder(MutableOptionsBundle.from(configuration));
    }

    @Override
    public MutableConfiguration getMutableConfiguration() {
      return mutableConfig;
    }

    /** The solution for the unchecked cast warning. */
    @Override
    public Builder builder() {
      return this;
    }

    @Override
    public AppConfiguration build() {
      return new AppConfiguration(OptionsBundle.from(mutableConfig));
    }
  }

  // Option Declarations:
  // ***********************************************************************************************

  static final Option<CameraFactory> OPTION_CAMERA_FACTORY =
      Option.create("camerax.core.appConfig.cameraFactory", CameraFactory.class);

  static final Option<CameraDeviceSurfaceManager> OPTION_DEVICE_SURFACE_MANAGER =
      Option.create(
          "camerax.core.appConfig.deviceSurfaceManager", CameraDeviceSurfaceManager.class);

  static final Option<UseCaseConfigurationFactory> OPTION_USECASE_CONFIG_FACTORY =
      Option.create(
          "camerax.core.appConfig.useCaseConfigFactory", UseCaseConfigurationFactory.class);
}
