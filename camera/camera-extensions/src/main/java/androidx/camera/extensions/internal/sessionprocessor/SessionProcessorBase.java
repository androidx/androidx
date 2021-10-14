/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.extensions.internal.sessionprocessor;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraXThreads;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.OutputSurface;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.core.impl.SessionProcessorSurface;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for SessionProcessor implementation. It is responsible for creating image readers and
 * maintaining the {@link ImageProcessor} associated with the image reader.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
abstract class SessionProcessorBase implements SessionProcessor {
    private static final String TAG = "SessionProcessorBase";
    @NonNull
    @GuardedBy("mLock")
    private Map<Integer, ImageReader> mImageReaderMap = new HashMap<>();
    @Nullable
    private HandlerThread mImageReaderHandlerThread;
    @GuardedBy("mLock")
    private List<DeferrableSurface> mSurfacesList = new ArrayList<>();
    private final Object mLock = new Object();
    private String mCameraId;

    @NonNull
    @Override
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    public final SessionConfig initSession(@NonNull CameraInfo cameraInfo,
            @NonNull OutputSurface previewSurfaceConfig,
            @NonNull OutputSurface imageCaptureSurfaceConfig,
            @Nullable OutputSurface imageAnalysisSurfaceConfig) {
        Camera2CameraInfo camera2CameraInfo = Camera2CameraInfo.from(cameraInfo);
        Camera2SessionConfig camera2SessionConfig = initSessionInternal(
                camera2CameraInfo.getCameraId(),
                camera2CameraInfo.getCameraCharacteristicsMap(),
                previewSurfaceConfig,
                imageCaptureSurfaceConfig,
                imageAnalysisSurfaceConfig
        );

        // TODO: Adding support for group id, surface sharing and physicalCameraId in SessionConfig
        synchronized (mLock) {
            for (Camera2OutputConfig outputConfig : camera2SessionConfig.getOutputConfigs()) {
                if (outputConfig instanceof SurfaceOutputConfig) {
                    SurfaceOutputConfig surfaceOutputConfig = (SurfaceOutputConfig) outputConfig;
                    SessionProcessorSurface surface =
                            new SessionProcessorSurface(surfaceOutputConfig.getSurface(),
                                    outputConfig.getId());
                    mSurfacesList.add(surface);
                } else if (outputConfig instanceof ImageReaderOutputConfig) {
                    ImageReaderOutputConfig imageReaderOutputConfig =
                            (ImageReaderOutputConfig) outputConfig;

                    ImageReader imageReader =
                            ImageReader.newInstance(imageReaderOutputConfig.getSize().getWidth(),
                                    imageReaderOutputConfig.getSize().getHeight(),
                                    imageReaderOutputConfig.getImageFormat(),
                                    imageReaderOutputConfig.getMaxImages());
                    mImageReaderMap.put(outputConfig.getId(), imageReader);
                    SessionProcessorSurface surface =
                            new SessionProcessorSurface(imageReader.getSurface(),
                                    outputConfig.getId());
                    surface.getTerminationFuture().addListener(() -> {
                        imageReader.close();
                    }, CameraXExecutors.directExecutor());
                    mSurfacesList.add(surface);
                } else if (outputConfig instanceof MultiResolutionImageReaderOutputConfig) {
                    // TODO: Support MultiResolutionImageReader
                    throw new UnsupportedOperationException(
                            "MultiResolutionImageReader not supported");
                }
            }
        }

        SessionConfig.Builder sessionConfigBuilder = new SessionConfig.Builder();
        synchronized (mLock) {
            for (DeferrableSurface surface : mSurfacesList) {
                sessionConfigBuilder.addSurface(surface);
            }
        }

        Camera2ImplConfig.Builder camera2ConfigurationBuilder = new Camera2ImplConfig.Builder();
        for (CaptureRequest.Key<?> key : camera2SessionConfig.getSessionParameters().keySet()) {
            @SuppressWarnings("unchecked")
            CaptureRequest.Key<Object> objKey = (CaptureRequest.Key<Object>) key;
            Object value = camera2SessionConfig.getSessionParameters().get(objKey);
            camera2ConfigurationBuilder.setCaptureRequestOption(objKey, value);
        }
        sessionConfigBuilder.setImplementationOptions(camera2ConfigurationBuilder.build());
        sessionConfigBuilder.setTemplateType(camera2SessionConfig.getSessionTemplateId());

        mImageReaderHandlerThread = new HandlerThread(
                CameraXThreads.TAG + "extensions_image_reader");
        mImageReaderHandlerThread.start();

        mCameraId = camera2CameraInfo.getCameraId();
        Logger.d(TAG, "initSession: cameraId=" + mCameraId);
        return sessionConfigBuilder.build();
    }

    @NonNull
    protected abstract Camera2SessionConfig initSessionInternal(
            @NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> cameraCharacteristicsMap,
            @NonNull OutputSurface previewSurfaceConfig,
            @NonNull OutputSurface imageCaptureSurfaceConfig,
            @Nullable OutputSurface imageAnalysisSurfaceConfig);


    protected void setImageProcessor(int outputConfigId,
            @NonNull ImageProcessor imageProcessor) {
        ImageReader imageReader;
        synchronized (mLock) {
            imageReader = mImageReaderMap.get(outputConfigId);
        }

        if (imageReader != null) {
            imageReader.setOnImageAvailableListener(reader -> {
                try {
                    Image image = reader.acquireNextImage();
                    ImageReference imageReference = new ImageRefHolder(image);
                    imageProcessor.onNextImageAvailable(outputConfigId, image.getTimestamp(),
                            imageReference, null);
                } catch (IllegalStateException e) {
                    Logger.e(TAG, "Failed to acquire next image.", e);
                }
            }, new Handler(mImageReaderHandlerThread.getLooper()));
        }
    }

    @Override
    public final void deInitSession() {
        Logger.e(TAG, "deInitSession: cameraId=" + mCameraId);

        deInitSessionInternal();

        synchronized (mLock) {
            for (DeferrableSurface deferrableSurface : mSurfacesList) {
                deferrableSurface.close();
            }
            mSurfacesList.clear();

            mImageReaderMap.clear();
        }

        if (mImageReaderHandlerThread != null) {
            mImageReaderHandlerThread.quitSafely();
            mImageReaderHandlerThread = null;
        }
    }

    protected abstract void deInitSessionInternal();

    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    private static class ImageRefHolder implements ImageReference {
        private int mRefCount;
        private Image mImage;
        private final Object mImageLock = new Object();

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        ImageRefHolder(@NonNull Image image) {
            mRefCount = 1;
            mImage = image;
        }

        @Override
        public boolean increment() {
            synchronized (mImageLock) {
                if (mRefCount <= 0) {
                    return false;
                }
                mRefCount++;
            }
            return true;
        }

        @Override
        public boolean decrement() {
            synchronized (mImageLock) {
                if (mRefCount <= 0) {
                    return false;
                }
                mRefCount--;
                if (mRefCount <= 0) {
                    mImage.close();
                }
            }
            return true;
        }

        @Nullable
        @Override
        public Image get() {
            return mImage;
        }
    }
}
