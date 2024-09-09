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

package androidx.camera.testing.impl

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import android.view.Surface
import androidx.camera.core.Logger
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async

private const val TAG = "CaptureSimulation"

/** Simulates a capture frame being drawn on all of the provided surfaces. */
internal suspend fun List<DeferrableSurface>.simulateCaptureFrame(): Unit = forEach {
    it.simulateCaptureFrame()
}

/**
 * Simulates a capture frame being drawn on the provided surface.
 *
 * @throws IllegalStateException If [DeferrableSurface.getSurface] provides a null surface.
 */
internal suspend fun DeferrableSurface.simulateCaptureFrame() {
    val deferred = CompletableDeferred<Unit>()

    Futures.addCallback(
        surface,
        object : FutureCallback<Surface?> {
            override fun onSuccess(surface: Surface?) {
                if (surface == null) {
                    Logger.w(TAG, "simulateCaptureFrame: surface obtained from $this is null!")
                    deferred.completeExceptionally(
                        IllegalStateException(
                            "Null surface obtained from ${this@simulateCaptureFrame}"
                        )
                    )
                    return
                }
                // TODO: Draw something on the canvas (e.g. fake image bitmap or alternating color).
                surface.simulateCaptureFrame(prescribedSize)

                deferred.complete(Unit)
            }

            override fun onFailure(t: Throwable) {
                deferred.completeExceptionally(t)
            }
        },
        CameraXExecutors.directExecutor()
    )

    deferred.await()
}

/**
 * Simulates a capture frame being drawn on a [Surface].
 *
 * @param canvasSize The canvas size for drawing.
 * @param bitmap A bitmap to draw as the capture frame, if not null.
 */
internal fun Surface.simulateCaptureFrame(canvasSize: Size, bitmap: Bitmap? = null) {
    val canvas = lockCanvas(Rect(0, 0, canvasSize.width, canvasSize.height))
    if (bitmap != null) {
        canvas.drawBitmap(bitmap, null, Rect(0, 0, canvasSize.width, canvasSize.height), null)
    }
    unlockCanvasAndPost(canvas)
}

// The following methods are adapters for Java invocations.

/**
 * Simulates a capture frame being drawn on all of the provided surfaces.
 *
 * This method uses the provided [Executor] for the asynchronous operations.
 *
 * @param executor The [Executor] to use for the asynchronous operations.
 * @return A [ListenableFuture] representing when the operation has been completed.
 */
@JvmOverloads
internal fun List<DeferrableSurface>.simulateCaptureFrameAsync(
    executor: Executor = Dispatchers.Default.asExecutor()
): ListenableFuture<Void> {
    val scope = CoroutineScope(SupervisorJob() + executor.asCoroutineDispatcher())
    return (scope.async { simulateCaptureFrame() } as Job).asListenableFuture()
}

/**
 * Simulates a capture frame being drawn on the provided surfaces.
 *
 * This method uses the provided [Executor] for the asynchronous operations.
 *
 * @param executor The [Executor] to use for the asynchronous operations.
 * @return A [ListenableFuture] representing when the operation has been completed.
 */
@JvmOverloads
internal fun DeferrableSurface.simulateCaptureFrameAsync(
    executor: Executor = Dispatchers.Default.asExecutor()
): ListenableFuture<Void> {
    val scope = CoroutineScope(SupervisorJob() + executor.asCoroutineDispatcher())
    return (scope.async { simulateCaptureFrame() } as Job).asListenableFuture()
}
