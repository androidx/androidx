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

package androidx.camera.extensions.internal.sessionprocessor;

import android.graphics.ImageFormat;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.Camera2CameraCaptureResult;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageReaderProxys;
import androidx.camera.core.Logger;
import androidx.camera.core.SettableImageProxy;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraCaptureResultImageInfo;
import androidx.camera.extensions.impl.CaptureProcessorImpl;
import androidx.camera.extensions.impl.ProcessResultImpl;
import androidx.camera.extensions.internal.ClientVersion;
import androidx.camera.extensions.internal.ExtensionVersion;
import androidx.camera.extensions.internal.Version;

import org.jetbrains.annotations.TestOnly;

import java.util.HashMap;
import java.util.List;

/**
 * A processor that is responsible for invoking OEM's CaptureProcessorImpl and converting the
 * processed YUV images into JPEG images. The JPEG images are written to the given output surface.
 *
 * <p> It only outputs JPEG format images, meaning that the given output surface must be of JPEG
 * format. To use this processor, follow the steps below:
 * <pre>
 * 1. Invoke start() with the required capture stage id list. A listener is given to
 *     be invoked when the processing is done and the resulting image is ready to appear in the
 *     output surface.
 * 2. When input images from camera and TotalCaptureResult arrives, notify the processor by
 *   {@link #notifyImage(ImageReference)} and {@link #notifyCaptureResult(TotalCaptureResult, int)}.
 * 3. Invoke {@link #setJpegQuality(int)} to adjust the jpeg quality and invoke
 *    {@link #setRotationDegrees(int)} if the device rotation changes.
 * 4. When camera session is finishing, invoke {@link #close()} to reclaim the resources.
 *    Please note that the output JPEG surface should be closed AFTER this processor is closed().
 * </pre>
 */
@RequiresApi(21)
class StillCaptureProcessor {
    private static final String TAG = "StillCaptureProcessor";
    private static final int MAX_IMAGES = 2;
    @NonNull
    final CaptureProcessorImpl mCaptureProcessorImpl;
    @NonNull
    final CaptureResultImageMatcher mCaptureResultImageMatcher = new CaptureResultImageMatcher();
    @NonNull
    final ImageReaderProxy mProcessedYuvImageReader;
    @NonNull
    YuvToJpegConverter mYuvToJpegConverter;

    final Object mLock = new Object();
    @GuardedBy("mLock")
    @NonNull
    HashMap<Integer, Pair<ImageReference, TotalCaptureResult>> mCaptureResults =
            new HashMap<>();

    @GuardedBy("mLock")
    OnCaptureResultCallback mOnCaptureResultCallback = null;
    // Stores the first capture result for injecting into the output JPEG ImageProxy.
    @GuardedBy("mLock")
    TotalCaptureResult mSourceCaptureResult = null;
    @GuardedBy("mLock")
    boolean mIsClosed = false;
    StillCaptureProcessor(@NonNull CaptureProcessorImpl captureProcessorImpl,
            @NonNull Surface captureOutputSurface,
            @NonNull Size surfaceSize) {
        mCaptureProcessorImpl = captureProcessorImpl;
        /*
           Processing flow:
           --> Collecting YUV images (from camera) via notifyImage and TotalCaptureResults
               via notifyCaptureResult
           --> mCaptureProcessorImpl.process (OEM process)
           --> mProcessedYuvImageReader
           --> mYuvToJpegProcessor (written to captureOutputSurface)
         */
        mProcessedYuvImageReader = ImageReaderProxys.createIsolatedReader(
                surfaceSize.getWidth(),
                surfaceSize.getHeight(),
                ImageFormat.YUV_420_888, MAX_IMAGES);

        mYuvToJpegConverter = new YuvToJpegConverter(100, captureOutputSurface);
        mProcessedYuvImageReader.setOnImageAvailableListener(
                imageReader -> {
                    OnCaptureResultCallback onCaptureResultCallback = null;
                    Exception errorException = null;
                    synchronized (mLock) {
                        if (mIsClosed) {
                            Logger.d(TAG, "Ignore JPEG processing in closed state");
                            return;
                        }
                        ImageProxy imageProxy = imageReader.acquireNextImage();
                        if (mSourceCaptureResult != null) {
                            imageProxy = new SettableImageProxy(imageProxy, null,
                                    new CameraCaptureResultImageInfo(
                                            new Camera2CameraCaptureResult(mSourceCaptureResult)));
                            mSourceCaptureResult = null;
                        }
                        if (imageProxy != null) {
                            try {
                                mYuvToJpegConverter.writeYuvImage(imageProxy);
                            } catch (YuvToJpegConverter.ConversionFailedException e) {
                                errorException = e;
                            }

                            if (mOnCaptureResultCallback != null) {
                                onCaptureResultCallback = mOnCaptureResultCallback;
                                mOnCaptureResultCallback = null;
                            }
                        }
                    }

                    if (onCaptureResultCallback != null) {
                        if (errorException != null) {
                            onCaptureResultCallback.onError(errorException);
                        } else {
                            onCaptureResultCallback.onCompleted();
                        }
                    }
                }, CameraXExecutors.ioExecutor());

        mCaptureProcessorImpl.onOutputSurface(mProcessedYuvImageReader.getSurface(),
                ImageFormat.YUV_420_888);
        mCaptureProcessorImpl.onImageFormatUpdate(ImageFormat.YUV_420_888);
        mCaptureProcessorImpl.onResolutionUpdate(surfaceSize);
    }

