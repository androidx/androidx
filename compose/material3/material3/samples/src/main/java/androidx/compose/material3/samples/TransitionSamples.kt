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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.animation.DeferredAnimatedContent
import androidx.compose.animation.core.DeferredTransitionState
import androidx.compose.animation.core.rememberDeferredTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTransitions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEvent

@Preview
@Sampled
@Composable
fun HorizontalSharedAxisTransitionSample() {
    var targetScreen by remember { mutableIntStateOf(1) }
    var isGestureInProgress by remember { mutableStateOf(false) }
    var backEvent by remember { mutableStateOf(NavigationEvent()) }

    val transitionState = remember { DeferredTransitionState(targetScreen) }
    val transition = rememberDeferredTransition(transitionState)

    LaunchedEffect(isGestureInProgress, targetScreen) {
        if (isGestureInProgress) {
            transitionState.defer(targetScreen)
        } else {
            transitionState.animateTo(targetScreen)
        }
    }

    val scrimColor = MaterialTransitions.scrimColor
    val density = LocalDensity.current

    transition.DeferredAnimatedContent(
        transitionSpec = {
            if (targetState > initialState) {
                MaterialTransitions.HorizontalSharedAxis.forwardTransform()
            } else {
                MaterialTransitions.HorizontalSharedAxis.backwardTransform()
            }
        },
        mutableTransformSpec = {
            if (initialState > targetState) {
                MaterialTransitions.HorizontalSharedAxis.predictiveBackTransform(
                    backEvent = { backEvent },
                    density = density,
                    scrimColor = scrimColor,
                )
            } else {
                null
            }
        },
    ) { screen ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color =
                if (screen == 1) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
        ) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Screen $screen", style = MaterialTheme.typography.headlineLarge)
            }
        }
    }
}

@Preview
@Sampled
@Composable
fun FadeThroughTransitionSample() {
    var targetTab by remember { mutableIntStateOf(1) }
    var isGestureInProgress by remember { mutableStateOf(false) }
    var backEvent by remember { mutableStateOf(NavigationEvent()) }

    val transitionState = remember { DeferredTransitionState(targetTab) }
    val transition = rememberDeferredTransition(transitionState)

    LaunchedEffect(isGestureInProgress, targetTab) {
        if (isGestureInProgress) {
            transitionState.defer(targetTab)
        } else {
            transitionState.animateTo(targetTab)
        }
    }

    val density = LocalDensity.current

    transition.DeferredAnimatedContent(
        transitionSpec = { MaterialTransitions.FadeThrough.transform() },
        mutableTransformSpec = {
            MaterialTransitions.FadeThrough.predictiveBackTransform(
                backEvent = { backEvent },
                density = density,
            )
        },
    ) { tab ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color =
                if (tab == 1) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
        ) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Tab $tab", style = MaterialTheme.typography.headlineLarge)
            }
        }
    }
}
