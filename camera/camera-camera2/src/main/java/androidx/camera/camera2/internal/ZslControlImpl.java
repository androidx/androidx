/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import static android.graphics.ImageFormat.PRIVATE;
import static android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW;
import static android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING;

import static androidx.camera.camera2.internal.ZslUtil.isCapabilitySupported;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageWriter;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.ZslDisablerQuirk;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.core.MetadataImageReader;
import androidx.camera.core.SafeCloseImageReaderProxy;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.compat.ImageWriterCompat;
import androidx.camera.core.internal.utils.ZslRingBuffer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation for {@link ZslControl}.
 */
@RequiresApi(23)
final class ZslControlImpl implements ZslControl {

    private static final String TAG = "ZslControlImpl";

    @VisibleForTesting
    static final int RING_BUFFER_CAPACITY = 3;

    @VisibleForTesting
    static final int MAX_IMAGES = RING_BUFFER_CAPACITY * 3;

    private final @NonNull CameraCharacteristicsCompat mCameraCharacteristicsCompat;
    private final @NonNull Executor mExecutor;

    @VisibleForTesting
    @SuppressWarnings("WeakerAccess")
    final @NonNull ZslRingBuffer mImageRingBuffer;

    private boolean mIsZslDisabledByUseCaseConfig = false;
    private boolean mIsZslDisabledByFlashMode = false;
    private boolean mIsPrivateReprocessingSupported = false;

    private boolean mShouldZslDisabledByQuirks = false;

    @SuppressWarnings("WeakerAccess")
    SafeCloseImageReaderProxy mReprocessingImageReader;
    private DeferrableSurface mReprocessingImageDeferrableSurface;

    @Nullable ImageWriterHolder mReprocessingImageWriterHolder;

    ZslControlImpl(@NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat,
            @NonNull Executor executor) {
        mCameraCharacteristicsCompat = cameraCharacteristicsCompat;
        mExecutor = executor;
        mIsPrivateReprocessingSupported =
                isCapabilitySupported(mCameraCharacteristicsCompat,
                        REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING);

        mShouldZslDisabledByQuirks = DeviceQuirks.get(ZslDisablerQuirk.class) != null;

        mImageRingBuffer = new ZslRingBuffer(
                RING_BUFFER_CAPACITY,
                ImageProxy::close);
    }

    @Override
    public void setZslDisabledByUserCaseConfig(boolean disabled) {
        if (mIsZslDisabledByUseCaseConfig != disabled && disabled) {
            clearRingBuffer();
        }
        mIsZslDisabledByUseCaseConfig = disabled;
    }

    @Override
    public boolean isZslDisabledByUserCaseConfig() {
        return mIsZslDisabledByUseCaseConfig;
    }

    @Override
    public void setZslDisabledByFlashMode(boolean disabled) {
        mIsZslDisabledByFlashMode = disabled;
    }

    @Override
    public boolean isZslDisabledByFlashMode() {
        return mIsZslDisabledByFlashMode;
    }

