/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.animation

import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.remember
import androidx.ui.core.AnimationClockAmbient
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.clipToBounds
import androidx.ui.core.composed
import androidx.compose.ui.unit.IntSize

/**
 * This modifier animates its own size when its child modifier (or the child composable if it
 * is already at the tail of the chain) changes size. This allows the parent modifier to observe
 * a smooth size change, resulting in an overall continuous visual change.
 *
 * An [AnimationSpec] can be optionally specified for the size change animation. By default,
 * [SpringSpec] will be used. Clipping defaults to true, such that the content outside of animated
 * size will not be shown.
 *
 * @sample androidx.compose.animation.samples.AnimateContent
 *
 * @param animSpec the animation that will be used to animate size change
 * @param clip whether content outside of animated size should be clipped
 */
fun Modifier.animateContentSize(
    animSpec: AnimationSpec<IntSize> = spring(),
    clip: Boolean = true
): Modifier = composed {
    val clock = AnimationClockAmbient.current.asDisposableClock()
    val animModifier = remember {
        SizeAnimationModifier(animSpec, clock)
    }
    if (clip) {
        this.clipToBounds().then(animModifier)
    } else {
        this.then(animModifier)
    }
}

/**
 * This class creates a [LayoutModifier] that measures children, and responds to children's size
 * change by animating to that size. The size reported to parents will be the animated size.
 */
private class SizeAnimationModifier(
    val animSpec: AnimationSpec<IntSize>,
    val clock: AnimationClockObservable
) : LayoutModifier {
    var sizeAnim: AnimatedValueModel<IntSize, AnimationVector2D>? = null

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {

        val placeable = measurable.measure(constraints)

        val measuredSize = IntSize(placeable.width, placeable.height)

        val anim = sizeAnim?.apply {
            if (targetValue != measuredSize) {
                animateTo(measuredSize, animSpec)
            }
        } ?: AnimatedValueModel(
            measuredSize, IntSizeToVectorConverter, clock, IntSize(1, 1)
        )
        sizeAnim = anim
        return layout(anim.value.width, anim.value.height) {
            placeable.place(0, 0)
        }
    }
}
