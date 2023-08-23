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

package androidx.camera.camera2.impl;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.camera2.interop.CaptureRequestOptions;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.ExtendableBuilder;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;

/**
 * Internal shared implementation details for camera 2 interop.
 */
@OptIn(markerClass = ExperimentalCamera2Interop.class)
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Camera2ImplConfig extends CaptureRequestOptions {

    @RestrictTo(Scope.LIBRARY)
    public static final String CAPTURE_REQUEST_ID_STEM = "camera2.captureRequest.option.";

    // Option Declarations:
    // *********************************************************************************************

    @RestrictTo(Scope.LIBRARY)
    public static final Config.Option<Integer> TEMPLATE_TYPE_OPTION =
            Option.create("camera2.captureRequest.templateType", int.class);
    @RestrictTo(Scope.LIBRARY)
    public static final Config.Option<Long> STREAM_USE_CASE_OPTION =
            Option.create("camera2.cameraCaptureSession.streamUseCase", long.class);
    @RestrictTo(Scope.LIBRARY)
    public static final Option<CameraDevice.StateCallback> DEVICE_STATE_CALLBACK_OPTION =
            Option.create("camera2.cameraDevice.stateCallback", CameraDevice.StateCallback.class);
    @RestrictTo(Scope.LIBRARY)
    public static final Option<CameraCaptureSession.StateCallback> SESSION_STATE_CALLBACK_OPTION =
            Option.create(
                    "camera2.cameraCaptureSession.stateCallback",
                    CameraCaptureSession.StateCallback.class);
    @RestrictTo(Scope.LIBRARY)
    public static final Option<CameraCaptureSession.CaptureCallback>
            SESSION_CAPTURE_CALLBACK_OPTION =
            Option.create("camera2.cameraCaptureSession.captureCallback",
                    CameraCaptureSession.CaptureCallback.class);
    @RestrictTo(Scope.LIBRARY)
    public static final Option<Object> CAPTURE_REQUEST_TAG_OPTION = Option.create(
            "camera2.captureRequest.tag", Object.class);

    @RestrictTo(Scope.LIBRARY)
    public static final Option<String> SESSION_PHYSICAL_CAMERA_ID_OPTION = Option.create(
            "camera2.cameraCaptureSession.physicalCameraId", String.class);

    // *********************************************************************************************

    /**
     * Creates a Camera2ImplConfig for reading Camera2 options from the given config.
     *
     * @param config The config that potentially contains Camera2 options.
     */
    public Camera2ImplConfig(@NonNull Config config) {
        super(config);
    }

    // Unfortunately, we can't get the Class<T> from the CaptureRequest.Key, so we're forced to
    // erase the type. This shouldn't be a problem as long as we are only using these options
    // within the Camera2ImplConfig and Camera2ImplConfig.Builder classes.

    @RestrictTo(Scope.LIBRARY)
    @NonNull
    public static Option<Object> createCaptureRequestOption(@NonNull CaptureRequest.Key<?> key) {
        return Option.create(CAPTURE_REQUEST_ID_STEM + key.getName(), Object.class, key);
    }

    /**
     * Returns all capture request options contained in this configuration.
     *
     */
    @RestrictTo(Scope.LIBRARY)
    @NonNull
    public CaptureRequestOptions getCaptureRequestOptions() {
        return CaptureRequestOptions.Builder.from(getConfig()).build();
    }

    /**
     * Returns a CameraDevice template on the given configuration. Requires API 33 or above.
     *
     * <p>See {@link android.hardware.camera2.CameraMetadata} for valid stream use cases.
     * See {@link android.hardware.camera2.params.OutputConfiguration} to see how
     * camera2 framework uses this.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public long getStreamUseCase(long valueIfMissing) {
        return getConfig().retrieveOption(STREAM_USE_CASE_OPTION, valueIfMissing);
    }

    /**
     * Returns the CameraDevice template from the given configuration.
     *
     * <p>See {@link CameraDevice} for valid template types. For example, {@link
     * CameraDevice#TEMPLATE_PREVIEW}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getCaptureRequestTemplate(int valueIfMissing) {
        return getConfig().retrieveOption(TEMPLATE_TYPE_OPTION, valueIfMissing);
    }

    /**
     * Returns the stored {@link CameraDevice.StateCallback}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    public CameraDevice.StateCallback getDeviceStateCallback(
            @Nullable CameraDevice.StateCallback valueIfMissing) {
        return getConfig().retrieveOption(DEVICE_STATE_CALLBACK_OPTION, valueIfMissing);
    }


    /**
     * Returns the stored {@link CameraCaptureSession.StateCallback}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    public CameraCaptureSession.StateCallback getSessionStateCallback(
            @Nullable CameraCaptureSession.StateCallback valueIfMissing) {
        return getConfig().retrieveOption(SESSION_STATE_CALLBACK_OPTION, valueIfMissing);
    }

    /**
     * Returns the stored {@link CameraCaptureSession.CaptureCallback}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    public CameraCaptureSession.CaptureCallback getSessionCaptureCallback(
            @Nullable CameraCaptureSession.CaptureCallback valueIfMissing) {
        return getConfig().retrieveOption(SESSION_CAPTURE_CALLBACK_OPTION, valueIfMissing);
    }

    /**
     * Returns the capture request tag.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    public Object getCaptureRequestTag(@Nullable Object valueIfMissing) {
        return getConfig().retrieveOption(CAPTURE_REQUEST_TAG_OPTION, valueIfMissing);
    }

    /**
     * Returns the physical camera ID.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    public String getPhysicalCameraId(@Nullable String valueIfMissing) {
        return getConfig().retrieveOption(SESSION_PHYSICAL_CAMERA_ID_OPTION, valueIfMissing);
    }

    /**
     * Builder for creating {@link Camera2ImplConfig} instance.
     *
     * <p>Use {@link Camera2ImplConfig.Builder} for creating {@link Config} which contains
     * camera2 options only. And use
     * {@link androidx.camera.camera2.interop.Camera2Interop.Extender} to add Camera2 options on
     * existing other {@link
     * ExtendableBuilder}.
     */
    public static final class Builder implements ExtendableBuilder<Camera2ImplConfig> {

        private final MutableOptionsBundle mMutableOptionsBundle = MutableOptionsBundle.create();

        @Override
        @NonNull
        public MutableConfig getMutableConfig() {
            return mMutableOptionsBundle;
        }

        /**
         * Inserts new capture request option with specific {@link CaptureRequest.Key} setting.
         */
        @NonNull
        public <ValueT> Camera2ImplConfig.Builder setCaptureRequestOption(
                @NonNull CaptureRequest.Key<ValueT> key, @NonNull ValueT value) {
            Option<Object> opt = Camera2ImplConfig.createCaptureRequestOption(key);
            mMutableOptionsBundle.insertOption(opt, value);
            return this;
        }

        /**
         * Inserts new capture request option with specific {@link CaptureRequest.Key} setting and
         * {@link OptionPriority}.
         */
        @NonNull
        public <ValueT> Builder setCaptureRequestOptionWithPriority(
                @NonNull CaptureRequest.Key<ValueT> key, @NonNull ValueT value,
                @NonNull OptionPriority priority) {
            Option<Object> opt = Camera2ImplConfig.createCaptureRequestOption(key);
            mMutableOptionsBundle.insertOption(opt, priority, value);
            return this;
        }

        /** Inserts options from other {@link Config} object. */
        @NonNull
        public Camera2ImplConfig.Builder insertAllOptions(@NonNull Config config) {
            for (Option<?> option : config.listOptions()) {
                @SuppressWarnings("unchecked") // Options/values are being copied directly
                Option<Object> objectOpt = (Option<Object>) option;
                mMutableOptionsBundle.insertOption(objectOpt, config.retrieveOption(objectOpt));
            }
            return this;
        }

        /**
         * Builds an immutable {@link Camera2ImplConfig} from the current state.
         *
         * @return A {@link Camera2ImplConfig} populated with the current state.
         */
        @Override
        @NonNull
        public Camera2ImplConfig build() {
            return new Camera2ImplConfig(OptionsBundle.from(mMutableOptionsBundle));
        }
    }
}
