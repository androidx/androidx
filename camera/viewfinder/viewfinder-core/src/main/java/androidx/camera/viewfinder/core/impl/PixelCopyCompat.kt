/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.viewfinder.core.impl

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import android.view.PixelCopy.ERROR_DESTINATION_INVALID
import android.view.PixelCopy.ERROR_SOURCE_INVALID
import android.view.PixelCopy.ERROR_SOURCE_NO_DATA
import android.view.PixelCopy.ERROR_TIMEOUT
import android.view.PixelCopy.ERROR_UNKNOWN
import android.view.PixelCopy.SUCCESS
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.camera.viewfinder.core.impl.PixelCopyCompat.PixelCopyApi24Impl.KEEP_ALIVE_MILLIS
import androidx.core.os.HandlerCompat
import androidx.core.util.Consumer
import androidx.tracing.Trace
import androidx.tracing.trace
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlinx.atomicfu.atomic

/** Compat class for [PixelCopy] to avoid [VerifyError] */
sealed interface PixelCopyCompat {

    fun requestImpl(
        source: Surface,
        dest: Bitmap,
        executor: Executor,
        listener: Consumer<@CopyResultStatus Int>,
    )

    companion object {
        /**
         * Requests that the contents of the source [Surface] be copied into the destination
         * [Bitmap]. The copy is performed synchronously, and the result of the copy is returned.
         *
         * @param source The source [Surface].
         * @param dest The destination [Bitmap].
         * @param timeoutMs The maximum time to wait for the copy to complete, in milliseconds.
         */
        @JvmStatic
        @JvmOverloads
        fun requestSync(
            source: Surface,
            dest: Bitmap,
            timeoutMs: Long = -1,
        ): @CopyResultStatus Int =
            trace("PixelCopyCompat.requestSync") {
                val result = atomic(ERROR_TIMEOUT)
                val semaphore = Semaphore(0)
                impl.requestImpl(source, dest, Runnable::run) {
                    result.value = it
                    semaphore.release()
                }

                if (timeoutMs >= 0) {
                    semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)
                } else {
                    semaphore.acquire()
                }

                result.value
            }

        /**
         * Requests that the contents of the source [Surface] be copied into the destination
         * [Bitmap]. The copy is performed asynchronously, and the result of the copy is provided to
         * the given listener.
         *
         * @param source The source [Surface].
         * @param dest The destination [Bitmap].
         * @param listener The listener to receive the copy result.
         * @param executor The executor to run the listener on.
         */
        @JvmStatic
        fun request(
            source: Surface,
            dest: Bitmap,
            executor: Executor,
            listener: Consumer<@CopyResultStatus Int>,
        ) {
            impl.requestImpl(source, dest, executor, listener)
        }

        /** The[PixelCopyCompat] instance for the current API level. */
        private val impl: PixelCopyCompat by lazy {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                PixelCopyApi34Impl
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PixelCopyApi24Impl
            } else {
                PixelCopyStub
            }
        }
    }

    /**
     * Stub implementation of [PixelCopyCompat] for APIs that support the handler version of
     * [PixelCopy].
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private object PixelCopyApi24Impl : PixelCopyCompat {
        const val KEEP_ALIVE_MILLIS = 500L

        private val lock = Any()
        @GuardedBy("lock") private var _backgroundHandler: RefCounted<Handler>? = null

        /**
         * Manages a handler thread that can be kept alive for a minimum time of [KEEP_ALIVE_MILLIS]
         *
         * Any `action` that uses the provided handler should call `onComplete` when finished with
         * the handler.
         *
         * This ensures we aren't generating a new thread for each PixelCopy request if requests are
         * happening in quick succession.
         */
        private fun withHandlerScope(action: (handler: Handler, onComplete: () -> Unit) -> Unit) {
            val (handler, refCountedHandler) =
                synchronized(lock) {
                    val refCounted = _backgroundHandler
                    refCounted?.acquire()?.let { Pair(it, refCounted) }
                        ?: run {
                            // backgroundHandler is uninitialized or previous one has been killed.
                            // Create a new backgroundHandler.
                            val bgThread =
                                HandlerThread("pixelCopyRequest Thread").apply { start() }
                            val handler = HandlerCompat.createAsync(bgThread.getLooper())
                            val newRefCounted =
                                RefCounted<Handler>(debugRefCounts = false) { bgThread.quit() }
                                    .apply { initialize(handler) }
                            _backgroundHandler = newRefCounted
                            Pair(handler, newRefCounted)
                        }
                }

            // Post a keepalive for the thread. This ensures the handler will stay valid for
            // at least `KEEP_ALIVE_MILLIS` so the thread can be reused for PixelCopy requests
            // that happen in quick succession.
            refCountedHandler.acquire()
            if (!handler.postDelayed(Runnable { refCountedHandler.release() }, KEEP_ALIVE_MILLIS)) {
                throw AssertionError("Handler thread killed unexpectedly.")
            }

            action(handler, refCountedHandler::release)
        }

        override fun requestImpl(
            source: Surface,
            dest: Bitmap,
            executor: Executor,
            listener: Consumer<@CopyResultStatus Int>,
        ) {
            Trace.beginAsyncSection("PixelCopyApi24Impl.request", dest.hashCode())
            withHandlerScope { handler, onComplete ->
                PixelCopy.request(
                    source,
                    dest,
                    PixelCopy.OnPixelCopyFinishedListener {
                        Trace.endAsyncSection("PixelCopyApi24Impl.request", dest.hashCode())
                        try {
                            executor.execute { listener.accept(it) }
                        } finally {
                            onComplete()
                        }
                    },
                    handler,
                )
            }
        }
    }

    /** API 34+ implementation of [PixelCopyCompat]. */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private object PixelCopyApi34Impl : PixelCopyCompat {
        override fun requestImpl(
            source: Surface,
            dest: Bitmap,
            executor: Executor,
            listener: Consumer<@CopyResultStatus Int>,
        ) {
            Trace.beginAsyncSection("PixelCopyApi34Impl.request", dest.hashCode())
            val request =
                PixelCopy.Request.Builder.ofSurface(source).setDestinationBitmap(dest).build()
            PixelCopy.request(request, executor) {
                Trace.endAsyncSection("PixelCopyApi34Impl.request", dest.hashCode())
                listener.accept(it.status)
            }
        }
    }

    /** Stub implementation of [PixelCopyCompat] for APIs without [PixelCopy]. */
    private object PixelCopyStub : PixelCopyCompat {
        override fun requestImpl(
            source: Surface,
            dest: Bitmap,
            executor: Executor,
            listener: Consumer<@CopyResultStatus Int>,
        ) {
            executor.execute { listener.accept(ERROR_UNKNOWN) }
        }
    }

    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
    @IntDef(
        value =
            [
                SUCCESS,
                ERROR_UNKNOWN,
                ERROR_TIMEOUT,
                ERROR_SOURCE_NO_DATA,
                ERROR_SOURCE_INVALID,
                ERROR_DESTINATION_INVALID,
            ]
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class CopyResultStatus
}
