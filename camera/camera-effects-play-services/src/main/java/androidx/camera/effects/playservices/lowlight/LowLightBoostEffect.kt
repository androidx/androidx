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
import android.util.Size
import androidx.annotation.RestrictTo
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.utils.ContextUtil
import androidx.camera.core.resolutionselector.ResolutionFilter
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.concurrent.futures.await
import androidx.core.util.Predicate
import androidx.lifecycle.LiveData
import com.google.common.util.concurrent.ListenableFuture

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
 * enhancement and reports [LowLightBoostEffectState.ERROR] via [state] once bound.
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
 *   reports [LowLightBoostEffectState.ERROR] via [state].
 *
 * Note: Google Play Services allows only one active Low Light Boost session at a time, so this
 * effect can't be bound to more than one camera at the same time, e.g. with concurrent cameras.
 *
 * @see CameraInfo.isLowLightBoostSupported
 * @see CameraControl.enableLowLightBoostAsync
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
        // never throws. Errors are reported via [state] (LowLightBoostEffectState.ERROR) while
        // frames are passed through.
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
     * The [LiveData] of the effect's runtime state.
     *
     * The initial state of this LiveData is [LowLightBoostEffectState.OFF]. The state can be
     * observed to update UI elements (e.g., a Low Light Boost indicator). See
     * [LowLightBoostEffectState] for the states, e.g. [LowLightBoostEffectState.ERROR] when Low
     * Light Boost can't be applied and frames are passed through without enhancement.
     */
    public val state: LiveData<LowLightBoostEffectState>
        get() = processor.stateLiveData

    /**
     * Requests to enable or disable the Low Light Boost effect.
     *
     * A newly created effect is enabled. The setting persists across bindings. The request is
     * asynchronous; [state] reflects the change. On an unsupported device running Android 11 (API
     * level 30) or higher, disabling the effect reports [LowLightBoostEffectState.OFF], and
     * enabling it reports [LowLightBoostEffectState.ERROR] again. On devices running below Android
     * 11, this method does nothing.
     *
     * @param enabled `true` to enable the enhancement, `false` to pass frames through without
     *   enhancement.
     */
    public fun setLowLightBoostEnabled(enabled: Boolean) {
        processor.setLowLightBoostEnabled(enabled)
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

    public companion object {
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
         * bound; see [LowLightBoostEffectState.PENDING_INSTALLATION]. Results reported by Play
         * Services are cached in memory so subsequent queries for the same camera return
         * immediately. On devices running below Android 11, the returned future resolves to
         * `false`.
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
         * bound; see [LowLightBoostEffectState.PENDING_INSTALLATION]. Results reported by Play
         * Services are cached in memory so subsequent queries for the same camera return
         * immediately. On devices running below Android 11, it returns `false`.
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
