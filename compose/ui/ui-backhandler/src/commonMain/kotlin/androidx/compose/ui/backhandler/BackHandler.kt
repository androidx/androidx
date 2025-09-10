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
import kotlinx.coroutines.flow.Flow

/**
 * An effect for handling predictive system back gestures.
 *
 * Calling this in your composable adds the given lambda to the BackGestureDispatcher.
 * The lambda passes in a Flow<BackEventCompat> where each
 * [BackEventCompat] reflects the progress of current gesture back. The lambda content should follow
 * this structure:
 * ```
 * PredictiveBackHandler { progress: Flow<BackEventCompat> ->
 *      // code for gesture back started
 *      try {
 *         progress.collect { backevent ->
 *              // code for progress
 *         }
 *         // code for completion
 *      } catch (e: CancellationException) {
 *         // code for cancellation
 *      }
 * }
 * ```
 *
 * If this is called by nested composables, if enabled, the inner most composable will consume the
 * call to system back and invoke its lambda. The call will continue to propagate up until it finds
 * an enabled BackHandler.
 *
 * @param enabled if this BackHandler should be enabled, true by default
 * @param onBack the action invoked by back gesture
 */
@Deprecated("Use NavigationEventHandler instead")
@ExperimentalComposeUiApi
@Composable
expect fun PredictiveBackHandler(
    enabled: Boolean = true,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit
)

/**
 * An effect for handling the back event.
 *
 * Calling this in your composable adds the given lambda to the BackGestureDispatcher.
 *
 * If this is called by nested composables, if enabled, the inner most composable will consume the
 * call to system back and invoke its lambda. The call will continue to propagate up until it finds
 * an enabled BackHandler.
 *
 * @param enabled if this BackHandler should be enabled
 * @param onBack the action invoked by system back event
 */
@Deprecated("Use NavigationEventHandler instead")
@ExperimentalComposeUiApi
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
