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

package androidx.compose.material3.adaptive

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.util.fastForEach
import androidx.window.layout.FoldingFeature

/**
 * Calculates the [Posture] for a given list of [FoldingFeature]s. This methods converts framework
 * folding info into the Material-opinionated posture info.
 */
@ExperimentalMaterial3AdaptiveApi
fun calculatePosture(foldingFeatures: List<FoldingFeature>): Posture {
    var hasVerticalHinge = false
    var isTableTop = false
    var hasSeparatingHinge = false
    val separatingHingeBounds = mutableListOf<Rect>()
    val occludingHingeBounds = mutableListOf<Rect>()
    val allHingeBounds = mutableListOf<Rect>()
    foldingFeatures.fastForEach {
        if (it.orientation == FoldingFeature.Orientation.VERTICAL) {
            hasVerticalHinge = true
        }
        if (it.isSeparating) {
            hasSeparatingHinge = true
        }
        if (it.orientation == FoldingFeature.Orientation.HORIZONTAL &&
            it.state == FoldingFeature.State.HALF_OPENED) {
            isTableTop = true
        }
        val hingeBounds = it.bounds.toComposeRect()
        // TODO(conradchen): Figure out how to deal with horizontal hinges
        if (it.orientation == FoldingFeature.Orientation.VERTICAL) {
            allHingeBounds.add(hingeBounds)
            if (it.isSeparating) {
                separatingHingeBounds.add(hingeBounds)
            }
            if (it.occlusionType == FoldingFeature.OcclusionType.FULL) {
                occludingHingeBounds.add(hingeBounds)
            }
        }
    }
    return Posture(
        hasVerticalHinge,
        isTableTop,
        hasSeparatingHinge,
        separatingHingeBounds,
        occludingHingeBounds,
        allHingeBounds
    )
}
