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

package androidx.camera.camera2;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.Config;
import androidx.camera.core.MutableConfig;
import androidx.camera.core.MutableOptionsBundle;
import androidx.camera.core.OptionsBundle;

import java.util.HashSet;
import java.util.Set;

/** Configuration options related to the {@link android.hardware.camera2} APIs. */
public final class Camera2Config implements Config.Reader {

    static final String CAPTURE_REQUEST_ID_STEM = "camera2.captureRequest.option.";
    static final Option<Integer> TEMPLATE_TYPE_OPTION =
            Option.create("camera2.captureRequest.templateType", int.class);
    static final Option<StateCallback> DEVICE_STATE_CALLBACK_OPTION =
            Option.create("camera2.cameraDevice.stateCallback", StateCallback.class);
    static final Option<CameraCaptureSession.StateCallback> SESSION_STATE_CALLBACK_OPTION =
            Option.create(
                    "camera2.cameraCaptureSession.stateCallback",
                    CameraCaptureSession.StateCallback.class);
    static final Option<CaptureCallback> SESSION_CAPTURE_CALLBACK_OPTION =
            Option.create("camera2.cameraCaptureSession.captureCallback", CaptureCallback.class);
    private final Config mConfig;

    /**
     * Creates a Camera2Config for reading Camera2 options from the given config.
     *
     * @param config The config that potentially contains Camera2 options.
     */
    public Camera2Config(Config config) {
        mConfig = config;
    }

    // Unfortunately, we can't get the Class<T> from the CaptureRequest.Key, so we're forced to
    // erase the type. This shouldn't be a problem as long as we are only using these options
    // within the Camera2Config and Camera2Config.Builder classes.
    static Option<Object> createCaptureRequestOption(CaptureRequest.Key<?> key) {
        return Option.create(CAPTURE_REQUEST_ID_STEM + key.getName(), Object.class, key);
    }

