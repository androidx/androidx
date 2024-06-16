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

package androidx.camera.viewfinder.impl.surface

import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.camera.impl.utils.Logger
import androidx.camera.impl.utils.futures.Futures.nonCancellationPropagating
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture

/** A class for creating and tracking use of a [Surface] in an asynchronous manner. */
abstract class DeferredSurface : AutoCloseable {
    private val lock = Any()
    private val terminationFuture: ListenableFuture<Void?>

    @GuardedBy("mLock") private var closed = false

    @GuardedBy("mLock")
    private var terminationCompleterInternal: CallbackToFutureAdapter.Completer<Void?>? = null

    init {
        terminationFuture =
            CallbackToFutureAdapter.getFuture {
                synchronized(lock) { terminationCompleterInternal = it }
                "ViewfinderSurface-termination(" + this@DeferredSurface + ")"
            }
    }

    fun getSurfaceAsync(): ListenableFuture<Surface> {
        return provideSurfaceAsync()
    }

    fun getTerminationFutureAsync(): ListenableFuture<Void?> {
        return nonCancellationPropagating(terminationFuture)
    }

    /**
     * Close the surface.
     *
     * After closing, the underlying surface resources can be safely released by [SurfaceView] or
     * [TextureView] implementation.
     */
    override fun close() {
        var terminationCompleter: CallbackToFutureAdapter.Completer<Void?>? = null
        synchronized(lock) {
            if (!closed) {
                closed = true
                terminationCompleter = terminationCompleterInternal
                terminationCompleterInternal = null
                Logger.d(TAG, "surface closed,  closed=true $this")
            }
        }
        if (terminationCompleter != null) {
            terminationCompleter!!.set(null)
        }
    }

    protected abstract fun provideSurfaceAsync(): ListenableFuture<Surface>

    /**
     * The exception that is returned by the ListenableFuture of [getSurfaceAsync] if the deferrable
     * surface is unable to produce a [Surface].
     */
    class SurfaceUnavailableException(message: String) : Exception(message)

    companion object {
        private const val TAG = "DeferredSurface"
    }
}
