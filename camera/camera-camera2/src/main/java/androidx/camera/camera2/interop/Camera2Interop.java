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

package androidx.camera.camera2.interop;

import static androidx.camera.camera2.impl.Camera2ImplConfig.DEVICE_STATE_CALLBACK_OPTION;
import static androidx.camera.camera2.impl.Camera2ImplConfig.SESSION_CAPTURE_CALLBACK_OPTION;
import static androidx.camera.camera2.impl.Camera2ImplConfig.SESSION_STATE_CALLBACK_OPTION;
import static androidx.camera.camera2.impl.Camera2ImplConfig.TEMPLATE_TYPE_OPTION;

import android.annotation.SuppressLint;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.core.ExtendableBuilder;
import androidx.camera.core.impl.Config;

/** Utilities related to interoperability with the {@link android.hardware.camera2} APIs. */
@ExperimentalCamera2Interop
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Camera2Interop {

    /**
     * Extends a {@link ExtendableBuilder} to add Camera2 options.
     *
     * @param <T> the type being built by the extendable builder.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Extender<T> {

        ExtendableBuilder<T> mBaseBuilder;

        /**
         * Creates an Extender that can be used to add Camera2 options to another Builder.
         *
         * @param baseBuilder The builder being extended.
         */
        public Extender(@NonNull ExtendableBuilder<T> baseBuilder) {
            mBaseBuilder = baseBuilder;
        }

        /**
         * Sets a {@link CaptureRequest.Key} and Value on the configuration.
         *
         * <p>The value will override any value set by CameraX internally with the risk of
         * interfering with some CameraX CameraControl APIs as well as 3A behavior.
         *
         * @param key      The {@link CaptureRequest.Key} which will be set.
         * @param value    The value for the key.
         * @param <ValueT> The type of the value.
         * @return The current Extender.
         */
        @NonNull
        public <ValueT> Extender<T> setCaptureRequestOption(
                @NonNull CaptureRequest.Key<ValueT> key, @NonNull ValueT value) {
            // Reify the type so we can obtain the class
            Config.Option<Object> opt = Camera2ImplConfig.createCaptureRequestOption(key);
            mBaseBuilder.getMutableConfig().insertOption(opt,
                    Config.OptionPriority.ALWAYS_OVERRIDE, value);
            return this;
        }

        /**
         * Sets a CameraDevice template on the given configuration.
         *
         * <p>See {@link CameraDevice} for valid template types. For example, {@link
         * CameraDevice#TEMPLATE_PREVIEW}.
         *
         * <p>Only used by {@link androidx.camera.core.ImageCapture} to set the template type
         * used. For all other {@link androidx.camera.core.UseCase} this value is ignored.
         *
         * @param templateType The template type to set.
         * @return The current Extender.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public Extender<T> setCaptureRequestTemplate(int templateType) {
            mBaseBuilder.getMutableConfig().insertOption(TEMPLATE_TYPE_OPTION, templateType);
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
        @SuppressLint("ExecutorRegistration")
        @NonNull
        public Extender<T> setDeviceStateCallback(
                @NonNull CameraDevice.StateCallback stateCallback) {
            mBaseBuilder.getMutableConfig().insertOption(DEVICE_STATE_CALLBACK_OPTION,
                    stateCallback);
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
         * session, (4) attaching a new {@link android.view.Surface} to the session. When the
         * caller uses the session beyond the safe usage limits, the usage may still work in
         * conjunction with CameraX, but any strong guarantees provided by CameraX about the
         * validity of the camera state become void.
         *
         * @param stateCallback The {@link CameraCaptureSession.StateCallback}.
         * @return The current Extender.
         */
        @SuppressLint("ExecutorRegistration")
        @NonNull
        public Extender<T> setSessionStateCallback(
                @NonNull CameraCaptureSession.StateCallback stateCallback) {
            mBaseBuilder.getMutableConfig().insertOption(SESSION_STATE_CALLBACK_OPTION,
                    stateCallback);
            return this;
        }

        /**
         * Sets a {@link CameraCaptureSession.CaptureCallback}.
         *
         * <p>The caller is expected to use the {@link CameraCaptureSession} instance accessed
         * through the callback methods responsibly. Generally safe usages include: (1) querying the
         * session for its properties. Generally unsafe usages include: (1) submitting a new {@link
         * CaptureRequest}, (2) stopping an existing {@link CaptureRequest}, (3) closing the
         * session, (4) attaching a new {@link android.view.Surface} to the session. When the
         * caller uses the session beyond the safe usage limits, the usage may still work in
         * conjunction with CameraX, but any strong guarantees provided by CameraX about the
         * validity of the camera state become void.
         *
         * <p>The caller is generally free to use the {@link CaptureRequest} and {@link
         * CaptureResult} instances accessed through the callback methods.
         *
         * @param captureCallback The {@link CameraCaptureSession.CaptureCallback}.
         * @return The current Extender.
         */
        @SuppressLint("ExecutorRegistration")
        @NonNull
        public Extender<T> setSessionCaptureCallback(
                @NonNull CameraCaptureSession.CaptureCallback captureCallback) {
            mBaseBuilder.getMutableConfig().insertOption(SESSION_CAPTURE_CALLBACK_OPTION,
                    captureCallback);
            return this;
        }
    }

    // Ensure this class isn't instantiated
    private Camera2Interop() {}
}
