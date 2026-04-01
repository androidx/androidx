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
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.flow.Flow

@OptIn(InternalComposeUiApi::class)
@Deprecated("Use NavigationEventHandler instead")
@ExperimentalComposeUiApi
@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit
) {
    val owner = LocalNavigationEventDispatcherOwner.current ?: error(
        "No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner"
    )
    val dispatcher = owner.navigationEventDispatcher
    val coroutineScope = rememberCoroutineScope()
    val compositeKey = currentCompositeKeyHashCode
    val handler = remember(compositeKey, onBack) {
        ProgressBackEventHandler(compositeKey, enabled, onBack, coroutineScope)
    }
    handler.isBackEnabled = enabled

    DisposableEffect(dispatcher, handler) {
        dispatcher.addHandler(handler)
        onDispose { handler.remove() }
    }
}

@OptIn(InternalComposeUiApi::class)
@Deprecated("Use NavigationEventHandler instead")
@ExperimentalComposeUiApi
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    val owner = LocalNavigationEventDispatcherOwner.current ?: error(
        "No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner"
    )
    val dispatcher = owner.navigationEventDispatcher
    val compositeKey = currentCompositeKeyHashCode
    val handler = remember(compositeKey, onBack) {
        BackEventHandler(compositeKey, enabled, onBack)
    }
    handler.isBackEnabled = enabled

    DisposableEffect(dispatcher, handler) {
        dispatcher.addHandler(handler)
        onDispose { handler.remove() }
    }
}
