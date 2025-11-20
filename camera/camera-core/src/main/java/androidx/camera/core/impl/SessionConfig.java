/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.util.Range;
import android.util.Size;

import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.MirrorMode;
import androidx.camera.core.impl.stabilization.StabilizationMode;
import androidx.camera.core.internal.HighSpeedFpsModifier;
import androidx.camera.core.internal.compat.workaround.SurfaceSorter;

import com.google.auto.value.AutoValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Configurations needed for a capture session.
 *
 * <p>The SessionConfig contains all the {@link android.hardware.camera2} parameters that are
 * required to initialize a {@link android.hardware.camera2.CameraCaptureSession} and issue a {@link
 * CaptureRequest}.
 */
public final class SessionConfig {
    /** Regular session type. */
    public static final int SESSION_TYPE_REGULAR = SessionConfiguration.SESSION_REGULAR;
    /** High-speed session type. */
    public static final int SESSION_TYPE_HIGH_SPEED = SessionConfiguration.SESSION_HIGH_SPEED;
    /** The default session type. */
    public static final int DEFAULT_SESSION_TYPE = SESSION_TYPE_REGULAR;
    // Current supported session template values and the bigger index in the list, the
    // priority is higher.
    private static final List<Integer> SUPPORTED_TEMPLATE_PRIORITY = Arrays.asList(
            CameraDevice.TEMPLATE_PREVIEW,
            // TODO(230673983): Based on the framework assumptions, we prioritize video capture
            //  and disable ZSL (fallback to regular) if both use cases are bound.
            CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG,
            CameraDevice.TEMPLATE_RECORD
    );
    /** The set of {@link OutputConfig} that data from the camera will be put into. */
    private final List<OutputConfig> mOutputConfigs;
    /** The {@link OutputConfig} for the postview. */
    private final OutputConfig mPostviewOutputConfig;
    /** The state callback for a {@link CameraDevice}. */
    private final List<CameraDevice.StateCallback> mDeviceStateCallbacks;
    /** The state callback for a {@link CameraCaptureSession}. */
    private final List<CameraCaptureSession.StateCallback> mSessionStateCallbacks;
    /** The callbacks used in single requests. */
    private final List<CameraCaptureCallback> mSingleCameraCaptureCallbacks;
    private final ErrorListener mErrorListener;
    /** The configuration for building the {@link CaptureRequest} used for repeating requests. */
    private final CaptureConfig mRepeatingCaptureConfig;
    /** The type of the session */
    private final int mSessionType;

    /**
     * Immutable class to store an input configuration that is used to create a reprocessable
     * capture session.
     */
    private @Nullable InputConfiguration mInputConfiguration;

    /**
     * The output configuration associated with the {@link DeferrableSurface} that will be used to
     * create the configuration needed to open a camera session. In camera2 this will be used to
     * create the corresponding {@link android.hardware.camera2.params.OutputConfiguration}.
     */
    @SuppressWarnings("AutoValueImmutableFields")  // avoid extra dependency for ImmutableList.
    @AutoValue
    public abstract static class OutputConfig {
        public static final int SURFACE_GROUP_ID_NONE = -1;

        /**
         * Returns the surface associated with the {@link OutputConfig}.
         */
        public abstract @NonNull DeferrableSurface getSurface();

        /**
         * Returns the shared surfaces. If non-empty, surface sharing will be enabled and the
         * shared surfaces will share the same memory buffer as the main surface returned in
         * {@link #getSurface()}.
         */
        public abstract @NonNull List<DeferrableSurface> getSharedSurfaces();

        /**
         * Returns the physical camera ID. By default it would be null. For cameras consisting of
         * multiple physical cameras, this allows output to be redirected to specific physical
         * camera.
         */
        public abstract @Nullable String getPhysicalCameraId();

        /**
         * Returns the mirror mode.
         *
         * @return {@link MirrorMode}
         */
        @MirrorMode.Mirror
        public abstract int getMirrorMode();

        /**
         * Returns the surface group ID. Default value is {@link #SURFACE_GROUP_ID_NONE} meaning
         * it doesn't belong to any surface group. A surface group ID is used to identify which
         * surface group this output surface belongs to. Output streams with the same
         * non-negative group ID won't receive the camera output simultaneously therefore it
         * could reduce the overall memory footprint.
         */
        public abstract int getSurfaceGroupId();

        /**
         * Returns the dynamic range for this output configuration.
         *
         * <p>The dynamic range will determine the dynamic range encoding and profile for pixels in
         * the surfaces associated with this output configuration.
         *
         * <p>If not set, this defaults to {@link DynamicRange#SDR}.
         */
        public abstract @NonNull DynamicRange getDynamicRange();

