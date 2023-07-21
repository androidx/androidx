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

package androidx.camera.camera2.internal.compat.params;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.params.OutputConfiguration;
import android.os.Build;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.camera2.internal.compat.ApiCompat;

import java.util.List;

/**
 * Helper for accessing features in OutputConfiguration in a backwards compatible fashion.
 */
@RequiresApi(21)
public final class OutputConfigurationCompat {

    /**
     * Invalid surface group ID.
     *
     * <p>An OutputConfiguration with this value indicates that the included surface
     * doesn't belong to any surface group.</p>
     */
    public static final int SURFACE_GROUP_ID_NONE = -1;

    /**
     * Invalid stream use case value.
     *
     * <p>An OutputConfiguration with this value indicates that the associated stream
     * doesn't support stream use case.</p>
     */
    public static final int STREAM_USE_CASE_NONE = -1;

    private final OutputConfigurationCompatImpl mImpl;

    public OutputConfigurationCompat(@NonNull Surface surface) {
        this(SURFACE_GROUP_ID_NONE, surface);
    }

    public OutputConfigurationCompat(int surfaceGroupId, @NonNull Surface surface) {
        if (Build.VERSION.SDK_INT >= 33) {
            mImpl = new OutputConfigurationCompatApi33Impl(surfaceGroupId, surface);
        } else if (Build.VERSION.SDK_INT >= 28) {
            mImpl = new OutputConfigurationCompatApi28Impl(surfaceGroupId, surface);
        } else if (Build.VERSION.SDK_INT >= 26) {
            mImpl = new OutputConfigurationCompatApi26Impl(surfaceGroupId, surface);
        } else if (Build.VERSION.SDK_INT >= 24) {
            mImpl = new OutputConfigurationCompatApi24Impl(surfaceGroupId, surface);
        } else {
            mImpl = new OutputConfigurationCompatBaseImpl(surface);
        }
    }

    /**
     * Create a new {@link OutputConfigurationCompat} instance, with desired Surface size and
     * Surface source class.
     *
     * <p>
     * This constructor takes an argument for desired Surface size and the Surface source class
     * without providing the actual output Surface. This is used to setup an output configuration
     * with a deferred Surface. The application can use this output configuration to create a
     * session.
     * </p>
     * <p>
     * However, the actual output Surface must be set via {@link #addSurface} and the deferred
     * Surface configuration must be finalized via {@link
     * CameraCaptureSession#finalizeOutputConfigurations} before submitting a request with this
     * Surface target. The deferred Surface can only be obtained either from {@link
     * android.view.SurfaceView} by calling {@link android.view.SurfaceHolder#getSurface}, or from
     * {@link android.graphics.SurfaceTexture} via
     * {@link android.view.Surface#Surface(android.graphics.SurfaceTexture)}).
     * </p>
     *
     * @param surfaceSize Size for the deferred surface.
     * @param klass       a non-{@code null} {@link Class} object reference that indicates the
     *                    source of
     *                    this surface. Only {@link android.view.SurfaceHolder SurfaceHolder
     *                    .class} and
     *                    {@link android.graphics.SurfaceTexture SurfaceTexture.class} are
     *                    supported.
     * @throws IllegalArgumentException if the Surface source class is not supported, or Surface
     *                                  size is zero.
     */
    @RequiresApi(26)
    public <T> OutputConfigurationCompat(@NonNull Size surfaceSize, @NonNull Class<T> klass) {
        OutputConfiguration deferredConfig =
                ApiCompat.Api26Impl.newOutputConfiguration(surfaceSize, klass);
        if (Build.VERSION.SDK_INT >= 33) {
            mImpl = OutputConfigurationCompatApi33Impl.wrap(deferredConfig);
        } else if (Build.VERSION.SDK_INT >= 28) {
            mImpl = OutputConfigurationCompatApi28Impl.wrap(deferredConfig);
        } else {
            mImpl = OutputConfigurationCompatApi26Impl.wrap(deferredConfig);
        }
    }

    private OutputConfigurationCompat(@NonNull OutputConfigurationCompatImpl impl) {
        mImpl = impl;
    }

