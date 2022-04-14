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

import static android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING;
import static android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING;

import static androidx.camera.camera2.internal.ZslUtil.isCapabilitySupported;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.InputConfiguration;
import android.media.ImageWriter;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageReaderProxys;
import androidx.camera.core.SafeCloseImageReaderProxy;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.compat.ImageWriterCompat;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Implementation for {@link ZslControl}.
 */
@RequiresApi(23)
final class ZslControlImpl implements ZslControl {

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    private static final int MAX_IMAGES = 2;

    @SuppressWarnings("WeakerAccess")
    @NonNull
    final Queue<ImageProxy> mImageRingBuffer = new LinkedList<>();

    @SuppressWarnings("WeakerAccess")
    @NonNull
    final Queue<TotalCaptureResult> mTotalCaptureResultRingBuffer = new LinkedList<>();

    private boolean mIsYuvReprocessingSupported = false;
    private boolean mIsPrivateReprocessingSupported = false;

    @SuppressWarnings("WeakerAccess")
    SafeCloseImageReaderProxy mReprocessingImageReader;
    private DeferrableSurface mReprocessingImageDeferrableSurface;

    @Nullable
    ImageWriter mReprocessingImageWriter;

    ZslControlImpl(@NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        mIsYuvReprocessingSupported =
                isCapabilitySupported(cameraCharacteristicsCompat,
                        REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING);
        mIsPrivateReprocessingSupported =
                isCapabilitySupported(cameraCharacteristicsCompat,
                        REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING);
    }


    @Override
    public void addZslConfig(
            @NonNull Size resolution,
            @NonNull SessionConfig.Builder sessionConfigBuilder) {
        if (!mIsYuvReprocessingSupported && !mIsPrivateReprocessingSupported) {
            return;
        }

        cleanup();

        // Init the reprocessing image reader and enqueue available images into the ring buffer.
        // TODO(b/226683183): Decide whether YUV or PRIVATE reprocessing should be the default.
        int reprocessingImageFormat = mIsYuvReprocessingSupported
                ? ImageFormat.YUV_420_888 : ImageFormat.PRIVATE;

        mReprocessingImageReader =
                new SafeCloseImageReaderProxy(
                        ImageReaderProxys.createIsolatedReader(
                                resolution.getWidth(),
                                resolution.getHeight(),
                                reprocessingImageFormat,
                                // TODO(226675509): Replace with RingBuffer interfaces and set the
                                //  appropriate value based on RingBuffer capacity.
                                MAX_IMAGES));
        mReprocessingImageReader.setOnImageAvailableListener(
                imageReader -> {
                    ImageProxy imageProxy = imageReader.acquireLatestImage();
                    if (imageProxy != null) {
                        // TODO(226675509): Replace with RingBuffer interfaces and close the
                        //  image if over capacity.
                        mImageRingBuffer.add(imageProxy);
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
        sessionConfigBuilder.addCameraCaptureCallback(new CameraCaptureCallback() {
            @Override
            public void onCaptureCompleted(
                    @NonNull CameraCaptureResult cameraCaptureResult) {
                super.onCaptureCompleted(cameraCaptureResult);
                CaptureResult captureResult = cameraCaptureResult.getCaptureResult();
                if (captureResult != null && captureResult instanceof TotalCaptureResult) {
                    mTotalCaptureResultRingBuffer.add((TotalCaptureResult) captureResult);
                }
            }
        });

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

    private void cleanup() {
        // We might need synchronization here when clearing ring buffer while image is enqueued
        // at the same time. Will test this case.
        Queue<ImageProxy> imageRingBuffer = mImageRingBuffer;
        while (!imageRingBuffer.isEmpty()) {
            ImageProxy imageProxy = imageRingBuffer.remove();
            imageProxy.close();
        }
        Queue<TotalCaptureResult> totalCaptureResultRingBuffer = mTotalCaptureResultRingBuffer;
        totalCaptureResultRingBuffer.clear();

        DeferrableSurface reprocessingImageDeferrableSurface = mReprocessingImageDeferrableSurface;
        if (reprocessingImageDeferrableSurface != null) {
            SafeCloseImageReaderProxy reprocessingImageReaderProxy = mReprocessingImageReader;
            if (reprocessingImageReaderProxy != null) {
                reprocessingImageDeferrableSurface.getTerminationFuture().addListener(
                        reprocessingImageReaderProxy::safeClose,
                        CameraXExecutors.mainThreadExecutor());
            }
            reprocessingImageDeferrableSurface.close();
        }

        ImageWriter reprocessingImageWriter = mReprocessingImageWriter;
        if (reprocessingImageWriter != null) {
            reprocessingImageWriter.close();
            mReprocessingImageWriter = null;
        }
    }
}
