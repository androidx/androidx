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

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.os.OperationCanceledException;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract Analyzer that wraps around {@link ImageAnalysis.Analyzer} and implements
 * {@link ImageReaderProxy.OnImageAvailableListener}.
 *
 * This is an extension of {@link ImageAnalysis}. It has the same lifecycle and share part of the
 * states.
 */
abstract class ImageAnalysisAbstractAnalyzer implements ImageReaderProxy.OnImageAvailableListener {

    // Member variables from ImageAnalysis.
    @GuardedBy("mAnalyzerLock")
    private ImageAnalysis.Analyzer mSubscribedAnalyzer;
    private volatile int mRelativeRotation;
    @GuardedBy("mAnalyzerLock")
    private Executor mUserExecutor;

    private final Object mAnalyzerLock = new Object();

    // Flag that reflects the state of ImageAnalysis.
    private AtomicBoolean mIsClosed;

    ImageAnalysisAbstractAnalyzer() {
        mIsClosed = new AtomicBoolean(false);
    }

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
    ListenableFuture<Void> analyzeImage(ImageProxy imageProxy) {
        Executor executor;
        ImageAnalysis.Analyzer analyzer;
        synchronized (mAnalyzerLock) {
            executor = mUserExecutor;
            analyzer = mSubscribedAnalyzer;
        }

        ListenableFuture<Void> future;

        if (analyzer != null && executor != null) {
            // When the analyzer exists and ImageAnalysis is active.
            future = CallbackToFutureAdapter.getFuture(
                    completer ->  {
                        executor.execute(() -> {
                            if (!isClosed()) {
                                ImageInfo imageInfo = ImmutableImageInfo.create(
                                        imageProxy.getImageInfo().getTag(),
                                        imageProxy.getImageInfo().getTimestamp(),
                                        mRelativeRotation);

                                analyzer.analyze(new SettableImageProxy(imageProxy, imageInfo));
                                completer.set(null);
                            } else {
                                completer.setException(new OperationCanceledException("Closed "
                                        + "before analysis"));
                            }
                        });
                    return "analyzeImage";
                    });
        } else {
            future = Futures.immediateFailedFuture(new OperationCanceledException("No analyzer "
                    + "or executor currently set."));
        }

        return future;
    }

    void setRelativeRotation(int relativeRotation) {
        mRelativeRotation = relativeRotation;
    }

    void setAnalyzer(@Nullable Executor userExecutor,
            @Nullable ImageAnalysis.Analyzer subscribedAnalyzer) {
        synchronized (mAnalyzerLock) {
            mSubscribedAnalyzer = subscribedAnalyzer;
            mUserExecutor = userExecutor;
        }
    }

    /**
     * Initialize the callback.
     */
    void open() {
        mIsClosed.set(false);
    }

    /**
     * Closes the callback so that it will stop posting to analyzer.
     */
    void close() {
        mIsClosed.set(true);
    }

    boolean isClosed() {
        return mIsClosed.get();
    }

}
