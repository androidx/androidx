/*
 * Copyright 2025 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.backhandler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

@Deprecated("Use NavigationEventHandler instead")
@ExperimentalComposeUiApi
@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    var progressChannel: Channel<BackEventCompat>? by remember(onBack) {
        mutableStateOf(null)
    }

    fun getActiveProgressChannel(): Channel<BackEventCompat> {
        val currentProgressChannel = progressChannel
        if (currentProgressChannel == null) {
            val progress = Channel<BackEventCompat>()
            progressChannel = progress
            coroutineScope.launch {
                onBack(progress.consumeAsFlow())
            }
            return progress
        } else {
            return currentProgressChannel
        }
    }

    val transitionState = navEventState.transitionState
    if (transitionState is NavigationEventTransitionState.InProgress) {
        LaunchedEffect(transitionState) {
            val navEvent = transitionState.latestEvent
            val swipeEdge = when (navEvent.swipeEdge) {
                NavigationEvent.EDGE_RIGHT -> BackEventCompat.EDGE_RIGHT
                else -> BackEventCompat.EDGE_LEFT
            }
            val event = BackEventCompat(
                navEvent.touchX, navEvent.touchY, navEvent.progress, swipeEdge
            )
            getActiveProgressChannel().send(event)
        }
    }

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = enabled,
        onBackCancelled = {
            getActiveProgressChannel().close(CancellationException("Cancelled"))
            progressChannel = null
        },
        onBackCompleted = {
            getActiveProgressChannel().close()
            progressChannel = null
        }
    )
    DisposableEffect(Unit) {
        onDispose {
            progressChannel?.close(CancellationException("Disposed"))
            progressChannel = null
        }
    }
}

@Deprecated("Use NavigationEventHandler instead")
@ExperimentalComposeUiApi
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = enabled,
        onBackCompleted = onBack
    )
}
