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
@file:JvmName("DisplayFeatureTesting")

package androidx.window.testing

import android.app.Activity
import android.graphics.Rect
import androidx.window.FoldingFeature
import androidx.window.FoldingFeature.Orientation
import androidx.window.FoldingFeature.Orientation.Companion.HORIZONTAL
import androidx.window.FoldingFeature.Orientation.Companion.VERTICAL
import androidx.window.FoldingFeature.State
import androidx.window.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.FoldingFeature.Type.Companion.FOLD
import androidx.window.FoldingFeature.Type.Companion.HINGE
import androidx.window.windowInfoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * A convenience method to get a test fold with default values provided. With the default values
 * it returns a [FoldingFeature.State.HALF_OPENED] feature that splits the screen along the
 * [FoldingFeature.Orientation.HORIZONTAL] axis.
 *
 * The bounds of the feature are calculated based on [orientation], [center], and [size]. If the
 * feature is [VERTICAL] then the top-left x-coordinate is [center] - ([size] / 2) and the top-right
 * x-coordinate is [center] + ([size] / 2). If the feature is [HORIZONTAL] then the top-left
 * y-coordinate is [center] - ([size] / 2) and the bottom-left y-coordinate is
 * [center] - ([size] / 2). The folding features always cover the window in one dimension and that
 * determines the other coordinates.
 *
 * @param activity [Activity] that will host the test [FoldingFeature].
 * @param center The coordinate along the axis matching the [Orientation]. The default is centered
 * along the [HORIZONTAL] axis.
 * @param size the smaller dimension  of the fold. The larger dimension  always covers the entire
 * window.
 * @param state [State] of the fold. The default value is [HALF_OPENED]
 * @param orientation [Orientation] of the fold. The default value is [HORIZONTAL]
 * @return [FoldingFeature] that is splitting if the width is not 0 and runs parallel to the
 * [Orientation] axis.
 */
@Suppress("FunctionName")
@ExperimentalCoroutinesApi
@JvmOverloads
@JvmName("createFoldingFeature")
public fun FoldingFeature(
    activity: Activity,
    center: Int = activity.windowInfoRepository().currentWindowMetrics.bounds.centerX(),
    size: Int = 0,
    state: State = HALF_OPENED,
    orientation: Orientation = HORIZONTAL
): FoldingFeature {
    val type = if (size == 0) {
        FOLD
    } else {
        HINGE
    }
    val offset = size / 2
    val start = center - offset
    val end = center + offset
    val bounds = if (orientation == VERTICAL) {
        val windowHeight = activity.windowInfoRepository().currentWindowMetrics.bounds.height()
        Rect(start, 0, end, windowHeight)
    } else {
        val windowWidth = activity.windowInfoRepository().currentWindowMetrics.bounds.width()
        Rect(0, start, windowWidth, end)
    }
    return FoldingFeature(bounds, type, state)
}