    /**
     * Creates an instance from a framework android.hardware.camera2.params.OutputConfiguration
     * object.
     *
     * <p>This method always returns {@code null} on API &lt;= 23.</p>
     *
     * @param outputConfiguration an android.hardware.camera2.params.OutputConfiguration object, or
     *                            {@code null} if none.
     * @return an equivalent {@link OutputConfigurationCompat} object, or {@code null} if not
     * supported.
     */
    @Nullable
    public static OutputConfigurationCompat wrap(@Nullable Object outputConfiguration) {
        if (outputConfiguration == null) {
            return null;
        }

        OutputConfigurationCompatImpl outputConfigurationCompatImpl = null;
        if (Build.VERSION.SDK_INT >= 33) {
            outputConfigurationCompatImpl = OutputConfigurationCompatApi33Impl.wrap(
                    (OutputConfiguration) outputConfiguration);
        } else if (Build.VERSION.SDK_INT >= 28) {
            outputConfigurationCompatImpl = OutputConfigurationCompatApi28Impl.wrap(
                    (OutputConfiguration) outputConfiguration);
        } else if (Build.VERSION.SDK_INT >= 26) {
            outputConfigurationCompatImpl = OutputConfigurationCompatApi26Impl.wrap(
                    (OutputConfiguration) outputConfiguration);
        } else if (Build.VERSION.SDK_INT >= 24) {
            outputConfigurationCompatImpl = OutputConfigurationCompatApi24Impl.wrap(
                    (OutputConfiguration) outputConfiguration);
        }

        if (outputConfigurationCompatImpl == null) {
            return null;
        }

        return new OutputConfigurationCompat(outputConfigurationCompatImpl);
    }

    /**
     * Enable multiple surfaces sharing the same OutputConfiguration.
     *
     * <p>For advanced use cases, a camera application may require more streams than the combination
     * guaranteed by {@code CameraDevice.createCaptureSession}. In this case, more than one
     * compatible surface can be attached to an OutputConfiguration so that they map to one
     * camera stream, and the outputs share memory buffers when possible. Due to buffer sharing
     * clients should be careful when adding surface outputs that modify their input data. If such
     * case exists, camera clients should have an additional mechanism to synchronize read and write
     * access between individual consumers.</p>
     *
     * <p>Two surfaces are compatible in the below cases:</p>
     *
     * <ol>
     * <li> Surfaces with the same size, format, dataSpace, and Surface source class. In this case,
     * {@code CameraDevice.createCaptureSessionByOutputConfigurations} is guaranteed to succeed.
     *
     * <li> Surfaces with the same size, format, and dataSpace, but different Surface source classes
     * that are generally not compatible. However, on some devices, the underlying camera device is
     * able to use the same buffer layout for both surfaces. The only way to discover if this is the
     * case is to create a capture session with that output configuration. For example, if the
     * camera device uses the same private buffer format between a SurfaceView/SurfaceTexture and a
     * MediaRecorder/MediaCodec, {@code CameraDevice.createCaptureSessionByOutputConfigurations}
     * will succeed. Otherwise, it fails with {@link
     * CameraCaptureSession.StateCallback#onConfigureFailed}.
     * </ol>
     *
     * <p>To enable surface sharing, this function must be called before {@code
     * CameraDevice.createCaptureSessionByOutputConfigurations} or {@code
     * CameraDevice.createReprocessableCaptureSessionByConfigurations}. Calling this function after
     * {@code CameraDevice.createCaptureSessionByOutputConfigurations} has no effect.</p>
     *
     * <p>Up to {@link #getMaxSharedSurfaceCount} surfaces can be shared for an OutputConfiguration.
     * The supported surfaces for sharing must be of type SurfaceTexture, SurfaceView,
     * MediaRecorder, MediaCodec, or implementation defined ImageReader.</p>
     */
    public void enableSurfaceSharing() {
        mImpl.enableSurfaceSharing();
    }

