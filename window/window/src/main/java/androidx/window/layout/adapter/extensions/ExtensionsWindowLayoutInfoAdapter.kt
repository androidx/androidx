/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.layout.adapter.extensions

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.UiContext
import androidx.window.core.Bounds
import androidx.window.core.ExtensionsUtil
import androidx.window.extensions.layout.DisplayFoldFeature
import androidx.window.extensions.layout.FoldingFeature as OEMFoldingFeature
import androidx.window.extensions.layout.SupportedWindowFeatures
import androidx.window.extensions.layout.WindowLayoutInfo as OEMWindowLayoutInfo
import androidx.window.extensions.layout.WindowLayoutInfo.ENGAGEMENT_MODE_FLAG_AUDIO_ON
import androidx.window.extensions.layout.WindowLayoutInfo.ENGAGEMENT_MODE_FLAG_VISUALS_ON
import androidx.window.layout.FoldingFeature
import androidx.window.layout.FoldingFeature.State.Companion.FLAT
import androidx.window.layout.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.layout.HardwareFoldingFeature
import androidx.window.layout.HardwareFoldingFeature.Type.Companion.FOLD
import androidx.window.layout.HardwareFoldingFeature.Type.Companion.HINGE
import androidx.window.layout.SupportedPosture
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculatorCompat

internal object ExtensionsWindowLayoutInfoAdapter {

    internal fun translate(
        windowMetrics: WindowMetrics,
        oemFeature: OEMFoldingFeature,
    ): FoldingFeature? {
        val type =
            when (oemFeature.type) {
                OEMFoldingFeature.TYPE_FOLD -> FOLD
                OEMFoldingFeature.TYPE_HINGE -> HINGE
                else -> return null
            }
        val state =
            when (oemFeature.state) {
                OEMFoldingFeature.STATE_FLAT -> FLAT
                OEMFoldingFeature.STATE_HALF_OPENED -> HALF_OPENED
                else -> return null
            }
        val bounds = Bounds(oemFeature.bounds)
        return if (validBounds(windowMetrics, bounds)) {
            HardwareFoldingFeature(Bounds(oemFeature.bounds), type, state)
        } else {
            null
        }
    }

    internal fun translate(
        @UiContext context: Context,
        info: OEMWindowLayoutInfo,
    ): WindowLayoutInfo {
        val calculator = WindowMetricsCalculatorCompat()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            translate(calculator.computeCurrentWindowMetrics(context), info)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && (context is Activity)) {
            translate(calculator.computeCurrentWindowMetrics(context), info)
        } else {
            throw UnsupportedOperationException(
                "Display Features are only supported after Q. Display features for non-Activity " +
                    "contexts are not expected to be reported on devices running Q."
            )
        }
    }

    @Suppress("DEPRECATION")
    internal fun translate(
        windowMetrics: WindowMetrics,
        info: OEMWindowLayoutInfo,
    ): WindowLayoutInfo {
        val features =
            info.displayFeatures.mapNotNull { feature ->
                when (feature) {
                    is OEMFoldingFeature -> translate(windowMetrics, feature)
                    else -> null
                }
            }

        val oemEngagementModeflags =
            if (ExtensionsUtil.safeVendorApiLevel >= 10) {
                info.engagementModeFlags
            } else {
                ENGAGEMENT_MODE_FLAG_VISUALS_ON or ENGAGEMENT_MODE_FLAG_AUDIO_ON // default
            }
        val engagementModeSet = mutableSetOf<WindowLayoutInfo.EngagementMode>()
        if ((oemEngagementModeflags and ENGAGEMENT_MODE_FLAG_VISUALS_ON) != 0) {
            engagementModeSet.add(WindowLayoutInfo.EngagementMode.VISUALS_ON)
        }
        if ((oemEngagementModeflags and ENGAGEMENT_MODE_FLAG_AUDIO_ON) != 0) {
            engagementModeSet.add(WindowLayoutInfo.EngagementMode.AUDIO_ON)
        }

        return WindowLayoutInfo(features, engagementModeSet.toSet())
    }

    internal fun translate(features: SupportedWindowFeatures): List<SupportedPosture> {
        val isTableTopSupported =
            features.displayFoldFeatures.any { feature ->
                feature.hasProperties(DisplayFoldFeature.FOLD_PROPERTY_SUPPORTS_HALF_OPENED)
            }
        return if (isTableTopSupported) {
            listOf(SupportedPosture.TABLETOP)
        } else {
            emptyList()
        }
    }

    /**
     * Checks the bounds for a [FoldingFeature] within a given [WindowMetrics]. Validates the
     * following:
     * - [Bounds] are not `0`
     * - [Bounds] are either full width or full height
     * - [Bounds] do not take up the entire [windowMetrics]
     *
     * @param windowMetrics Extracted from a [UiContext] housing the [FoldingFeature].
     * @param bounds the bounds of a [FoldingFeature]
     * @return true if the bounds are valid for the [WindowMetrics], false otherwise.
     */
    private fun validBounds(windowMetrics: WindowMetrics, bounds: Bounds): Boolean {
        val windowBounds = windowMetrics.bounds
        if (bounds.isZero) {
            return false
        }
        if (bounds.width != windowBounds.width() && bounds.height != windowBounds.height()) {
            return false
        }
        if (bounds.width < windowBounds.width() && bounds.height < windowBounds.height()) {
            return false
        }
        if (bounds.width == windowBounds.width() && bounds.height == windowBounds.height()) {
            return false
        }

        return true
    }
}
