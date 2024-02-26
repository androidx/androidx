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

package androidx.compose.material3.carousel

import androidx.collection.IntIntMap
import androidx.collection.emptyIntIntMap
import androidx.collection.mutableIntIntMapOf
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Maps each item's position to the first focal keyline in a specific keyline state.
 *
 * The assigned keyline state guarantees the item will be at a focal position (ie. fully unmasked).
 * Keyline states are assigned in order of start state to end state for each item in order, with
 * the default keyline state assigned for extra items in the middle.
 */
internal fun calculateSnapPositions(strategy: Strategy?, itemCount: Int): IntIntMap {
    if (strategy == null || !strategy.isValid()) {
        return emptyIntIntMap()
    }
    val map = mutableIntIntMapOf()
    val defaultKeylines = strategy.defaultKeylines
    val startKeylineSteps = strategy.startKeylineSteps
    val endKeylineSteps = strategy.endKeylineSteps
    val numOfFocalKeylines = defaultKeylines.lastFocalIndex - defaultKeylines.firstFocalIndex
    val startStepsSize = startKeylineSteps.size + numOfFocalKeylines
    val endStepsSize = endKeylineSteps.size + numOfFocalKeylines

    for (itemIndex in 0 until itemCount) {
        map[itemIndex] = (defaultKeylines.firstFocal.offset -
            defaultKeylines.firstFocal.size / 2F).roundToInt()
        if (itemIndex < startStepsSize) {
            var startIndex = max(0, startStepsSize - 1 - itemIndex)
            startIndex = min(startKeylineSteps.size - 1, startIndex)
            val startKeylines = startKeylineSteps[startIndex]
            map[itemIndex] = (startKeylines.firstFocal.offset -
                startKeylines.firstFocal.size / 2f).roundToInt()
        }
        if (itemCount > numOfFocalKeylines + 1 && itemIndex >= itemCount - endStepsSize) {
            var endIndex = max(0, itemIndex - itemCount + endStepsSize)
            endIndex = min(endKeylineSteps.size - 1, endIndex)
            val endKeylines = endKeylineSteps[endIndex]
            map[itemIndex] = (endKeylines.firstFocal.offset -
                endKeylines.firstFocal.size / 2f).roundToInt()
        }
    }
    return map
}

internal fun KeylineSnapPosition(snapPositions: IntIntMap): SnapPosition =
    object : SnapPosition {
        override fun position(
            layoutSize: Int,
            itemSize: Int,
            beforeContentPadding: Int,
            afterContentPadding: Int,
            itemIndex: Int,
            itemCount: Int
        ): Int {
            return if (snapPositions.size > 0) snapPositions[itemIndex] else 0
        }
    }

@ExperimentalMaterial3Api
@Composable
internal fun rememberDecaySnapFlingBehavior(): TargetedFlingBehavior {
    val splineDecay = rememberSplineBasedDecay<Float>()
    val decayLayoutInfoProvider = remember {
        object : SnapLayoutInfoProvider {
            override fun calculateApproachOffset(initialVelocity: Float): Float {
                return splineDecay.calculateTargetValue(0f, initialVelocity)
            }

            override fun calculateSnappingOffset(currentVelocity: Float): Float = 0f
        }
    }

    return rememberSnapFlingBehavior(snapLayoutInfoProvider = decayLayoutInfoProvider)
}
