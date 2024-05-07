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
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraXThreads;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.OutputSurfaceConfiguration;
import androidx.camera.core.impl.RestrictedCameraInfo;
import androidx.camera.core.impl.RestrictedCameraInfo.CameraOperation;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.core.impl.SessionProcessorSurface;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.extensions.CameraExtensionsControl;
import androidx.camera.extensions.CameraExtensionsInfo;
import androidx.camera.extensions.internal.ExtensionsUtils;
import androidx.camera.extensions.internal.RequestOptionConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for SessionProcessor implementation. It is responsible for creating image readers and
 * maintaining the {@link ImageProcessor} associated with the image reader.
 */
abstract class SessionProcessorBase implements SessionProcessor, CameraExtensionsInfo,
        CameraExtensionsControl {
    private static final String TAG = "SessionProcessorBase";
    /**
     * Unknown extension strength.
     */
    protected static final int EXTENSION_STRENGTH_UNKNOWN = -1;
    @NonNull
    @GuardedBy("mLock")
    private final Map<Integer, ImageReader> mImageReaderMap = new HashMap<>();
    @GuardedBy("mLock")
    private final Map<Integer, Camera2OutputConfig> mOutputConfigMap = new HashMap<>();

    @Nullable
    private HandlerThread mImageReaderHandlerThread;
    @GuardedBy("mLock")
    private final List<DeferrableSurface> mSurfacesList = new ArrayList<>();
    protected final Object mLock = new Object();
    private String mCameraId;

    @NonNull
    private final @CameraOperation Set<Integer> mSupportedCameraOperations;
    @GuardedBy("mLock")
    protected int mExtensionStrength = EXTENSION_STRENGTH_UNKNOWN;

    SessionProcessorBase(@NonNull List<CaptureRequest.Key> supportedParameterKeys) {
        mSupportedCameraOperations = getSupportedCameraOperations(supportedParameterKeys);
    }

    private @CameraOperation Set<Integer> getSupportedCameraOperations(
            @NonNull List<CaptureRequest.Key> supportedParameterKeys) {
        @CameraOperation Set<Integer> operations = new HashSet<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (supportedParameterKeys.contains(CaptureRequest.CONTROL_ZOOM_RATIO)
                    || supportedParameterKeys.contains(CaptureRequest.SCALER_CROP_REGION)) {
                operations.add(RestrictedCameraInfo.CAMERA_OPERATION_ZOOM);
            }
        } else {
            if (supportedParameterKeys.contains(CaptureRequest.SCALER_CROP_REGION)) {
                operations.add(RestrictedCameraInfo.CAMERA_OPERATION_ZOOM);
            }
        }

        if (supportedParameterKeys.containsAll(
                Arrays.asList(
                        CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_MODE))) {
            operations.add(RestrictedCameraInfo.CAMERA_OPERATION_AUTO_FOCUS);
        }

        if (supportedParameterKeys.contains(CaptureRequest.CONTROL_AF_REGIONS)) {
            operations.add(RestrictedCameraInfo.CAMERA_OPERATION_AF_REGION);
        }

        if (supportedParameterKeys.contains(CaptureRequest.CONTROL_AE_REGIONS)) {
            operations.add(RestrictedCameraInfo.CAMERA_OPERATION_AE_REGION);
        }

        if (supportedParameterKeys.contains(CaptureRequest.CONTROL_AWB_REGIONS)) {
            operations.add(RestrictedCameraInfo.CAMERA_OPERATION_AWB_REGION);
        }

        if (supportedParameterKeys.containsAll(
                Arrays.asList(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER))) {
            operations.add(RestrictedCameraInfo.CAMERA_OPERATION_FLASH);
        }

        if (supportedParameterKeys.containsAll(
                Arrays.asList(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.FLASH_MODE))) {
            operations.add(RestrictedCameraInfo.CAMERA_OPERATION_TORCH);
        }

        if (supportedParameterKeys.contains(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION)) {
            operations.add(RestrictedCameraInfo.CAMERA_OPERATION_EXPOSURE_COMPENSATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && supportedParameterKeys.contains(CaptureRequest.EXTENSION_STRENGTH)) {
            operations.add(RestrictedCameraInfo.CAMERA_OPERATION_EXTENSION_STRENGTH);
        }

        return operations;
    }

    @NonNull
    private static SessionProcessorSurface createOutputConfigSurface(
            @NonNull Camera2OutputConfig outputConfig, Map<Integer, ImageReader> imageReaderMap) {
        if (outputConfig instanceof SurfaceOutputConfig) {
            SurfaceOutputConfig surfaceOutputConfig = (SurfaceOutputConfig) outputConfig;
            SessionProcessorSurface surface =
                    new SessionProcessorSurface(surfaceOutputConfig.getSurface(),
                            outputConfig.getId());
            return surface;
        } else if (outputConfig instanceof ImageReaderOutputConfig) {
            ImageReaderOutputConfig imageReaderOutputConfig =
                    (ImageReaderOutputConfig) outputConfig;

            ImageReader imageReader =
                    ImageReader.newInstance(imageReaderOutputConfig.getSize().getWidth(),
                            imageReaderOutputConfig.getSize().getHeight(),
                            imageReaderOutputConfig.getImageFormat(),
                            imageReaderOutputConfig.getMaxImages());
            imageReaderMap.put(outputConfig.getId(), imageReader);
            SessionProcessorSurface surface =
                    new SessionProcessorSurface(imageReader.getSurface(),
                            outputConfig.getId());
            surface.getTerminationFuture().addListener(() -> {
                imageReader.close();
            }, CameraXExecutors.directExecutor());
            return surface;
        } else if (outputConfig instanceof MultiResolutionImageReaderOutputConfig) {
            throw new UnsupportedOperationException("MultiResolutionImageReader not supported yet");
        }
        throw new UnsupportedOperationException("Unsupported Camera2OutputConfig:" + outputConfig);
    }

    @NonNull
    @Override
    public final SessionConfig initSession(@NonNull CameraInfo cameraInfo,
            @NonNull OutputSurfaceConfiguration outputSurfaceConfiguration) {
        CameraInfoInternal cameraInfoInternal = (CameraInfoInternal) cameraInfo;
        Map<String, CameraCharacteristics> characteristicsMap =
                ExtensionsUtils.getCameraCharacteristicsMap(cameraInfoInternal);
        Camera2SessionConfig camera2SessionConfig = initSessionInternal(
                cameraInfoInternal.getCameraId(), characteristicsMap, outputSurfaceConfiguration);

        SessionConfig.Builder sessionConfigBuilder = new SessionConfig.Builder();
        synchronized (mLock) {
            for (Camera2OutputConfig outputConfig : camera2SessionConfig.getOutputConfigs()) {
                SessionProcessorSurface sessionProcessorSurface =
                        createOutputConfigSurface(outputConfig, mImageReaderMap);
                mSurfacesList.add(sessionProcessorSurface);
                mOutputConfigMap.put(outputConfig.getId(), outputConfig);

                SessionConfig.OutputConfig.Builder outputConfigBuilder =
                        SessionConfig.OutputConfig.builder(sessionProcessorSurface)
                                .setPhysicalCameraId(outputConfig.getPhysicalCameraId())
                                .setSurfaceGroupId(outputConfig.getSurfaceGroupId());

                List<Camera2OutputConfig> sharedOutputs =
                        outputConfig.getSurfaceSharingOutputConfigs();
                if (sharedOutputs != null && !sharedOutputs.isEmpty()) {
                    List<DeferrableSurface> sharedSurfaces = new ArrayList<>();
                    for (Camera2OutputConfig sharedOutput : sharedOutputs) {
                        mOutputConfigMap.put(sharedOutput.getId(), sharedOutput);
                        sharedSurfaces.add(createOutputConfigSurface(sharedOutput,
                                mImageReaderMap));
                    }
                    outputConfigBuilder.setSharedSurfaces(sharedSurfaces);
                }
                sessionConfigBuilder.addOutputConfig(outputConfigBuilder.build());
            }
        }

        RequestOptionConfig.Builder camera2ConfigurationBuilder = new RequestOptionConfig.Builder();
        for (CaptureRequest.Key<?> key : camera2SessionConfig.getSessionParameters().keySet()) {
            @SuppressWarnings("unchecked")
            CaptureRequest.Key<Object> objKey = (CaptureRequest.Key<Object>) key;
            Object value = camera2SessionConfig.getSessionParameters().get(objKey);
            camera2ConfigurationBuilder.setCaptureRequestOption(objKey, value);
        }
        sessionConfigBuilder.setImplementationOptions(camera2ConfigurationBuilder.build());
        sessionConfigBuilder.setTemplateType(camera2SessionConfig.getSessionTemplateId());
        sessionConfigBuilder.setSessionType(camera2SessionConfig.getSessionType());

        mImageReaderHandlerThread = new HandlerThread(
                CameraXThreads.TAG + "extensions_image_reader");
        mImageReaderHandlerThread.start();

        mCameraId = cameraInfoInternal.getCameraId();
        Logger.d(TAG, "initSession: cameraId=" + mCameraId);
        return sessionConfigBuilder.build();
    }

    @NonNull
    @Override
    public @CameraOperation Set<Integer> getSupportedCameraOperations() {
        return mSupportedCameraOperations;
    }

    @NonNull
    protected abstract Camera2SessionConfig initSessionInternal(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> cameraCharacteristicsMap,
            @NonNull OutputSurfaceConfiguration outputSurfaceConfig);


    protected void setImageProcessor(int outputConfigId,
            @NonNull ImageProcessor imageProcessor) {
        ImageReader imageReader;
        String physicalCameraId;
        synchronized (mLock) {
            imageReader = mImageReaderMap.get(outputConfigId);
            Camera2OutputConfig outputConfig = mOutputConfigMap.get(outputConfigId);
            physicalCameraId = (outputConfig == null ? null : outputConfig.getPhysicalCameraId());
        }

        if (imageReader != null) {
            imageReader.setOnImageAvailableListener(reader -> {
                try {
                    Image image = reader.acquireNextImage();
                    ImageReference imageReference = new ImageRefHolder(image);
                    imageProcessor.onNextImageAvailable(outputConfigId, image.getTimestamp(),
                            imageReference, physicalCameraId);
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
            mOutputConfigMap.clear();
            mExtensionStrength = EXTENSION_STRENGTH_UNKNOWN;
        }

        if (mImageReaderHandlerThread != null) {
            mImageReaderHandlerThread.quitSafely();
            mImageReaderHandlerThread = null;
        }
    }

    protected abstract void deInitSessionInternal();

    private static class ImageRefHolder implements ImageReference {
        private int mRefCount;
        private final Image mImage;
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