        /**
         * Creates the {@link Builder} instance with specified {@link DeferrableSurface}.
         */
        public static @NonNull Builder builder(@NonNull DeferrableSurface surface) {
            return new AutoValue_SessionConfig_OutputConfig.Builder()
                    .setSurface(surface)
                    .setSharedSurfaces(Collections.emptyList())
                    .setPhysicalCameraId(null)
                    .setMirrorMode(MirrorMode.MIRROR_MODE_UNSPECIFIED)
                    .setSurfaceGroupId(SURFACE_GROUP_ID_NONE)
                    .setDynamicRange(DynamicRange.SDR);
        }

        /**
         * Builder to create the {@link OutputConfig}
         */
        @AutoValue.Builder
        public abstract static class Builder {
            /**
             * Sets the surface associated with the {@link OutputConfig}.
             */
            public abstract @NonNull Builder setSurface(@NonNull DeferrableSurface surface);

            /**
             * Sets the shared surfaces. After being set, surface sharing will be enabled and the
             * shared surfaces will share the same memory buffer as the main surface returned in
             * {@link #getSurface()}.
             */
            public abstract @NonNull Builder setSharedSurfaces(
                    @NonNull List<DeferrableSurface> surface);

            /**
             * Sets the physical camera ID. For cameras consisting of multiple physical cameras,
             * this allows output to be redirected to specific physical camera.
             */
            public abstract @NonNull Builder setPhysicalCameraId(@Nullable String cameraId);

            /**
             * Sets the mirror mode. It specifies mirroring mode for
             * {@link android.hardware.camera2.params.OutputConfiguration}.
             * @see android.hardware.camera2.params.OutputConfiguration#setMirrorMode(int)
             */
            public abstract @NonNull Builder setMirrorMode(@MirrorMode.Mirror int mirrorMode);

            /**
             * Sets the surface group ID. A surface group ID is used to identify which surface group
             * this output surface belongs to. Output streams with the same non-negative group ID
             * won't receive the camera output simultaneously therefore it could be used to reduce
             * the overall memory footprint.
             */
            public abstract @NonNull Builder setSurfaceGroupId(int surfaceGroupId);

            /**
             * Returns the dynamic range for this output configuration.
             *
             * <p>The dynamic range will determine the dynamic range encoding and profile for
             * pixels in the surfaces associated with this output configuration.
             */
            public abstract @NonNull Builder setDynamicRange(@NonNull DynamicRange dynamicRange);

            /**
             * Creates the instance.
             */
            public abstract @NonNull OutputConfig build();
        }
    }

    /**
     * Private constructor for a SessionConfig.
     *
     * <p>In practice, the {@link SessionConfig.BaseBuilder} will be used to construct a
     * SessionConfig.
     *
     * @param outputConfigs          The list of {@link OutputConfig} where data will be put into.
     * @param deviceStateCallbacks   The state callbacks for a {@link CameraDevice}.
     * @param sessionStateCallbacks  The state callbacks for a {@link CameraCaptureSession}.
     * @param repeatingCaptureConfig The configuration for building the {@link CaptureRequest}.
     * @param inputConfiguration     The input configuration to create a reprocessable capture
     *                               session.
     * @param sessionType            The session type for the {@link CameraCaptureSession}.
     */
    SessionConfig(
            List<OutputConfig> outputConfigs,
            List<StateCallback> deviceStateCallbacks,
            List<CameraCaptureSession.StateCallback> sessionStateCallbacks,
            List<CameraCaptureCallback> singleCameraCaptureCallbacks,
            CaptureConfig repeatingCaptureConfig,
            @Nullable ErrorListener errorListener,
            @Nullable InputConfiguration inputConfiguration,
            int sessionType,
            @Nullable OutputConfig postviewOutputConfig) {
        mOutputConfigs = outputConfigs;
        mDeviceStateCallbacks = Collections.unmodifiableList(deviceStateCallbacks);
        mSessionStateCallbacks = Collections.unmodifiableList(sessionStateCallbacks);
        mSingleCameraCaptureCallbacks =
                Collections.unmodifiableList(singleCameraCaptureCallbacks);
        mErrorListener = errorListener;
        mRepeatingCaptureConfig = repeatingCaptureConfig;
        mInputConfiguration = inputConfiguration;
        mSessionType = sessionType;
        mPostviewOutputConfig = postviewOutputConfig;
    }

    /** Returns an instance of a session configuration with minimal configurations. */
    public static @NonNull SessionConfig defaultEmptySessionConfig() {
        return new SessionConfig(
                new ArrayList<OutputConfig>(),
                new ArrayList<CameraDevice.StateCallback>(0),
                new ArrayList<CameraCaptureSession.StateCallback>(0),
                new ArrayList<CameraCaptureCallback>(0),
                new CaptureConfig.Builder().build(),
                /* errorListener */ null,
                /* inputConfiguration */ null,
                DEFAULT_SESSION_TYPE,
                /* postviewOutputConfig */ null);
    }

