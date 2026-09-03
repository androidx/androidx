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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.player.compose.embedded.LocalAnimatedVisibilityScope
import androidx.compose.remote.player.compose.embedded.LocalSharedTransitionScope
import androidx.compose.remote.player.compose.embedded.RcPlayerComponent
import androidx.compose.remote.player.compose.embedded.animationSpecReflection
import androidx.compose.remote.player.compose.embedded.horizontalPositioningReflection
import androidx.compose.remote.player.compose.embedded.mapEasing
import androidx.compose.remote.player.compose.embedded.verticalPositioningReflection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull

/**
 * Renders a [FitBoxLayout]: its children are *alternatives* and only the first one whose natural
 * size fits the available space is displayed.
 *
 * When available space or layout constraints change, [RcPlayerFitBoxLayout] transitions smoothly
 * between alternatives using [SharedTransitionLayout] and [AnimatedContent], allowing shared
 * elements with matching animation IDs to morph across states.
 *
 * Uses a two-phase [SubcomposeLayout]:
 * 1. A probe subcomposition measures all child alternatives with unbounded constraints to determine
 *    their real Compose sizes, clearing semantics so unselected nodes do not pollute accessibility.
 * 2. A content subcomposition renders the selected alternative with [AnimatedContent] and
 *    [SharedTransitionLayout], driving shared element animations when constraints change.
 */
@Suppress("ComposableLambdaInMeasurePolicy")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun RcPlayerFitBoxLayout(layout: FitBoxLayout, modifier: Modifier) {
    val children = remember(layout) { ArrayList<Component>().apply { layout.getComponents(this) } }
    if (children.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    val duration = layout.animationSpecReflection?.motionDuration?.toInt() ?: 300
    val easing = mapEasing(layout.animationSpecReflection?.motionEasingType ?: 0)
    val alignment =
        mapFitBoxAlignment(
            layout.horizontalPositioningReflection,
            layout.verticalPositioningReflection,
        )

    SubcomposeLayout(modifier = modifier) { constraints ->
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE

        // Phase 1 (Intrinsics Probe): Query intrinsic sizes on child measurables without
        // measuring full placeables. Semantics are cleared on probe nodes.
        val probeMeasurables =
            subcompose(FitBoxSlot.Probe) {
                children.fastForEach { component ->
                    Box(modifier = Modifier.clearAndSetSemantics {}) {
                        RcPlayerComponent(component)
                    }
                }
            }

        var chosen = -1
        for (i in 0 until probeMeasurables.size) {
            val m = probeMeasurables[i]
            val w = m.maxIntrinsicWidth(maxHeight)
            val h = m.maxIntrinsicHeight(maxWidth)
            if (w <= maxWidth && h <= maxHeight) {
                chosen = i
                break
            }
        }
        if (chosen < 0) {
            chosen =
                probeMeasurables.indices.minByOrNull {
                    probeMeasurables[it].maxIntrinsicWidth(maxHeight)
                } ?: 0
        }

        // Phase 2 (Content): Subcompose the chosen alternative with AnimatedContent.
        // targetState is provided directly by 'chosen' without writing mutable Compose State in
        // measure.
        val contentMeasurables =
            subcompose(FitBoxSlot.Content) {
                SharedTransitionLayout {
                    AnimatedContent(
                        targetState = chosen,
                        contentAlignment = alignment,
                        label = "RcPlayerFitBoxLayout",
                        transitionSpec = {
                            fadeIn(
                                animationSpec = tween(durationMillis = duration, easing = easing)
                            ) togetherWith
                                fadeOut(
                                    animationSpec =
                                        tween(durationMillis = duration, easing = easing)
                                )
                        },
                    ) { currentIndex ->
                        CompositionLocalProvider(
                            LocalSharedTransitionScope provides this@SharedTransitionLayout,
                            LocalAnimatedVisibilityScope provides this@AnimatedContent,
                        ) {
                            Box(contentAlignment = alignment) {
                                RcPlayerComponent(children[currentIndex])
                            }
                        }
                    }
                }
            }

        val contentPlaceables = contentMeasurables.fastMap { it.measure(constraints) }
        val width = constraints.constrainWidth(contentPlaceables.fastMaxOfOrNull { it.width } ?: 0)
        val height =
            constraints.constrainHeight(contentPlaceables.fastMaxOfOrNull { it.height } ?: 0)

        layout(width, height) { contentPlaceables.fastForEach { it.placeRelative(0, 0) } }
    }
}

private enum class FitBoxSlot {
    Probe,
    Content,
}

private fun mapFitBoxAlignment(horizontal: Int, vertical: Int): Alignment {
    return when {
        horizontal == FitBoxLayout.START && vertical == FitBoxLayout.TOP -> Alignment.TopStart
        horizontal == FitBoxLayout.START && vertical == FitBoxLayout.BOTTOM -> Alignment.BottomStart
        horizontal == FitBoxLayout.START -> Alignment.CenterStart
        horizontal == FitBoxLayout.END && vertical == FitBoxLayout.TOP -> Alignment.TopEnd
        horizontal == FitBoxLayout.END && vertical == FitBoxLayout.BOTTOM -> Alignment.BottomEnd
        horizontal == FitBoxLayout.END -> Alignment.CenterEnd
        vertical == FitBoxLayout.TOP -> Alignment.TopCenter
        vertical == FitBoxLayout.BOTTOM -> Alignment.BottomCenter
        else -> Alignment.Center
    }
}
