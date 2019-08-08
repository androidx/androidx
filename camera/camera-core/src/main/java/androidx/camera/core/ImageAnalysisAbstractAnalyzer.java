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

import android.os.Handler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract Analyzer that wraps around {@link ImageAnalysis.Analyzer} and implements
 * {@link ImageReaderProxy.OnImageAvailableListener}.
 *
 * This is an extension of {@link ImageAnalysis}. It has the same lifecycle and share part of the
 * states.
 */
abstract class ImageAnalysisAbstractAnalyzer implements ImageReaderProxy.OnImageAvailableListener {

    // Member variables from ImageAnalysis.
    private final AtomicReference<ImageAnalysis.Analyzer> mSubscribedAnalyzer;
    private final AtomicInteger mRelativeRotation;
    final Handler mUserHandler;

    // Flag that reflects the state of ImageAnalysis.
    private AtomicBoolean mIsClosed;

    ImageAnalysisAbstractAnalyzer(AtomicReference<ImageAnalysis.Analyzer> subscribedAnalyzer,
            AtomicInteger relativeRotation, Handler userHandler) {
        mSubscribedAnalyzer = subscribedAnalyzer;
        mRelativeRotation = relativeRotation;
        mUserHandler = userHandler;
        mIsClosed = new AtomicBoolean(false);
    }

    /**
     * Analyzes a {@link ImageProxy} using the wrapped {@link ImageAnalysis.Analyzer}.
     */
    void analyzeImage(ImageProxy imageProxy) {
        ImageAnalysis.Analyzer analyzer = mSubscribedAnalyzer.get();
        if (analyzer != null && !isClosed()) {
            // When the analyzer exists and ImageAnalysis is active.
            analyzer.analyze(imageProxy, mRelativeRotation.get());
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
