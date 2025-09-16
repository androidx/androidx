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

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toComposeRect
import androidx.window.layout.FoldingFeature

/**
 * Calculates the [Posture] for a given list of [FoldingFeature]s. This methods converts framework
 * folding info into the Material-opinionated posture info.
 */
@VisibleForTesting
internal fun calculatePosture(foldingFeatures: List<FoldingFeature>): Posture {
    var isTableTop = false
    val hingeList = mutableListOf<HingeInfo>()
    @Suppress("ListIterator")
    foldingFeatures.forEach {
        if (
            it.orientation == FoldingFeature.Orientation.HORIZONTAL &&
                it.state == FoldingFeature.State.HALF_OPENED
        ) {
            isTableTop = true
        }
        hingeList.add(
            HingeInfo(
                bounds = it.bounds.toComposeRect(),
                isFlat = it.state == FoldingFeature.State.FLAT,
                isVertical = it.orientation == FoldingFeature.Orientation.VERTICAL,
                isSeparating = it.isSeparating,
                isOccluding = it.occlusionType == FoldingFeature.OcclusionType.FULL,
            )
        )
    }
    return Posture(isTableTop, hingeList)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal actual fun calculatePosture(): Posture =
    calculatePosture(collectFoldingFeaturesAsState().value)