    /**
     * Retrieve the physical camera ID set by {@link #setPhysicalCameraId(String)}.
     *
     */
    @RestrictTo(Scope.LIBRARY)
    @Nullable
    public String getPhysicalCameraId() {
        return mImpl.getPhysicalCameraId();
    }

    /**
     * Set the id of the physical camera for this OutputConfiguration.
     *
     * <p>In the case one logical camera is made up of multiple physical cameras, it could be
     * desirable for the camera application to request streams from individual physical cameras.
     * This call achieves it by mapping the OutputConfiguration to the physical camera id.</p>
     *
     * <p>The valid physical camera ids can be queried by {@code
     * CameraCharacteristics.getPhysicalCameraIds} on API &gt;= 28.
     * </p>
     *
     * <p>On API &lt;= 27, the physical camera id will be ignored since logical camera is not
     * supported on these API levels.
     * </p>
     *
     * <p>Passing in a null physicalCameraId means that the OutputConfiguration is for a logical
     * stream.</p>
     *
     * <p>This function must be called before {@code
     * CameraDevice.createCaptureSessionByOutputConfigurations} or {@code
     * CameraDevice.createReprocessableCaptureSessionByConfigurations}. Calling this function
     * after {@code CameraDevice.createCaptureSessionByOutputConfigurations} or {@code
     * CameraDevice.createReprocessableCaptureSessionByConfigurations} has no effect.</p>
     *
     * <p>The surface belonging to a physical camera OutputConfiguration must not be used as input
     * or output of a reprocessing request. </p>
     */
    public void setPhysicalCameraId(@Nullable String physicalCameraId) {
        mImpl.setPhysicalCameraId(physicalCameraId);
    }

    /**
     * Add a surface to this OutputConfiguration.
     *
     * <p> This method will always throw on API &lt;= 25, as these API levels do not support surface
     * sharing. Users should always check {@link #getMaxSharedSurfaceCount} before attempting to
     * add a surface.
     *
     * <p> This function can be called before or after {@code
     * CameraDevice#createCaptureSessionByOutputConfigurations}. If it's called after,
     * the application must finalize the capture session with
     * {@code CameraCaptureSession.finalizeOutputConfigurations}. It is possible to call this method
     * after the output configurations have been finalized only in cases of enabled surface sharing
     * see {@link #enableSurfaceSharing}. The modified output configuration must be updated with
     * {@code CameraCaptureSession.updateOutputConfiguration}.</p>
     *
     * <p> If the OutputConfiguration was constructed with a deferred surface by {@link
     * OutputConfigurationCompat#OutputConfigurationCompat(Size, Class)}, the added surface must
     * be obtained
     * from {@link android.view.SurfaceView} by calling
     * {@link android.view.SurfaceHolder#getSurface},
     * or from {@link android.graphics.SurfaceTexture} via
     * {@link android.view.Surface#Surface(android.graphics.SurfaceTexture)}).</p>
     *
     * <p> If the OutputConfiguration was constructed by other constructors, the added
     * surface must be compatible with the existing surface. See {@link #enableSurfaceSharing} for
     * details of compatible surfaces.</p>
     *
     * <p> If the OutputConfiguration already contains a Surface, {@link #enableSurfaceSharing} must
     * be called before calling this function to add a new Surface.</p>
     *
     * @param surface The surface to be added.
     * @throws IllegalArgumentException if the Surface is invalid, the Surface's
     *                                  dataspace/format doesn't match, or adding the Surface
     *                                  would exceed number of
     *                                  shared surfaces supported.
     * @throws IllegalStateException    if the Surface was already added to this
     *                                  OutputConfiguration,
     *                                  or if the OutputConfiguration is not shared and it
     *                                  already has a surface associated
     *                                  with it.
     */
    public void addSurface(@NonNull Surface surface) {
        mImpl.addSurface(surface);
    }

