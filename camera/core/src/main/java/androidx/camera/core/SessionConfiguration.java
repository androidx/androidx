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
 * <p>The SessionConfiguration contains all the {@link android.hardware.camera2} parameters that are
 * required to initialize a {@link android.hardware.camera2.CameraCaptureSession} and issue a {@link
 * CaptureRequest}.
 *
 * @hide
 */
public final class SessionConfiguration {

    /** The set of {@link Surface} that data from the camera will be put into. */
    private final List<DeferrableSurface> surfaces;
    /** The state callback for a {@link CameraDevice}. */
    private final CameraDevice.StateCallback deviceStateCallback;
    /** The state callback for a {@link CameraCaptureSession}. */
    private final CameraCaptureSession.StateCallback sessionStateCallback;
    /** The configuration for building the {@link CaptureRequest}. */
    private final CaptureRequestConfiguration captureRequestConfiguration;

    /**
     * Private constructor for a SessionConfiguration.
     *
     * <p>In practice, the {@link SessionConfiguration.BaseBuilder} will be used to construct a
     * SessionConfiguration.
     *
     * @param surfaces                    The set of {@link Surface} where data will be put into.
     * @param deviceStateCallback         The state callback for a {@link CameraDevice}.
     * @param sessionStateCallback        The state callback for a {@link CameraCaptureSession}.
     * @param captureRequestConfiguration The configuration for building the {@link CaptureRequest}.
     */
    private SessionConfiguration(
            List<DeferrableSurface> surfaces,
            StateCallback deviceStateCallback,
            CameraCaptureSession.StateCallback sessionStateCallback,
            CaptureRequestConfiguration captureRequestConfiguration) {
        this.surfaces = surfaces;
        this.deviceStateCallback = deviceStateCallback;
        this.sessionStateCallback = sessionStateCallback;
        this.captureRequestConfiguration = captureRequestConfiguration;
    }

    /** Returns an instance of a session configuration with minimal configurations. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static SessionConfiguration defaultEmptySessionConfiguration() {
        return new SessionConfiguration(
                new ArrayList<>(),
                CameraDeviceStateCallbacks.createNoOpCallback(),
                CameraCaptureSessionStateCallbacks.createNoOpCallback(),
                new CaptureRequestConfiguration.Builder().build());
    }

    public List<DeferrableSurface> getSurfaces() {
        return Collections.unmodifiableList(surfaces);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public Map<Key<?>, CaptureRequestParameter<?>> getCameraCharacteristics() {
        return captureRequestConfiguration.getCameraCharacteristics();
    }

    public Configuration getImplementationOptions() {
        return captureRequestConfiguration.getImplementationOptions();
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getTemplateType() {
        return captureRequestConfiguration.getTemplateType();
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraDevice.StateCallback getDeviceStateCallback() {
        return deviceStateCallback;
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraCaptureSession.StateCallback getSessionStateCallback() {
        return sessionStateCallback;
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraCaptureCallback getCameraCaptureCallback() {
        return captureRequestConfiguration.getCameraCaptureCallback();
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public CaptureRequestConfiguration getCaptureRequestConfiguration() {
        return captureRequestConfiguration;
    }

    /**
     * Interface for unpacking a configuration into a SessionConfiguration.Builder
     *
     * <p>TODO(b/120949879): This will likely be removed once SessionConfiguration is refactored to
     * remove camera2 dependencies.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public interface OptionUnpacker {
        void unpack(UseCaseConfiguration<?> config, SessionConfiguration.Builder builder);
    }

    /** Base builder for easy modification/rebuilding of a {@link SessionConfiguration}. */
    static class BaseBuilder {
        protected final Set<DeferrableSurface> surfaces = new HashSet<>();
        protected final CaptureRequestConfiguration.Builder captureRequestConfigBuilder =
                new CaptureRequestConfiguration.Builder();
        protected CameraDevice.StateCallback deviceStateCallback =
                CameraDeviceStateCallbacks.createNoOpCallback();
        protected CameraCaptureSession.StateCallback sessionStateCallback =
                CameraCaptureSessionStateCallbacks.createNoOpCallback();
    }