    public @Nullable InputConfiguration getInputConfiguration() {
        return mInputConfiguration;
    }

    /**
     * Returns all {@link DeferrableSurface}s that are used to configure the session. It includes
     * both the {@link DeferrableSurface} of the all {@link OutputConfig}s and its shared
     * surfaces.
     */
    public @NonNull List<DeferrableSurface> getSurfaces() {
        List<DeferrableSurface> deferrableSurfaces = new ArrayList<>();
        for (OutputConfig outputConfig : mOutputConfigs) {
            deferrableSurfaces.add(outputConfig.getSurface());
            for (DeferrableSurface sharedSurface : outputConfig.getSharedSurfaces()) {
                deferrableSurfaces.add(sharedSurface);
            }
        }
        return Collections.unmodifiableList(deferrableSurfaces);
    }

    public @NonNull List<OutputConfig> getOutputConfigs() {
        return mOutputConfigs;
    }

    public @Nullable OutputConfig getPostviewOutputConfig() {
        return mPostviewOutputConfig;
    }

    public @NonNull Config getImplementationOptions() {
        return mRepeatingCaptureConfig.getImplementationOptions();
    }

    public int getTemplateType() {
        return mRepeatingCaptureConfig.getTemplateType();
    }

    public int getSessionType() {
        return mSessionType;
    }

    public @NonNull Range<Integer> getExpectedFrameRateRange() {
        return mRepeatingCaptureConfig.getExpectedFrameRateRange();
    }

    /** Obtains all registered {@link CameraDevice.StateCallback} callbacks. */
    public @NonNull List<CameraDevice.StateCallback> getDeviceStateCallbacks() {
        return mDeviceStateCallbacks;
    }

    /** Obtains all registered {@link CameraCaptureSession.StateCallback} callbacks. */
    public @NonNull List<CameraCaptureSession.StateCallback> getSessionStateCallbacks() {
        return mSessionStateCallbacks;
    }

    /** Obtains all registered {@link CameraCaptureCallback} callbacks for repeating requests. */
    public @NonNull List<CameraCaptureCallback> getRepeatingCameraCaptureCallbacks() {
        return mRepeatingCaptureConfig.getCameraCaptureCallbacks();
    }

    /** Obtains the registered {@link ErrorListener} callback. */
    public @Nullable ErrorListener getErrorListener() {
        return mErrorListener;
    }

    /** Obtains all registered {@link CameraCaptureCallback} callbacks for single requests. */
    public @NonNull List<CameraCaptureCallback> getSingleCameraCaptureCallbacks() {
        return mSingleCameraCaptureCallbacks;
    }

    public @NonNull CaptureConfig getRepeatingCaptureConfig() {
        return mRepeatingCaptureConfig;
    }

    /** Returns the one which has higher priority. */
    public static int getHigherPriorityTemplateType(int type1, int type2) {
        return SUPPORTED_TEMPLATE_PRIORITY.indexOf(type1)
                >= SUPPORTED_TEMPLATE_PRIORITY.indexOf(type2) ? type1 : type2;
    }

    public enum SessionError {
        /**
         * A {@link DeferrableSurface} contained in the config needs to be reset.
         *
         * <p>The surface is no longer valid, for example the surface has already been closed.
         */
        SESSION_ERROR_SURFACE_NEEDS_RESET,
        /** An unknown error has occurred. */
        SESSION_ERROR_UNKNOWN
    }

    /**
     * Callback for errors that occur when accessing the session config.
     */
    public interface ErrorListener {
        /**
         * Called when an error has occurred.
         *
         * @param sessionConfig The {@link SessionConfig} that generated the error.
         * @param error         The error that was generated.
         */
        void onError(@NonNull SessionConfig sessionConfig, @NonNull SessionError error);
    }

    /**
     * A closeable ErrorListener that onError callback won't be invoked after it is closed.
     */
    public static final class CloseableErrorListener implements ErrorListener {
        private final AtomicBoolean mIsClosed = new AtomicBoolean(false);
        private final ErrorListener mErrorListener;

        public CloseableErrorListener(@NonNull ErrorListener errorListener) {
            mErrorListener = errorListener;
        }

        @Override
        public void onError(@NonNull SessionConfig sessionConfig, @NonNull SessionError error) {
            if (!mIsClosed.get()) {
                mErrorListener.onError(sessionConfig, error);
            }
        }

        /**
         * Closes the ErrorListener to not invoke the onError callback function.
         */
        public void close() {
            mIsClosed.set(true);
        }
    }

