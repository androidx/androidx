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

package androidx.camera.testing.fakes;

import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.processing.SurfaceProcessorInternal;

import java.util.concurrent.Executor;

/**
 * A fake {@link CameraEffect} with {@link SurfaceProcessor}.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FakeSurfaceEffect extends CameraEffect {

    private SurfaceProcessorInternal mSurfaceProcessorInternal;

    public FakeSurfaceEffect(
            @NonNull Executor processorExecutor,
            @NonNull SurfaceProcessor surfaceProcessor) {
        super(PREVIEW, processorExecutor, surfaceProcessor, throwable -> {
        });
    }

    /**
     * Create a fake {@link CameraEffect} the {@link #createSurfaceProcessorInternal} value
     * overridden.
     */
    public FakeSurfaceEffect(@NonNull SurfaceProcessorInternal surfaceProcessorInternal) {
        this(PREVIEW, surfaceProcessorInternal);
    }

    /**
     * Create a fake {@link CameraEffect} the {@link #createSurfaceProcessorInternal} value
     * overridden.
     *
     * <p> This is helpful when we want to make sure the {@link SurfaceProcessorInternal} is
     * released properly.
     */
    public FakeSurfaceEffect(@Targets int targets,
            @NonNull SurfaceProcessorInternal surfaceProcessorInternal) {
        super(targets, mainThreadExecutor(), surfaceProcessorInternal, throwable -> {
        });
        mSurfaceProcessorInternal = surfaceProcessorInternal;
    }

    @NonNull
    @Override
    public SurfaceProcessorInternal createSurfaceProcessorInternal() {
        if (mSurfaceProcessorInternal != null) {
            return mSurfaceProcessorInternal;
        } else {
            return super.createSurfaceProcessorInternal();
        }
    }
}
