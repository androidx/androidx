/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.extensions.impl.advanced;

import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;

import androidx.annotation.NonNull;

public class DefaultRequestProcessorImpl implements RequestProcessorImpl.Callback {
    @Override
    public void onCaptureStarted(@NonNull RequestProcessorImpl.Request request, long frameNumber,
            long timestamp) {

    }

    @Override
    public void onCaptureProgressed(@NonNull RequestProcessorImpl.Request request,
            @NonNull CaptureResult partialResult) {

    }

    @Override
    public void onCaptureCompleted(@NonNull RequestProcessorImpl.Request request,
            @NonNull TotalCaptureResult totalCaptureResult) {

    }

    @Override
    public void onCaptureFailed(@NonNull RequestProcessorImpl.Request request,
            @NonNull CaptureFailure captureFailure) {

    }

    @Override
    public void onCaptureBufferLost(@NonNull RequestProcessorImpl.Request request, long frameNumber,
            int outputStreamId) {

    }

    @Override
    public void onCaptureSequenceCompleted(int sequenceId, long frameNumber) {

    }

    @Override
    public void onCaptureSequenceAborted(int sequenceId) {

    }
}
