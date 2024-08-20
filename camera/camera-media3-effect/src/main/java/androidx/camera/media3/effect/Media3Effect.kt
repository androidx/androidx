/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.media3.effect

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.camera.core.CameraEffect
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.core.util.Consumer
import androidx.media3.common.Effect
import java.util.concurrent.Executor

/**
 * A CameraEffect that applies media3 [Effect] to the CameraX pipeline
 *
 * This class is an adapter between the CameraX [CameraEffect] API and the media3 [Effect] API. It
 * allows the media3 [Effect] to be applied to the CameraX pipeline on the fly without restarting
 * the camera.
 *
 * @param context the Android context
 * @param targets the target UseCase to which this effect should be applied. For details, see
 *   [CameraEffect].
 * @param executor the [Executor] on which the errorListener will be invoked.
 * @param errorListener invoked if the effect runs into unrecoverable errors. The [Throwable] will
 *   be the error thrown by this [Media3Effect]. This is invoked on the provided executor.
 */
@SuppressLint("UnsafeOptInUsageError")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Media3Effect(
    context: Context,
    targets: Int,
    executor: Executor,
    errorListener: Consumer<Throwable>,
) :
    CameraEffect(
        targets,
        mainThreadExecutor(),
        Media3SurfaceProcessor(
            context,
            executor,
            errorListener,
        ),
        {}
    ),
    AutoCloseable {

    /**
     * Closes the [Media3Effect] and releases all the resources.
     *
     * <p>The caller should only close when the effect when it's no longer in use. Once closed, the
     * effect should not be used again.
     */
    public override fun close() {
        val glSurfaceProcessor = surfaceProcessor
        if (glSurfaceProcessor is Media3SurfaceProcessor) {
            glSurfaceProcessor.release()
        }
    }

    /**
     * Applies a list of media3 effects to the camera output.
     *
     * <p>Once set, the effects will be effective immediately. To clear the effect, call this method
     * again with a empty list.
     */
    @SuppressLint("UnsafeOptInUsageError")
    public fun setEffects(effects: List<Effect>) {
        val glSurfaceProcessor = surfaceProcessor
        if (glSurfaceProcessor is Media3SurfaceProcessor) {
            glSurfaceProcessor.setEffects(effects)
        }
    }
}

internal const val TAG: String = "Media3Effect"