    @Override
    public void addZslConfig(SessionConfig.@NonNull Builder sessionConfigBuilder) {
        cleanup();

        // Early return only if use case config doesn't support zsl. If flash mode doesn't
        // support zsl, we still create reprocessing capture session but will create a
        // regular capture request when taking pictures. So when user switches flash mode, we
        // could create reprocessing capture request if flash mode allows.
        if (mIsZslDisabledByUseCaseConfig) {
            sessionConfigBuilder.setTemplateType(TEMPLATE_PREVIEW);
            return;
        }

        if (mShouldZslDisabledByQuirks) {
            sessionConfigBuilder.setTemplateType(TEMPLATE_PREVIEW);
            return;
        }

        Map<Integer, Size> mReprocessingInputSizeMap =
                createReprocessingInputSizeMap(mCameraCharacteristicsCompat);

        // Due to b/232268355 and feedback from pixel team that private format will have better
        // performance, we will use private only for zsl.
        if (!mIsPrivateReprocessingSupported
                || mReprocessingInputSizeMap.isEmpty()
                || !mReprocessingInputSizeMap.containsKey(PRIVATE)
                || !isJpegValidOutputForInputFormat(mCameraCharacteristicsCompat, PRIVATE)) {
            sessionConfigBuilder.setTemplateType(TEMPLATE_PREVIEW);
            return;
        }

        int reprocessingImageFormat = PRIVATE;
        Size resolution = mReprocessingInputSizeMap.get(reprocessingImageFormat);
        MetadataImageReader metadataImageReader = new MetadataImageReader(
                resolution.getWidth(),
                resolution.getHeight(),
                reprocessingImageFormat,
                MAX_IMAGES);

        // Init the reprocessing image reader surface and add into the target surfaces of capture
        SafeCloseImageReaderProxy reprocessingImageReaderProxy =
                new SafeCloseImageReaderProxy(metadataImageReader);
        DeferrableSurface reprocessingImageDeferrableSurface = new ImmediateSurface(
                Objects.requireNonNull(reprocessingImageReaderProxy.getSurface()),
                new Size(reprocessingImageReaderProxy.getWidth(),
                        reprocessingImageReaderProxy.getHeight()),
                reprocessingImageFormat);
        ImageWriterHolder imageWriterHolder = new ImageWriterHolder(mExecutor);

        mReprocessingImageReader = reprocessingImageReaderProxy;
        mReprocessingImageDeferrableSurface = reprocessingImageDeferrableSurface;
        mReprocessingImageWriterHolder = imageWriterHolder;

        reprocessingImageReaderProxy.setOnImageAvailableListener(
                imageReader -> {
                    try {
                        ImageProxy imageProxy = imageReader.acquireLatestImage();
                        if (imageProxy != null) {
                            mImageRingBuffer.enqueue(imageProxy);
                        }
                    } catch (IllegalStateException e) {
                        Logger.e(TAG, "Failed to acquire latest image IllegalStateException = "
                                + e.getMessage());
                    }

                }, CameraXExecutors.ioExecutor());

        reprocessingImageDeferrableSurface.getTerminationFuture().addListener(() -> {
            reprocessingImageReaderProxy.safeClose();
            imageWriterHolder.release();
        }, mExecutor);
        sessionConfigBuilder.addSurface(reprocessingImageDeferrableSurface);

        // Init capture and session state callback and enqueue the total capture result
        sessionConfigBuilder.addCameraCaptureCallback(
                metadataImageReader.getCameraCaptureCallback());
        sessionConfigBuilder.addSessionStateCallback(
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(
                            @NonNull CameraCaptureSession cameraCaptureSession) {
                        Surface surface = cameraCaptureSession.getInputSurface();
                        if (surface != null) {
                            imageWriterHolder.onImageWriterCreated(
                                    ImageWriterCompat.newInstance(surface, 1));
                        }
                    }

                    @Override
                    public void onConfigureFailed(
                            @NonNull CameraCaptureSession cameraCaptureSession) { }
                });

