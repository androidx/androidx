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

package androidx.camera.core.processing;

import static androidx.camera.core.ImageCapture.ERROR_UNKNOWN;
import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Objects.requireNonNull;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProcessor;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * An internal {@link ImageProcessor} that wraps a {@link CameraEffect} targeting
 * {@link CameraEffect#IMAGE_CAPTURE}.
 *
 * <p>This class wrap calls to {@link ImageProcessor} with the effect-provided {@link Executor}.
 * It also provides additional from Camera
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class InternalImageProcessor {

    @NonNull
    private final Executor mExecutor;
    @NonNull
    private final ImageProcessor mImageProcessor;

    public InternalImageProcessor(@NonNull CameraEffect cameraEffect) {
        checkArgument(cameraEffect.getTargets() == CameraEffect.IMAGE_CAPTURE);
        mExecutor = cameraEffect.getExecutor();
        mImageProcessor = requireNonNull(cameraEffect.getImageProcessor());
    }

    /**
     * Forwards the call to {@link ImageProcessor#process} on the effect-provided executor.
     */
    @NonNull
    public ImageProcessor.Response safeProcess(@NonNull ImageProcessor.Request request)
            throws ImageCaptureException {
        try {
            return CallbackToFutureAdapter.getFuture(
                    (CallbackToFutureAdapter.Resolver<ImageProcessor.Response>) completer -> {
                        mExecutor.execute(() -> {
                            try {
                                completer.set(mImageProcessor.process(request));
                            } catch (Exception e) {
                                // Catch all exceptions and forward it CameraX.
                                completer.setException(e);
                            }
                        });
                        return "InternalImageProcessor#process " + request.hashCode();
                    }).get();
        } catch (ExecutionException | InterruptedException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new ImageCaptureException(
                    ERROR_UNKNOWN, "Failed to invoke ImageProcessor.", cause);
        }
    }
}
