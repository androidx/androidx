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

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CaptureRequest;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.Config.Option;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Configurations needed for a capture session.
 *
 * <p>The SessionConfig contains all the {@link android.hardware.camera2} parameters that are
 * required to initialize a {@link android.hardware.camera2.CameraCaptureSession} and issue a {@link
 * CaptureRequest}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class SessionConfig {

    /** The set of {@link Surface} that data from the camera will be put into. */
    private final List<DeferrableSurface> mSurfaces;
    /** The state callback for a {@link CameraDevice}. */
    private final List<CameraDevice.StateCallback> mDeviceStateCallbacks;
    /** The state callback for a {@link CameraCaptureSession}. */
    private final List<CameraCaptureSession.StateCallback> mSessionStateCallbacks;
    /** The callbacks used in single requests. */
    private final List<CameraCaptureCallback> mSingleCameraCaptureCallbacks;
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
            CaptureConfig repeatingCaptureConfig) {
        mSurfaces = surfaces;
        mDeviceStateCallbacks = Collections.unmodifiableList(deviceStateCallbacks);
        mSessionStateCallbacks = Collections.unmodifiableList(sessionStateCallbacks);
        mSingleCameraCaptureCallbacks =
                Collections.unmodifiableList(singleCameraCaptureCallbacks);
        mRepeatingCaptureConfig = repeatingCaptureConfig;
    }

    /** Returns an instance of a session configuration with minimal configurations. */
    public static SessionConfig defaultEmptySessionConfig() {
        return new SessionConfig(
                new ArrayList<DeferrableSurface>(),
                new ArrayList<CameraDevice.StateCallback>(0),
                new ArrayList<CameraCaptureSession.StateCallback>(0),
                new ArrayList<CameraCaptureCallback>(0),
                new CaptureConfig.Builder().build());
    }

    public List<DeferrableSurface> getSurfaces() {
        return Collections.unmodifiableList(mSurfaces);
    }

    public Config getImplementationOptions() {
        return mRepeatingCaptureConfig.getImplementationOptions();
    }

    public int getTemplateType() {
        return mRepeatingCaptureConfig.getTemplateType();
    }

    /** Obtains all registered {@link CameraDevice.StateCallback} callbacks. */
    public List<CameraDevice.StateCallback> getDeviceStateCallbacks() {
        return mDeviceStateCallbacks;
    }

    /** Obtains all registered {@link CameraCaptureSession.StateCallback} callbacks. */
    public List<CameraCaptureSession.StateCallback> getSessionStateCallbacks() {
        return mSessionStateCallbacks;
    }

    /** Obtains all registered {@link CameraCaptureCallback} callbacks for repeating requests. */
    public List<CameraCaptureCallback> getRepeatingCameraCaptureCallbacks() {
        return mRepeatingCaptureConfig.getCameraCaptureCallbacks();
    }

    /** Obtains all registered {@link CameraCaptureCallback} callbacks for single requests. */
    public List<CameraCaptureCallback> getSingleCameraCaptureCallbacks() {
        return mSingleCameraCaptureCallbacks;
    }

    public CaptureConfig getRepeatingCaptureConfig() {
        return mRepeatingCaptureConfig;
    }

    /**
     * Interface for unpacking a configuration into a SessionConfig.Builder
     *
     * <p>TODO(b/120949879): This will likely be removed once SessionConfig is refactored to
     * remove camera2 dependencies.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public interface OptionUnpacker {

        /**
         * Apply the options from the config onto the builder
         * @param config the set of options to apply
         * @param builder the builder on which to apply the options
         */
        void unpack(UseCaseConfig<?> config, SessionConfig.Builder builder);
    }

    /**
     * Base builder for easy modification/rebuilding of a {@link SessionConfig}.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    static class BaseBuilder {
        protected final Set<DeferrableSurface> mSurfaces = new HashSet<>();
        protected final CaptureConfig.Builder mCaptureConfigBuilder = new CaptureConfig.Builder();
        protected List<CameraDevice.StateCallback> mDeviceStateCallbacks = new ArrayList<>();
        protected List<CameraCaptureSession.StateCallback> mSessionStateCallbacks =
                new ArrayList<>();
        protected final List<CameraCaptureCallback> mInteropCameraCaptureCallbacks =
                new ArrayList<>();
    }

    /**
     * Builder for easy modification/rebuilding of a {@link SessionConfig}.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static class Builder extends BaseBuilder {
        /**
         * Creates a {@link Builder} from a {@link UseCaseConfig}.
         *
         * <p>Populates the builder with all the properties defined in the base configuration.
         */
        public static Builder createFrom(UseCaseConfig<?> config) {
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
         * Set the tag of the SessionConfig. For tracking the source.
         */
        public void setTag(Object tag) {
            mCaptureConfigBuilder.setTag(tag);
        }

        /**
         * Adds a {@link CameraDevice.StateCallback} callback.
         * @throws IllegalArgumentException if the callback already exists in the configuration.
         */
        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        public void addDeviceStateCallback(CameraDevice.StateCallback deviceStateCallback) {
            if (mDeviceStateCallbacks.contains(deviceStateCallback)) {
                throw new IllegalArgumentException("Duplicate device state callback.");
            }
            mDeviceStateCallbacks.add(deviceStateCallback);
        }

        /**
         * Adds all {@link CameraDevice.StateCallback} callbacks.
         * * @throws IllegalArgumentException if any callback already exists in the configuration.
         */
        public void addAllDeviceStateCallbacks(
                Collection<CameraDevice.StateCallback> deviceStateCallbacks) {
            for (CameraDevice.StateCallback callback : deviceStateCallbacks) {
                addDeviceStateCallback(callback);
            }
        }

        /**
         * Adds a {@link CameraCaptureSession.StateCallback} callback.
         * @throws IllegalArgumentException if the callback already exists in the configuration.
         */
        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        public void addSessionStateCallback(
                CameraCaptureSession.StateCallback sessionStateCallback) {
            if (mSessionStateCallbacks.contains(sessionStateCallback)) {
                throw new IllegalArgumentException("Duplicate session state callback.");
            }
            mSessionStateCallbacks.add(sessionStateCallback);
        }

        /**
         * Adds all {@link CameraCaptureSession.StateCallback} callbacks.
         * @throws IllegalArgumentException if any callback already exists in the configuration.
         */
        public void addAllSessionStateCallbacks(
                List<CameraCaptureSession.StateCallback> sessionStateCallbacks) {
            for (CameraCaptureSession.StateCallback callback : sessionStateCallbacks) {
                addSessionStateCallback(callback);
            }
        }

        /**
         * Adds a {@link CameraCaptureCallback} callback for repeating requests.
         * <p>This callback does not call for single requests.
         * @throws IllegalArgumentException if the callback already exists in the configuration.
         */
        public void addRepeatingCameraCaptureCallback(CameraCaptureCallback cameraCaptureCallback) {
            mCaptureConfigBuilder.addCameraCaptureCallback(cameraCaptureCallback);
        }

        /**
         * Adds all {@link CameraCaptureCallback} callbacks.
         * <p>These callbacks do not call for single requests.
         * @throws IllegalArgumentException if any callback already exists in the configuration.
         */
        public void addAllRepeatingCameraCaptureCallbacks(
                Collection<CameraCaptureCallback> cameraCaptureCallbacks) {
            mCaptureConfigBuilder.addAllCameraCaptureCallbacks(cameraCaptureCallbacks);
        }

        /**
         * Adds a {@link CameraCaptureCallback} callback for single and repeating requests.
         * <p>Listeners added here are available in both the
         * {@link #getRepeatingCameraCaptureCallbacks()} and
         * {@link #getSingleCameraCaptureCallbacks()} methods.
         * @throws IllegalArgumentException if the callback already exists in the configuration.
         */
        public void addCameraCaptureCallback(CameraCaptureCallback cameraCaptureCallback) {
            mCaptureConfigBuilder.addCameraCaptureCallback(cameraCaptureCallback);
            mInteropCameraCaptureCallbacks.add(cameraCaptureCallback);
        }

        /**
         * Adds all {@link CameraCaptureCallback} callbacks for single and repeating requests.
         * <p>Listeners added here are available in both the
         * {@link #getRepeatingCameraCaptureCallbacks()} and
         * {@link #getSingleCameraCaptureCallbacks()} methods.
         * @throws IllegalArgumentException if any callback already exists in the configuration.
         */
        public void addAllCameraCaptureCallbacks(
                Collection<CameraCaptureCallback> cameraCaptureCallbacks) {
            mCaptureConfigBuilder.addAllCameraCaptureCallbacks(cameraCaptureCallbacks);
            mInteropCameraCaptureCallbacks.addAll(cameraCaptureCallbacks);
        }

        /** Obtain all {@link CameraCaptureCallback} callbacks for single requests. */
        public List<CameraCaptureCallback> getSingleCameraCaptureCallbacks() {
            return Collections.unmodifiableList(mInteropCameraCaptureCallbacks);
        }


        /** Add a surface to the set that the session repeatedly writes data to. */
        public void addSurface(DeferrableSurface surface) {
            mSurfaces.add(surface);
            mCaptureConfigBuilder.addSurface(surface);
        }

        /** Add a surface for the session which only used for single captures. */
        public void addNonRepeatingSurface(DeferrableSurface surface) {
            mSurfaces.add(surface);
        }

        /** Remove a surface from the set which the session repeatedly writes to. */
        public void removeSurface(DeferrableSurface surface) {
            mSurfaces.remove(surface);
            mCaptureConfigBuilder.removeSurface(surface);
        }

        /** Clears all surfaces from the set which the session writes to. */
        public void clearSurfaces() {
            mSurfaces.clear();
            mCaptureConfigBuilder.clearSurfaces();
        }

        /** Set the {@link Config} for options that are implementation specific. */
        public void setImplementationOptions(Config config) {
            mCaptureConfigBuilder.setImplementationOptions(config);
        }

        /** Add a set of {@link Config} to the implementation specific options. */
        public void addImplementationOptions(Config config) {
            mCaptureConfigBuilder.addImplementationOptions(config);
        }

        /**
         * Builds an instance of a SessionConfig that has all the combined parameters of the
         * SessionConfig that have been added to the Builder.
         */
        public SessionConfig build() {
            return new SessionConfig(
                    new ArrayList<>(mSurfaces),
                    mDeviceStateCallbacks,
                    mSessionStateCallbacks,
                    mInteropCameraCaptureCallbacks,
                    mCaptureConfigBuilder.build());
        }
    }

    /**
     * Builder for combining multiple instances of {@link SessionConfig}. This will check if all
     * the parameters for the {@link SessionConfig} are compatible with each other
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class ValidatingBuilder extends BaseBuilder {
        private static final String TAG = "ValidatingBuilder";
        private final List<CameraDevice.StateCallback> mDeviceStateCallbacks = new ArrayList<>();
        private final List<CameraCaptureSession.StateCallback> mSessionStateCallbacks =
                new ArrayList<>();
        private final List<CameraCaptureCallback> mSingleCameraCaptureCallbacks =
                new ArrayList<>();
        private boolean mValid = true;
        private boolean mTemplateSet = false;

        /**
         * Add the SessionConfig to the set of SessionConfig that have been aggregated by the
         * ValidatingBuilder
         */
        public void add(SessionConfig sessionConfig) {
            CaptureConfig captureConfig = sessionConfig.getRepeatingCaptureConfig();

            // Check template
            if (!mTemplateSet) {
                mCaptureConfigBuilder.setTemplateType(captureConfig.getTemplateType());
                mTemplateSet = true;
            } else if (mCaptureConfigBuilder.getTemplateType() != captureConfig.getTemplateType()) {
                String errorMessage =
                        "Invalid configuration due to template type: "
                                + mCaptureConfigBuilder.getTemplateType()
                                + " != "
                                + captureConfig.getTemplateType();
                Log.d(TAG, errorMessage);
                mValid = false;
            }

            Object tag = sessionConfig.getRepeatingCaptureConfig().getTag();
            if (tag != null) {
                mCaptureConfigBuilder.setTag(tag);
            }

            // Check device state callbacks
            mDeviceStateCallbacks.addAll(sessionConfig.getDeviceStateCallbacks());

            // Check session state callbacks
            mSessionStateCallbacks.addAll(sessionConfig.getSessionStateCallbacks());

            // Check camera capture callbacks for repeating requests.
            mCaptureConfigBuilder.addAllCameraCaptureCallbacks(
                    sessionConfig.getRepeatingCameraCaptureCallbacks());

            // Check camera capture callbacks for single requests.
            mSingleCameraCaptureCallbacks.addAll(sessionConfig.getSingleCameraCaptureCallbacks());

            // Check surfaces
            mSurfaces.addAll(sessionConfig.getSurfaces());

            // Check capture request surfaces
            mCaptureConfigBuilder.getSurfaces().addAll(captureConfig.getSurfaces());

            if (!mSurfaces.containsAll(mCaptureConfigBuilder.getSurfaces())) {
                String errorMessage =
                        "Invalid configuration due to capture request surfaces are not a subset "
                                + "of surfaces";
                Log.d(TAG, errorMessage);
                mValid = false;
            }

            // Check options
            Config newOptions = captureConfig.getImplementationOptions();
            Config oldOptions = mCaptureConfigBuilder.getImplementationOptions();
            MutableOptionsBundle addedOptions = MutableOptionsBundle.create();
            for (Option<?> option : newOptions.listOptions()) {
                @SuppressWarnings("unchecked")
                Option<Object> typeErasedOption = (Option<Object>) option;
                Object newValue = newOptions.retrieveOption(typeErasedOption, null);
                if (!(newValue instanceof MultiValueSet)
                        && oldOptions.containsOption(typeErasedOption)) {
                    Object oldValue = oldOptions.retrieveOption(typeErasedOption, null);
                    if (!Objects.equals(newValue, oldValue)) {
                        String errorMessage =
                                "Invalid configuration due to conflicting option: "
                                        + typeErasedOption.getId()
                                        + " : "
                                        + newValue
                                        + " != "
                                        + oldValue;
                        Log.d(TAG, errorMessage);
                        mValid = false;
                    }
                } else {
                    addedOptions.insertOption(typeErasedOption,
                            newOptions.retrieveOption(typeErasedOption));
                }
            }
            mCaptureConfigBuilder.addImplementationOptions(addedOptions);
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
            return new SessionConfig(
                    new ArrayList<>(mSurfaces),
                    mDeviceStateCallbacks,
                    mSessionStateCallbacks,
                    mSingleCameraCaptureCallbacks,
                    mCaptureConfigBuilder.build());
        }
    }
}
