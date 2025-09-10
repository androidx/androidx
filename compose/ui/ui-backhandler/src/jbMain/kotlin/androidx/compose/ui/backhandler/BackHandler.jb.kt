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

package androidx.compose.ui.backhandler

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationEventHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Deprecated("Use NavigationEventHandler instead")
@ExperimentalComposeUiApi
@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit
) {
    LocalNavigationEventDispatcherOwner.current ?: return
    NavigationEventHandler(enabled) { progress ->
        val compatProgress = progress.map { navEvent ->
            BackEventCompat(navEvent.touchX, navEvent.touchY, navEvent.progress, navEvent.swipeEdge)
        }
        onBack(compatProgress)
    }
}

@Deprecated("Use NavigationEventHandler instead")
@ExperimentalComposeUiApi
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    PredictiveBackHandler(enabled) { progress ->
        try {
            progress.collect { /*ignore*/ }
            onBack()
        } catch (e: CancellationException) {
            //ignore
        }
    }
}
