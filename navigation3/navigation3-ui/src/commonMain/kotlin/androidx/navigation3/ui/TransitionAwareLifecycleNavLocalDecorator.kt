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

package androidx.navigation3.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleOwner
import androidx.navigation3.runtime.navEntryDecorator

@Composable
internal fun rememberTransitionAwareLifecycleNavEntryDecorator(backStack: List<Any>) =
    remember(backStack) {
        navEntryDecorator<Any> { entry ->
            val isSettled = LocalNavTransitionSettledState.current
            val isInBackStack = entry.isInBackStack(backStack)
            val maxLifecycle =
                when {
                    isInBackStack && isSettled -> Lifecycle.State.RESUMED
                    isInBackStack && !isSettled -> Lifecycle.State.STARTED
                    else /* !isInBackStack */ -> Lifecycle.State.CREATED
                }
            LifecycleOwner(maxLifecycle = maxLifecycle) { entry.Content() }
        }
    }

internal val LocalNavTransitionSettledState: ProvidableCompositionLocal<Boolean> =
    compositionLocalOf {
        // If there is no transition state available, assume we are settled already
        true
    }
