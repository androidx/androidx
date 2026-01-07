/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.compose.animation.scene.demo

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.android.compose.animation.scene.Edge
import com.android.compose.animation.scene.FixedSizeEdgeDetector
import com.android.compose.animation.scene.SwipeSource
import com.android.compose.animation.scene.SwipeSourceDetector

/** Identifies an area of the [SceneContainer] to detect swipe gestures on. */
sealed class SceneContainerArea(private val resolveArea: (LayoutDirection) -> Resolved) :
    SwipeSource {
    data object StartEdge :
        SceneContainerArea(
            resolveArea = {
                if (it == LayoutDirection.Ltr) Resolved.LeftEdge else Resolved.RightEdge
            }
        )

    data object StartHalf :
        SceneContainerArea(
            resolveArea = {
                if (it == LayoutDirection.Ltr) Resolved.LeftHalf else Resolved.RightHalf
            }
        )

    data object EndEdge :
        SceneContainerArea(
            resolveArea = {
                if (it == LayoutDirection.Ltr) Resolved.RightEdge else Resolved.LeftEdge
            }
        )

    data object EndHalf :
        SceneContainerArea(
            resolveArea = {
                if (it == LayoutDirection.Ltr) Resolved.RightHalf else Resolved.LeftHalf
            }
        )

    override fun resolve(layoutDirection: LayoutDirection): Resolved {
        return resolveArea(layoutDirection)
    }

    sealed interface Resolved : SwipeSource.Resolved {
        data object LeftEdge : Resolved

        data object LeftHalf : Resolved

        data object BottomEdge : Resolved

        data object RightEdge : Resolved

        data object RightHalf : Resolved
    }
}

/**
 * A [SwipeSourceDetector] that detects edges similarly to [FixedSizeEdgeDetector], but additionally
 * detects the left and right halves of the screen (besides the edges).
 *
 * Corner cases (literally): A vertical swipe on the top-left corner of the screen will be resolved
 * to [SceneContainerArea.Resolved.LeftHalf], whereas a horizontal swipe in the same position will
 * be resolved to [SceneContainerArea.Resolved.LeftEdge]. The behavior is similar on the top-right
 * corner of the screen.
 *
 * Callers who need to detect the start and end edges based on the layout direction (LTR vs RTL)
 * should subscribe to [SceneContainerArea.StartEdge] and [SceneContainerArea.EndEdge] instead.
 * These will be resolved at runtime to [SceneContainerArea.Resolved.LeftEdge] and
 * [SceneContainerArea.Resolved.RightEdge] appropriately. Similarly, [SceneContainerArea.StartHalf]
 * and [SceneContainerArea.EndHalf] will be resolved appropriately to
 * [SceneContainerArea.Resolved.LeftHalf] and [SceneContainerArea.Resolved.RightHalf].
 *
 * @param edgeSize The fixed size of each edge.
 */
class SceneContainerSwipeDetector(val edgeSize: Dp) : SwipeSourceDetector {

    private val fixedEdgeDetector = FixedSizeEdgeDetector(edgeSize)

    override fun source(
        layoutSize: IntSize,
        position: IntOffset,
        density: Density,
        orientation: Orientation,
    ): SceneContainerArea.Resolved {
        val fixedEdge = fixedEdgeDetector.source(layoutSize, position, density, orientation)
        return when (fixedEdge) {
            Edge.Resolved.Left -> SceneContainerArea.Resolved.LeftEdge
            Edge.Resolved.Bottom -> SceneContainerArea.Resolved.BottomEdge
            Edge.Resolved.Right -> SceneContainerArea.Resolved.RightEdge
            else -> {
                // Note: This intentionally includes Edge.Resolved.Top. At the moment, we don't need
                // to detect swipes on the top edge, and consider them part of the right/left half.
                if (position.x < layoutSize.width * 0.5f) {
                    SceneContainerArea.Resolved.LeftHalf
                } else {
                    SceneContainerArea.Resolved.RightHalf
                }
            }
        }
    }
}
