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

package androidx.window.embedding

import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.util.Consumer
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The controller to manage overlay [ActivityStack], which is launched by the activityOptions that
 * [setOverlayCreateParams].
 *
 * See linked sample below for how to launch an [android.app.Activity] into an overlay
 * [ActivityStack].
 *
 * Supported operations are:
 * - [setOverlayAttributesCalculator] to update overlay presentation with device or window state and
 *   [OverlayCreateParams.tag].
 *
 * @sample androidx.window.samples.embedding.launchOverlayActivityStackSample
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class OverlayController
@VisibleForTesting
internal constructor(private val backend: EmbeddingBackend) {

    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    internal fun setOverlayCreateParams(
        options: Bundle,
        overlayCreateParams: OverlayCreateParams,
    ): Bundle = backend.setOverlayCreateParams(options, overlayCreateParams)

    /**
     * Sets an overlay calculator function to update overlay presentation with device or window
     * state and [OverlayCreateParams.tag].
     *
     * Overlay calculator function is triggered with following scenarios:
     * - An overlay [ActivityStack] is launched.
     * - The parent task configuration changes. i.e. orientation change, enter/exit multi-window
     *   mode or resize apps in multi-window mode.
     * - Device folding state changes.
     * - Device is attached to an external display and the app is forwarded to that display.
     *
     * If there's no [calculator] set, the overlay presentation will be calculated with the previous
     * set [OverlayAttributes], either from [OverlayCreateParams] to initialize the overlay
     * container, or from the runtime API to update the overlay container's [OverlayAttributes].
     *
     * See the sample linked below for how to use [OverlayAttributes] calculator
     *
     * @param calculator The overlay calculator function to compute [OverlayAttributes] by
     *   [OverlayAttributesCalculatorParams]. It will replace the previously set if it exists.
     * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion] is less
     *   than 6.
     * @sample androidx.window.samples.embedding.overlayAttributesCalculatorSample
     */
    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    fun setOverlayAttributesCalculator(
        calculator: (OverlayAttributesCalculatorParams) -> OverlayAttributes
    ) {
        backend.setOverlayAttributesCalculator(calculator)
    }

    /**
     * Clears the overlay calculator function previously set by [setOverlayAttributesCalculator].
     *
     * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion] is less
     *   than 6.
     */
    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    fun clearOverlayAttributesCalculator() {
        backend.clearOverlayAttributesCalculator()
    }

    /**
     * Updates [OverlayAttributes] of the overlay [ActivityStack] specified by [overlayTag]. It's no
     * op if there's no such overlay [ActivityStack] associated with [overlayTag].
     *
     * If an [OverlayAttributes] calculator function is specified, the updated [overlayAttributes]
     * will be passed by [OverlayAttributesCalculatorParams.defaultOverlayAttributes] when the
     * calculator function applies to the overlay [ActivityStack] specified by [overlayTag].
     *
     * In most cases it is suggested to use
     * [ActivityEmbeddingController.invalidateVisibleActivityStacks] if a calculator has been set
     * through [OverlayController.setOverlayAttributesCalculator].
     *
     * @param overlayTag The overlay [ActivityStack]'s tag
     * @param overlayAttributes The [OverlayAttributes] to update
     * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion] is less
     *   than 6.
     */
    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    fun updateOverlayAttributes(overlayTag: String, overlayAttributes: OverlayAttributes) {
        backend.updateOverlayAttributes(overlayTag, overlayAttributes)
    }

    /**
     * A [Flow] of [OverlayInfo] that [overlayTag] is associated with.
     *
     * If there's an active overlay [ActivityStack] associated with [overlayTag], it will be
     * reported in [OverlayInfo.activityStack]. Otherwise, [OverlayInfo.activityStack] is `null`.
     *
     * Note that launching an overlay [ActivityStack] only supports on the device with
     * [WindowSdkExtensions.extensionVersion] equal to or larger than 6. If
     * [WindowSdkExtensions.extensionVersion] is less than 6, this flow will always report
     * [OverlayInfo] without associated [OverlayInfo.activityStack].
     *
     * @param overlayTag The overlay [ActivityStack]'s tag which is set through
     *   [OverlayCreateParams]
     * @return a [Flow] of [OverlayInfo] this [overlayTag] is associated with
     */
    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    fun overlayInfo(overlayTag: String): Flow<OverlayInfo> = callbackFlow {
        val listener = Consumer { info: OverlayInfo -> trySend(info) }
        backend.addOverlayInfoCallback(overlayTag, Runnable::run, listener)
        awaitClose { backend.removeOverlayInfoCallback(listener) }
    }

    companion object {

        internal const val OVERLAY_FEATURE_VERSION = 8

        /**
         * Obtains an instance of [OverlayController].
         *
         * @param context the [Context] to initialize the controller with
         */
        @JvmStatic
        fun getInstance(context: Context): OverlayController {
            val backend = EmbeddingBackend.getInstance(context)
            return OverlayController(backend)
        }
    }
}
