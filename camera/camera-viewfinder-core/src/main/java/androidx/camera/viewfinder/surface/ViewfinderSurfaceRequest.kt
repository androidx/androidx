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

package androidx.camera.viewfinder.surface

import android.annotation.SuppressLint
import android.util.Size
import android.view.Surface
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.camera.impl.utils.Logger
import androidx.camera.impl.utils.executor.ViewfinderExecutors
import androidx.camera.impl.utils.futures.FutureCallback
import androidx.camera.impl.utils.futures.Futures
import androidx.camera.viewfinder.impl.surface.DeferredSurface
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.await
import androidx.core.util.Consumer
import androidx.core.util.Preconditions
import com.google.auto.value.AutoValue
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

/**
 * The request to get a [Surface] to display viewfinder input.
 *
 * This request contains requirements for the surface resolution and viewfinder input and output
 * information.
 *
 * Calling [ViewfinderSurfaceRequest.markSurfaceSafeToRelease] will notify the surface provider that
 * the surface is not needed and related resources can be released.
 *
 * Creates a new surface request with surface resolution, viewfinder input and output information.
 *
 * @param resolution The requested surface resolution.
 * @param outputMirrorMode The viewfinder output mirror mode.
 * @param sourceOrientation The viewfinder source orientation.
 * @param implementationMode The {@link ImplementationMode} to apply to the viewfinder.
 */
