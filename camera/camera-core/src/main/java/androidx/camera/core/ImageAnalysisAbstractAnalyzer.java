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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.os.OperationCanceledException;

import com.google.common.util.concurrent.ListenableFuture;

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

    // Member variables from ImageAnalysis.
    @GuardedBy("mAnalyzerLock")
    private ImageAnalysis.Analyzer mSubscribedAnalyzer;
    private volatile int mRelativeRotation;
    @ImageAnalysis.OutputImageFormat
    private volatile int mOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888;
    private volatile boolean mOnePixelShiftEnabled;
    @GuardedBy("mAnalyzerLock")
    private Executor mUserExecutor;

    @GuardedBy("mAnalyzerLock")
    @Nullable
    private ImageReaderProxy mRGBImageReaderProxy;

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
     * androidx.camera.core.ImageAnalysis.Analyzer} was not called so the image needs to be closed.
     *
     * @return The future which will complete once analysis has finished or it failed.
     */
    ListenableFuture<Void> analyzeImage(@NonNull ImageProxy imageProxy) {
        Executor executor;
        ImageAnalysis.Analyzer analyzer;
        ImageReaderProxy rgbImageReaderProxy;
        synchronized (mAnalyzerLock) {
            executor = mUserExecutor;
            analyzer = mSubscribedAnalyzer;
            rgbImageReaderProxy = mRGBImageReaderProxy;
        }

        ListenableFuture<Void> future;

        if (analyzer != null && executor != null && mIsAttached) {
            final ImageProxy rgbImageProxy =
                    (mOutputImageFormat == OUTPUT_IMAGE_FORMAT_RGBA_8888
                            && rgbImageReaderProxy != null)
                            ? convertYUVToRGB(
                                    imageProxy, rgbImageReaderProxy, mOnePixelShiftEnabled)
                            : null;
            if (mOutputImageFormat == ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
                    && mOnePixelShiftEnabled) {
                applyPixelShiftForYUV(imageProxy);
            }

            // When the analyzer exists and ImageAnalysis is active.
            future = CallbackToFutureAdapter.getFuture(
                    completer -> {
                        executor.execute(() -> {
                            if (mIsAttached) {
                                ImageInfo imageInfo = ImmutableImageInfo.create(
                                        imageProxy.getImageInfo().getTagBundle(),
                                        imageProxy.getImageInfo().getTimestamp(),
                                        mRelativeRotation);

                                analyzer.analyze(new SettableImageProxy(rgbImageProxy == null
                                        ? imageProxy : rgbImageProxy, imageInfo));
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

    void setRelativeRotation(int relativeRotation) {
        mRelativeRotation = relativeRotation;
    }

    void setOutputImageFormat(@ImageAnalysis.OutputImageFormat int outputImageFormat) {
        mOutputImageFormat = outputImageFormat;
    }

    void setOnePixelShiftEnabled(boolean onePixelShiftEnabled) {
        mOnePixelShiftEnabled = onePixelShiftEnabled;
    }

    void setRGBImageReaderProxy(@NonNull ImageReaderProxy rgbImageReaderProxy) {
        synchronized (mAnalyzerLock) {
            mRGBImageReaderProxy = rgbImageReaderProxy;
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
}
