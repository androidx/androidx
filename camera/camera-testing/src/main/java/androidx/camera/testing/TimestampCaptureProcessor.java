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

package androidx.camera.testing;

import android.media.Image;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.ImageProxyBundle;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * A {@link CaptureProcessor} that wraps another CaptureProcessor and captures the timestamps of all
 * the {@link ImageProxy} that are processed by it.
 *
 * <p>This class is used for testing of preview processing only. The expectation is that each
 * {@link ImageProxyBundle} will only have a single {@link ImageProxy}.
 *
 * @hide
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(RestrictTo.Scope.TESTS)
public class TimestampCaptureProcessor implements CaptureProcessor {
    private CaptureProcessor mCaptureProcessor;
    private TimestampListener mTimestampListener;

    /**
     * @param captureProcessor  The {@link CaptureProcessor} that is wrapped.
     * @param timestampListener The listener which receives the timestamp of all
     *                          {@link ImageProxy} which are processed.
     */
    public TimestampCaptureProcessor(@NonNull CaptureProcessor captureProcessor,
            @NonNull TimestampListener timestampListener) {
        mCaptureProcessor = captureProcessor;
        mTimestampListener = timestampListener;
    }

    /**
     * Interface for receiving the timestamps of all {@link ImageProxy} which are processed by the
     * wrapped {@link CaptureProcessor}.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public interface TimestampListener {
        /**
         * Called whenever an {@link ImageProxy} is processed.
         *
         * @param timestamp The timestamp of the {@link ImageProxy} that is processed.
         */
        void onTimestampAvailable(long timestamp);
    }

    @Override
    public void onOutputSurface(@NonNull Surface surface, int imageFormat) {
        mCaptureProcessor.onOutputSurface(surface, imageFormat);
    }

    @Override
    @ExperimentalGetImage
    public void process(@NonNull ImageProxyBundle bundle) {
        List<Integer> ids = bundle.getCaptureIds();
        Preconditions.checkArgument(ids.size() == 1,
                "Processing preview bundle must be 1, but found " + ids.size());

        ListenableFuture<ImageProxy> imageProxyListenableFuture = bundle.getImageProxy(ids.get(0));
        Preconditions.checkArgument(imageProxyListenableFuture.isDone());

        try {
            ImageProxy imageProxy = imageProxyListenableFuture.get();
            Image image = imageProxy.getImage();
            if (image == null) {
                return;
            }

            // Send timestamp
            mTimestampListener.onTimestampAvailable(image.getTimestamp());
            mCaptureProcessor.process(bundle);
        } catch (ExecutionException | InterruptedException e) {
            // Intentionally empty. Only the ImageProxy which can be retrieved need to have its
            // timestamp captured.
        }
    }

    @Override
    public void onResolutionUpdate(@NonNull Size size) {
        mCaptureProcessor.onResolutionUpdate(size);
    }
}
