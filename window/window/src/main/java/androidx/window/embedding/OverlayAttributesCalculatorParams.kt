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

import android.content.res.Configuration
import androidx.annotation.RestrictTo
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics

/**
 * The parameter container used to report the current device and window state in
 * [OverlayController.setOverlayAttributesCalculator] and references the corresponding overlay
 * [ActivityStack] by [overlayTag].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class OverlayAttributesCalculatorParams
internal constructor(
    /** The parent container's [WindowMetrics] */
    val parentWindowMetrics: WindowMetrics,
    /** The parent container's [Configuration] */
    val parentConfiguration: Configuration,
    /** The parent container's [WindowLayoutInfo] */
    val parentWindowLayoutInfo: WindowLayoutInfo,
    /**
     * The unique identifier of the overlay [ActivityStack] specified by [OverlayCreateParams.tag]
     */
    val overlayTag: String,
    /**
     * The overlay [ActivityStack]'s [OverlayAttributes] specified by [overlayTag], which is the
     * [OverlayAttributes] that is not calculated by calculator. It should be either initialized by
     * [OverlayCreateParams.overlayAttributes] or [OverlayController.updateOverlayAttributes].
     */
    val defaultOverlayAttributes: OverlayAttributes,
) {
    override fun toString(): String =
        "${OverlayAttributesCalculatorParams::class.java}:{" +
            "parentWindowMetrics=$parentWindowMetrics" +
            "parentConfiguration=$parentConfiguration" +
            "parentWindowLayoutInfo=$parentWindowLayoutInfo" +
            "overlayTag=$overlayTag" +
            "defaultOverlayAttributes=$defaultOverlayAttributes"
}
