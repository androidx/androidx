/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.effects.playservices.lowlight

import android.content.Context
import android.os.Looper
import android.util.Size
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
import androidx.camera.core.LowLightBoostState
import androidx.camera.core.impl.utils.ContextUtil
import androidx.camera.core.resolutionselector.ResolutionFilter
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.concurrent.futures.await
import androidx.core.os.HandlerCompat
import androidx.core.util.Predicate
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * A [CameraEffect] that applies Google Play Services-based Low Light Boost to camera frames. It
 * brightens camera frames and improves their quality in dark environments.
 *
 * This effect serves as an alternative for devices that do not support the hardware-level Low Light
 * Boost Auto Exposure (AE) mode (see [CameraInfo.isLowLightBoostSupported]). It is only supported
 * on devices running Android 11 (API level 30) or higher. Apps can check the availability of this
 * effect on a device and camera via [isSupported] or [isSupportedAsync], and if supported, apply it
 * to the camera pipeline.
 *
 * The effect can be created and bound on any API level. On an unsupported device, including any
 * device running below Android 11, the effect always passes camera frames through without
 * enhancement and, when bound with [isLowLightBoostEnabled] set to `true`, reports an
 * [UnsupportedOperationException] via [error] with [STATUS_OFF].
 *
 * **State:** Apps can read [isLowLightBoostEnabled], [status], and [error], e.g. to show a Low
 * Light Boost button or status indicator, and get notified of changes via [addListener] or
 * [observe].
 *
 * **Resolution constraints:**
 * - Low Light Boost supports camera stream resolutions at most 2560 pixels wide or high and at most
 *   4,194,304 pixels in total (e.g. 2560x1440 or 2048x2048).
 * - Within each use case's allowed resolutions, CameraX prefers the ones this effect supports, so
 *   applying it may change the selected resolutions. For example, a `VideoCapture` whose
 *   `QualitySelector` allows both UHD and FHD records in FHD.
 * - If a use case only allows unsupported resolutions (for example, a `QualitySelector` that only
 *   allows UHD, or a strict [ResolutionSelector] without a lower-resolution fallback), or its
 *   [ResolutionFilter] explicitly places an unsupported resolution first, CameraX honors the
 *   requested resolution. This effect then passes camera frames through without enhancement and
 *   reports an [IllegalArgumentException] via [error].
 *
 * Note: Google Play Services allows only one active Low Light Boost session at a time, so this
 * effect can't be bound to more than one camera at the same time, e.g. with concurrent cameras.
 *
 * @see CameraInfo.isLowLightBoostSupported
 * @see CameraControl.enableLowLightBoostAsync
 * @see LowLightBoostState
 * @see CameraEffect
 */
