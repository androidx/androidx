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
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.player.compose.embedded.LocalAnimatedVisibilityScope
import androidx.compose.remote.player.compose.embedded.LocalSharedTransitionScope
import androidx.compose.remote.player.compose.embedded.RcPlayerComponent
import androidx.compose.remote.player.compose.embedded.animationSpecReflection
import androidx.compose.remote.player.compose.embedded.indexIdReflection
import androidx.compose.remote.player.compose.embedded.mapEasing
import androidx.compose.remote.player.compose.embedded.rcComponentContentInspector
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteIntAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun RcPlayerStateLayout(layout: StateLayout, modifier: Modifier) {
    val index by rememberRemoteIntAsState(layout.indexIdReflection)
    val children = remember(layout) { ArrayList<Component>().apply { layout.getComponents(this) } }

    if (children.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    val targetIndex = index.coerceIn(0, children.size - 1)
    for (i in 0 until children.size) {
        val child = children[i]
        val vis = if (i == targetIndex) Component.Visibility.VISIBLE else Component.Visibility.GONE
        child.mVisibility = vis
    }

    val transitionConfig = rememberLayoutTransitionConfig(layout, children)
    val duration = transitionConfig.duration
    val easing = transitionConfig.easing

    if (duration <= 0) {
        Box(
            modifier = modifier.rcComponentContentInspector(layout, forStateLayoutContent = true),
            contentAlignment = Alignment.Center,
        ) {
            RcPlayerComponent(children[targetIndex])
        }
        return
    }

    @Composable
    fun AnimatedStateContent(
        sharedTransitionScope: SharedTransitionScope?,
        contentModifier: Modifier,
    ) {
        AnimatedContent(
            targetState = targetIndex,
            modifier =
                contentModifier.rcComponentContentInspector(layout, forStateLayoutContent = true),
            contentAlignment = Alignment.Center,
            label = "RcPlayerStateLayout",
            transitionSpec = {
                (fadeIn(
                        animationSpec = tween(durationMillis = duration, easing = easing)
                    ) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = duration, easing = easing)))
                    .using(
                        SizeTransform(clip = false) { _, _ ->
                            tween(durationMillis = duration, easing = easing)
                        }
                    )
            },
        ) { currentIndex ->
            CompositionLocalProvider(
                LocalSharedTransitionScope provides sharedTransitionScope,
                LocalAnimatedVisibilityScope provides this@AnimatedContent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RcPlayerComponent(children[currentIndex])
                }
            }
        }
    }

    if (transitionConfig.hasSharedElements) {
        SharedTransitionLayout(modifier = modifier) {
            AnimatedStateContent(
                sharedTransitionScope = this@SharedTransitionLayout,
                contentModifier = Modifier,
            )
        }
    } else {
        AnimatedStateContent(sharedTransitionScope = null, contentModifier = modifier)
    }
}

internal class LayoutTransitionConfig(
    val duration: Int,
    val easing: Easing,
    val hasSharedElements: Boolean,
)

@Composable
internal fun rememberLayoutTransitionConfig(
    layout: LayoutComponent,
    children: List<Component>,
): LayoutTransitionConfig {
    return remember(layout, children) {
        val layoutSpec =
            layout.componentModifiers?.list?.fastFirstOrNull { it is AnimationSpec }
                as? AnimationSpec
                ?: layout.animationSpecReflection?.takeIf { it != AnimationSpec.DEFAULT }

        var childSpec: AnimationSpec? = null
        var hasSharedElements = false

        fun search(comp: Component) {
            if (comp.animationId != -1 && comp.animationId != 0) {
                hasSharedElements = true
            }
            val s =
                (comp as? LayoutComponent)?.componentModifiers?.list?.fastFirstOrNull {
                    it is AnimationSpec
                } as? AnimationSpec
                    ?: comp.animationSpecReflection?.takeIf { it != AnimationSpec.DEFAULT }
            if (
                s != null &&
                    (childSpec == null ||
                        s.effectiveMotionDuration() > childSpec!!.effectiveMotionDuration())
            ) {
                childSpec = s
            }
            if (comp is LayoutComponent && comp !is StateLayout && comp !is FitBoxLayout) {
                comp.childrenComponents.fastForEach { search(it) }
            }
        }

        children.fastForEach { search(it) }

        val spec = layoutSpec ?: childSpec
        val duration = spec?.effectiveMotionDuration()?.toInt() ?: 300
        val easing = mapEasing(spec?.motionEasingType ?: 0)
        LayoutTransitionConfig(
            duration = duration,
            easing = easing,
            hasSharedElements = duration > 0 && hasSharedElements,
        )
    }
}

internal fun AnimationSpec.effectiveMotionDuration(): Float =
    if (isAnimationEnabled) motionDuration else 0f