    /**
     * Interface for unpacking a configuration into a SessionConfig.Builder
     *
     * <p>TODO(b/120949879): This will likely be removed once SessionConfig is refactored to
     * remove camera2 dependencies.
     */
    public interface OptionUnpacker {

        /**
         * Apply the options from the config onto the builder
         *
         * @param resolution the suggested resolution
         * @param config  the set of options to apply
         * @param builder the builder on which to apply the options
         */
        void unpack(
                @NonNull Size resolution,
                @NonNull UseCaseConfig<?> config,
                SessionConfig.@NonNull Builder builder);
    }

    /**
     * Base builder for easy modification/rebuilding of a {@link SessionConfig}.
     */
    static class BaseBuilder {
        // Use LinkedHashSet to preserve the adding order for bug fixing and testing.
        final Set<OutputConfig> mOutputConfigs = new LinkedHashSet<>();
        final CaptureConfig.Builder mCaptureConfigBuilder = new CaptureConfig.Builder();
        final List<CameraDevice.StateCallback> mDeviceStateCallbacks = new ArrayList<>();
        final List<CameraCaptureSession.StateCallback> mSessionStateCallbacks = new ArrayList<>();
        final List<CameraCaptureCallback> mSingleCameraCaptureCallbacks = new ArrayList<>();
        @Nullable ErrorListener mErrorListener;
        @Nullable InputConfiguration mInputConfiguration;
        int mSessionType = DEFAULT_SESSION_TYPE;
        @Nullable OutputConfig mPostviewOutputConfig;
    }

    /**
     * Builder for easy modification/rebuilding of a {@link SessionConfig}.
     */
    public static class Builder extends BaseBuilder {
        /**
         * Creates a {@link Builder} from a {@link UseCaseConfig}.
         *
         * <p>Populates the builder with all the properties defined in the base configuration.
         */
        public static @NonNull Builder createFrom(
                @NonNull UseCaseConfig<?> config,
                @NonNull Size resolution) {
            OptionUnpacker unpacker = config.getSessionOptionUnpacker(null);
            if (unpacker == null) {
                throw new IllegalStateException(
                        "Implementation is missing option unpacker for "
                                + config.getTargetName(config.toString()));
            }

            Builder builder = new Builder();

            // Unpack the configuration into this builder
            unpacker.unpack(resolution, config, builder);
            return builder;
        }

        /**
         * Set the input configuration for reprocessable capture session.
         *
         * @param inputConfiguration The input configuration.
         */
        public @NonNull Builder setInputConfiguration(
                @Nullable InputConfiguration inputConfiguration) {
            mInputConfiguration = inputConfiguration;
            return this;
        }

        /**
         * Set the template characteristics of the SessionConfig.
         *
         * @param templateType Template constant that must match those defined by {@link
         *                     CameraDevice}
         *                     <p>TODO(b/120949879): This is camera2 implementation detail that
         *                     should be moved
         */
        public @NonNull Builder setTemplateType(int templateType) {
            mCaptureConfigBuilder.setTemplateType(templateType);
            return this;
        }

        /**
         * Sets the session type.
         */
        public @NonNull Builder setSessionType(int sessionType) {
            mSessionType = sessionType;
            return this;
        }

        /**
         * Set the expected frame rate range of the SessionConfig.
         *
         * @param expectedFrameRateRange The frame rate range calculated from the UseCases for
         *                               {@link CameraDevice}
         */
        public @NonNull Builder setExpectedFrameRateRange(
                @NonNull Range<Integer> expectedFrameRateRange) {
            mCaptureConfigBuilder.setExpectedFrameRateRange(expectedFrameRateRange);
            return this;
        }

        /**
         * Set the preview stabilization mode of the SessionConfig.
         * @param mode {@link StabilizationMode}
         */
        public @NonNull Builder setPreviewStabilization(@StabilizationMode.Mode int mode) {
            if (mode != StabilizationMode.UNSPECIFIED) {
                mCaptureConfigBuilder.setPreviewStabilization(mode);
            }
            return this;
        }

        /**
         * Set the video stabilization mode of the SessionConfig.
         * @param mode {@link StabilizationMode}
         */
        public @NonNull Builder setVideoStabilization(@StabilizationMode.Mode int mode) {
            if (mode != StabilizationMode.UNSPECIFIED) {
                mCaptureConfigBuilder.setVideoStabilization(mode);
            }
            return this;
        }

        /**
         * Adds a tag to the SessionConfig with a key. For tracking the source.
         */
        public @NonNull Builder addTag(@NonNull String key, @NonNull Object tag) {
            mCaptureConfigBuilder.addTag(key, tag);
            return this;
        }

