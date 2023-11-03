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

package androidx.camera.effects.stillportrait;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.UseCase;

import java.util.concurrent.Executor;

/**
 * Provides a portrait post-processing effect.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class StillPortrait extends CameraEffect {

    /**
     * @param targets           the target {@link UseCase} to which this effect should be applied.
     * @param processorExecutor the {@link Executor} on which the processor will be invoked.
     * @param surfaceProcessor  a {@link SurfaceProcessor} implementation.
     */
    protected StillPortrait(int targets,
            @NonNull Executor processorExecutor,
            @NonNull SurfaceProcessor surfaceProcessor) {
        super(targets, processorExecutor, surfaceProcessor, throwable -> {
        });
        // TODO: implement this.
    }
}