        // Set input configuration for reprocessing capture request
        sessionConfigBuilder.setInputConfiguration(new InputConfiguration(
                reprocessingImageReaderProxy.getWidth(),
                reprocessingImageReaderProxy.getHeight(),
                reprocessingImageReaderProxy.getImageFormat()));
    }

    @Override
    public void clearZslConfig() {
        cleanup();
    }

    @Override
    public @Nullable ImageProxy dequeueImageFromBuffer() {
        ImageProxy imageProxy = null;
        try {
            imageProxy = mImageRingBuffer.dequeue();
        } catch (NoSuchElementException e) {
            Logger.e(TAG, "dequeueImageFromBuffer no such element");
        }

        return imageProxy;
    }

    @ExecutedBy("mExecutor")
    @Override
    public boolean enqueueImageToImageWriter(@NonNull ImageProxy imageProxy) {
        if (mReprocessingImageWriterHolder != null) {
            return mReprocessingImageWriterHolder.enqueueImageToImageWriter(imageProxy);
        }
        return false;
    }

    private void cleanup() {
        if (mReprocessingImageReader != null) {
            // Remove the onImageAvailable listener to prevent new images from being
            // added to the ring buffer. The ImageReader itself will be closed
            // automatically when the associated DeferrableSurface is terminated.
            mReprocessingImageReader.clearOnImageAvailableListener();
            mReprocessingImageReader = null;
        }
        if (mReprocessingImageWriterHolder != null) {
            // The ImageWriter will be released when the associated
            // DeferrableSurface is terminated.
            mReprocessingImageWriterHolder.deactivate();
            mReprocessingImageWriterHolder = null;
        }

        // We might need synchronization here when clearing ring buffer while image is enqueued
        // at the same time. Will test this case.
        clearRingBuffer();

        if (mReprocessingImageDeferrableSurface != null) {
            // Termination is triggered when the use count reaches 0.
            mReprocessingImageDeferrableSurface.close();
            mReprocessingImageDeferrableSurface = null;
        }
    }

    private void clearRingBuffer() {
        ZslRingBuffer imageRingBuffer = mImageRingBuffer;
        while (!imageRingBuffer.isEmpty()) {
            ImageProxy imageProxy = imageRingBuffer.dequeue();
            imageProxy.close();
        }
    }

    private @NonNull Map<Integer, Size> createReprocessingInputSizeMap(
            @NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        StreamConfigurationMap map = null;
        try {
            map = cameraCharacteristicsCompat.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        } catch (AssertionError e) {
            // Some devices may throw AssertionError when retrieving the stream configuration map.
            Logger.e(TAG, "Failed to retrieve StreamConfigurationMap, error = "
                    + e.getMessage());
        }

        if (map == null || map.getInputFormats() == null) {
            return new HashMap<>();
        }

        Map<Integer, Size> inputSizeMap = new HashMap<>();
        for (int format: map.getInputFormats()) {
            Size[] inputSizes = map.getInputSizes(format);
            if (inputSizes != null) {
                // Sort by descending order
                Arrays.sort(inputSizes, new CompareSizesByArea(true));

                // TODO(b/233696144): Check if selecting an input size closer to output size will
                //  improve performance or not.
                inputSizeMap.put(format, inputSizes[0]);
            }
        }
        return inputSizeMap;
    }

    private boolean isJpegValidOutputForInputFormat(
            @NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat,
            int inputFormat) {
        StreamConfigurationMap map =
                cameraCharacteristicsCompat.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
            return false;
        }

        int[] validOutputFormats = map.getValidOutputFormatsForInput(inputFormat);
        if (validOutputFormats == null) {
            return false;
        }
        for (int outputFormat : validOutputFormats) {
            if (outputFormat == ImageFormat.JPEG) {
                return true;
            }
        }
        return false;
    }

    /**
     * A holder for an {@link ImageWriter} which takes care of the creation and release of the
     * {@link ImageWriter}.
     */
    @VisibleForTesting
    static class ImageWriterHolder {
        @Nullable
        private ImageWriter mImageWriter;
        private final AtomicBoolean mIsOpened = new AtomicBoolean(true);
        private final Executor mExecutor;

        ImageWriterHolder(Executor executor) {
            mExecutor = executor;
        }

        /**
         * Called when the {@link ImageWriter} is created.
         *
         * <p>This method is used to set the {@link ImageWriter} that will be used by this
         * ImageWriterHolder.  If an {@link ImageWriter} already exists when this method is called,
         * the previous one will be closed and replaced with the new one.
         *
         * <p>If this method is called after {@link #deactivate()}, the provided
         * {@link ImageWriter} will be ignored, and the internal {@link ImageWriter} will
         * remain null.
         *
         * @param imageWriter The newly created {@link ImageWriter}.
         */
        @ExecutedBy("mExecutor")
        public void onImageWriterCreated(@NonNull ImageWriter imageWriter) {
            if (mIsOpened.get()) {
                if (mImageWriter != null) {
                    Logger.w(TAG, "ImageWriter already existed in the ImageWriter holder. "
                            + "Closing the previous one.");
                    mImageWriter.close();
                }
                mImageWriter = imageWriter;
            }
        }

        /**
         * Enqueues the {@link ImageProxy} to the {@link ImageWriter}.
         *
         * @param imageProxy The {@link ImageProxy} to be enqueued.
         * @return True if the {@link ImageProxy} is enqueued successfully.
         */
        @ExecutedBy("mExecutor")
        public boolean enqueueImageToImageWriter(@NonNull ImageProxy imageProxy) {
            @OptIn(markerClass = ExperimentalGetImage.class)
            Image image = imageProxy.getImage();
            if (mIsOpened.get() && mImageWriter != null && image != null) {
                try {
                    ImageWriterCompat.queueInputImage(mImageWriter, image);
                    // It's essential to call ImageProxy#close().
                    // Add an OnImageReleasedListener to ensure the ImageProxy is closed after
                    // the image is written to the output surface. This is crucial to prevent
                    // resource leaks, where images might not be closed properly if CameraX fails
                    // to propagate close events to its internal components.
                    ImageWriterCompat.setOnImageReleasedListener(
                            mImageWriter, writer -> imageProxy.close(), mExecutor);
                } catch (IllegalStateException e) {
                    Logger.e(TAG, "enqueueImageToImageWriter throws IllegalStateException = "
                            + e.getMessage());
                    return false;
                }
                return true;
            }
            return false;
        }

        /**
         * Deactivates the {@link ImageWriterHolder}.
         *
         * <p>Once deactivated, the {@link ImageWriterHolder} will no longer accept new images.
         */
        public void deactivate() {
            mIsOpened.set(false);
        }

        /**
         * Releases the {@link ImageWriter}.
         */
        @ExecutedBy("mExecutor")
        public void release() {
            deactivate();
            if (mImageWriter != null) {
                mImageWriter.close();
            }
        }
    }
}