        /**
         * Adds a {@link CameraDevice.StateCallback} callback.
         */
        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        public @NonNull Builder addDeviceStateCallback(
                CameraDevice.@NonNull StateCallback deviceStateCallback) {
            if (mDeviceStateCallbacks.contains(deviceStateCallback)) {
                return this;
            }
            mDeviceStateCallbacks.add(deviceStateCallback);
            return this;
        }

        /**
         * Adds all {@link CameraDevice.StateCallback} callbacks.
         */
        public @NonNull Builder addAllDeviceStateCallbacks(
                @NonNull Collection<CameraDevice.StateCallback> deviceStateCallbacks) {
            for (CameraDevice.StateCallback callback : deviceStateCallbacks) {
                addDeviceStateCallback(callback);
            }
            return this;
        }

        /**
         * Adds a {@link CameraCaptureSession.StateCallback} callback.
         */
        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        public @NonNull Builder addSessionStateCallback(
                CameraCaptureSession.@NonNull StateCallback sessionStateCallback) {
            if (mSessionStateCallbacks.contains(sessionStateCallback)) {
                return this;
            }
            mSessionStateCallbacks.add(sessionStateCallback);
            return this;
        }

        /**
         * Adds all {@link CameraCaptureSession.StateCallback} callbacks.
         */
        public @NonNull Builder addAllSessionStateCallbacks(
                @NonNull List<CameraCaptureSession.StateCallback> sessionStateCallbacks) {
            for (CameraCaptureSession.StateCallback callback : sessionStateCallbacks) {
                addSessionStateCallback(callback);
            }
            return this;
        }

        /**
         * Adds a {@link CameraCaptureCallback} callback for repeating requests.
         * <p>This callback does not call for single requests.
         */
        public @NonNull Builder addRepeatingCameraCaptureCallback(
                @NonNull CameraCaptureCallback cameraCaptureCallback) {
            mCaptureConfigBuilder.addCameraCaptureCallback(cameraCaptureCallback);
            return this;
        }

        /**
         * Adds all {@link CameraCaptureCallback} callbacks.
         * <p>These callbacks do not call for single requests.
         */
        public @NonNull Builder addAllRepeatingCameraCaptureCallbacks(
                @NonNull Collection<CameraCaptureCallback> cameraCaptureCallbacks) {
            mCaptureConfigBuilder.addAllCameraCaptureCallbacks(cameraCaptureCallbacks);
            return this;
        }

        /**
         * Adds a {@link CameraCaptureCallback} callback for single and repeating requests.
         * <p>Listeners added here are available in both the
         * {@link #getRepeatingCameraCaptureCallbacks()} and
         * {@link #getSingleCameraCaptureCallbacks()} methods.
         */
        public @NonNull Builder addCameraCaptureCallback(
                @NonNull CameraCaptureCallback cameraCaptureCallback) {
            mCaptureConfigBuilder.addCameraCaptureCallback(cameraCaptureCallback);
            if (!mSingleCameraCaptureCallbacks.contains(cameraCaptureCallback)) {
                mSingleCameraCaptureCallbacks.add(cameraCaptureCallback);
            }
            return this;
        }

        /**
         * Adds all {@link CameraCaptureCallback} callbacks for single and repeating requests.
         * <p>Listeners added here are available in both the
         * {@link #getRepeatingCameraCaptureCallbacks()} and
         * {@link #getSingleCameraCaptureCallbacks()} methods.
         */
        public @NonNull Builder addAllCameraCaptureCallbacks(
                @NonNull Collection<CameraCaptureCallback> cameraCaptureCallbacks) {
            for (CameraCaptureCallback c : cameraCaptureCallbacks) {
                mCaptureConfigBuilder.addCameraCaptureCallback(c);
                if (!mSingleCameraCaptureCallbacks.contains(c)) {
                    mSingleCameraCaptureCallbacks.add(c);
                }
            }
            return this;
        }

        /**
         * Removes a previously added {@link CameraCaptureCallback} callback for single and/or
         * repeating requests.
         *
         * @param cameraCaptureCallback The callback to remove.
         * @return {@code true} if the callback was successfully removed. {@code false} if the
         * callback wasn't present in this builder.
         */
        public boolean removeCameraCaptureCallback(
                @NonNull CameraCaptureCallback cameraCaptureCallback) {
            boolean removedFromRepeating =
                    mCaptureConfigBuilder.removeCameraCaptureCallback(cameraCaptureCallback);
            boolean removedFromSingle =
                    mSingleCameraCaptureCallbacks.remove(cameraCaptureCallback);
            return removedFromRepeating || removedFromSingle;
        }

        /** Obtain all {@link CameraCaptureCallback} callbacks for single requests. */
        public @NonNull List<CameraCaptureCallback> getSingleCameraCaptureCallbacks() {
            return Collections.unmodifiableList(mSingleCameraCaptureCallbacks);
        }

