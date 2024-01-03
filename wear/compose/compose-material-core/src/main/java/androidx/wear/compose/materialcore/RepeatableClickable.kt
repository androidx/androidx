/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.materialcore

import androidx.annotation.RestrictTo
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This modifier provides functionality to increment or decrement values repeatedly
 * by holding down the composable
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Modifier.repeatableClickable(
    enabled: Boolean,
    initialDelay: Long = 500L,
    incrementalDelay: Long = 60L,
    onClick: () -> Unit
): Modifier = composed {

    val currentOnClick by rememberUpdatedState(onClick)

    pointerInput(enabled) {
        coroutineScope {
            awaitEachGesture {
                awaitFirstDown()
                val repeatingJob = launch {
                    delay(initialDelay)
                    while (enabled) {
                        currentOnClick()
                        delay(incrementalDelay)
                    }
                }
                waitForUpOrCancellation()
                repeatingJob.cancel()
            }
        }
    }
}
