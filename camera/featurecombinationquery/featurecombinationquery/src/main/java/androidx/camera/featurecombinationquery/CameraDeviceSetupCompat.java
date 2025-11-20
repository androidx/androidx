/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.featurecombinationquery;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.SessionConfiguration;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Interface for checking if a {@link SessionConfiguration} is supported by the device.
 *
 * <p>This interface is a compatible version of the {@link CameraDevice.CameraDeviceSetup}
 * class.
 *
 * <p>Implementations of this interface must be able to check if a {@link SessionConfiguration}
 * is supported. They will check both the output streams and the session parameters, then return
 * whether the combination works for the given camera. For example, a camera device may support
 * HDR and 60FPS frame rate, but not both at the same time.
 *
 * @see CameraDevice.CameraDeviceSetup
 */
public interface CameraDeviceSetupCompat {

    /**
     * Checks if the {@link SessionConfiguration} is supported.
     *
     * <p><b>Note:</b> The Play Services implementation does not support querying
     * {@link android.hardware.camera2.params.OutputConfiguration}s without an associated surface
     * and will throw an {@link IllegalArgumentException} if an {@code OutputConfiguration} without
     * an associated surface is passed. A {@link android.view.Surface} can be associated with an
     * {@code OutputConfiguration} either during construction, or later with
     * {@link android.hardware.camera2.params.OutputConfiguration#addSurface}. This is an
     * unfortunate limitation of Android Framework APIs. For queries that are performance sensitive
     * and need to happen before the final surface is ready, it is recommended to create temporary
     * {@link android.hardware.camera2.params.OutputConfiguration} with a concrete surface from
     * {@link android.media.ImageReader#newInstance} that matches the desired final surface instead.
     *
     * <p>Following are examples of some common final surfaces, and how ImageReader can be used to
     * query them:
     * <ul>
     * <li> {@code SurfaceTexture.class}:
     * <pre>
     * ImageReader.newInstance(1920, 1080, PixelFormat.PRIVATE, 1,
     *                         HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE)
     * </pre>
     * <li> {@code MediaRecorder.class}/{@code MediaCodec.class}:
     * <pre>
     * ImageReader.newInstance(1920, 1080, ImageFormat.PRIVATE, 1,
     *                         HardwareBuffer.USAGE_VIDEO_ENCODE)
     * </pre>
     * <li> {@code SurfaceView.class}:
     * <pre>
     * ImageReader.newInstance(1920, 1080, ImageFormat.PRIVATE, 1,
     *                         HardwareBuffer.USAGE_COMPOSER_OVERLAY)
     * </pre>
     * </ul>
     *
     * @param sessionConfig The {@link SessionConfiguration} to check.
     * @return a {@link SupportQueryResult} indicating if the {@link SessionConfiguration} is
     * supported.
     * @throws CameraAccessException    if the camera device is no longer connected or has
     *                                  encountered a fatal error.
     * @throws IllegalArgumentException if play-services implementation is included and an
     *                                  {@link android.hardware.camera2.params.OutputConfiguration}
     *                                  with a deferred Surface is passed.
     * @see CameraDevice.CameraDeviceSetup#isSessionConfigurationSupported
     */
    @NonNull
    SupportQueryResult isSessionConfigurationSupported(@NonNull SessionConfiguration sessionConfig)
            throws CameraAccessException;

    /**
     * Checks if the set of features in {@link SessionConfigurationLegacy} is supported for the
     * given camera.
     * <p>
     * This method and the legacy classes are provided to allow querying camera device
     * capabilities without the need to call
     * {@link android.hardware.camera2.CameraManager#openCamera}. It must only be used for
     * devices that do not support {@link CameraDevice.CameraDeviceSetup} and will throw an
     * {@link IllegalStateException} when used for a {@code cameraId} for which
     * {@link android.hardware.camera2.CameraManager#isCameraDeviceSetupSupported} returns true.
     * <p>
     * Even on older devices that do not support
     * {@link android.hardware.camera2.CameraDevice.CameraDeviceSetup}, if the
     * {@link CameraDevice} object can be obtained using
     * {@link android.hardware.camera2.CameraManager#openCamera}, then
     * {@link #isSessionConfigurationSupported(SessionConfiguration)} should be used as the
     * {@link android.hardware.camera2.CaptureRequest} returned by Camera2 generally has more
     * information pre-filled from the template which may change the result.
     * <p>
     * Note that this method does not, and can not query camera2's
     * {@link
     * android.hardware.camera2.CameraDevice.CameraDeviceSetup#isSessionConfigurationSupported},
     * so the returned {@link SupportQueryResult#getSource()} will never be
     * {@link SupportQueryResult#SOURCE_ANDROID_FRAMEWORK}. Additionally, there is a small
     * possibility that the result of Camera2's
     * {@link CameraDevice#isSessionConfigurationSupported(SessionConfiguration)} contradicts the
     * result of this call. In such cases, a {@link SupportQueryResult#RESULT_UNSUPPORTED} result
     * can be considered authoritative, but {@link SupportQueryResult#RESULT_SUPPORTED} may or
     * may not be correct and is considered best effort.
     * <p>
     * <b>WARNING:</b> Use this method is discouraged for most developers.
     * {@link #isSessionConfigurationSupported(SessionConfiguration)} should be used wherever
     * possible. This method should only be used if performance is critical on older devices and
     * you understand and protect against the caveats documented above.
     *
     * @param sessionConfig The {@link SessionConfigurationLegacy} to check.
     * @return a {@link SupportQueryResult} indicating if the {@link SessionConfigurationLegacy}
     * is supported.
     * @throws IllegalStateException    if the camera device supports
     *                                  {@link
     *                                  android.hardware.camera2.CameraDevice.CameraDeviceSetup}
     * @throws IllegalArgumentException if play-services implementation is included and an
     *                                  {@link android.hardware.camera2.params.OutputConfiguration}
     *                                  with a deferred Surface is passed. See note in
     *                                  {@link #isSessionConfigurationSupported}
     * @see SessionConfigurationLegacy
     * @see SessionParametersLegacy
     */
    @NonNull
    SupportQueryResult isSessionConfigurationSupportedLegacy(
            @NonNull SessionConfigurationLegacy sessionConfig);