    /** Builder for easy modification/rebuilding of a {@link SessionConfiguration}. */
    public static class Builder extends BaseBuilder {
        /**
         * Creates a {@link Builder} from a {@link UseCaseConfiguration}.
         *
         * <p>Populates the builder with all the properties defined in the base configuration.
         */
        public static Builder createFrom(UseCaseConfiguration<?> configuration) {
            OptionUnpacker unpacker = configuration.getOptionUnpacker(null);
            if (unpacker == null) {
                throw new IllegalStateException(
                        "Implementation is missing option unpacker for "
                                + configuration.getTargetName(configuration.toString()));
            }

            Builder builder = new Builder();

            // Unpack the configuration into this builder
            unpacker.unpack(configuration, builder);
            return builder;
        }

        /**
         * Set the template characteristics of the SessionConfiguration.
         *
         * @param templateType Template constant that must match those defined by {@link
         *                     CameraDevice}
         *                     <p>TODO(b/120949879): This is camera2 implementation detail that
         *                     should be moved
         */
        public void setTemplateType(int templateType) {
            captureRequestConfigBuilder.setTemplateType(templateType);
        }

        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        public void setDeviceStateCallback(CameraDevice.StateCallback deviceStateCallback) {
            this.deviceStateCallback = deviceStateCallback;
        }

        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        public void setSessionStateCallback(
                CameraCaptureSession.StateCallback sessionStateCallback) {
            this.sessionStateCallback = sessionStateCallback;
        }

        public void setCameraCaptureCallback(CameraCaptureCallback cameraCaptureCallback) {
            captureRequestConfigBuilder.setCameraCaptureCallback(cameraCaptureCallback);
        }

        public void addSurface(DeferrableSurface surface) {
            surfaces.add(surface);
            captureRequestConfigBuilder.addSurface(surface);
        }

        public void addNonRepeatingSurface(DeferrableSurface surface) {
            surfaces.add(surface);
        }

        public void removeSurface(DeferrableSurface surface) {
            surfaces.remove(surface);
            captureRequestConfigBuilder.removeSurface(surface);
        }

        public void clearSurfaces() {
            surfaces.clear();
            captureRequestConfigBuilder.clearSurfaces();
        }

        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        public <T> void addCharacteristic(Key<T> key, T value) {
            captureRequestConfigBuilder.addCharacteristic(key, value);
        }

        // TODO(b/120949879): This is camera2 implementation detail that should be moved
        @RestrictTo(Scope.LIBRARY_GROUP)
        public void addCharacteristics(Map<Key<?>, CaptureRequestParameter<?>> characteristics) {
            captureRequestConfigBuilder.addCharacteristics(characteristics);
        }

        public void setImplementationOptions(Configuration config) {
            captureRequestConfigBuilder.setImplementationOptions(config);
        }

        /**
         * Builds an instance of a SessionConfiguration that has all the combined parameters of the
         * SessionConfiguration that have been added to the Builder.
         */
        public SessionConfiguration build() {
            return new SessionConfiguration(
                    new ArrayList<>(surfaces),
                    deviceStateCallback,
                    sessionStateCallback,
                    captureRequestConfigBuilder.build());
        }
    }

