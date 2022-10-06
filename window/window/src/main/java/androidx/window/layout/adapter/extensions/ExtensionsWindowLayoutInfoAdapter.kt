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

import androidx.window.extensions.layout.FoldingFeature as OEMFoldingFeature
import androidx.window.extensions.layout.WindowLayoutInfo as OEMWindowLayoutInfo
import android.app.Activity
import androidx.window.core.Bounds
import androidx.window.layout.FoldingFeature
import androidx.window.layout.FoldingFeature.State.Companion.FLAT
import androidx.window.layout.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.layout.HardwareFoldingFeature
import androidx.window.layout.HardwareFoldingFeature.Type.Companion.FOLD
import androidx.window.layout.HardwareFoldingFeature.Type.Companion.HINGE
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetricsCalculatorCompat.computeCurrentWindowMetrics

internal object ExtensionsWindowLayoutInfoAdapter {

    internal fun translate(activity: Activity, oemFeature: OEMFoldingFeature): FoldingFeature? {
        val type = when (oemFeature.type) {
            OEMFoldingFeature.TYPE_FOLD -> FOLD
            OEMFoldingFeature.TYPE_HINGE -> HINGE
            else -> return null
        }
        val state = when (oemFeature.state) {
            OEMFoldingFeature.STATE_FLAT -> FLAT
            OEMFoldingFeature.STATE_HALF_OPENED -> HALF_OPENED
            else -> return null
        }
        val bounds = Bounds(oemFeature.bounds)
        return if (validBounds(activity, bounds)) {
            HardwareFoldingFeature(Bounds(oemFeature.bounds), type, state)
        } else {
            null
        }
    }

    internal fun translate(activity: Activity, info: OEMWindowLayoutInfo): WindowLayoutInfo {
        val features = info.displayFeatures.mapNotNull { feature ->
            when (feature) {
                is OEMFoldingFeature -> translate(activity, feature)
                else -> null
            }
        }
        return WindowLayoutInfo(features)
    }

    /**
     * Validate the bounds for a [FoldingFeature] within a given [Activity]. Check the following
     * <ul>
     *     <li>Bounds are not 0</li>
     *     <li>Bounds are either full width or full height</li>
     *     <li>Bounds do not take up the entire window</li>
     * </ul>
     *
     * @param activity housing the [FoldingFeature].
     * @param bounds the bounds of a [FoldingFeature]
     * @return true if the bounds are valid for the [Activity], false otherwise.
     */
    private fun validBounds(activity: Activity, bounds: Bounds): Boolean {
        val windowBounds = computeCurrentWindowMetrics(activity).bounds
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