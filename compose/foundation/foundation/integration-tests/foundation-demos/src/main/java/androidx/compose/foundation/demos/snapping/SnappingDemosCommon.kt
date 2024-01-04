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

package androidx.compose.foundation.demos.snapping

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import kotlin.math.absoluteValue
import kotlin.math.sign

@Suppress("PrimitiveInLambda")
@OptIn(ExperimentalFoundationApi::class)
internal class ViewPortBasedSnappingLayoutInfoProvider(
    private val baseSnapLayoutInfoProvider: SnapLayoutInfoProvider,
    private val decayAnimationSpec: DecayAnimationSpec<Float>,
    private val viewPortStep: () -> Float,
    private val itemSize: () -> Float
) : SnapLayoutInfoProvider by baseSnapLayoutInfoProvider {
    override fun calculateApproachOffset(initialVelocity: Float): Float {
        val offset = decayAnimationSpec.calculateTargetValue(0f, initialVelocity)
        val finalOffset = (offset.absoluteValue - itemSize()).coerceAtLeast(0.0f) * offset.sign
        val viewPortOffset = viewPortStep()
        return finalOffset.coerceIn(-viewPortOffset, viewPortOffset)
    }
}