        /**
         * Adds all {@link ErrorListener} listeners repeating requests.
         */
        public @NonNull Builder setErrorListener(@NonNull ErrorListener errorListener) {
            mErrorListener = errorListener;
            return this;
        }


        /**
         * Add a surface to the set that the session repeatedly writes data to.
         *
         * <p> The dynamic range of this surface will default to {@link DynamicRange#SDR},
         * the physical camera ID to null and the mirror mode to
         * {@link MirrorMode#MIRROR_MODE_UNSPECIFIED}. To manually set them, use
         * {@link #addSurface(DeferrableSurface, DynamicRange, String, int)}.
         */
        public @NonNull Builder addSurface(@NonNull DeferrableSurface surface) {
            return addSurface(surface, DynamicRange.SDR);
        }

        /**
         * Add a surface with the provided dynamic range to the set that the session repeatedly
         * writes data to.
         *
         * <p> The physical camera ID of this surface will default to null and the mirror mode will
         * default to {@link MirrorMode#MIRROR_MODE_UNSPECIFIED}. To manually set them, use
         * {@link #addSurface(DeferrableSurface, DynamicRange, String, int)}.
         */
        public @NonNull Builder addSurface(@NonNull DeferrableSurface surface,
                @NonNull DynamicRange dynamicRange) {
            return addSurface(surface, dynamicRange, null,
                    MirrorMode.MIRROR_MODE_UNSPECIFIED);
        }

        /**
         * Add a surface with the provided dynamic range to the set that the session repeatedly
         * writes data to.
         */
        public @NonNull Builder addSurface(@NonNull DeferrableSurface surface,
                @NonNull DynamicRange dynamicRange,
                @Nullable String physicalCameraId,
                @MirrorMode.Mirror int mirrorMode) {
            OutputConfig outputConfig = OutputConfig.builder(surface)
                    .setPhysicalCameraId(physicalCameraId)
                    .setDynamicRange(dynamicRange)
                    .setMirrorMode(mirrorMode)
                    .build();
            mOutputConfigs.add(outputConfig);
            mCaptureConfigBuilder.addSurface(surface);
            return this;
        }

        /**
         * Adds an {@link OutputConfig} to create the capture session with. The surface set in
         * the {@link OutputConfig} will be added to the repeating request.
         */
        public @NonNull Builder addOutputConfig(@NonNull OutputConfig outputConfig) {
            mOutputConfigs.add(outputConfig);
            mCaptureConfigBuilder.addSurface(outputConfig.getSurface());
            for (DeferrableSurface sharedSurface : outputConfig.getSharedSurfaces()) {
                mCaptureConfigBuilder.addSurface(sharedSurface);
            }
            return this;
        }

        /**
         * Add a surface for the session which only used for single captures.
         *
         * <p>The dynamic range of this surface will default to {@link DynamicRange#SDR}. To
         * manually set the dynamic range, use
         * {@link #addNonRepeatingSurface(DeferrableSurface, DynamicRange)}.
         */
        public @NonNull Builder addNonRepeatingSurface(@NonNull DeferrableSurface surface) {
            return addNonRepeatingSurface(surface, DynamicRange.SDR);
        }

        /**
         * Add a surface with the provided dynamic range for the session which only used for
         * single captures.
         */
        public @NonNull Builder addNonRepeatingSurface(@NonNull DeferrableSurface surface,
                @NonNull DynamicRange dynamicRange) {
            OutputConfig outputConfig = OutputConfig.builder(surface)
                    .setDynamicRange(dynamicRange)
                    .build();
            mOutputConfigs.add(outputConfig);
            return this;
        }

        /**
         * Sets the postview surface.
         */
        public @NonNull Builder setPostviewSurface(@NonNull DeferrableSurface surface) {
            mPostviewOutputConfig = OutputConfig.builder(surface).build();
            return this;
        }

        /** Remove a surface from the set which the session repeatedly writes to. */
        public @NonNull Builder removeSurface(@NonNull DeferrableSurface surface) {
            OutputConfig outputConfigToRemove = null;
            for (OutputConfig config : mOutputConfigs) {
                if (config.getSurface().equals(surface)) {
                    outputConfigToRemove = config;
                    break;
                }
            }

            if (outputConfigToRemove != null) {
                mOutputConfigs.remove(outputConfigToRemove);
            }
            mCaptureConfigBuilder.removeSurface(surface);
            return this;
        }

        /** Clears all surfaces from the set which the session writes to. */
        public @NonNull Builder clearSurfaces() {
            mOutputConfigs.clear();
            mCaptureConfigBuilder.clearSurfaces();
            return this;
        }

        /** Set the {@link Config} for options that are implementation specific. */
        public @NonNull Builder setImplementationOptions(@NonNull Config config) {
            mCaptureConfigBuilder.setImplementationOptions(config);
            return this;
        }

