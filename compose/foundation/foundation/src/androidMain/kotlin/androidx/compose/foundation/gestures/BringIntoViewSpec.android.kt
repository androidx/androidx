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

package androidx.compose.foundation.gestures

import android.content.pm.PackageManager.FEATURE_LEANBACK
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec.Companion.DefaultBringIntoViewSpec
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/**
 * A composition local to customize the focus scrolling behavior used by some scrollable containers.
 * [LocalBringIntoViewSpec] has a platform defined behavior. If the App is running on a TV device,
 * the scroll behavior will pivot around 30% of the container size. For other platforms, the scroll
 * behavior will move the least to bring the requested region into view.
 */
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@get:ExperimentalFoundationApi
@ExperimentalFoundationApi
actual val LocalBringIntoViewSpec: ProvidableCompositionLocal<BringIntoViewSpec> =
    compositionLocalWithComputedDefaultOf {
        val hasTvFeature =
            LocalContext.currentValue.packageManager.hasSystemFeature(FEATURE_LEANBACK)
        if (!hasTvFeature) {
            DefaultBringIntoViewSpec
        } else {
            PivotBringIntoViewSpec
        }
    }

@OptIn(ExperimentalFoundationApi::class)
internal val PivotBringIntoViewSpec =
    object : BringIntoViewSpec {
        val parentFraction = 0.3f
        val childFraction = 0f

        override fun calculateScrollDistance(
            offset: Float,
            size: Float,
            containerSize: Float
        ): Float {
            val leadingEdgeOfItemRequestingFocus = offset
            val trailingEdgeOfItemRequestingFocus = offset + size

            val sizeOfItemRequestingFocus =
                abs(trailingEdgeOfItemRequestingFocus - leadingEdgeOfItemRequestingFocus)
            val childSmallerThanParent = sizeOfItemRequestingFocus <= containerSize
            val initialTargetForLeadingEdge =
                parentFraction * containerSize - (childFraction * sizeOfItemRequestingFocus)
            val spaceAvailableToShowItem = containerSize - initialTargetForLeadingEdge

            val targetForLeadingEdge =
                if (
                    childSmallerThanParent && spaceAvailableToShowItem < sizeOfItemRequestingFocus
                ) {
                    containerSize - sizeOfItemRequestingFocus
                } else {
                    initialTargetForLeadingEdge
                }

            return leadingEdgeOfItemRequestingFocus - targetForLeadingEdge
        }
    }
