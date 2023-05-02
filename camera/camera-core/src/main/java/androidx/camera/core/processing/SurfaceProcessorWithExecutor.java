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

import static androidx.camera.core.impl.utils.futures.Futures.immediateFailedFuture;

import static java.util.Objects.requireNonNull;

import android.os.Build;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.Logger;
import androidx.camera.core.ProcessingException;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;
import androidx.core.util.Consumer;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * A wrapper of a pair of {@link SurfaceProcessor} and {@link Executor}.
 *
 * <p> Wraps the external {@link SurfaceProcessor} and {@link Executor} provided by the app. It
 * makes sure that CameraX always invoke the {@link SurfaceProcessor} on the correct
 * {@link Executor}.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SurfaceProcessorWithExecutor implements SurfaceProcessorInternal {

    private static final String TAG = "SurfaceProcessor";

    @NonNull
    private final SurfaceProcessor mSurfaceProcessor;
    @NonNull
    private final Executor mExecutor;
    @NonNull
    private final Consumer<Throwable> mErrorListener;

    public SurfaceProcessorWithExecutor(@NonNull CameraEffect cameraEffect) {
        mSurfaceProcessor = requireNonNull(cameraEffect.getSurfaceProcessor());
        mExecutor = cameraEffect.getExecutor();
        mErrorListener = cameraEffect.getErrorListener();
    }

    @NonNull
    @VisibleForTesting
    public SurfaceProcessor getProcessor() {
        return mSurfaceProcessor;
    }

    @NonNull
    @VisibleForTesting
    public Executor getExecutor() {
        return mExecutor;
    }

    @Override
    public void onInputSurface(@NonNull SurfaceRequest request) {
        mExecutor.execute(() -> {
            try {
                mSurfaceProcessor.onInputSurface(request);
            } catch (ProcessingException e) {
                Logger.e(TAG, "Failed to setup SurfaceProcessor input.", e);
                mErrorListener.accept(e);
            }
        });
    }

    @Override
    public void onOutputSurface(@NonNull SurfaceOutput surfaceOutput) {
        mExecutor.execute(() -> {
            try {
                mSurfaceProcessor.onOutputSurface(surfaceOutput);
            } catch (ProcessingException e) {
                Logger.e(TAG, "Failed to setup SurfaceProcessor output.", e);
                mErrorListener.accept(e);
            }
        });
    }

    @NonNull
    @Override
    public ListenableFuture<Void> snapshot(
            @IntRange(from = 0, to = 100) int jpegQuality,
            @IntRange(from = 0, to = 359) int rotationDegrees) {
        return immediateFailedFuture(
                new Exception("Snapshot not supported by external SurfaceProcessor"));
    }

    @Override
    public void release() {
        // No-op. External SurfaceProcessor should not be released by CameraX.
    }
}
