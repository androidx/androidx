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
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.internal.compat.workaround.SurfaceSorter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Configurations needed for a capture session.
 *
 * <p>The SessionConfig contains all the {@link android.hardware.camera2} parameters that are
 * required to initialize a {@link android.hardware.camera2.CameraCaptureSession} and issue a {@link
 * CaptureRequest}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class SessionConfig {

    /** The set of {@link Surface} that data from the camera will be put into. */
    private final List<DeferrableSurface> mSurfaces;
    /** The state callback for a {@link CameraDevice}. */
    private final List<CameraDevice.StateCallback> mDeviceStateCallbacks;
    /** The state callback for a {@link CameraCaptureSession}. */
    private final List<CameraCaptureSession.StateCallback> mSessionStateCallbacks;
    /** The callbacks used in single requests. */
    private final List<CameraCaptureCallback> mSingleCameraCaptureCallbacks;
    private final List<ErrorListener> mErrorListeners;
    /** The configuration for building the {@link CaptureRequest} used for repeating requests. */
    private final CaptureConfig mRepeatingCaptureConfig;

    /**
     * Private constructor for a SessionConfig.
     *
     * <p>In practice, the {@link SessionConfig.BaseBuilder} will be used to construct a
     * SessionConfig.
     *
     * @param surfaces               The set of {@link Surface} where data will be put into.
     * @param deviceStateCallbacks   The state callbacks for a {@link CameraDevice}.
     * @param sessionStateCallbacks  The state callbacks for a {@link CameraCaptureSession}.
     * @param repeatingCaptureConfig The configuration for building the {@link CaptureRequest}.
     */
    SessionConfig(
            List<DeferrableSurface> surfaces,
            List<StateCallback> deviceStateCallbacks,
            List<CameraCaptureSession.StateCallback> sessionStateCallbacks,
            List<CameraCaptureCallback> singleCameraCaptureCallbacks,
            List<ErrorListener> errorListeners,
            CaptureConfig repeatingCaptureConfig) {
        mSurfaces = surfaces;
        mDeviceStateCallbacks = Collections.unmodifiableList(deviceStateCallbacks);
        mSessionStateCallbacks = Collections.unmodifiableList(sessionStateCallbacks);
        mSingleCameraCaptureCallbacks =
                Collections.unmodifiableList(singleCameraCaptureCallbacks);
        mErrorListeners = Collections.unmodifiableList(errorListeners);
        mRepeatingCaptureConfig = repeatingCaptureConfig;
    }

    /** Returns an instance of a session configuration with minimal configurations. */
    @NonNull
    public static SessionConfig defaultEmptySessionConfig() {
        return new SessionConfig(
                new ArrayList<DeferrableSurface>(),
                new ArrayList<CameraDevice.StateCallback>(0),
                new ArrayList<CameraCaptureSession.StateCallback>(0),
                new ArrayList<CameraCaptureCallback>(0),
                new ArrayList<>(0),
                new CaptureConfig.Builder().build());
    }

    @NonNull
    public List<DeferrableSurface> getSurfaces() {
        return Collections.unmodifiableList(mSurfaces);
    }

    @NonNull
    public Config getImplementationOptions() {
        return mRepeatingCaptureConfig.getImplementationOptions();
    }

    public int getTemplateType() {
        return mRepeatingCaptureConfig.getTemplateType();
    }

    /** Obtains all registered {@link CameraDevice.StateCallback} callbacks. */
    @NonNull
    public List<CameraDevice.StateCallback> getDeviceStateCallbacks() {
        return mDeviceStateCallbacks;
    }

    /** Obtains all registered {@link CameraCaptureSession.StateCallback} callbacks. */
    @NonNull
    public List<CameraCaptureSession.StateCallback> getSessionStateCallbacks() {
        return mSessionStateCallbacks;
    }

    /** Obtains all registered {@link CameraCaptureCallback} callbacks for repeating requests. */
    @NonNull
    public List<CameraCaptureCallback> getRepeatingCameraCaptureCallbacks() {
        return mRepeatingCaptureConfig.getCameraCaptureCallbacks();
    }

    /** Obtains all registered {@link ErrorListener} callbacks. */
    @NonNull
    public List<ErrorListener> getErrorListeners() {
        return mErrorListeners;
    }

    /** Obtains all registered {@link CameraCaptureCallback} callbacks for single requests. */
    @NonNull
    public List<CameraCaptureCallback> getSingleCameraCaptureCallbacks() {
        return mSingleCameraCaptureCallbacks;
    }

    @NonNull
    public CaptureConfig getRepeatingCaptureConfig() {
        return mRepeatingCaptureConfig;
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
     * Interface for unpacking a configuration into a SessionConfig.Builder
     *
     * <p>TODO(b/120949879): This will likely be removed once SessionConfig is refactored to
     * remove camera2 dependencies.
     */
    public interface OptionUnpacker {

        /**
         * Apply the options from the config onto the builder
         *
         * @param config  the set of options to apply
         * @param builder the builder on which to apply the options
         */
        void unpack(@NonNull UseCaseConfig<?> config, @NonNull SessionConfig.Builder builder);
    }

    /**
     * Base builder for easy modification/rebuilding of a {@link SessionConfig}.
     */
    static class BaseBuilder {
        // Use LinkedHashSet to preserve the adding order for bug fixing and testing.
        final Set<DeferrableSurface> mSurfaces = new LinkedHashSet<>();
        final CaptureConfig.Builder mCaptureConfigBuilder = new CaptureConfig.Builder();
        final List<CameraDevice.StateCallback> mDeviceStateCallbacks = new ArrayList<>();
        final List<CameraCaptureSession.StateCallback> mSessionStateCallbacks = new ArrayList<>();
        final List<ErrorListener> mErrorListeners = new ArrayList<>();
        final List<CameraCaptureCallback> mSingleCameraCaptureCallbacks = new ArrayList<>();
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
        @NonNull
        public static Builder createFrom(@NonNull UseCaseConfig<?> config) {
            OptionUnpacker unpacker = config.getSessionOptionUnpacker(null);
            if (unpacker == null) {
                throw new IllegalStateException(
                        "Implementation is missing option unpacker for "
                                + config.getTargetName(config.toString()));
            }

            Builder builder = new Builder();

            // Unpack the configuration into this builder
            unpacker.unpack(config, builder);
            return builder;
        }

        /**
         * Set the template characteristics of the SessionConfig.
         *
         * @param templateType Template constant that must match those defined by {@link
         *                     CameraDevice}
         *                     <p>TODO(b/120949879): This is camera2 implementation detail that
         *                     should be moved
         */
        public void setTemplateType(int templateType) {
            mCaptureConfigBuilder.setTemplateType(templateType);
        }

        /**
         * Adds a tag to the SessionConfig with a key. For tracking the source.
         */
        public void addTag(@NonNull String key, @NonNull Object tag) {
            mCaptureConfigBuilder.addTag(key, tag);
        }

        /**
         * Adds a {@link CameraDevice.StateCallback} callback.
         */
        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        public void addDeviceStateCallback(
                @NonNull CameraDevice.StateCallback deviceStateCallback) {
            if (mDeviceStateCallbacks.contains(deviceStateCallback)) {
                return;
            }
            mDeviceStateCallbacks.add(deviceStateCallback);
        }

        /**
         * Adds all {@link CameraDevice.StateCallback} callbacks.
         */
        public void addAllDeviceStateCallbacks(@NonNull
                Collection<CameraDevice.StateCallback> deviceStateCallbacks) {
            for (CameraDevice.StateCallback callback : deviceStateCallbacks) {
                addDeviceStateCallback(callback);
            }
        }

        /**
         * Adds a {@link CameraCaptureSession.StateCallback} callback.
         */
        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        public void addSessionStateCallback(@NonNull
                CameraCaptureSession.StateCallback sessionStateCallback) {
            if (mSessionStateCallbacks.contains(sessionStateCallback)) {
                return;
            }
            mSessionStateCallbacks.add(sessionStateCallback);
        }

        /**
         * Adds all {@link CameraCaptureSession.StateCallback} callbacks.
         */
        public void addAllSessionStateCallbacks(@NonNull
                List<CameraCaptureSession.StateCallback> sessionStateCallbacks) {
            for (CameraCaptureSession.StateCallback callback : sessionStateCallbacks) {
                addSessionStateCallback(callback);
            }
        }

        /**
         * Adds a {@link CameraCaptureCallback} callback for repeating requests.
         * <p>This callback does not call for single requests.
         */
        public void addRepeatingCameraCaptureCallback(
                @NonNull CameraCaptureCallback cameraCaptureCallback) {
            mCaptureConfigBuilder.addCameraCaptureCallback(cameraCaptureCallback);
        }

        /**
         * Adds all {@link CameraCaptureCallback} callbacks.
         * <p>These callbacks do not call for single requests.
         */
        public void addAllRepeatingCameraCaptureCallbacks(@NonNull
                Collection<CameraCaptureCallback> cameraCaptureCallbacks) {
            mCaptureConfigBuilder.addAllCameraCaptureCallbacks(cameraCaptureCallbacks);
        }

        /**
         * Adds a {@link CameraCaptureCallback} callback for single and repeating requests.
         * <p>Listeners added here are available in both the
         * {@link #getRepeatingCameraCaptureCallbacks()} and
         * {@link #getSingleCameraCaptureCallbacks()} methods.
         */
        public void addCameraCaptureCallback(@NonNull CameraCaptureCallback cameraCaptureCallback) {
            mCaptureConfigBuilder.addCameraCaptureCallback(cameraCaptureCallback);
            if (!mSingleCameraCaptureCallbacks.contains(cameraCaptureCallback)) {
                mSingleCameraCaptureCallbacks.add(cameraCaptureCallback);
            }
        }

        /**
         * Adds all {@link CameraCaptureCallback} callbacks for single and repeating requests.
         * <p>Listeners added here are available in both the
         * {@link #getRepeatingCameraCaptureCallbacks()} and
         * {@link #getSingleCameraCaptureCallbacks()} methods.
         */
        public void addAllCameraCaptureCallbacks(@NonNull
                Collection<CameraCaptureCallback> cameraCaptureCallbacks) {
            for (CameraCaptureCallback c : cameraCaptureCallbacks) {
                mCaptureConfigBuilder.addCameraCaptureCallback(c);
                if (!mSingleCameraCaptureCallbacks.contains(c)) {
                    mSingleCameraCaptureCallbacks.add(c);
                }
            }
        }

        /** Obtain all {@link CameraCaptureCallback} callbacks for single requests. */
        @NonNull
        public List<CameraCaptureCallback> getSingleCameraCaptureCallbacks() {
            return Collections.unmodifiableList(mSingleCameraCaptureCallbacks);
        }

        /**
         * Adds all {@link ErrorListener} listeners repeating requests.
         */
        public void addErrorListener(@NonNull ErrorListener errorListener) {
            mErrorListeners.add(errorListener);
        }


        /** Add a surface to the set that the session repeatedly writes data to. */
        public void addSurface(@NonNull DeferrableSurface surface) {
            mSurfaces.add(surface);
            mCaptureConfigBuilder.addSurface(surface);
        }

        /** Add a surface for the session which only used for single captures. */
        public void addNonRepeatingSurface(@NonNull DeferrableSurface surface) {
            mSurfaces.add(surface);
        }

        /** Remove a surface from the set which the session repeatedly writes to. */
        public void removeSurface(@NonNull DeferrableSurface surface) {
            mSurfaces.remove(surface);
            mCaptureConfigBuilder.removeSurface(surface);
        }

        /** Clears all surfaces from the set which the session writes to. */
        public void clearSurfaces() {
            mSurfaces.clear();
            mCaptureConfigBuilder.clearSurfaces();
        }

        /** Set the {@link Config} for options that are implementation specific. */
        public void setImplementationOptions(@NonNull Config config) {
            mCaptureConfigBuilder.setImplementationOptions(config);
        }

        /** Add a set of {@link Config} to the implementation specific options. */
        public void addImplementationOptions(@NonNull Config config) {
            mCaptureConfigBuilder.addImplementationOptions(config);
        }

        /**
         * Builds an instance of a SessionConfig that has all the combined parameters of the
         * SessionConfig that have been added to the Builder.
         */
        @NonNull
        public SessionConfig build() {
            return new SessionConfig(
                    new ArrayList<>(mSurfaces),
                    mDeviceStateCallbacks,
                    mSessionStateCallbacks,
                    mSingleCameraCaptureCallbacks,
                    mErrorListeners,
                    mCaptureConfigBuilder.build());
        }
    }

    /**
     * Builder for combining multiple instances of {@link SessionConfig}. This will check if all
     * the parameters for the {@link SessionConfig} are compatible with each other
     */
    public static final class ValidatingBuilder extends BaseBuilder {
        // Current supported session template values and prioritize from small to large.
        private static final List<Integer> SUPPORTED_TEMPLATE_PRIORITY = Arrays.asList(
                CameraDevice.TEMPLATE_PREVIEW,
                CameraDevice.TEMPLATE_RECORD
        );

        private static final String TAG = "ValidatingBuilder";
        private final SurfaceSorter mSurfaceSorter = new SurfaceSorter();
        private boolean mValid = true;
        private boolean mTemplateSet = false;

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
                        selectTemplateType(captureConfig.getTemplateType(),
                                mCaptureConfigBuilder.getTemplateType()));
            }

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

            mErrorListeners.addAll(sessionConfig.getErrorListeners());

            // Check surfaces
            mSurfaces.addAll(sessionConfig.getSurfaces());

            // Check capture request surfaces
            mCaptureConfigBuilder.getSurfaces().addAll(captureConfig.getSurfaces());

            if (!mSurfaces.containsAll(mCaptureConfigBuilder.getSurfaces())) {
                String errorMessage =
                        "Invalid configuration due to capture request surfaces are not a subset "
                                + "of surfaces";
                Logger.d(TAG, errorMessage);
                mValid = false;
            }

            // The conflicting of options is handled in addImplementationOptions where it could
            // throw an IllegalArgumentException if the conflict cannot be resolved.
            mCaptureConfigBuilder.addImplementationOptions(
                    captureConfig.getImplementationOptions());
        }

        /** Clears all surfaces from the set which the session writes to. */
        public void clearSurfaces() {
            mSurfaces.clear();
            mCaptureConfigBuilder.clearSurfaces();
        }

        /** Check if the set of SessionConfig that have been combined are valid */
        public boolean isValid() {
            return mTemplateSet && mValid;
        }

        /**
         * Builds an instance of a SessionConfig that has all the combined parameters of the
         * SessionConfig that have been added to the ValidatingBuilder.
         */
        @NonNull
        public SessionConfig build() {
            if (!mValid) {
                throw new IllegalArgumentException("Unsupported session configuration combination");
            }

            List<DeferrableSurface> surfaces = new ArrayList<>(mSurfaces);
            mSurfaceSorter.sort(surfaces);

            return new SessionConfig(
                    surfaces,
                    mDeviceStateCallbacks,
                    mSessionStateCallbacks,
                    mSingleCameraCaptureCallbacks,
                    mErrorListeners,
                    mCaptureConfigBuilder.build());
        }

        private int selectTemplateType(int type1, int type2) {
            return SUPPORTED_TEMPLATE_PRIORITY.indexOf(type1)
                    >= SUPPORTED_TEMPLATE_PRIORITY.indexOf(type2) ? type1 : type2;
        }
    }
}