    /**
     * Builder for combining multiple instances of {@link SessionConfiguration}. This will check if
     * all the parameters for the {@link SessionConfiguration} are compatible with each other
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class ValidatingBuilder extends BaseBuilder {
        private static final String TAG = "ValidatingBuilder";
        private final List<CameraDevice.StateCallback> deviceStateCallbacks = new ArrayList<>();
        private final List<CameraCaptureSession.StateCallback> sessionStateCallbacks =
                new ArrayList<>();
        private final List<CameraCaptureCallback> cameraCaptureCallbacks = new ArrayList<>();
        private boolean valid = true;
        private boolean templateSet = false;

        /**
         * Add the SessionConfiguration to the set of SessionConfiguration that have been aggregated
         * by the ValidatingBuilder
         */
        public void add(SessionConfiguration sessionConfiguration) {
            CaptureRequestConfiguration captureRequestConfiguration =
                    sessionConfiguration.getCaptureRequestConfiguration();

            // Check template
            if (!templateSet) {
                captureRequestConfigBuilder.setTemplateType(
                        captureRequestConfiguration.getTemplateType());
                templateSet = true;
            } else if (captureRequestConfigBuilder.getTemplateType()
                    != captureRequestConfiguration.getTemplateType()) {
                String errorMessage =
                        "Invalid configuration due to template type: "
                                + captureRequestConfigBuilder.getTemplateType()
                                + " != "
                                + captureRequestConfiguration.getTemplateType();
                Log.d(TAG, errorMessage);
                valid = false;
            }

            // Check device state callback
            deviceStateCallbacks.add(sessionConfiguration.getDeviceStateCallback());

            // Check session state callback
            sessionStateCallbacks.add(sessionConfiguration.getSessionStateCallback());

            // Check camera capture callback
            cameraCaptureCallbacks.add(captureRequestConfiguration.getCameraCaptureCallback());

            // Check surfaces
            surfaces.addAll(sessionConfiguration.getSurfaces());

            // Check capture request surfaces
            captureRequestConfigBuilder
                    .getSurfaces()
                    .addAll(captureRequestConfiguration.getSurfaces());

            captureRequestConfigBuilder.addImplementationOptions(
                    captureRequestConfiguration.getImplementationOptions());

            if (!surfaces.containsAll(captureRequestConfigBuilder.getSurfaces())) {
                String errorMessage =
                        "Invalid configuration due to capture request surfaces are not a subset "
                                + "of surfaces";
                Log.d(TAG, errorMessage);
                valid = false;
            }

            // Check characteristics
            for (Map.Entry<Key<?>, CaptureRequestParameter<?>> entry :
                    captureRequestConfiguration.getCameraCharacteristics().entrySet()) {
                Key<?> addedKey = entry.getKey();
                if (captureRequestConfigBuilder.getCharacteristic().containsKey(entry.getKey())) {
                    // value is equal
                    CaptureRequestParameter<?> addedValue = entry.getValue();
                    CaptureRequestParameter<?> oldValue =
                            captureRequestConfigBuilder.getCharacteristic().get(addedKey);
                    if (!addedValue.getValue().equals(oldValue.getValue())) {
                        String errorMessage =
                                "Invalid configuration due to conflicting CaptureRequest.Keys: "
                                        + addedValue
                                        + " != "
                                        + oldValue;
                        Log.d(TAG, errorMessage);
                        valid = false;
                    }
                } else {
                    captureRequestConfigBuilder
                            .getCharacteristic()
                            .put(entry.getKey(), entry.getValue());
                }
            }
        }

        /** Check if the set of SessionConfiguration that have been combined are valid */
        public boolean isValid() {
            return templateSet && valid;
        }

        /**
         * Builds an instance of a SessionConfiguration that has all the combined parameters of the
         * SessionConfiguration that have been added to the ValidatingBuilder.
         */
        public SessionConfiguration build() {
            if (!valid) {
                throw new IllegalArgumentException("Unsupported session configuration combination");
            }
            captureRequestConfigBuilder.setCameraCaptureCallback(
                    CameraCaptureCallbacks.createComboCallback(cameraCaptureCallbacks));
            return new SessionConfiguration(
                    new ArrayList<>(surfaces),
                    CameraDeviceStateCallbacks.createComboCallback(deviceStateCallbacks),
                    CameraCaptureSessionStateCallbacks.createComboCallback(sessionStateCallbacks),
                    captureRequestConfigBuilder.build());
        }
    }
}