        /** Add a set of {@link Config} to the implementation specific options. */
        public @NonNull Builder addImplementationOptions(@NonNull Config config) {
            mCaptureConfigBuilder.addImplementationOptions(config);
            return this;
        }

        /**
         * Builds an instance of a SessionConfig that has all the combined parameters of the
         * SessionConfig that have been added to the Builder.
         */
        public @NonNull SessionConfig build() {
            return new SessionConfig(
                    new ArrayList<>(mOutputConfigs),
                    new ArrayList<>(mDeviceStateCallbacks),
                    new ArrayList<>(mSessionStateCallbacks),
                    new ArrayList<>(mSingleCameraCaptureCallbacks),
                    mCaptureConfigBuilder.build(),
                    mErrorListener,
                    mInputConfiguration,
                    mSessionType,
                    mPostviewOutputConfig);
        }
    }

    /**
     * Builder for combining multiple instances of {@link SessionConfig}. This will check if all
     * the parameters for the {@link SessionConfig} are compatible with each other
     */
    public static final class ValidatingBuilder extends BaseBuilder {
        private static final String TAG = "ValidatingBuilder";
        private final SurfaceSorter mSurfaceSorter = new SurfaceSorter();
        private boolean mValid = true;
        private StringBuilder mInvalidReason = new StringBuilder();
        private boolean mTemplateSet = false;
        private List<ErrorListener> mErrorListeners = new ArrayList<>();

        /**
         * Add an implementation option to the ValidatingBuilder's CaptureConfigBuilder. If it
         * already has an option with the same key, write it over.
         */
        public <T> void addImplementationOption(Config.@NonNull Option<T> option,
                @NonNull T value) {
            mCaptureConfigBuilder.addImplementationOption(option, value);
        }

        /**
         * Add the SessionConfig to the set of SessionConfig that have been aggregated by the
         * ValidatingBuilder
         */
        public void add(@NonNull SessionConfig sessionConfig) {
            CaptureConfig captureConfig = sessionConfig.getRepeatingCaptureConfig();

            // Check template
            if (captureConfig.getTemplateType() != CaptureConfig.TEMPLATE_TYPE_NONE) {
                mTemplateSet = true;
                mCaptureConfigBuilder.setTemplateType(
                        getHigherPriorityTemplateType(captureConfig.getTemplateType(),
                                mCaptureConfigBuilder.getTemplateType()));
            }

            setOrVerifyExpectFrameRateRange(captureConfig.getExpectedFrameRateRange());
            setPreviewStabilizationMode(captureConfig.getPreviewStabilizationMode());
            setVideoStabilizationMode(captureConfig.getVideoStabilizationMode());

            TagBundle tagBundle = sessionConfig.getRepeatingCaptureConfig().getTagBundle();
            mCaptureConfigBuilder.addAllTags(tagBundle);

            // Check device state callbacks
            mDeviceStateCallbacks.addAll(sessionConfig.getDeviceStateCallbacks());

            // Check session state callbacks
            mSessionStateCallbacks.addAll(sessionConfig.getSessionStateCallbacks());

            // Check camera capture callbacks for repeating requests.
            mCaptureConfigBuilder.addAllCameraCaptureCallbacks(
                    sessionConfig.getRepeatingCameraCaptureCallbacks());

            // Check camera capture callbacks for single requests.
            mSingleCameraCaptureCallbacks.addAll(sessionConfig.getSingleCameraCaptureCallbacks());

            if (sessionConfig.getErrorListener() != null) {
                mErrorListeners.add(sessionConfig.getErrorListener());
            }

            // Check input configuration for reprocessable capture session.
            if (sessionConfig.getInputConfiguration() != null) {
                mInputConfiguration = sessionConfig.getInputConfiguration();
            }

            // Check surfaces
            mOutputConfigs.addAll(sessionConfig.getOutputConfigs());

            // Check capture request surfaces
            mCaptureConfigBuilder.getSurfaces().addAll(captureConfig.getSurfaces());

            if (!getSurfaces().containsAll(mCaptureConfigBuilder.getSurfaces())) {
                String errorMessage =
                        "Invalid configuration due to capture request surfaces are not a subset "
                                + "of surfaces";
                Logger.d(TAG, errorMessage);
                mValid = false;
                mInvalidReason.append(errorMessage);
            }

            if (sessionConfig.getSessionType() != mSessionType
                    && sessionConfig.getSessionType() != DEFAULT_SESSION_TYPE
                    && mSessionType != DEFAULT_SESSION_TYPE) {
                String errorMessage =
                        "Invalid configuration due to that two non-default session types are set";
                Logger.d(TAG, errorMessage);
                mValid = false;
                mInvalidReason.append(errorMessage);
            } else {
                if (sessionConfig.getSessionType() != DEFAULT_SESSION_TYPE) {
                    mSessionType = sessionConfig.getSessionType();
                }
            }

            if (sessionConfig.mPostviewOutputConfig != null) {
                if (mPostviewOutputConfig != sessionConfig.mPostviewOutputConfig
                        && mPostviewOutputConfig != null) {
                    String errorMessage =
                            "Invalid configuration due to that two different postview output "
                                    + "configs are set";
                    Logger.d(TAG, errorMessage);
                    mValid = false;
                    mInvalidReason.append(errorMessage);
                } else {
                    mPostviewOutputConfig = sessionConfig.mPostviewOutputConfig;
                }
            }

            // The conflicting of options is handled in addImplementationOptions where it could
            // throw an IllegalArgumentException if the conflict cannot be resolved.
            mCaptureConfigBuilder.addImplementationOptions(
                    captureConfig.getImplementationOptions());
        }

