/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation3.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.MutableContentTransform
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp

private fun <T> FiniteAnimationSpec<T>?.extractEasing(): Easing {
    return (this as? TweenSpec<T>)?.easing ?: LinearEasing
}

internal fun ContentTransform.toMutableTransform(
    progressState: State<Float>
): MutableContentTransform {
    val enterConfig = this.targetContentEnter.config
    val exitConfig = this.initialContentExit.config

    return MutableContentTransform(
        initialVeilMatchParentSize = exitConfig.veil?.matchParentSize ?: false,
        targetVeilMatchParentSize = enterConfig.veil?.matchParentSize ?: false,
    ) {
        targetContentTransform { fullSize ->
            val fadeConfig = enterConfig.fade
            if (fadeConfig != null) {
                val progress =
                    fadeConfig.animationSpec.extractEasing().transform(progressState.value)
                this.alpha = lerp(fadeConfig.alpha, 1f, progress)
            }

            val scaleConfig = enterConfig.scale
            if (scaleConfig != null) {
                val progress =
                    scaleConfig.animationSpec.extractEasing().transform(progressState.value)
                this.scale = lerp(scaleConfig.scale, 1f, progress)
                this.transformOrigin = scaleConfig.transformOrigin
            }

            val slideConfig = enterConfig.slide
            if (slideConfig != null) {
                val progress =
                    slideConfig.animationSpec.extractEasing().transform(progressState.value)
                val initialOffset = slideConfig.slideOffset.invoke(fullSize)
                this.offset =
                    IntOffset(
                        lerp(initialOffset.x, 0, progress),
                        lerp(initialOffset.y, 0, progress),
                    )
            }

            val veilConfig = enterConfig.veil
            if (veilConfig != null) {
                val progress =
                    veilConfig.animationSpec.extractEasing().transform(progressState.value)
                this.veil = lerp(veilConfig.initialColor, veilConfig.targetColor, progress)
            }
        }

        initialContentTransform { fullSize ->
            val fadeConfig = exitConfig.fade
            if (fadeConfig != null) {
                val progress =
                    fadeConfig.animationSpec.extractEasing().transform(progressState.value)
                this.alpha = lerp(1f, fadeConfig.alpha, progress)
            }

            val scaleConfig = exitConfig.scale
            if (scaleConfig != null) {
                val progress =
                    scaleConfig.animationSpec.extractEasing().transform(progressState.value)
                this.scale = lerp(1f, scaleConfig.scale, progress)
                this.transformOrigin = scaleConfig.transformOrigin
            }

            val slideConfig = exitConfig.slide
            if (slideConfig != null) {
                val progress =
                    slideConfig.animationSpec.extractEasing().transform(progressState.value)
                val targetOffset = slideConfig.slideOffset.invoke(fullSize)
                this.offset =
                    IntOffset(lerp(0, targetOffset.x, progress), lerp(0, targetOffset.y, progress))
            }

            val veilConfig = exitConfig.veil
            if (veilConfig != null) {
                val progress =
                    veilConfig.animationSpec.extractEasing().transform(progressState.value)
                this.veil = lerp(veilConfig.initialColor, veilConfig.targetColor, progress)
            }
        }
    }
}
