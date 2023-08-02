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

import static androidx.core.util.Preconditions.checkState;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.SurfaceEffect;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceRequest;

import java.util.concurrent.Executor;

/**
 * A wrapper of a pair of {@link SurfaceEffect} and {@link Executor}.
 *
 * <p> Wraps the external {@link SurfaceEffect} and {@link Executor} provided by the app. It
 * makes sure that CameraX always invoke the {@link SurfaceEffect} on the correct {@link Executor}.
 */
public class SurfaceEffectWithExecutor implements SurfaceEffectInternal {

    @NonNull
    private final SurfaceEffect mSurfaceEffect;
    @NonNull
    private final Executor mExecutor;

    public SurfaceEffectWithExecutor(
            @NonNull SurfaceEffect surfaceEffect,
            @NonNull Executor executor) {
        checkState(!(surfaceEffect instanceof SurfaceEffectInternal),
                "SurfaceEffectInternal should always be thread safe. Do not wrap.");
        mSurfaceEffect = surfaceEffect;
        mExecutor = executor;
    }

    @NonNull
    @VisibleForTesting
    public SurfaceEffect getSurfaceEffect() {
        return mSurfaceEffect;
    }

    @NonNull
    @VisibleForTesting
    public Executor getExecutor() {
        return mExecutor;
    }

    @Override
    public void onInputSurface(@NonNull SurfaceRequest request) {
        mExecutor.execute(() -> mSurfaceEffect.onInputSurface(request));
    }

    @Override
    public void onOutputSurface(@NonNull SurfaceOutput surfaceOutput) {
        mExecutor.execute(() -> mSurfaceEffect.onOutputSurface(surfaceOutput));
    }

    @Override
    public void release() {
        // No-op. External SurfaceEffect should not be released by CameraX.
    }
}
