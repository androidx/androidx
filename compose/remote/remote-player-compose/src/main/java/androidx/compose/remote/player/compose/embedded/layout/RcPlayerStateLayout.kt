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
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.player.compose.embedded.LocalAnimatedVisibilityScope
import androidx.compose.remote.player.compose.embedded.LocalSharedTransitionScope
import androidx.compose.remote.player.compose.embedded.RcPlayerComponent
import androidx.compose.remote.player.compose.embedded.animationSpecReflection
import androidx.compose.remote.player.compose.embedded.indexIdReflection
import androidx.compose.remote.player.compose.embedded.mapEasing
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteIntAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

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
    val duration = layout.animationSpecReflection?.motionDuration?.toInt() ?: 300
    val easing = mapEasing(layout.animationSpecReflection?.motionEasingType ?: 0)

    SharedTransitionLayout(modifier = modifier) {
        AnimatedContent(
            targetState = targetIndex,
            contentAlignment = Alignment.Center,
            label = "RcPlayerStateLayout",
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(durationMillis = duration, easing = easing)
                ) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = duration, easing = easing))
            },
        ) { currentIndex ->
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this@SharedTransitionLayout,
                LocalAnimatedVisibilityScope provides this@AnimatedContent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RcPlayerComponent(children[currentIndex])
                }
            }
        }
    }
}
