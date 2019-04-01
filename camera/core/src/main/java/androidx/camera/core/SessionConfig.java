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
import android.hardware.camera2.CaptureRequest.Key;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final CameraDevice.StateCallback mDeviceStateCallback;
    /** The state callback for a {@link CameraCaptureSession}. */
    private final CameraCaptureSession.StateCallback mSessionStateCallback;
    /** The configuration for building the {@link CaptureRequest}. */
    private final CaptureRequestConfig mCaptureRequestConfig;

    /**
     * Private constructor for a SessionConfig.
     *
     * <p>In practice, the {@link SessionConfig.BaseBuilder} will be used to construct a
     * SessionConfig.
     *
     * @param surfaces             The set of {@link Surface} where data will be put into.
     * @param deviceStateCallback  The state callback for a {@link CameraDevice}.
     * @param sessionStateCallback The state callback for a {@link CameraCaptureSession}.
     * @param captureRequestConfig The configuration for building the {@link CaptureRequest}.
     */
    SessionConfig(
            List<DeferrableSurface> surfaces,
            StateCallback deviceStateCallback,
            CameraCaptureSession.StateCallback sessionStateCallback,
            CaptureRequestConfig captureRequestConfig) {
        mSurfaces = surfaces;
        mDeviceStateCallback = deviceStateCallback;
        mSessionStateCallback = sessionStateCallback;
        mCaptureRequestConfig = captureRequestConfig;
    }

    /** Returns an instance of a session configuration with minimal configurations. */
    public static SessionConfig defaultEmptySessionConfig() {
        return new SessionConfig(
                new ArrayList<DeferrableSurface>(),
                CameraDeviceStateCallbacks.createNoOpCallback(),
                CameraCaptureSessionStateCallbacks.createNoOpCallback(),
                new CaptureRequestConfig.Builder().build());
    }

    public List<DeferrableSurface> getSurfaces() {
        return Collections.unmodifiableList(mSurfaces);
    }

    public Map<Key<?>, CaptureRequestParameter<?>> getCameraCharacteristics() {
        return mCaptureRequestConfig.getCameraCharacteristics();
    }

    public Config getImplementationOptions() {
        return mCaptureRequestConfig.getImplementationOptions();
    }

    public int getTemplateType() {
        return mCaptureRequestConfig.getTemplateType();
    }

    public CameraDevice.StateCallback getDeviceStateCallback() {
        return mDeviceStateCallback;
    }

    public CameraCaptureSession.StateCallback getSessionStateCallback() {
        return mSessionStateCallback;
    }

    public CameraCaptureCallback getCameraCaptureCallback() {
        return mCaptureRequestConfig.getCameraCaptureCallback();
    }

    public CaptureRequestConfig getCaptureRequestConfig() {
        return mCaptureRequestConfig;
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
        protected final CaptureRequestConfig.Builder mCaptureRequestConfigBuilder =
                new CaptureRequestConfig.Builder();
        protected CameraDevice.StateCallback mDeviceStateCallback =
                CameraDeviceStateCallbacks.createNoOpCallback();
        protected CameraCaptureSession.StateCallback mSessionStateCallback =
                CameraCaptureSessionStateCallbacks.createNoOpCallback();
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
            OptionUnpacker unpacker = config.getOptionUnpacker(null);
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
            mCaptureRequestConfigBuilder.setTemplateType(templateType);
        }

        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        public void setDeviceStateCallback(CameraDevice.StateCallback deviceStateCallback) {
            mDeviceStateCallback = deviceStateCallback;
        }

        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        public void setSessionStateCallback(
                CameraCaptureSession.StateCallback sessionStateCallback) {
            mSessionStateCallback = sessionStateCallback;
        }

        /** Set the {@link CameraCaptureCallback}. */
        public void setCameraCaptureCallback(CameraCaptureCallback cameraCaptureCallback) {
            mCaptureRequestConfigBuilder.setCameraCaptureCallback(cameraCaptureCallback);
        }

        /** Add a surface to the set that the session repeatedly writes data to. */
        public void addSurface(DeferrableSurface surface) {
            mSurfaces.add(surface);
            mCaptureRequestConfigBuilder.addSurface(surface);
        }

        /** Add a surface for the session which only used for single captures. */
        public void addNonRepeatingSurface(DeferrableSurface surface) {
            mSurfaces.add(surface);
        }

        /** Remove a surface from the set which the session repeatedly writes to. */
        public void removeSurface(DeferrableSurface surface) {
            mSurfaces.remove(surface);
            mCaptureRequestConfigBuilder.removeSurface(surface);
        }

        /** Clears all surfaces from the set which the session writes to. */
        public void clearSurfaces() {
            mSurfaces.clear();
            mCaptureRequestConfigBuilder.clearSurfaces();
        }

        /** Add the {@link CaptureRequest.Key} value pair that will be applied. */
        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        public <T> void addCharacteristic(Key<T> key, T value) {
            mCaptureRequestConfigBuilder.addCharacteristic(key, value);
        }

        /** Add the {@link CaptureRequestParameter} that will be applied. */
        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        public void addCharacteristics(Map<Key<?>, CaptureRequestParameter<?>> characteristics) {
            mCaptureRequestConfigBuilder.addCharacteristics(characteristics);
        }

        /** Set the {@link Config} for options that are implementation specific. */
        public void setImplementationOptions(Config config) {
            mCaptureRequestConfigBuilder.setImplementationOptions(config);
        }

        /**
         * Builds an instance of a SessionConfig that has all the combined parameters of the
         * SessionConfig that have been added to the Builder.
         */
        public SessionConfig build() {
            return new SessionConfig(
                    new ArrayList<>(mSurfaces),
                    mDeviceStateCallback,
                    mSessionStateCallback,
                    mCaptureRequestConfigBuilder.build());
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
        private final List<CameraCaptureCallback> mCameraCaptureCallbacks = new ArrayList<>();
        private boolean mValid = true;
        private boolean mTemplateSet = false;

        /**
         * Add the SessionConfig to the set of SessionConfig that have been aggregated by the
         * ValidatingBuilder
         */
        public void add(SessionConfig sessionConfig) {
            CaptureRequestConfig captureRequestConfig = sessionConfig.getCaptureRequestConfig();

            // Check template
            if (!mTemplateSet) {
                mCaptureRequestConfigBuilder.setTemplateType(
                        captureRequestConfig.getTemplateType());
                mTemplateSet = true;
            } else if (mCaptureRequestConfigBuilder.getTemplateType()
                    != captureRequestConfig.getTemplateType()) {
                String errorMessage =
                        "Invalid configuration due to template type: "
                                + mCaptureRequestConfigBuilder.getTemplateType()
                                + " != "
                                + captureRequestConfig.getTemplateType();
                Log.d(TAG, errorMessage);
                mValid = false;
            }

            // Check device state callback
            mDeviceStateCallbacks.add(sessionConfig.getDeviceStateCallback());

            // Check session state callback
            mSessionStateCallbacks.add(sessionConfig.getSessionStateCallback());

            // Check camera capture callback
            mCameraCaptureCallbacks.add(captureRequestConfig.getCameraCaptureCallback());

            // Check surfaces
            mSurfaces.addAll(sessionConfig.getSurfaces());

            // Check capture request surfaces
            mCaptureRequestConfigBuilder
                    .getSurfaces()
                    .addAll(captureRequestConfig.getSurfaces());

            mCaptureRequestConfigBuilder.addImplementationOptions(
                    captureRequestConfig.getImplementationOptions());

            if (!mSurfaces.containsAll(mCaptureRequestConfigBuilder.getSurfaces())) {
                String errorMessage =
                        "Invalid configuration due to capture request surfaces are not a subset "
                                + "of surfaces";
                Log.d(TAG, errorMessage);
                mValid = false;
            }

            // Check characteristics
            for (Map.Entry<Key<?>, CaptureRequestParameter<?>> entry :
                    captureRequestConfig.getCameraCharacteristics().entrySet()) {
                Key<?> addedKey = entry.getKey();
                if (mCaptureRequestConfigBuilder.getCharacteristic().containsKey(entry.getKey())) {
                    // value is equal
                    CaptureRequestParameter<?> addedValue = entry.getValue();
                    CaptureRequestParameter<?> oldValue =
                            mCaptureRequestConfigBuilder.getCharacteristic().get(addedKey);
                    if (!addedValue.getValue().equals(oldValue.getValue())) {
                        String errorMessage =
                                "Invalid configuration due to conflicting CaptureRequest.Keys: "
                                        + addedValue
                                        + " != "
                                        + oldValue;
                        Log.d(TAG, errorMessage);
                        mValid = false;
                    }
                } else {
                    mCaptureRequestConfigBuilder
                            .getCharacteristic()
                            .put(entry.getKey(), entry.getValue());
                }
            }
        }

        /** Check if the set of SessionConfig that have been combined are valid */
        public boolean isValid() {
            return mTemplateSet && mValid;
        }

        /**
         * Builds an instance of a SessionConfig that has all the combined parameters of the
         * SessionConfig that have been added to the ValidatingBuilder.
         */
        public SessionConfig build() {
            if (!mValid) {
                throw new IllegalArgumentException("Unsupported session configuration combination");
            }
            mCaptureRequestConfigBuilder.setCameraCaptureCallback(
                    CameraCaptureCallbacks.createComboCallback(mCameraCaptureCallbacks));
            return new SessionConfig(
                    new ArrayList<>(mSurfaces),
                    CameraDeviceStateCallbacks.createComboCallback(mDeviceStateCallbacks),
                    CameraCaptureSessionStateCallbacks.createComboCallback(mSessionStateCallbacks),
                    mCaptureRequestConfigBuilder.build());
        }
    }
}