    @TestOnly
    StillCaptureProcessor(@NonNull CaptureProcessorImpl captureProcessorImpl,
            @NonNull Surface captureOutputSurface,
            @NonNull Size surfaceSize,
            @NonNull YuvToJpegConverter yuvToJpegConverter) {
        this(captureProcessorImpl, captureOutputSurface, surfaceSize);
        mYuvToJpegConverter = yuvToJpegConverter;
    }

    interface OnCaptureResultCallback {
        void onCompleted();
        void onCaptureResult(long shutterTimestamp,
                @NonNull List<Pair<CaptureResult.Key, Object>> result);
        void onError(@NonNull Exception e);
    }

    void clearCaptureResults() {
        synchronized (mLock) {
            for (Pair<ImageReference, TotalCaptureResult> value :
                    mCaptureResults.values()) {
                value.first.decrement();
            }
            mCaptureResults.clear();
        }
    }
    void startCapture(@NonNull List<Integer> captureIdList,
            @NonNull OnCaptureResultCallback onCaptureResultCallback) {
        Logger.d(TAG, "Start the processor");

        synchronized (mLock) {
            mOnCaptureResultCallback = onCaptureResultCallback;
            clearCaptureResults();
        }

        mCaptureResultImageMatcher.clear();
        mCaptureResultImageMatcher.setImageReferenceListener(
                (imageReference, totalCaptureResult, captureStageId) -> {
                    Exception errorException = null;
                    synchronized (mLock) {
                        if (mIsClosed) {
                            imageReference.decrement();
                            Logger.d(TAG, "Ignore image in closed state");
                            return;
                        }
                        Logger.d(TAG, "onImageReferenceIncoming  captureStageId=" + captureStageId);

                        mCaptureResults.put(captureStageId, new Pair<>(imageReference,
                                totalCaptureResult));

                        Logger.d(TAG, "mCaptureResult has capture stage Id: "
                                + mCaptureResults.keySet());
                        if (mCaptureResults.keySet().containsAll(captureIdList)) {
                            HashMap<Integer, Pair<Image, TotalCaptureResult>> convertedResult =
                                    new HashMap<>();
                            for (Integer id : mCaptureResults.keySet()) {
                                Pair<ImageReference, TotalCaptureResult> pair =
                                        mCaptureResults.get(id);
                                convertedResult.put(id,
                                        new Pair<>(pair.first.get(), pair.second));
                            }
                            Logger.d(TAG, "CaptureProcessorImpl.process()");
                            try {
                                if (ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_3)
                                        && ClientVersion.isMinimumCompatibleVersion(
                                                Version.VERSION_1_3)) {
                                    mCaptureProcessorImpl.process(convertedResult,
                                            new ProcessResultImpl() {
                                                @Override
                                                public void onCaptureCompleted(
                                                        long shutterTimestamp,
                                                        @NonNull List<Pair<CaptureResult.Key,
                                                                Object>> result) {
                                                    onCaptureResultCallback.onCaptureResult(
                                                            shutterTimestamp, result);
                                                }

                                                @Override
                                                public void onCaptureProcessProgressed(
                                                        int progress) {

                                                }
                                            }, CameraXExecutors.ioExecutor());
                                } else {
                                    mCaptureProcessorImpl.process(convertedResult);
                                }
                            } catch (Exception e) {
                                mOnCaptureResultCallback = null;
                                errorException = e;
                            }
                            clearCaptureResults();
                        }
                    }
                    if (errorException != null) {
                        if (onCaptureResultCallback != null) {
                            onCaptureResultCallback.onError(errorException);
                        }
                    }
                });
    }

    void notifyCaptureResult(@NonNull TotalCaptureResult captureResult,
            int captureStageId) {
        mCaptureResultImageMatcher.captureResultIncoming(captureResult,
                captureStageId);

        synchronized (mLock) {
            if (mSourceCaptureResult == null) {
                mSourceCaptureResult = captureResult;
            }
        }
    }

    void notifyImage(@NonNull ImageReference imageReference) {
        mCaptureResultImageMatcher.imageIncoming(imageReference);
    }

    void setJpegQuality(@IntRange(from = 0, to = 100) int quality) {
        mYuvToJpegConverter.setJpegQuality(quality);
    }

    void setRotationDegrees(
            @ImageOutputConfig.RotationDegreesValue int rotationDegrees) {
        mYuvToJpegConverter.setRotationDegrees(rotationDegrees);
    }

    /**
     * Close the processor. Please note that captureOutputSurface passed in must be closed AFTER
     * invoking this function.
     */
    void close() {
        Logger.d(TAG, "Close the processor");
        synchronized (mLock) {
            mIsClosed = true;
            clearCaptureResults();
            mProcessedYuvImageReader.clearOnImageAvailableListener();
            mCaptureResultImageMatcher.clearImageReferenceListener();
            mCaptureResultImageMatcher.clear();
            mProcessedYuvImageReader.close();
        }
    }
}
