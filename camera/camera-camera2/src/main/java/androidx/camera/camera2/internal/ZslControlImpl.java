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
import android.os.Build;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.compat.ImageWriterCompat;
import androidx.camera.core.internal.utils.ZslRingBuffer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

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

    @NonNull
    private final CameraCharacteristicsCompat mCameraCharacteristicsCompat;

    @VisibleForTesting
    @SuppressWarnings("WeakerAccess")
    @NonNull
    final ZslRingBuffer mImageRingBuffer;

    private boolean mIsZslDisabledByUseCaseConfig = false;
    private boolean mIsZslDisabledByFlashMode = false;
    private boolean mIsPrivateReprocessingSupported = false;

    private boolean mShouldZslDisabledByQuirks = false;

    @SuppressWarnings("WeakerAccess")
    SafeCloseImageReaderProxy mReprocessingImageReader;
    private CameraCaptureCallback mMetadataMatchingCaptureCallback;
    private DeferrableSurface mReprocessingImageDeferrableSurface;

    @Nullable
    ImageWriter mReprocessingImageWriter;

    ZslControlImpl(@NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        mCameraCharacteristicsCompat = cameraCharacteristicsCompat;
        mIsPrivateReprocessingSupported =
                isCapabilitySupported(mCameraCharacteristicsCompat,
                        REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING);

        mShouldZslDisabledByQuirks = DeviceQuirks.get(ZslDisablerQuirk.class) != null;

        mImageRingBuffer = new ZslRingBuffer(
                RING_BUFFER_CAPACITY,
                imageProxy -> imageProxy.close());
    }

    @Override
    public void setZslDisabledByUserCaseConfig(boolean disabled) {
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
    public void addZslConfig(@NonNull SessionConfig.Builder sessionConfigBuilder) {
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
        mMetadataMatchingCaptureCallback = metadataImageReader.getCameraCaptureCallback();
        mReprocessingImageReader = new SafeCloseImageReaderProxy(metadataImageReader);
        metadataImageReader.setOnImageAvailableListener(
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

        // Init the reprocessing image reader surface and add into the target surfaces of capture
        mReprocessingImageDeferrableSurface = new ImmediateSurface(
                mReprocessingImageReader.getSurface(),
                new Size(mReprocessingImageReader.getWidth(),
                        mReprocessingImageReader.getHeight()),
                reprocessingImageFormat);

        SafeCloseImageReaderProxy reprocessingImageReaderProxy = mReprocessingImageReader;
        mReprocessingImageDeferrableSurface.getTerminationFuture().addListener(
                reprocessingImageReaderProxy::safeClose,
                CameraXExecutors.mainThreadExecutor());
        sessionConfigBuilder.addSurface(mReprocessingImageDeferrableSurface);

        // Init capture and session state callback and enqueue the total capture result
        sessionConfigBuilder.addCameraCaptureCallback(mMetadataMatchingCaptureCallback);
        sessionConfigBuilder.addSessionStateCallback(
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(
                            @NonNull CameraCaptureSession cameraCaptureSession) {
                        Surface surface = cameraCaptureSession.getInputSurface();
                        if (surface != null) {
                            mReprocessingImageWriter =
                                    ImageWriterCompat.newInstance(surface, 1);
                        }
                    }

                    @Override
                    public void onConfigureFailed(
                            @NonNull CameraCaptureSession cameraCaptureSession) { }
                });

        // Set input configuration for reprocessing capture request
        sessionConfigBuilder.setInputConfiguration(new InputConfiguration(
                mReprocessingImageReader.getWidth(),
                mReprocessingImageReader.getHeight(),
                mReprocessingImageReader.getImageFormat()));
    }

    @Nullable
    @Override
    public ImageProxy dequeueImageFromBuffer() {
        ImageProxy imageProxy = null;
        try {
            imageProxy = mImageRingBuffer.dequeue();
        } catch (NoSuchElementException e) {
            Logger.e(TAG, "dequeueImageFromBuffer no such element");
        }

        return imageProxy;
    }

    @Override
    public boolean enqueueImageToImageWriter(@NonNull ImageProxy imageProxy) {
        @OptIn(markerClass = ExperimentalGetImage.class)
        Image image = imageProxy.getImage();

        if (Build.VERSION.SDK_INT >= 23 && mReprocessingImageWriter != null && image != null) {
            try {
                ImageWriterCompat.queueInputImage(mReprocessingImageWriter, image);
            } catch (IllegalStateException e) {
                Logger.e(TAG, "enqueueImageToImageWriter throws IllegalStateException = "
                        + e.getMessage());
                return false;
            }
            return true;
        }
        return false;
    }

    private void cleanup() {
        // We might need synchronization here when clearing ring buffer while image is enqueued
        // at the same time. Will test this case.
        ZslRingBuffer imageRingBuffer = mImageRingBuffer;
        while (!imageRingBuffer.isEmpty()) {
            ImageProxy imageProxy = imageRingBuffer.dequeue();
            imageProxy.close();
        }

        DeferrableSurface reprocessingImageDeferrableSurface = mReprocessingImageDeferrableSurface;
        if (reprocessingImageDeferrableSurface != null) {
            SafeCloseImageReaderProxy reprocessingImageReaderProxy = mReprocessingImageReader;
            if (reprocessingImageReaderProxy != null) {
                reprocessingImageDeferrableSurface.getTerminationFuture().addListener(
                        reprocessingImageReaderProxy::safeClose,
                        CameraXExecutors.mainThreadExecutor());
                mReprocessingImageReader = null;
            }
            reprocessingImageDeferrableSurface.close();
            mReprocessingImageDeferrableSurface = null;
        }

        ImageWriter reprocessingImageWriter = mReprocessingImageWriter;
        if (reprocessingImageWriter != null) {
            reprocessingImageWriter.close();
            mReprocessingImageWriter = null;
        }
    }

    @NonNull
    private Map<Integer, Size> createReprocessingInputSizeMap(
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
}