    /**
     * Remove a surface from this OutputConfiguration.
     *
     * <p> Surfaces added via calls to {@link #addSurface} can also be removed from the
     * OutputConfiguration. The only notable exception is the surface associated with
     * the OutputConfigration see {@link #getSurface} which was passed as part of the constructor
     * or was added first in the deferred case
     * {@link OutputConfigurationCompat#OutputConfigurationCompat(Size, Class)}.</p>
     *
     * @param surface The surface to be removed.
     * @throws IllegalArgumentException If the surface is associated with this OutputConfiguration
     *                                  (see {@link #getSurface}) or the surface didn't get added
     *                                  with {@link #addSurface}.
     */
    public void removeSurface(@NonNull Surface surface) {
        mImpl.removeSurface(surface);
    }

    /**
     * Get the maximum supported shared {@link Surface} count.
     *
     * @return the maximum number of surfaces that can be added per each OutputConfiguration.
     * @see #enableSurfaceSharing
     */
    public int getMaxSharedSurfaceCount() {
        return mImpl.getMaxSharedSurfaceCount();
    }

    /**
     * Get the {@link Surface} associated with this {@link OutputConfigurationCompat}.
     *
     * If more than one surface is associated with this {@link OutputConfigurationCompat}, return
     * the
     * first one as specified in the constructor or {@link OutputConfigurationCompat#addSurface}.
     */
    @Nullable
    public Surface getSurface() {
        return mImpl.getSurface();
    }

    /**
     * Get the immutable list of surfaces associated with this {@link OutputConfigurationCompat}.
     *
     * @return the list of surfaces associated with this {@link OutputConfigurationCompat} as
     * specified in
     * the constructor and {@link OutputConfigurationCompat#addSurface}. The list should not be
     * modified.
     */
    @NonNull
    public List<Surface> getSurfaces() {
        return mImpl.getSurfaces();
    }

    /**
     * Get the surface group ID associated with this {@link OutputConfigurationCompat}.
     *
     * @return the surface group ID associated with this {@link OutputConfigurationCompat}.
     * The default value is {@value #SURFACE_GROUP_ID_NONE}.
     */
    public int getSurfaceGroupId() {
        return mImpl.getSurfaceGroupId();
    }

    /**
     * Return current dynamic range profile.
     *
     * <p>On API level 32 and lower, this value will return what is set by
     * {@link #setDynamicRangeProfile(long)}, but when the output configuration is used in a
     * {@link SessionConfigurationCompat} that is used to
     * {@link androidx.camera.camera2.internal.compat.CameraDeviceCompat#createCaptureSession(
     * SessionConfigurationCompat) create a capture session}, the value will be ignored and
     * camera will run the output as {@code STANDARD} dynamic range.
     */
    public long getDynamicRangeProfile() {
        return mImpl.getDynamicRangeProfile();
    }

    /**
     * Set a specific device supported dynamic range profile.
     *
     * <p>Clients can choose from any profile advertised as supported in
     * {@link
     * android.hardware.camera2.CameraCharacteristics#REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES}
     * queried using
     * {@link android.hardware.camera2.params.DynamicRangeProfiles#getSupportedProfiles()}. If this
     * is not explicitly set, then the default profile will be
     * {@link android.hardware.camera2.params.DynamicRangeProfiles#STANDARD}.
     *
     * <p>Do note that invalid combinations between the registered output surface pixel format and
     * the configured dynamic range profile will cause capture session initialization failure.
     * Invalid combinations include any 10-bit dynamic range profile advertised in
     * {@link android.hardware.camera2.params.DynamicRangeProfiles#getSupportedProfiles()}
     * combined with an output Surface pixel format
     * different from {@link android.graphics.ImageFormat#PRIVATE} (the default for Surfaces
     * initialized by {@link android.view.SurfaceView}, {@link android.view.TextureView},
     * {@link android.media.MediaRecorder}, {@link android.media.MediaCodec} etc.) or
     * {@link android.graphics.ImageFormat#YCBCR_P010}.
     *
     * <p>On API level 32 and lower, the only supported dynamic range is
     * {@link android.hardware.camera2.params.DynamicRangeProfiles#STANDARD}. On those API
     * levels, any other values will be ignored when the output configuring is used in a
     * {@link SessionConfigurationCompat} that is used to
     * {@link androidx.camera.camera2.internal.compat.CameraDeviceCompat#createCaptureSession(
     * SessionConfigurationCompat) create a capture session}, and the
     * dynamic range used by the camera will remain {@code STANDARD} dynamic range.
     */
    public void setDynamicRangeProfile(long profile) {
        mImpl.setDynamicRangeProfile(profile);
    }