    /**
     * Result of a {@link CameraDeviceSetupCompat#isSessionConfigurationSupported} query.
     */
    final class SupportQueryResult {

        /**
         * Source of the result is undefined. This is always accompanied by
         * {@link #RESULT_UNDEFINED}.
         */
        public static final int SOURCE_UNDEFINED = 0;
        /**
         * Source of the result is Google Play Services.
         */
        public static final int SOURCE_PLAY_SERVICES = 1;
        /**
         * Source of the result is Android framework.
         */
        public static final int SOURCE_ANDROID_FRAMEWORK = 2;

        /**
         * The library cannot determine if the {@link SessionConfiguration} is supported.
         *
         * <p>For API levels 29 to 34 inclusive, the app may continue to call
         * {@link CameraDevice#isSessionConfigurationSupported} to check if the session
         * configuration is supported.
         *
         * @see CameraDeviceSetupCompatFactory#getCameraDeviceSetupCompat for sample code.
         */
        public static final int RESULT_UNDEFINED = 0;
        /**
         * The {@link SessionConfiguration} is supported by the camera.
         */
        public static final int RESULT_SUPPORTED = 1;
        /**
         * The {@link SessionConfiguration} is not supported by the camera.
         */
        public static final int RESULT_UNSUPPORTED = 2;

        /**
         * Options for where the result is coming from.
         */
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @IntDef(value = {RESULT_UNDEFINED, RESULT_SUPPORTED, RESULT_UNSUPPORTED})
        public @interface Supported {
        }

        /**
         * Options for where the result is coming from.
         */
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @IntDef(value = {SOURCE_UNDEFINED, SOURCE_PLAY_SERVICES, SOURCE_ANDROID_FRAMEWORK})
        @interface Sources {
        }

        // Whether the configuration is supported.
        @Supported
        private final int mSupported;
        // The source of the result.
        @Sources
        private final int mSource;
        // The timestamp of when the result was updated.
        private final long mTimestampMillis;

        /**
         * Creates a new instance of {@link SupportQueryResult}.
         *
         * @param supported       Whether the {@link SessionConfiguration} is supported.
         * @param source          The source of the result.
         * @param timestampMillis The epoch timestamp of when the result was updated.
         */
        public SupportQueryResult(@Supported int supported, @Sources int source,
                long timestampMillis) {
            mSupported = supported;
            mSource = source;
            mTimestampMillis = timestampMillis;
        }

        /**
         * Whether the {@link SessionConfiguration} is supported.
         *
         * <p> If the value is {@link #RESULT_SUPPORTED}, the configuration is
         * supported by the camera; if the value is {@link #RESULT_UNSUPPORTED}, the
         * configuration is not supported by the camera; if the value is
         * {@link #RESULT_UNDEFINED}, then the library cannot determine if the configuration is
         * supported or not.
         */
        @Supported
        public int getSupported() {
            return mSupported;
        }

        /**
         * Returns the source of the result.
         *
         * <p> If the source of the information is Play Services, the value is
         * {@link #SOURCE_PLAY_SERVICES}; if the source is Android framework, the value is
         * {@link #SOURCE_ANDROID_FRAMEWORK}; otherwise, the value is {@link #SOURCE_UNDEFINED}.
         */
        @Sources
        public int getSource() {
            return mSource;
        }

        /**
         * Returns the epoch timestamp of when the result was updated.
         *
         * <p> If the source is {@link #SOURCE_PLAY_SERVICES}, the value is the time when the
         * data is updated on the Play Services server; if the source is
         * {@link #SOURCE_ANDROID_FRAMEWORK}, the value is the build property "ro.build.date.utc"
         * if available; otherwise, it will return 0.
         */
        public long getTimestampMillis() {
            return mTimestampMillis;
        }
    }

}
