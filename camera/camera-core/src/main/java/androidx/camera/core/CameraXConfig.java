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

import android.app.Application;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.internal.TargetConfig;

import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * A configuration for adding implementation and user-specific behavior to CameraX.
 *
 * <p>CameraXConfig provides customizable options for camera provider instances that persist for
 * the lifetime of the provider.
 *
 * <p>An implementation of AppConfig must be provided by subclassing the
 * {@link Application} object and implementing {@link CameraXConfig.Provider}.
 * {@linkplain androidx.camera.lifecycle.ProcessCameraProvider#getInstance(android.content.Context)
 * An example} of how this is used can be found in the {@link androidx.camera.lifecycle} package.
 *
 * <p>Applications can create and use {@linkplain androidx.camera.camera2.Camera2Config the
 * implementation} of AppConfig provided in {@link androidx.camera.camera2}.
 *
 * @see androidx.camera.lifecycle
 * @see CameraXConfig.Builder
 */
public final class CameraXConfig implements TargetConfig<CameraX> {

    /**
     * An interface which can be implemented to provide the configuration for CameraX.
     *
     * <p>When implemented by an {@link Application}, this can provide on-demand initialization
     * of CameraX.
     *
     * <p>{@linkplain
     * androidx.camera.lifecycle.ProcessCameraProvider#getInstance(android.content.Context) An
     * example} of how this is used can be found in the {@link androidx.camera.lifecycle} package.
     */
    public interface Provider {
        /** Returns the configuration to use for initializing an instance of CameraX. */
        @NonNull
        CameraXConfig getCameraXConfig();
    }

    // Option Declarations:
    // *********************************************************************************************

    static final Option<CameraFactory.Provider> OPTION_CAMERA_FACTORY_PROVIDER =
            Option.create("camerax.core.appConfig.cameraFactoryProvider",
                    CameraFactory.Provider.class);
    static final Option<CameraDeviceSurfaceManager.Provider>
            OPTION_DEVICE_SURFACE_MANAGER_PROVIDER =
            Option.create(
                    "camerax.core.appConfig.deviceSurfaceManagerProvider",
                    CameraDeviceSurfaceManager.Provider.class);
    static final Option<UseCaseConfigFactory.Provider> OPTION_USECASE_CONFIG_FACTORY_PROVIDER =
            Option.create(
                    "camerax.core.appConfig.useCaseConfigFactoryProvider",
                    UseCaseConfigFactory.Provider.class);
    static final Option<Executor> OPTION_CAMERA_EXECUTOR =
            Option.create(
                    "camerax.core.appConfig.cameraExecutor",
                    Executor.class);
    static final Option<Handler> OPTION_SCHEDULER_HANDLER =
            Option.create(
                    "camerax.core.appConfig.schedulerHandler",
                    Handler.class);

    // *********************************************************************************************

    private final OptionsBundle mConfig;

    CameraXConfig(OptionsBundle options) {
        mConfig = options;
    }

