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

package androidx.camera.core;

import static androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888;
import static androidx.camera.core.ImageProcessingUtil.applyPixelShiftForYUV;
import static androidx.camera.core.ImageProcessingUtil.convertYUVToRGB;
import static androidx.camera.core.ImageProcessingUtil.rotateYUV;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ImageWriter;
import android.os.Build;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.internal.compat.ImageWriterCompat;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.os.OperationCanceledException;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

/**
 * Abstract Analyzer that wraps around {@link ImageAnalysis.Analyzer} and implements
 * {@link ImageReaderProxy.OnImageAvailableListener}.
 *
 * This is an extension of {@link ImageAnalysis}. It has the same lifecycle and share part of the
 * states.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
abstract class ImageAnalysisAbstractAnalyzer implements ImageReaderProxy.OnImageAvailableListener {

    private static final String TAG = "ImageAnalysisAnalyzer";
    private static final RectF NORMALIZED_RECT = new RectF(-1, -1, 1, 1);

    // Member variables from ImageAnalysis.
    @GuardedBy("mAnalyzerLock")
    private ImageAnalysis.Analyzer mSubscribedAnalyzer;

    // Relative rotation degree provided to user in image info based on sensor to buffer rotation
    // degrees and target rotation degrees.
    @IntRange(from = 0, to = 359)
    private volatile int mRelativeRotation;

    // Cache buffer rotation degree for previous frame to decide whether new image reader proxy
    // needs to be created.
    @IntRange(from = 0, to = 359)
    private volatile int mPrevBufferRotationDegrees;

    @ImageAnalysis.OutputImageFormat
    private volatile int mOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888;
    private volatile boolean mOutputImageRotationEnabled;
    private volatile boolean mOnePixelShiftEnabled;
    private volatile boolean mOutputImageDirty;

    @GuardedBy("mAnalyzerLock")
    private Executor mUserExecutor;

    @GuardedBy("mAnalyzerLock")
    @Nullable
    private SafeCloseImageReaderProxy mProcessedImageReaderProxy;

    @GuardedBy("mAnalyzerLock")
    @Nullable
    private ImageWriter mProcessedImageWriter;

    @GuardedBy("mAnalyzerLock")
    private Rect mOriginalViewPortCropRect = new Rect();

    @GuardedBy("mAnalyzerLock")
    private Rect mUpdatedViewPortCropRect = new Rect();

    @GuardedBy("mAnalyzerLock")
    private Matrix mOriginalSensorToBufferTransformMatrix = new Matrix();

    @GuardedBy("mAnalyzerLock")
    private Matrix mUpdatedSensorToBufferTransformMatrix = new Matrix();

    @GuardedBy("mAnalyzerLock")
    @Nullable
    private ByteBuffer mRGBConvertedBuffer;

    @GuardedBy("mAnalyzerLock")
    @Nullable
    private ByteBuffer mYRotatedBuffer;

    @GuardedBy("mAnalyzerLock")
    @Nullable
    private ByteBuffer mURotatedBuffer;

    @GuardedBy("mAnalyzerLock")
    @Nullable
    private ByteBuffer mVRotatedBuffer;

    // Lock that synchronizes the access to mSubscribedAnalyzer/mUserExecutor to prevent mismatch.
    private final Object mAnalyzerLock = new Object();

    // Flag that reflects the attaching state of the holding ImageAnalysis object.
    protected boolean mIsAttached = true;

    @Override
    public void onImageAvailable(@NonNull ImageReaderProxy imageReaderProxy) {
        try {
            ImageProxy imageProxy = acquireImage(imageReaderProxy);
            if (imageProxy != null) {
                onValidImageAvailable(imageProxy);
            }
        } catch (IllegalStateException e) {
            // This happens if image is not closed in STRATEGY_BLOCK_PRODUCER mode. Catch the
            // exception and fail with an error log.
            // TODO(b/175851631): it may also happen when STRATEGY_KEEP_ONLY_LATEST not closing
            //  the cached image properly. We are unclear why it happens but catching the
            //  exception should improve the situation by not crashing.
            Logger.e(TAG, "Failed to acquire image.", e);
        }
    }

    /**
     * Implemented by children to acquireImage via {@link ImageReaderProxy#acquireLatestImage()} or
     * {@link ImageReaderProxy#acquireNextImage()}.
     */
    @Nullable
    abstract ImageProxy acquireImage(@NonNull ImageReaderProxy imageReaderProxy);

    /**
     * Called when a new valid {@link ImageProxy} becomes available via
     * {@link ImageReaderProxy.OnImageAvailableListener}.
     */
    abstract void onValidImageAvailable(@NonNull ImageProxy imageProxy);

    /**
     * Called by {@link ImageAnalysis} to release cached images.
     */
    abstract void clearCache();

    /**
     * Analyzes a {@link ImageProxy} using the wrapped {@link ImageAnalysis.Analyzer}.
     *
     * <p> The analysis will run on the executor provided by {@link #setAnalyzer(Executor,
     * ImageAnalysis.Analyzer)}. Once the analysis successfully finishes the returned
     * ListenableFuture will succeed. If the future fails then it means the {@link
     * ImageAnalysis.Analyzer} was not called so the image needs to be closed.
     *
     * @return The future which will complete once analysis has finished or it failed.
     */
    ListenableFuture<Void> analyzeImage(@NonNull ImageProxy imageProxy) {
        Executor executor;
        ImageAnalysis.Analyzer analyzer;
        SafeCloseImageReaderProxy processedImageReaderProxy;
        ImageWriter processedImageWriter;
        ByteBuffer rgbConvertedBuffer;
        ByteBuffer yRotatedBuffer;
        ByteBuffer uRotatedBuffer;
        ByteBuffer vRotatedBuffer;
        int currentBufferRotationDegrees = mOutputImageRotationEnabled ? mRelativeRotation : 0;

        synchronized (mAnalyzerLock) {
            executor = mUserExecutor;
            analyzer = mSubscribedAnalyzer;

            // Set dirty flag to indicate the output image transform matrix (for both YUV and RGB)
            // and image reader proxy (for YUV) needs to be recreated.
            mOutputImageDirty = mOutputImageRotationEnabled
                    && currentBufferRotationDegrees != mPrevBufferRotationDegrees;

            // Cache the image reader proxy and image write for reuse and only recreate when
            // relative rotation degree changes.
            if (mOutputImageDirty) {
                recreateImageReaderProxy(imageProxy, currentBufferRotationDegrees);
            }

            // Cache memory buffer for image rotation
            createHelperBuffer(imageProxy);

            processedImageReaderProxy = mProcessedImageReaderProxy;
            processedImageWriter = mProcessedImageWriter;
            rgbConvertedBuffer = mRGBConvertedBuffer;
            yRotatedBuffer = mYRotatedBuffer;
            uRotatedBuffer = mURotatedBuffer;
            vRotatedBuffer = mVRotatedBuffer;
        }

        ListenableFuture<Void> future;

        if (analyzer != null && executor != null && mIsAttached) {
            ImageProxy processedImageProxy = null;

            if (processedImageReaderProxy != null) {
                if (mOutputImageFormat == OUTPUT_IMAGE_FORMAT_RGBA_8888) {
                    processedImageProxy =
                            convertYUVToRGB(
                                    imageProxy,
                                    processedImageReaderProxy,
                                    rgbConvertedBuffer,
                                    currentBufferRotationDegrees,
                                    mOnePixelShiftEnabled);
                } else if (mOutputImageFormat == ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) {
                    // Apply one pixel shift before other processing, e.g. rotation.
                    if (mOnePixelShiftEnabled) {
                        applyPixelShiftForYUV(imageProxy);
                    }
                    if (processedImageWriter != null) {
                        processedImageProxy = rotateYUV(
                                imageProxy,
                                processedImageReaderProxy,
                                processedImageWriter,
                                yRotatedBuffer,
                                uRotatedBuffer,
                                vRotatedBuffer,
                                currentBufferRotationDegrees);
                    }
                }
            }

            // Flag to indicate YUV2RGB conversion or YUV/RGB rotation failed, not including one
            // pixel shift process for YUV.
            final boolean outputProcessedImageFailed = processedImageProxy == null;
            final ImageProxy outputImageProxy = outputProcessedImageFailed ? imageProxy :
                    processedImageProxy;

            // recalculate transform matrix and update crop rect only if
            // rotation succeeded and relative rotation degree changed
            Rect cropRect = new Rect();
            Matrix transformMatrix = new Matrix();
            synchronized (mAnalyzerLock) {
                if (mOutputImageDirty && !outputProcessedImageFailed) {
                    recalculateTransformMatrixAndCropRect(
                            imageProxy.getWidth(),
                            imageProxy.getHeight(),
                            outputImageProxy.getWidth(),
                            outputImageProxy.getHeight());
                }
                mPrevBufferRotationDegrees = currentBufferRotationDegrees;

                cropRect.set(mUpdatedViewPortCropRect);
                transformMatrix.set(mUpdatedSensorToBufferTransformMatrix);
            }

            // When the analyzer exists and ImageAnalysis is active.
            future = CallbackToFutureAdapter.getFuture(
                    completer -> {
                        executor.execute(() -> {
                            if (mIsAttached) {
                                ImageInfo imageInfo = ImmutableImageInfo.create(
                                        imageProxy.getImageInfo().getTagBundle(),
                                        imageProxy.getImageInfo().getTimestamp(),
                                        mOutputImageRotationEnabled ? 0
                                                : mRelativeRotation,
                                        transformMatrix);

                                ImageProxy outputSettableImageProxy = new SettableImageProxy(
                                        outputImageProxy, imageInfo);
                                outputSettableImageProxy.setCropRect(cropRect);
                                analyzer.analyze(outputSettableImageProxy);
                                completer.set(null);
                            } else {
                                completer.setException(new OperationCanceledException(
                                        "ImageAnalysis is detached"));
                            }
                        });
                        return "analyzeImage";
                    });
        } else {
            future = Futures.immediateFailedFuture(new OperationCanceledException(
                    "No analyzer or executor currently set."));
        }

        return future;
    }

    @NonNull
    private static SafeCloseImageReaderProxy createImageReaderProxy(
            int imageWidth,
            int imageHeight,
            int rotation,
            int format,
            int maxImages) {
        boolean flipWH = (rotation == 90 || rotation == 270);
        int width = flipWH ? imageHeight : imageWidth;
        int height = flipWH ? imageWidth : imageHeight;

        return new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        width,
                        height,
                        format,
                        maxImages));
    }

    void setRelativeRotation(int relativeRotation) {
        mRelativeRotation = relativeRotation;
    }

    void setOutputImageRotationEnabled(boolean outputImageRotationEnabled) {
        mOutputImageRotationEnabled = outputImageRotationEnabled;
    }

    void setOutputImageFormat(@ImageAnalysis.OutputImageFormat int outputImageFormat) {
        mOutputImageFormat = outputImageFormat;
    }

    void setOnePixelShiftEnabled(boolean onePixelShiftEnabled) {
        mOnePixelShiftEnabled = onePixelShiftEnabled;
    }

    void setViewPortCropRect(@NonNull Rect viewPortCropRect) {
        synchronized (mAnalyzerLock) {
            mOriginalViewPortCropRect = viewPortCropRect;
            mUpdatedViewPortCropRect = new Rect(mOriginalViewPortCropRect);
        }
    }

    void setSensorToBufferTransformMatrix(@NonNull Matrix sensorToBufferTransformMatrix) {
        synchronized (mAnalyzerLock) {
            mOriginalSensorToBufferTransformMatrix = sensorToBufferTransformMatrix;
            mUpdatedSensorToBufferTransformMatrix =
                    new Matrix(mOriginalSensorToBufferTransformMatrix);
        }
    }

    void setProcessedImageReaderProxy(
            @NonNull SafeCloseImageReaderProxy processedImageReaderProxy) {
        synchronized (mAnalyzerLock) {
            mProcessedImageReaderProxy = processedImageReaderProxy;
        }

    }

    void setAnalyzer(@Nullable Executor userExecutor,
            @Nullable ImageAnalysis.Analyzer subscribedAnalyzer) {
        synchronized (mAnalyzerLock) {
            if (subscribedAnalyzer == null) {
                clearCache();
            }
            mSubscribedAnalyzer = subscribedAnalyzer;
            mUserExecutor = userExecutor;
        }
    }

    /**
     * Initialize the callback.
     */
    void attach() {
        mIsAttached = true;
    }

    /**
     * Closes the callback so that it will stop posting to analyzer.
     */
    void detach() {
        mIsAttached = false;
        clearCache();
    }

    @GuardedBy("mAnalyzerLock")
    private void createHelperBuffer(@NonNull ImageProxy imageProxy) {
        if (mOutputImageFormat == ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) {
            if (mYRotatedBuffer == null) {
                mYRotatedBuffer = ByteBuffer.allocateDirect(
                        imageProxy.getWidth() * imageProxy.getHeight());
            }
            mYRotatedBuffer.position(0);

            if (mURotatedBuffer == null) {
                mURotatedBuffer = ByteBuffer.allocateDirect(
                        imageProxy.getWidth() * imageProxy.getHeight() / 4);
            }
            mURotatedBuffer.position(0);

            if (mVRotatedBuffer == null) {
                mVRotatedBuffer = ByteBuffer.allocateDirect(
                        imageProxy.getWidth() * imageProxy.getHeight() / 4);
            }
            mVRotatedBuffer.position(0);
        } else if (mOutputImageFormat == OUTPUT_IMAGE_FORMAT_RGBA_8888) {
            if (mRGBConvertedBuffer == null) {
                mRGBConvertedBuffer = ByteBuffer.allocateDirect(
                        imageProxy.getWidth() * imageProxy.getHeight() * 4);
            }
        }
    }

    @GuardedBy("mAnalyzerLock")
    private void recreateImageReaderProxy(
            @NonNull ImageProxy imageProxy,
            @IntRange(from = 0, to = 359) int relativeRotation) {
        if (mProcessedImageReaderProxy == null) {
            return;
        }

        mProcessedImageReaderProxy.safeClose();
        mProcessedImageReaderProxy = createImageReaderProxy(
                imageProxy.getWidth(),
                imageProxy.getHeight(),
                relativeRotation,
                mProcessedImageReaderProxy.getImageFormat(),
                mProcessedImageReaderProxy.getMaxImages());

        if (Build.VERSION.SDK_INT >= 23
                && mOutputImageFormat == ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) {

            if (mProcessedImageWriter != null) {
                ImageWriterCompat.close(mProcessedImageWriter);
            }

            mProcessedImageWriter = ImageWriterCompat.newInstance(
                    mProcessedImageReaderProxy.getSurface(),
                    mProcessedImageReaderProxy.getMaxImages());
        }
    }

    @GuardedBy("mAnalyzerLock")
    private void recalculateTransformMatrixAndCropRect(
            int originalWidth,
            int originalHeight,
            int rotatedWidth,
            int rotatedHeight) {
        Matrix additionalTransformMatrix = getAdditionalTransformMatrixAppliedByProcessor(
                originalWidth,
                originalHeight,
                rotatedWidth,
                rotatedHeight,
                mRelativeRotation);
        mUpdatedViewPortCropRect = getUpdatedCropRect(
                mOriginalViewPortCropRect, additionalTransformMatrix);
        mUpdatedSensorToBufferTransformMatrix.setConcat(mOriginalSensorToBufferTransformMatrix,
                additionalTransformMatrix);
    }

    @NonNull
    static Rect getUpdatedCropRect(
            @NonNull Rect originalCropRect,
            @NonNull Matrix additionalTransformMatrix) {
        RectF rectF = new RectF(originalCropRect);
        additionalTransformMatrix.mapRect(rectF);
        Rect rect = new Rect();
        rectF.round(rect);
        return rect;
    }

    @VisibleForTesting
    @NonNull
    static Matrix getAdditionalTransformMatrixAppliedByProcessor(
            int originalWidth,
            int originalHeight,
            int rotatedWidth,
            int rotatedHeight,
            @IntRange(from = 0, to = 359) int relativeRotation) {
        Matrix matrix = new Matrix();
        if (relativeRotation > 0) {
            matrix.setRectToRect(
                    new RectF(0, 0, originalWidth, originalHeight),
                    NORMALIZED_RECT,
                    Matrix.ScaleToFit.FILL);
            matrix.postRotate(relativeRotation);
            matrix.postConcat(getNormalizedToBuffer(new RectF(0, 0, rotatedWidth,
                    rotatedHeight)));
        }
        return matrix;
    }

    @NonNull
    private static Matrix getNormalizedToBuffer(@NonNull RectF buffer) {
        Matrix normalizedToBuffer = new Matrix();
        normalizedToBuffer.setRectToRect(NORMALIZED_RECT, buffer, Matrix.ScaleToFit.FILL);
        return normalizedToBuffer;
    }
}