        private void setOrVerifyExpectFrameRateRange(
                @NonNull Range<Integer> expectedFrameRateRange) {
            if (expectedFrameRateRange.equals(StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)) {
                return;
            }

            if (mCaptureConfigBuilder.getExpectedFrameRateRange().equals(
                    StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)) {
                mCaptureConfigBuilder.setExpectedFrameRateRange(expectedFrameRateRange);
                return;
            }

            if (!mCaptureConfigBuilder.getExpectedFrameRateRange().equals(expectedFrameRateRange)) {
                mValid = false;

                String errorMessage = "Different ExpectedFrameRateRange values; current = "
                        + mCaptureConfigBuilder.getExpectedFrameRateRange() + ", new = "
                        + expectedFrameRateRange;
                Logger.e(TAG, errorMessage);
                mInvalidReason.append(errorMessage);
            }
        }

        private void setPreviewStabilizationMode(@StabilizationMode.Mode int mode) {
            if (mode != StabilizationMode.UNSPECIFIED) {
                mCaptureConfigBuilder.setPreviewStabilization(mode);
            }
        }

        private void setVideoStabilizationMode(@StabilizationMode.Mode int mode) {
            if (mode != StabilizationMode.UNSPECIFIED) {
                mCaptureConfigBuilder.setVideoStabilization(mode);
            }
        }

        private List<DeferrableSurface> getSurfaces() {
            List<DeferrableSurface> surfaces = new ArrayList<>();
            for (OutputConfig outputConfig : mOutputConfigs) {
                surfaces.add(outputConfig.getSurface());
                for (DeferrableSurface sharedSurface : outputConfig.getSharedSurfaces()) {
                    surfaces.add(sharedSurface);
                }
            }
            return surfaces;
        }

        /** Clears all surfaces from the set which the session writes to. */
        public void clearSurfaces() {
            mOutputConfigs.clear();
            mCaptureConfigBuilder.clearSurfaces();
        }

        /** Check if the set of SessionConfig that have been combined are valid */
        public boolean isValid() {
            return mTemplateSet && mValid;
        }

        /** Gets the reason(s) for {@link #isValid} returning false. */
        public @NonNull String getInvalidReason() {
            if (!mTemplateSet) {
                return "Template is not set";
            }
            return mInvalidReason.toString();
        }

        /**
         * Builds an instance of a SessionConfig that has all the combined parameters of the
         * SessionConfig that have been added to the ValidatingBuilder.
         */
        public @NonNull SessionConfig build() {
            if (!mValid) {
                throw new IllegalArgumentException("Unsupported session configuration combination");
            }

            List<OutputConfig> outputConfigs = new ArrayList<>(mOutputConfigs);
            mSurfaceSorter.sort(outputConfigs);

            if (mSessionType == SESSION_TYPE_HIGH_SPEED) {
                // HighSpeedFpsModifier may modify the expected frame rate range for
                // mCaptureConfigBuilder.
                new HighSpeedFpsModifier().modifyFpsForPreviewOnlyRepeating(outputConfigs,
                        mCaptureConfigBuilder);
            }

            ErrorListener errorListener = null;
            // Creates an error listener to notify errors to the underlying error listeners.
            if (!mErrorListeners.isEmpty()) {
                errorListener = (sessionConfig, error) -> {
                    for (ErrorListener listener: mErrorListeners) {
                        listener.onError(sessionConfig, error);
                    }
                };
            }

            return new SessionConfig(
                    outputConfigs,
                    new ArrayList<>(mDeviceStateCallbacks),
                    new ArrayList<>(mSessionStateCallbacks),
                    new ArrayList<>(mSingleCameraCaptureCallbacks),
                    mCaptureConfigBuilder.build(),
                    errorListener,
                    mInputConfiguration,
                    mSessionType,
                    mPostviewOutputConfig);
        }
    }
}
