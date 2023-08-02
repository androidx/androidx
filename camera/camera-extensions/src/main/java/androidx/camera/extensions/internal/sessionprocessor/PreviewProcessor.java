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
import android.graphics.PixelFormat;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.extensions.impl.PreviewImageProcessorImpl;
import androidx.camera.extensions.impl.ProcessResultImpl;
import androidx.camera.extensions.internal.ClientVersion;
import androidx.camera.extensions.internal.ExtensionVersion;
import androidx.camera.extensions.internal.Version;

import java.util.List;

/**
 * A preview processor that is responsible for invoking OEM's PreviewImageProcessorImpl and
 * output to the given PRIVATE surface.
 *
 * <p>To start the processing, invoke {@link #start()} and then feed {@link ImageReference} and
 * {@link TotalCaptureResult} instances by {@link #notifyImage(ImageReference)} and
 * {@link #notifyCaptureResult(TotalCaptureResult)} respectively. The output will be written to the
 * given output surface. Invoke {@link #close()} to close the processor and reclaim the resources.
 *
 * <p>Please note that output preview surface must be closed AFTER this processor is closed.
 */
@RequiresApi(21)
class PreviewProcessor {
    private static final String TAG = "PreviewProcessor";
    @NonNull
    final PreviewImageProcessorImpl mPreviewImageProcessor;
    @NonNull
    final CaptureResultImageMatcher mCaptureResultImageMatcher = new CaptureResultImageMatcher();
    final Object mLock = new Object();
    @GuardedBy("mLock")
    boolean mIsClosed = false;

    PreviewProcessor(@NonNull PreviewImageProcessorImpl previewImageProcessor,
            @NonNull Surface previewOutputSurface, @NonNull Size surfaceSize) {
        mPreviewImageProcessor = previewImageProcessor;
        mPreviewImageProcessor.onResolutionUpdate(surfaceSize);
        mPreviewImageProcessor.onOutputSurface(previewOutputSurface, PixelFormat.RGBA_8888);
        mPreviewImageProcessor.onImageFormatUpdate(ImageFormat.YUV_420_888);
    }

    interface OnCaptureResultCallback {
        void onCaptureResult(long shutterTimestamp,
                @NonNull List<Pair<CaptureResult.Key, Object>> result);
    }

    void start(@NonNull OnCaptureResultCallback onResultCallback) {
        mCaptureResultImageMatcher.setImageReferenceListener(
                (imageReference, totalCaptureResult, captureStageId) -> {
                    synchronized (mLock) {
                        if (mIsClosed) {
                            imageReference.decrement();
                            Logger.d(TAG, "Ignore image in closed state");
                            return;
                        }
                        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_3)
                                && ExtensionVersion
                                .isMinimumCompatibleVersion(Version.VERSION_1_3)) {
                            mPreviewImageProcessor.process(imageReference.get(), totalCaptureResult,
                                    new ProcessResultImpl() {
                                        @Override
                                        public void onCaptureCompleted(long shutterTimestamp,
                                                @NonNull List<Pair<CaptureResult.Key, Object>>
                                                        result) {
                                            onResultCallback.onCaptureResult(shutterTimestamp,
                                                    result);
                                        }

                                        @Override
                                        public void onCaptureProcessProgressed(int progress) {

                                        }
                                    }, CameraXExecutors.ioExecutor());
                        } else {
                            mPreviewImageProcessor.process(imageReference.get(),
                                    totalCaptureResult);
                        }
                        imageReference.decrement();
                    }
                });
    }

    void notifyCaptureResult(@NonNull TotalCaptureResult captureResult) {
        mCaptureResultImageMatcher.captureResultIncoming(captureResult);
    }

    void notifyImage(@NonNull ImageReference imageReference) {
        mCaptureResultImageMatcher.imageIncoming(imageReference);
    }

    /**
     * Close the processor. Please note that output preview surface must be closed AFTER this
     * processor is closed.
     */
    void close() {
        synchronized (mLock) {
            mIsClosed = true;
            mCaptureResultImageMatcher.clear();
            mCaptureResultImageMatcher.clearImageReferenceListener();
        }
    }

}