    /**
     * Returns a value for the given {@link CaptureRequest.Key}.
     *
     * @param key            The key to retrieve.
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @param <ValueT>       The type of the value.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public <ValueT> ValueT getCaptureRequestOption(
            CaptureRequest.Key<ValueT> key, @Nullable ValueT valueIfMissing) {
        @SuppressWarnings(
                "unchecked") // Type should have been only set via Builder#setCaptureRequestOption()
                Option<ValueT> opt = (Option<ValueT>) Camera2Config.createCaptureRequestOption(key);
        return getConfig().retrieveOption(opt, valueIfMissing);
    }

    /** Returns all capture request options contained in this configuration. */
    Set<Option<?>> getCaptureRequestOptions() {
        final Set<Option<?>> optionSet = new HashSet<>();
        findOptions(
                Camera2Config.CAPTURE_REQUEST_ID_STEM,
                new OptionMatcher() {
                    @Override
                    public boolean onOptionMatched(Option<?> option) {
                        optionSet.add(option);
                        return true;
                    }
                });
        return optionSet;
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
    int getCaptureRequestTemplate(int valueIfMissing) {
        return getConfig().retrieveOption(TEMPLATE_TYPE_OPTION, valueIfMissing);
    }

    /**
     * Returns the stored {@link CameraDevice.StateCallback}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public CameraDevice.StateCallback getDeviceStateCallback(
            CameraDevice.StateCallback valueIfMissing) {
        return getConfig().retrieveOption(DEVICE_STATE_CALLBACK_OPTION, valueIfMissing);
    }

    /**
     * Returns the stored {@link CameraCaptureSession.StateCallback}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public CameraCaptureSession.StateCallback getSessionStateCallback(
            CameraCaptureSession.StateCallback valueIfMissing) {
        return getConfig().retrieveOption(SESSION_STATE_CALLBACK_OPTION, valueIfMissing);
    }

    // Option Declarations:
    // *********************************************************************************************

    /**
     * Returns the stored {@link CameraCaptureSession.CaptureCallback}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public CameraCaptureSession.CaptureCallback getSessionCaptureCallback(
            CameraCaptureSession.CaptureCallback valueIfMissing) {
        return getConfig().retrieveOption(SESSION_CAPTURE_CALLBACK_OPTION, valueIfMissing);
    }

    /**
     * Returns the underlying immutable {@link Config} object.
     *
     * @return The underlying {@link Config} object.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Config getConfig() {
        return mConfig;
    }

    /** Extends a {@link Config.Builder} to add Camera2 options. */
    public static final class Extender {

        Config.Builder<?, ?> mBaseBuilder;

        /**
         * Creates an Extender that can be used to add Camera2 options to another Builder.
         *
         * @param baseBuilder The builder being extended.
         */
        public Extender(Config.Builder<?, ?> baseBuilder) {
            mBaseBuilder = baseBuilder;
        }

        /**
         * Sets a {@link CaptureRequest.Key} and Value on the configuration.
         *
         * @param key      The {@link CaptureRequest.Key} which will be set.
         * @param value    The value for the key.
         * @param <ValueT> The type of the value.
         * @return The current Extender.
         */
        public <ValueT> Extender setCaptureRequestOption(
                CaptureRequest.Key<ValueT> key, ValueT value) {
            // Reify the type so we can obtain the class
            Option<Object> opt = Camera2Config.createCaptureRequestOption(key);
            mBaseBuilder.insertOption(opt, value);
            return this;
        }

        /**
         * Sets a CameraDevice template on the given configuration.
         *
         * <p>See {@link CameraDevice} for valid template types. For example, {@link
         * CameraDevice#TEMPLATE_PREVIEW}.
         *
         * @param templateType The template type to set.
         * @return The current Extender.
         */
        Extender setCaptureRequestTemplate(int templateType) {
            mBaseBuilder.insertOption(TEMPLATE_TYPE_OPTION, templateType);
            return this;
        }

        /**
         * Sets a {@link CameraDevice.StateCallback}.
         *
         * <p>The caller is expected to use the {@link CameraDevice} instance accessed through the
         * callback methods responsibly. Generally safe usages include: (1) querying the device for
         * its id, (2) using the callbacks to determine what state the device is currently in.
         * Generally unsafe usages include: (1) creating a new {@link CameraCaptureSession}, (2)
         * creating a new {@link CaptureRequest}, (3) closing the device. When the caller uses the
         * device beyond the safe usage limits, the usage may still work in conjunction with
         * CameraX, but any strong guarantees provided by CameraX about the validity of the camera
         * state become void.
         *
         * @param stateCallback The {@link CameraDevice.StateCallback}.
         * @return The current Extender.
         */
        public Extender setDeviceStateCallback(CameraDevice.StateCallback stateCallback) {
            mBaseBuilder.insertOption(DEVICE_STATE_CALLBACK_OPTION, stateCallback);
            return this;
        }

        /**
         * Sets a {@link CameraCaptureSession.StateCallback}.
         *
         * <p>The caller is expected to use the {@link CameraCaptureSession} instance accessed
         * through the callback methods responsibly. Generally safe usages include: (1) querying the
         * session for its properties, (2) using the callbacks to determine what state the session
         * is currently in. Generally unsafe usages include: (1) submitting a new {@link
         * CaptureRequest}, (2) stopping an existing {@link CaptureRequest}, (3) closing the
         * session, (4) attaching a new {@link Surface} to the session. When the caller uses the
         * session beyond the safe usage limits, the usage may still work in conjunction with
         * CameraX, but any strong gurantees provided by CameraX about the validity of the camera
         * state become void.
         *
         * @param stateCallback The {@link CameraCaptureSession.StateCallback}.
         * @return The current Extender.
         */
        public Extender setSessionStateCallback(CameraCaptureSession.StateCallback stateCallback) {
            mBaseBuilder.insertOption(SESSION_STATE_CALLBACK_OPTION, stateCallback);
            return this;
        }

        /**
         * Sets a {@link CameraCaptureSession.CaptureCallback}.
         *
         * <p>The caller is expected to use the {@link CameraCaptureSession} instance accessed
         * through the callback methods responsibly. Generally safe usages include: (1) querying the
         * session for its properties. Generally unsafe usages include: (1) submitting a new {@link
         * CaptureRequest}, (2) stopping an existing {@link CaptureRequest}, (3) closing the
         * session, (4) attaching a new {@link Surface} to the session. When the caller uses the
         * session beyond the safe usage limits, the usage may still work in conjunction with
         * CameraX, but any strong gurantees provided by CameraX about the validity of the camera
         * state become void.
         *
         * <p>The caller is generally free to use the {@link CaptureRequest} and {@link
         * CaptureResult} instances accessed through the callback methods.
         *
         * @param captureCallback The {@link CameraCaptureSession.CaptureCallback}.
         * @return The current Extender.
         */
        public Extender setSessionCaptureCallback(
                CameraCaptureSession.CaptureCallback captureCallback) {
            mBaseBuilder.insertOption(SESSION_CAPTURE_CALLBACK_OPTION, captureCallback);
            return this;
        }
    }

    /**
     * Builder for creating {@link Camera2Config} instance.
     *
     * <p>Use {@link Builder} for creating {@link Config} which contains camera2 options only.
     * And use {@link Extender} to add Camera2 options on existing other {@link
     * Config.Builder}.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class Builder implements Config.Builder<Camera2Config, Builder> {

        private final MutableOptionsBundle mMutableOptionsBundle = MutableOptionsBundle.create();

        @Override
        public MutableConfig getMutableConfig() {
            return mMutableOptionsBundle;
        }

        @Override
        public Builder builder() {
            return this;
        }

        /**
         * Inserts new capture request option with specific {@link CaptureRequest.Key} setting.
         */
        public <ValueT> Builder setCaptureRequestOption(
                CaptureRequest.Key<ValueT> key, ValueT value) {
            Option<Object> opt = Camera2Config.createCaptureRequestOption(key);
            insertOption(opt, value);
            return this;
        }

        /** Inserts options from other {@link Config} object. */
        public Builder insertAllOptions(Config config) {
            for (Option<?> option : config.listOptions()) {
                @SuppressWarnings("unchecked") // Options/values are being copied directly
                        Option<Object> objectOpt = (Option<Object>) option;
                insertOption(objectOpt, config.retrieveOption(objectOpt));
            }
            return this;
        }

        @Override
        public Camera2Config build() {
            return new Camera2Config(OptionsBundle.from(mMutableOptionsBundle));
        }

        // Start of the default implementation of Config.Builder
        // *****************************************************************************************

        // Implementations of Config.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public <ValueT> Builder insertOption(Option<ValueT> opt, ValueT value) {
            getMutableConfig().insertOption(opt, value);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @Nullable
        public <ValueT> Builder removeOption(Option<ValueT> opt) {
            getMutableConfig().removeOption(opt);
            return builder();
        }

        // End of the default implementation of Config.Builder
        // *****************************************************************************************
    }

    // Start of the default implementation of Config
    // *********************************************************************************************

    // Implementations of Config.Reader default methods

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public boolean containsOption(Option<?> id) {
        return getConfig().containsOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id) {
        return getConfig().retrieveOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id, @Nullable ValueT valueIfMissing) {
        return getConfig().retrieveOption(id, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void findOptions(String idStem, OptionMatcher matcher) {
        getConfig().findOptions(idStem, matcher);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Set<Option<?>> listOptions() {
        return getConfig().listOptions();
    }

    // End of the default implementation of Config
    // *********************************************************************************************
}
