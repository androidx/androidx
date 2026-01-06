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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.mechanics.rememberGestureContext
import com.android.compose.modifiers.thenIf
import com.android.mechanics.behavior.VerticalExpandContainerSpec
import com.android.mechanics.behavior.verticalExpandContainerBackground
import com.android.mechanics.compose.modifier.motionDriver

object PartialShade {
    object Colors {
        val Background
            @Composable get() = Shade.Colors.Scrim
    }
}

@Composable
fun ContentScope.PartialShade(
    rootElement: ElementKey,
    modifier: Modifier = Modifier,
    revealEffect: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val isSplitShade = shouldUseSplitScenes(calculateWindowSizeClass())

    // TODO b/401500734: this should use the same motion spec as the transition. Skipped the wiring
    //  up for now, since I want to experiment with "semantic gestures" in STL first, which will
    //  likely make a lot fo the wiring obsolete.
    val shadeMotionSpec =
        remember(isSplitShade) { VerticalExpandContainerSpec(isFloating = isSplitShade) }

    val contentOverscrollEffect = checkNotNull(rememberOverscrollEffect())
    val gestureContext = rememberGestureContext()
    Box(
        modifier
            .fillMaxWidth(if (isSplitShade) 0.5f else 1f)
            .overscroll(verticalOverscrollEffect)
            .overscroll(contentOverscrollEffect)
            .thenIf(isSplitShade) { Modifier.padding(16.dp) }
            .element(rootElement)
            .thenIf(revealEffect) {
                Modifier.motionDriver(gestureContext = gestureContext, label = "PartialShade")
            }
            .verticalExpandContainerBackground(
                backgroundColor = PartialShade.Colors.Background,
                spec = shadeMotionSpec,
            )
            .disableSwipesWhenScrolling()
            .verticalScroll(
                rememberScrollState(),
                overscrollEffect = contentOverscrollEffect.withoutVisualEffect(),
            ),
        content = content,
    )
}
