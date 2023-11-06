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
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This modifier provides functionality to increment or decrement values repeatedly
 * by holding down the composable.
 * Should be used instead of clickable modifier to achieve clickable and repeatable
 * clickable behavior. Can't be used along with clickable modifier as it already implements it.
 *
 * Callbacks [onClick] and [onRepeatableClick] are different. [onClick] is triggered only
 * when the hold duration is shorter than [initialDelay] and no repeatable clicks happened.
 * [onRepeatableClick] is repeatedly triggered when the hold duration is longer
 * than [initialDelay] with [incrementalDelay] intervals.
 *
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 * [PressInteraction.Press] when this clickable is pressed. Only the initial (first) press will be
 * recorded and dispatched with [MutableInteractionSource].
 * @param indication indication to be shown when modified element is pressed. By default,
 * indication from [LocalIndication] will be used. Pass `null` to show no indication, or
 * current value from [LocalIndication] to show theme default
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will
 * appear disabled for accessibility services
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param initialDelay The initial delay before the click starts repeating, in ms
 * @param incrementalDelay The delay between each repeated click, in ms
 * @param onClick will be called when user clicks on the element
 * @param onRepeatableClick will be called after the [initialDelay] with [incrementalDelay]
 * between each call until the touch is released
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Modifier.repeatableClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    initialDelay: Long = 500L,
    incrementalDelay: Long = 60L,
    onClick: () -> Unit,
    onRepeatableClick: () -> Unit = onClick
): Modifier = composed {
    val currentOnRepeatableClick by rememberUpdatedState(onRepeatableClick)
    val currentOnClick by rememberUpdatedState(onClick)
    // This flag is used for checking whether the onClick should be ignored or not.
    // If this flag is true, then it means that repeatable click happened and onClick
    // shouldn't be triggered.
    var ignoreOnClick by remember { mutableStateOf(false) }

    // Repeatable logic should always follow the clickable, as the lowest modifier finishes first,
    // and we have to be sure that repeatable goes before clickable.
    clickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = {
            if (!ignoreOnClick) {
                currentOnClick()
            }
            ignoreOnClick = false
        },
    )
        .pointerInput(enabled) {
            coroutineScope {
                awaitEachGesture {
                    awaitFirstDown()
                    ignoreOnClick = false
                    val repeatingJob = launch {
                        delay(initialDelay)
                        ignoreOnClick = true
                        while (enabled) {
                            currentOnRepeatableClick()
                            delay(incrementalDelay)
                        }
                    }
                    // Waiting for up or cancellation of the gesture.
                    waitForUpOrCancellation()
                    repeatingJob.cancel()
                }
            }
        }
}