    /**
     * Returns the {@link CameraFactory} implementation for the application.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public CameraFactory.Provider getCameraFactoryProvider(
            @Nullable CameraFactory.Provider valueIfMissing) {
        return mConfig.retrieveOption(OPTION_CAMERA_FACTORY_PROVIDER, valueIfMissing);
    }

    /**
     * Returns the {@link CameraDeviceSurfaceManager} implementation for the application.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public CameraDeviceSurfaceManager.Provider getDeviceSurfaceManagerProvider(
            @Nullable CameraDeviceSurfaceManager.Provider valueIfMissing) {
        return mConfig.retrieveOption(OPTION_DEVICE_SURFACE_MANAGER_PROVIDER, valueIfMissing);
    }

    /**
     * Returns the {@link UseCaseConfigFactory} implementation for the application.
     *
     * <p>This factory should produce all default configurations for the application's use cases.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public UseCaseConfigFactory.Provider getUseCaseConfigFactoryProvider(
            @Nullable UseCaseConfigFactory.Provider valueIfMissing) {
        return mConfig.retrieveOption(OPTION_USECASE_CONFIG_FACTORY_PROVIDER, valueIfMissing);
    }

    /**
     * Returns the camera executor.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Executor getCameraExecutor(@Nullable Executor valueIfMissing) {
        return mConfig.retrieveOption(OPTION_CAMERA_EXECUTOR, valueIfMissing);
    }

    /**
     * Returns the scheduling handler.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Handler getSchedulerHandler(@Nullable Handler valueIfMissing) {
        return mConfig.retrieveOption(OPTION_SCHEDULER_HANDLER, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public Config getConfig() {
        return mConfig;
    }

    /** A builder for generating {@link CameraXConfig} objects. */
    public static final class Builder
            implements TargetConfig.Builder<CameraX, CameraXConfig.Builder> {

        private final MutableOptionsBundle mMutableConfig;

        /**
         * Creates a new Builder object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfig.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(CameraX.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + CameraXConfig.Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(CameraX.class);
        }

        /**
         * Generates a Builder from another {@link CameraXConfig} object
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        @NonNull
        public static Builder fromConfig(@NonNull CameraXConfig configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * Sets the {@link CameraFactory} implementation for the application.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setCameraFactoryProvider(@NonNull CameraFactory.Provider cameraFactory) {
            getMutableConfig().insertOption(OPTION_CAMERA_FACTORY_PROVIDER, cameraFactory);
            return this;
        }

        /**
         * Sets the {@link CameraDeviceSurfaceManager} implementation for the application.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setDeviceSurfaceManagerProvider(
                @NonNull CameraDeviceSurfaceManager.Provider surfaceManagerProvider) {
            getMutableConfig().insertOption(OPTION_DEVICE_SURFACE_MANAGER_PROVIDER,
                    surfaceManagerProvider);
            return this;
        }

        /**
         * Sets the {@link UseCaseConfigFactory} implementation for the application.
         *
         * <p>This factory should produce all default configurations for the application's use
         * cases.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setUseCaseConfigFactoryProvider(
                @NonNull UseCaseConfigFactory.Provider configFactoryProvider) {
            getMutableConfig().insertOption(OPTION_USECASE_CONFIG_FACTORY_PROVIDER,
                    configFactoryProvider);
            return this;
        }

        /**
         * Sets an executor which CameraX will use to drive the camera stack.
         *
         * <p>This option can be used to override the default internal executor created by
         * CameraX, and will be used by the implementation to drive all cameras.
         *
         * <p>It is not necessary to set an executor for normal use, and should only be used in
         * applications with very specific threading requirements. If not set, CameraX will
         * create and use an optimized default internal executor.
         */
        @NonNull
        public Builder setCameraExecutor(@NonNull Executor executor) {
            getMutableConfig().insertOption(OPTION_CAMERA_EXECUTOR, executor);
            return this;
        }

        /**
         * Sets a handler that CameraX will use internally for scheduling future tasks.
         *
         * <p>This scheduler may also be used for legacy APIs which require a {@link Handler}. Tasks
         * that are scheduled with this handler will always be executed by the camera executor. No
         * business logic will be executed directly by this handler.
         *
         * <p>It is not necessary to set a scheduler handler for normal use, and should only be
         * used in applications with very specific threading requirements. If not set, CameraX
         * will create and use an optimized default internal handler.
         *
         * @see #setCameraExecutor(Executor)
         */
        @ExperimentalCustomizableThreads
        @NonNull
        public Builder setSchedulerHandler(@NonNull Handler handler) {
            getMutableConfig().insertOption(OPTION_SCHEDULER_HANDLER, handler);
            return this;
        }

        @NonNull
        private MutableConfig getMutableConfig() {
            return mMutableConfig;
        }

        /**
         * Builds an immutable {@link CameraXConfig} from the current state.
         *
         * @return A {@link CameraXConfig} populated with the current state.
         */
        @NonNull
        public CameraXConfig build() {
            return new CameraXConfig(OptionsBundle.from(mMutableConfig));
        }

        // Implementations of TargetConfig.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setTargetClass(@NonNull Class<CameraX> targetClass) {
            getMutableConfig().insertOption(OPTION_TARGET_CLASS, targetClass);

            // If no name is set yet, then generate a unique name
            if (null == getMutableConfig().retrieveOption(OPTION_TARGET_NAME, null)) {
                String targetName = targetClass.getCanonicalName() + "-" + UUID.randomUUID();
                setTargetName(targetName);
            }

            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setTargetName(@NonNull String targetName) {
            getMutableConfig().insertOption(OPTION_TARGET_NAME, targetName);
            return this;
        }
    }
}