class ViewfinderSurfaceRequest
internal constructor(
    val resolution: Size,
    @OutputMirrorMode val outputMirrorMode: Int,
    @SourceOrientationDegreesValue val sourceOrientation: Int,
    val implementationMode: ImplementationMode?
) {
    private val mInternalDeferredSurface: DeferredSurface
    private val cancellationCompleter: CallbackToFutureAdapter.Completer<Void?>
    private val sessionStatusFuture: ListenableFuture<Void?>
    private val surfaceCompleter: CallbackToFutureAdapter.Completer<Surface>

    private val surfaceFutureAsync: ListenableFuture<Surface>

    init {
        // To ensure concurrency and ordering, operations are chained. Completion can only be
        // triggered externally by the top-level completer (mSurfaceCompleter). The other future
        // completers are only completed by callbacks set up within the constructor of this class
        // to ensure correct ordering of events.

        // Cancellation listener must be called last to ensure the result can be retrieved from
        // the session listener.
        val surfaceRequestString =
            "SurfaceRequest[size: " + resolution + ", id: " + this.hashCode() + "]"
        val cancellationCompleterRef =
            AtomicReference<CallbackToFutureAdapter.Completer<Void?>?>(null)
        val requestCancellationFuture =
            CallbackToFutureAdapter.getFuture {
                cancellationCompleterRef.set(it)
                "$surfaceRequestString-cancellation"
            }
        val requestCancellationCompleter =
            Preconditions.checkNotNull(cancellationCompleterRef.get())
        cancellationCompleter = requestCancellationCompleter

        // Surface session status future completes and is responsible for finishing the
        // cancellation listener.
        val sessionStatusCompleterRef =
            AtomicReference<CallbackToFutureAdapter.Completer<Void?>?>(null)
        sessionStatusFuture =
            CallbackToFutureAdapter.getFuture<Void?> {
                sessionStatusCompleterRef.set(it)
                "$surfaceRequestString-status"
            }
        Futures.addCallback<Void?>(
            sessionStatusFuture,
            object : FutureCallback<Void?> {
                override fun onSuccess(result: Void?) {
                    // Cancellation didn't occur, so complete the cancellation future. There
                    // shouldn't ever be any standard listeners on this future, so nothing should be
                    // invoked.
                    Preconditions.checkState(requestCancellationCompleter.set(null))
                }

                override fun onFailure(t: Throwable) {
                    if (t is RequestCancelledException) {
                        // Cancellation occurred. Notify listeners.
                        Preconditions.checkState(requestCancellationFuture.cancel(false))
                    } else {
                        // Cancellation didn't occur, complete the future so cancellation listeners
                        // are not invoked.
                        Preconditions.checkState(requestCancellationCompleter.set(null))
                    }
                }
            },
            ViewfinderExecutors.directExecutor()
        )

        // Create the surface future/completer. This will be used to complete the rest of the
        // future chain and can be set externally via SurfaceRequest methods.
        val sessionStatusCompleter = Preconditions.checkNotNull(sessionStatusCompleterRef.get())
        val surfaceCompleterRef = AtomicReference<CallbackToFutureAdapter.Completer<Surface>?>(null)
        surfaceFutureAsync =
            CallbackToFutureAdapter.getFuture {
                surfaceCompleterRef.set(it)
                "$surfaceRequestString-Surface"
            }
        surfaceCompleter = Preconditions.checkNotNull(surfaceCompleterRef.get())

        // Create the viewfinder surface which will be used for communicating when the
        // producer and consumer are done using the surface. Note this anonymous inner class holds
        // an implicit reference to the ViewfinderSurfaceRequest. This is by design, and ensures the
        // ViewfinderSurfaceRequest and all contained future completers will not be garbage
        // collected as long as the ViewfinderSurface is referenced externally (via
        // getViewfinderSurface()).
        mInternalDeferredSurface =
            object : DeferredSurface() {
                override fun provideSurfaceAsync(): ListenableFuture<Surface> {
                    Logger.d(TAG, "mInternalViewfinderSurface + $this provideSurface")
                    return surfaceFutureAsync
                }
            }
        val terminationFuture: ListenableFuture<Void?> =
            mInternalDeferredSurface.getTerminationFutureAsync()

        // Propagate surface completion to the session future.
        Futures.addCallback<Surface>(
            surfaceFutureAsync,
            object : FutureCallback<Surface?> {
                override fun onSuccess(result: Surface?) {
                    // On successful setting of a surface, defer completion of the session future to
                    // the ViewfinderSurface termination future. Once that future completes, then it
                    // is safe to release the Surface and associated resources.
                    Futures.propagate(terminationFuture, sessionStatusCompleter)
                }

                override fun onFailure(t: Throwable) {
                    // Translate cancellation into a SurfaceRequestCancelledException. Other
                    // exceptions mean either the request was completed via willNotProvideSurface()
                    // or a
                    // programming error occurred. In either case, the user will never see the
                    // session future (an immediate future will be returned instead), so complete
                    // the
                    // future so cancellation listeners are never called.
                    if (t is CancellationException) {
                        Preconditions.checkState(
                            sessionStatusCompleter.setException(
                                RequestCancelledException("$surfaceRequestString cancelled.", t)
                            )
                        )
                    } else {
                        sessionStatusCompleter.set(null)
                    }
                }
            },
            ViewfinderExecutors.directExecutor()
        )

        // If the viewfinder surface is terminated, there are two cases:
        // 1. The surface has not yet been provided to the producer (or marked as 'will not
        //    complete'). Treat this as if the surface request has been cancelled.
        // 2. The surface was already provided to the producer. In this case the producer is now
        //    finished with the surface, so cancelling the surface future below will be a no-op.
        terminationFuture.addListener(
            {
                Logger.d(
                    TAG,
                    ("mInternalViewfinderSurface + " +
                        mInternalDeferredSurface +
                        " " +
                        "terminateFuture triggered")
                )
                surfaceFutureAsync.cancel(true)
            },
            ViewfinderExecutors.directExecutor()
        )
    }

    /**
     * Closes the viewfinder surface to mark it as safe to release.
     *
     * This method should be called by the user when the requested surface is not needed and related
     * resources can be released.
     */
    fun markSurfaceSafeToRelease() {
        mInternalDeferredSurface.close()
    }

    /**
     * Retrieves the [Surface] provided to the [ViewfinderSurfaceRequest].
     *
     * This can be used to get access to the [Surface] that's provided to the
     * [ViewfinderSurfaceRequest].
     *
     * If the application is shutting down and a [Surface] will never be provided, this will throw a
     * [kotlinx.coroutines.CancellationException].
     *
     * The returned [Surface] must not be used after [markSurfaceSafeToRelease] has been called.
     */
    suspend fun getSurface(): Surface {
        return mInternalDeferredSurface.getSurfaceAsync().await()
    }

    /**
     * Retrieves the [Surface] provided to the [ViewfinderSurfaceRequest].
     *
     * This can be used to get access to the [Surface] that's provided to the
     * [ViewfinderSurfaceRequest].
     *
     * If the application is shutting down and a [Surface] will never be provided, the
     * [ListenableFuture] will fail with a [CancellationException].
     *
     * The returned [Surface] must not be used after [markSurfaceSafeToRelease] has been called.
     */
    fun getSurfaceAsync(): ListenableFuture<Surface> {
        return mInternalDeferredSurface.getSurfaceAsync()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @SuppressLint("PairedRegistration")
    fun addRequestCancellationListener(executor: Executor, listener: Runnable) {
        cancellationCompleter.addCancellationListener(listener, executor)
    }

    /**
     * Completes the request for a [Surface] if it has not already been completed or cancelled.
     *
     * Once the producer no longer needs the provided surface, the `resultListener` will be invoked
     * with a [Result] containing [Result.RESULT_SURFACE_USED_SUCCESSFULLY]. At this point it is
     * safe to release the surface and any underlying resources. Releasing the surface before
     * receiving this signal may cause undesired behavior on lower API levels.
     *
     * If the request is cancelled by the producer before successfully attaching the provided
     * surface to the producer, then the `resultListener` will be invoked with a [Result] containing
     * [Result.RESULT_REQUEST_CANCELLED].
     *
     * If the request was previously completed via [.willNotProvideSurface], then `resultListener`
     * will be invoked with a [Result] containing [Result.RESULT_WILL_NOT_PROVIDE_SURFACE].
     *
     * Upon returning from this method, the surface request is guaranteed to be complete. However,
     * only the `resultListener` provided to the first invocation of this method should be used to
     * track when the provided [Surface] is no longer in use by the producer, as subsequent
     * invocations will always invoke the `resultListener` with a [Result] containing
     * [Result.RESULT_SURFACE_ALREADY_PROVIDED].
     *
     * @param surface The surface which will complete the request.
     * @param executor Executor used to execute the `resultListener`.
     * @param resultListener Listener used to track how the surface is used by the producer in
     *   response to being provided by this method.
     */
    fun provideSurface(surface: Surface, executor: Executor, resultListener: Consumer<Result?>) {
        if (surfaceCompleter.set(surface) || surfaceFutureAsync.isCancelled) {
            // Session will be pending completion (or surface request was cancelled). Return the
            // session future.
            Futures.addCallback(
                sessionStatusFuture,
                object : FutureCallback<Void?> {
                    override fun onSuccess(result: Void?) {
                        resultListener.accept(
                            Result(Result.RESULT_SURFACE_USED_SUCCESSFULLY, surface)
                        )
                    }

                    override fun onFailure(t: Throwable) {
                        Preconditions.checkState(
                            t is RequestCancelledException,
                            ("Producer " +
                                "surface session should only fail with request " +
                                "cancellation. Instead failed due to:\n" +
                                t)
                        )
                        resultListener.accept(Result(Result.RESULT_REQUEST_CANCELLED, surface))
                    }
                },
                executor
            )
        } else {
            // Surface request is already complete
            Preconditions.checkState(surfaceFutureAsync.isDone)
            try {
                surfaceFutureAsync.get()
                // Getting this far means the surface was already provided.
                executor.execute {
                    resultListener.accept(Result(Result.RESULT_SURFACE_ALREADY_PROVIDED, surface))
                }
            } catch (e: InterruptedException) {
                executor.execute {
                    resultListener.accept(Result(Result.RESULT_WILL_NOT_PROVIDE_SURFACE, surface))
                }
            } catch (e: ExecutionException) {
                executor.execute {
                    resultListener.accept(Result(Result.RESULT_WILL_NOT_PROVIDE_SURFACE, surface))
                }
            }
        }
    }

    /**
     * Signals that the request will never be fulfilled.
     *
     * This may be called in the case where the application may be shutting down and a surface will
     * never be produced to fulfill the request.
     *
     * This will be called by the producer as soon as it is known that the request will not be
     * fulfilled. Failure to complete the SurfaceRequest via `willNotProvideSurface()` or
     * [.provideSurface] may cause long delays in shutting down the producer.
     *
     * Upon returning from this method, the request is guaranteed to be complete, regardless of the
     * return value. If the request was previously successfully completed by [.provideSurface],
     * invoking this method will return `false`, and will have no effect on how the surface is used
     * by the producer.
     *
     * @return `true` if this call to `willNotProvideSurface()` successfully completes the request,
     *   i.e., the request has not already been completed via [.provideSurface] or by a previous
     *   call to `willNotProvideSurface()` and has not already been cancelled by the producer.
     */
    fun willNotProvideSurface(): Boolean {
        return surfaceCompleter.setException(
            DeferredSurface.SurfaceUnavailableException(("Surface request will not complete."))
        )
    }

    /** Builder for [ViewfinderSurfaceRequest]. */
    class Builder {
        private val resolution: Size

        @OutputMirrorMode private var outputMirrorMode = MIRROR_MODE_NONE

        @SourceOrientationDegreesValue private var sourceOrientation = 0
        private var implementationMode: ImplementationMode? = null

        /**
         * Constructor for [Builder].
         *
         * Creates a builder with viewfinder resolution.
         *
         * @param resolution viewfinder resolution.
         */
        constructor(resolution: Size) {
            this.resolution = resolution
        }

        /**
         * Constructor for [Builder].
         *
         * Creates a builder with other builder instance. The returned builder will be pre-populated
         * with the state of the provided builder.
         *
         * @param builder [Builder] instance.
         */
        constructor(builder: Builder) {
            resolution = builder.resolution
            implementationMode = builder.implementationMode
            outputMirrorMode = builder.outputMirrorMode
            sourceOrientation = builder.sourceOrientation
        }

        /**
         * Constructor for [Builder].
         *
         * Creates a builder with other [ViewfinderSurfaceRequest] instance. The returned builder
         * will be pre-populated with the state of the provided [ViewfinderSurfaceRequest] instance.
         *
         * @param surfaceRequest [ViewfinderSurfaceRequest] instance.
         */
        constructor(surfaceRequest: ViewfinderSurfaceRequest) {
            resolution = surfaceRequest.resolution
            implementationMode = surfaceRequest.implementationMode
            outputMirrorMode = surfaceRequest.outputMirrorMode
            sourceOrientation = surfaceRequest.sourceOrientation
        }

        /**
         * Sets the [ImplementationMode].
         *
         * **Possible values:**
         * * [PERFORMANCE][ImplementationMode.EXTERNAL]
         * * [COMPATIBLE][ImplementationMode.EMBEDDED]
         *
         * @param implementationMode The [ImplementationMode].
         * @return This builder.
         */
        fun setImplementationMode(implementationMode: ImplementationMode?): Builder {
            this.implementationMode = implementationMode
            return this
        }

        /**
         * Sets the output mirror mode.
         *
         * **Possible values:**
         * * [MIRROR_MODE_NONE][MIRROR_MODE_NONE]
         * * [MIRROR_MODE_HORIZONTAL][MIRROR_MODE_HORIZONTAL]
         *
         * If not set, [MIRROR_MODE_NONE] will be used by default.
         *
         * @param outputMirrorMode The viewfinder output mirror mode.
         * @return This builder.
         */
        fun setOutputMirrorMode(@OutputMirrorMode outputMirrorMode: Int): Builder {
            this.outputMirrorMode = outputMirrorMode
            return this
        }

        /**
         * Sets the sensor orientation.
         *
         * **Range of valid values:**<br></br> 0, 90, 180, 270
         *
         * If it is not set, 0 will be used by default.
         *
         * @param sourceOrientation The viewfinder source orientation.
         * @return This builder.
         */
        fun setSourceOrientation(@SourceOrientationDegreesValue sourceOrientation: Int): Builder {
            this.sourceOrientation = sourceOrientation
            return this
        }

        /**
         * Builds the [ViewfinderSurfaceRequest].
         *
         * @return the instance of [ViewfinderSurfaceRequest].
         * @throws IllegalArgumentException
         */
        fun build(): ViewfinderSurfaceRequest {
            if (
                (outputMirrorMode != MIRROR_MODE_NONE) &&
                    (outputMirrorMode != MIRROR_MODE_HORIZONTAL)
            ) {
                throw IllegalArgumentException(
                    ("Output mirror mode : $outputMirrorMode is invalid")
                )
            }
            if (
                (sourceOrientation != 0) &&
                    (sourceOrientation != 90) &&
                    (sourceOrientation != 180) &&
                    (sourceOrientation != 270)
            ) {
                throw IllegalArgumentException(
                    ("Source orientation value: $sourceOrientation is invalid")
                )
            }
            return ViewfinderSurfaceRequest(
                resolution,
                outputMirrorMode,
                sourceOrientation,
                implementationMode
            )
        }
    }

    internal class RequestCancelledException(message: String, cause: Throwable) :
        RuntimeException(message, cause)

    /**
     * Result of providing a surface to a [ViewfinderSurfaceRequest] via [.provideSurface].
     *
     * Can be used to compare to results returned to `resultListener` in [.provideSurface].
     *
     * @param code One of [.RESULT_SURFACE_USED_SUCCESSFULLY], [.RESULT_REQUEST_CANCELLED],
     *   [.RESULT_INVALID_SURFACE], [.RESULT_SURFACE_ALREADY_PROVIDED], or
     *   [.RESULT_WILL_NOT_PROVIDE_SURFACE].
     * @param surface The [Surface] used to complete the [ViewfinderSurfaceRequest].
     */
    @AutoValue
    data class Result(@ResultCode val code: Int, val surface: Surface) {
        /** Possible result codes. */
        @IntDef(
            RESULT_SURFACE_USED_SUCCESSFULLY,
            RESULT_REQUEST_CANCELLED,
            RESULT_INVALID_SURFACE,
            RESULT_SURFACE_ALREADY_PROVIDED,
            RESULT_WILL_NOT_PROVIDE_SURFACE
        )
        @Retention(AnnotationRetention.SOURCE)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        annotation class ResultCode()

        companion object {
            /**
             * Provided surface was successfully used by the producer and eventually detached once
             * no longer needed by the producer.
             *
             * This result denotes that it is safe to release the [Surface] and any underlying
             * resources.
             *
             * For compatibility reasons, the [Surface] object should not be reused by future
             * [SurfaceRequests][ViewfinderSurfaceRequest], and a new surface should be created
             * instead.
             */
            const val RESULT_SURFACE_USED_SUCCESSFULLY = 0

            /**
             * Provided surface was never attached to the producer due to the
             * [ViewfinderSurfaceRequest] being cancelled by the producer.
             *
             * It is safe to release or reuse [Surface], assuming it was not previously attached to
             * a producer via [.provideSurface]. If reusing the surface for a future surface
             * request, it should be verified that the surface still matches the resolution
             * specified by [ViewfinderSurfaceRequest.resolution].
             */
            const val RESULT_REQUEST_CANCELLED = 1

            /**
             * Provided surface could not be used by the producer.
             *
             * This is likely due to the [Surface] being closed prematurely or the resolution of the
             * surface not matching the resolution specified by
             * [ViewfinderSurfaceRequest.resolution].
             */
            const val RESULT_INVALID_SURFACE = 2

            /**
             * Surface was not attached to the producer through this invocation of [.provideSurface]
             * due to the [ViewfinderSurfaceRequest] already being complete with a surface.
             *
             * The [ViewfinderSurfaceRequest] has already been completed by a previous invocation of
             * [.provideSurface].
             *
             * It is safe to release or reuse the [Surface], assuming it was not previously attached
             * to a producer via [.provideSurface].
             */
            const val RESULT_SURFACE_ALREADY_PROVIDED = 3

            /**
             * Surface was not attached to the producer through this invocation of [.provideSurface]
             * due to the [ViewfinderSurfaceRequest] already being marked as "will not provide
             * surface".
             *
             * The [ViewfinderSurfaceRequest] has already been marked as 'will not provide surface'
             * by a previous invocation of [.willNotProvideSurface].
             *
             * It is safe to release or reuse the [Surface], assuming it was not previously attached
             * to a producer via [.provideSurface].
             */
            const val RESULT_WILL_NOT_PROVIDE_SURFACE = 4
        }
    }

    /** Valid integer source orientation degrees values. */
    @IntDef(0, 90, 180, 270)
    @Retention(AnnotationRetention.SOURCE)
    private annotation class SourceOrientationDegreesValue

    /** Valid integer output mirror mode. */
    @IntDef(MIRROR_MODE_NONE, MIRROR_MODE_HORIZONTAL)
    @Retention(AnnotationRetention.SOURCE)
    private annotation class OutputMirrorMode

    companion object {
        private const val TAG = "SurfaceRequest"

        /** No mirror transform needs to be applied to the viewfinder output. */
        const val MIRROR_MODE_NONE = 0

        /**
         * Horizontal mirror transform needs to be applied to the viewfinder output.
         *
         * The mirror transform should be applied in display coordinate.
         */
        const val MIRROR_MODE_HORIZONTAL = 1
    }
}