    /**
     * Set the stream use case associated with this {@link OutputConfigurationCompat}.
     *
     * Stream use case is used to describe the purpose of the stream, whether it's for live
     * preview, still image capture, video recording, or their combinations. This flag is
     * useful
     * for scenarios where the immediate consumer target isn't sufficient to indicate the
     * stream's usage.
     *
     * The main difference between stream use case and capture intent is that the former
     * enables
     * the camera device to optimize camera hardware and software pipelines based on user
     * scenarios for each stream, whereas the latter is mainly a hint to camera to decide
     * optimal
     * 3A strategy that's applicable to the whole session. The camera device carries out
     * configurations such as selecting tuning parameters, choosing camera sensor mode, and
     * constructing image processing pipeline based on the streams's use cases. Capture
     * intents
     * are then used to fine tune 3A behaviors such as adjusting AE/AF convergence speed, and
     * capture intents may change during the lifetime of a session. For example, for a
     * session
     * with a PREVIEW_VIDEO_STILL use case stream and a STILL_CAPTURE use case stream, the
     * capture intents may be PREVIEW with fast 3A convergence speed and flash metering with
     * automatic control for live preview, STILL_CAPTURE with best 3A parameters for still
     * photo
     * capture, or VIDEO_RECORD with slower 3A convergence speed for better video playback
     * experience.
     *
     * <p> Stream use case is a API 33 and above concept for optimizing image process pipeline
     * for a given stream session. If not set,{@value #SURFACE_GROUP_ID_NONE} is used.
     * </p>
     *
     * @param streamUseCase Stream use case for the stream session associated with this
     *                      configuration.
     */
    public void setStreamUseCase(long streamUseCase) {
        mImpl.setStreamUseCase(streamUseCase);
    }

    /**
     * Set the stream use case associated with this {@link OutputConfigurationCompat}.
     *
     * @return the stream use case associated with this {@link OutputConfigurationCompat}.
     * The default value is
     * {@value #SURFACE_GROUP_ID_NONE}.
     */
    public long getStreamUseCase() {
        return mImpl.getStreamUseCase();
    }

    /**
     * Check if this {@link OutputConfigurationCompat} is equal to another
     * {@link OutputConfigurationCompat}.
     *
     * <p>Two output configurations are only equal if and only if the underlying surfaces, surface
     * properties (width, height, format, dataspace) when the output configurations are created,
     * and all other configuration parameters are equal. </p>
     *
     * @return {@code true} if the objects were equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OutputConfigurationCompat)) {
            return false;
        }

        return mImpl.equals(((OutputConfigurationCompat) obj).mImpl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return mImpl.hashCode();
    }

    /**
     * Gets the underlying framework android.hardware.camera2.params.OutputConfiguration object.
     *
     * <p>This method always returns {@code null} on API &lt;= 23.</p>
     *
     * @return an equivalent android.hardware.camera2.params.OutputConfiguration object, or {@code
     * null} if not supported.
     */
    @Nullable
    public Object unwrap() {
        return mImpl.getOutputConfiguration();
    }

    interface OutputConfigurationCompatImpl {
        void enableSurfaceSharing();

        @Nullable
        String getPhysicalCameraId();

        void setPhysicalCameraId(@Nullable String physicalCameraId);

        void addSurface(@NonNull Surface surface);

        void removeSurface(@NonNull Surface surface);

        int getMaxSharedSurfaceCount();

        long getDynamicRangeProfile();

        void setDynamicRangeProfile(long profile);

        void setStreamUseCase(long streamUseCase);

        long getStreamUseCase();

        @Nullable
        Surface getSurface();

        List<Surface> getSurfaces();

        int getSurfaceGroupId();

        @Nullable
        Object getOutputConfiguration();
    }
}