// TODO: b/448524973 - Remove @RestrictTo when making this API public in next alpha.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LowLightBoostEffect
internal constructor(
    @CameraEffect.Targets targets: Int,
    processor: PlayServicesLowLightBoostSurfaceProcessor,
) :
    CameraEffect(
        validateTargets(targets),
        // The processor callbacks run on its GL thread, like all of its session and OpenGL work.
        processor.glExecutor,
        processor,
        // CameraX only sends a ProcessingException to this error listener, which the processor
        // never throws. Errors are reported via [error] while frames are passed through.
        {},
    ) {

    /**
     * Creates a [LowLightBoostEffect].
     *
     * @param context The context used to connect to Google Play Services.
     * @param targets The target use cases to which this effect should be applied. Supported bitmask
     *   combinations are:
     *     - [CameraEffect.PREVIEW]
     *     - [CameraEffect.PREVIEW] | [CameraEffect.VIDEO_CAPTURE]
     *     - [CameraEffect.PREVIEW] | [CameraEffect.VIDEO_CAPTURE] | [CameraEffect.IMAGE_CAPTURE]
     *
     *   With [CameraEffect.IMAGE_CAPTURE], captured images are only enhanced when `ImageCapture` is
     *   bound together with `Preview` or `VideoCapture`.
     *
     * @throws IllegalArgumentException if the provided targets combination is not supported.
     */
    public constructor(
        context: Context,
        @CameraEffect.Targets targets: Int,
    ) : this(
        validateTargets(targets),
        PlayServicesLowLightBoostSurfaceProcessor(
            ContextUtil.getPersistentApplicationContext(context)
        ),
    )

    private val processor: PlayServicesLowLightBoostSurfaceProcessor
        get() = surfaceProcessor as PlayServicesLowLightBoostSurfaceProcessor

    /**
     * Runs [observe] on the main looper without an extra dispatch when already on it, using async
     * messages so callbacks aren't delayed by window synchronization barriers.
     */
    private val mainImmediateDispatcher =
        HandlerCompat.createAsync(Looper.getMainLooper()).asCoroutineDispatcher().immediate

    /**
     * Runtime status of the effect: [STATUS_OFF], [STATUS_INACTIVE], [STATUS_ACTIVE], or
     * [STATUS_PENDING_INSTALLATION]. Initial value is [STATUS_OFF].
     *
     * On failure, [status] is [STATUS_OFF] and [error] is non-null while frames pass through
     * without enhancement. Updated atomically with [error] on the main thread before [Listener] and
     * [observe] callbacks run; read both on the main thread for a consistent pair.
     */
    @get:Status
    public val status: Int
        get() = processor.committedSnapshot.status

    /**
     * Latest [Exception] preventing Low Light Boost, or `null` if no error is active.
     *
     * Reported exceptions use three standard types wrapping internal Google Play Services causes:
     * - [UnsupportedOperationException] (**permanent**): Low Light Boost cannot be used on this
     *   Android version (below API level 30), device, or camera. Check [isSupported] or
     *   [isSupportedAsync] beforehand to hide or disable Low Light Boost controls on unsupported
     *   devices and cameras.
     * - [IllegalArgumentException] (**permanent for the current stream configuration**): Bound
     *   stream resolution (`> 2560px` on a side or `> 4,194,304` total pixels) or dataspace is
     *   unsupported.
     * - [IllegalStateException] (**runtime or session failure**): Module download or installation
     *   failed or was cancelled, a transient Play Services or session creation failure exhausted
     *   retries, or the active session disconnected. The effect retries transient failures
     *   automatically, starts a fresh attempt when a paused stream resumes or rebinds, and retries
     *   if [isLowLightBoostEnabled] is toggled `false` then `true`. If the OpenGL pipeline or the
     *   Google Play Services client cannot be set up, or Google Play Services rejects the session
     *   as non-retryable, the error persists for the current stream and only rebinding retries.
     *
     * When non-null, [status] is always [STATUS_OFF] and frames pass through without enhancement.
     * Cleared to `null` when Low Light Boost recovers or waits for module installation, after
     * unbinding, when another effect takes over, or when [isLowLightBoostEnabled] is set to
     * `false`. Updated atomically with [status] on the main thread before [Listener] and [observe]
     * callbacks run; read both on the main thread for a consistent pair.
     */
    public val error: Exception?
        get() = processor.committedSnapshot.error

    /**
     * Whether Low Light Boost enhancement is enabled by the caller.
     *
     * Defaults to `true` and persists across camera bindings.
     *
     * The new value is visible immediately on all threads, while [status], [error], and frame
     * processing update asynchronously once the OpenGL thread handles the request.
     *
     * Setting `false` passes frames through without enhancement and clears any active [error].
     * Setting the current value is a no-op. Toggling `false` then `true`, even synchronously
     * back-to-back, notifies both changes in order and clears any [error]. While the effect is
     * bound, it then retries a failed session or reports again an error that persists for the
     * current stream.
     */
    public var isLowLightBoostEnabled: Boolean
        get() = processor.isLowLightBoostEnabled
        set(value) {
            processor.isLowLightBoostEnabled = value
        }

    /**
     * Registers [listener] to receive future changes on the main thread ([Looper.getMainLooper]).
     * Status and error callbacks run after [status] and [error] update.
     *
     * Does not emit the current state on registration. To start from it without missing a
     * transition or setup failure, call [addListener] and read [isLowLightBoostEnabled], [status],
     * and [error] on the main thread, or use [observe]. Re-registering an active listener is a
     * no-op (call [removeListener] first to change its executor). Can be called from any thread.
     *
     * @param listener listener to register
     */
    public fun addListener(listener: Listener) {
        processor.addListener(executor = null, listener)
    }

    /**
     * Registers [listener] to receive future changes sequentially on [executor]. Status and error
     * callbacks run after [status] and [error] update.
     *
     * Does not emit the current state on registration. Track the values passed to [listener] rather
     * than reading [isLowLightBoostEnabled], [status], or [error], which may already reflect a
     * later transition when [executor] runs (or use [addListener] on the main thread or [observe]).
     * Re-registering an active listener is a no-op (call [removeListener] first to change its
     * executor). Can be called from any thread.
     *
     * @param executor executor on which [listener] is invoked
     * @param listener listener to register
     */
    public fun addListener(executor: Executor, listener: Listener) {
        processor.addListener(executor, listener)
    }

    /**
     * Unregisters [listener] so it receives no further callbacks, including pending tasks on a
     * custom executor. Unregistered instances are ignored. Can be called from any thread.
     *
     * @param listener listener to unregister
     */
    public fun removeListener(listener: Listener) {
        processor.removeListener(listener)
    }

    /**
     * Suspends on the main thread, invoking [onChanged] initially and after each enabled or state
     * transition.
     *
     * Registers before the initial [onChanged] call (preventing missed transitions) and unregisters
     * on cancellation or failure. Because [onChanged] reads the current [isLowLightBoostEnabled],
     * [status], and [error] values on the main thread, rapid updates such as back-to-back
     * [isLowLightBoostEnabled] toggles can observe the latest value on both wakeups; use [Listener]
     * to receive every individual transition argument. Suitable for Compose `LaunchedEffect`. Never
     * returns normally.
     *
     * @param onChanged invoked with this [LowLightBoostEffect] as receiver for initial sync and
     *   each enabled or state transition
     */
    public suspend fun observe(onChanged: LowLightBoostEffect.() -> Unit): Nothing =
        withContext(mainImmediateDispatcher) {
            var observer: Runnable? = null
            try {
                suspendCancellableCoroutine<Nothing> { continuation ->
                    // Set once the observation ends, so onChanged isn't called anymore.
                    val isDone = AtomicBoolean(false)
                    val notifyChanged = Runnable {
                        if (isDone.get()) {
                            return@Runnable
                        }
                        try {
                            this@LowLightBoostEffect.onChanged()
                        } catch (t: Throwable) {
                            if (isDone.compareAndSet(false, true)) {
                                processor.removeTransitionObserver(checkNotNull(observer))
                                continuation.resumeWithException(t)
                            }
                        }
                    }
                    observer = notifyChanged
                    // Register before invokeOnCancellation and the initial call. Both run on the
                    // main thread (which also commits transitions), so no transition is missed and
                    // immediate or off-thread cancellation unregisters synchronously.
                    processor.addTransitionObserver(notifyChanged)
                    continuation.invokeOnCancellation {
                        isDone.set(true)
                        processor.removeTransitionObserver(notifyChanged)
                    }
                    notifyChanged.run()
                }
            } finally {
                observer?.let { processor.removeTransitionObserver(it) }
            }
        }

    /**
     * Returns a stateless [Predicate] that checks whether a given resolution is supported by Low
     * Light Boost.
     *
     * Low Light Boost requires resolutions at most 2560 pixels wide or high and at most 4,194,304
     * pixels in total (e.g. 2560x1440 or 2048x2048).
     *
     * @return a stateless [Predicate] that returns true if the resolution is supported, false
     *   otherwise
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun getResolutionValidator(): Predicate<Size> = RESOLUTION_VALIDATOR

    /**
     * Listener for [LowLightBoostEffect] enabled-setting, status, and error changes. All methods
     * default to no-ops.
     *
     * [status] and [error] both hold the new values on the main thread before [onStatusChanged] or
     * [onErrorChanged] runs, so listeners added via [addListener] without an executor can read them
     * together in either callback. When one transition changes both, [onStatusChanged] runs before
     * [onErrorChanged] in the same task. Because a failure reports [STATUS_OFF] immediately before
     * the error, trigger error-specific side effects from [onErrorChanged] rather than from
     * [STATUS_OFF] alone.
     */
    public interface Listener {

        /**
         * Called when [LowLightBoostEffect.isLowLightBoostEnabled] changes.
         *
         * @param enabled new [LowLightBoostEffect.isLowLightBoostEnabled] value
         */
        public fun onLowLightBoostEnabledChanged(enabled: Boolean) {}

        /**
         * Called when [LowLightBoostEffect.status] changes.
         *
         * @param status new [LowLightBoostEffect.status]
         */
        public fun onStatusChanged(@Status status: Int) {}

        /**
         * Called when [LowLightBoostEffect.error] changes, including clearing to `null`.
         *
         * @param error new [LowLightBoostEffect.error], or `null` if no error is active
         */
        public fun onErrorChanged(error: Exception?) {}
    }

    /** Valid status constants for [LowLightBoostEffect.status]. */
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Target(
        AnnotationTarget.TYPE,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.FIELD,
    )
    @IntDef(STATUS_OFF, STATUS_INACTIVE, STATUS_ACTIVE, STATUS_PENDING_INSTALLATION)
    public annotation class Status

    public companion object {
        /**
         * Effect is disabled (`!isLowLightBoostEnabled`), unbound or still initializing a session
         * (`isLowLightBoostEnabled` and `error == null`), or failed (`error != null`). Frames pass
         * through without enhancement. Aligns numerically with [LowLightBoostState.OFF] (`-1`).
         */
        public const val STATUS_OFF: Int = -1

        /**
         * Effect is enabled, but the scene is bright enough that no noticeable boost is applied.
         * Aligns with [LowLightBoostState.INACTIVE] (`0`).
         */
        public const val STATUS_INACTIVE: Int = 0

        /**
         * Effect is enabled and actively brightening dark frames. Aligns with
         * [LowLightBoostState.ACTIVE] (`1`).
         */
        public const val STATUS_ACTIVE: Int = 1

        /**
         * Required Google Play Services module is downloading in the background; frames pass
         * through without enhancement until it is installed.
         */
        public const val STATUS_PENDING_INSTALLATION: Int = 2

        private val RESOLUTION_VALIDATOR =
            Predicate<Size> { size ->
                LowLightBoostSupport.isResolutionSupported(size)
            }

        private val VALID_TARGET_COMBINATIONS =
            setOf(
                CameraEffect.PREVIEW,
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE,
            )

        // When more than one bound use case matches the targets, CameraX applies the effect on
        // StreamSharing, whose children are those use cases. With IMAGE_CAPTURE, ImageCapture then
        // captures the boosted shared stream. PREVIEW | IMAGE_CAPTURE without VIDEO_CAPTURE isn't
        // a supported combination.
        private fun validateTargets(targets: Int): Int {
            require(targets in VALID_TARGET_COMBINATIONS) {
                "Unsupported targets combination: $targets. Supported combinations are " +
                    "PREVIEW, PREVIEW | VIDEO_CAPTURE, or PREVIEW | VIDEO_CAPTURE | IMAGE_CAPTURE."
            }
            return targets
        }

        /**
         * Asynchronously checks if the Google Play Services-based Low Light Boost effect is
         * supported and available.
         *
         * This checks that the device is running Android 11 (API level 30) or higher and that
         * Google Play Services reports the device and the specified camera as supported. The
         * required Google Play Services module may still need to be downloaded after the effect is
         * bound; see [STATUS_PENDING_INSTALLATION]. Results reported by Play Services are cached in
         * memory so subsequent queries for the same camera return immediately. On devices running
         * below Android 11, the returned future resolves to `false`.
         *
         * @param context The context to check Google Play Services availability.
         * @param cameraInfo The [CameraInfo] of the camera to check.
         * @return A [ListenableFuture] that resolves to `true` if the Google Play Services-based
         *   Low Light Boost effect can be used, or `false` otherwise. `false` is also returned if
         *   Google Play Services can't be reached in time; this result isn't cached, so the check
         *   can be retried.
         * @see isSupported
         */
        @JvmStatic
        public fun isSupportedAsync(
            context: Context,
            cameraInfo: CameraInfo,
        ): ListenableFuture<Boolean> {
            return PlayServicesLowLightBoostAvailability.isSupportedAsync(context, cameraInfo)
        }

        /**
         * Checks if the Google Play Services-based Low Light Boost effect is supported and
         * available.
         *
         * This checks that the device is running Android 11 (API level 30) or higher and that
         * Google Play Services reports the device and the specified camera as supported. The
         * required Google Play Services module may still need to be downloaded after the effect is
         * bound; see [STATUS_PENDING_INSTALLATION]. Results reported by Play Services are cached in
         * memory so subsequent queries for the same camera return immediately. On devices running
         * below Android 11, it returns `false`.
         *
         * @param context The context to check Google Play Services availability.
         * @param cameraInfo The [CameraInfo] of the camera to check.
         * @return `true` if the Google Play Services-based Low Light Boost effect can be used, or
         *   `false` otherwise. `false` is also returned if Google Play Services can't be reached in
         *   time; this result isn't cached, so the check can be retried.
         * @see isSupportedAsync
         */
        @JvmStatic
        public suspend fun isSupported(context: Context, cameraInfo: CameraInfo): Boolean {
            return PlayServicesLowLightBoostAvailability.isSupportedAsync(context, cameraInfo)
                .await()
        }
    }
}
