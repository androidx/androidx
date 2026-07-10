/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.compose.modifiers

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * A custom modifier that handles click gestures without making the component focusable.
 *
 * This is a low-level replacement for the standard `clickable` modifier, used specifically when you
 * need to capture a tap event but want to prevent the component from gaining focus, which is the
 * default behavior of `clickable`.
 *
 * @param interactionSource The [MutableInteractionSource] that will be used to observe press state.
 * @param indication The [Indication] to show when the component is pressed. Pass `null` to disable
 *   the indication. Defaults to [LocalIndication].
 * @param onClick The lambda to be executed when a tap gesture is detected.
 */
@Composable
fun Modifier.clickableWithoutFocus(
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = LocalIndication.current,
    onClick: () -> Unit,
): Modifier {
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()

    return this.hoverable(interactionSource = internalInteractionSource)
        .indication(interactionSource = internalInteractionSource, indication = indication)
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    scope.launch { internalInteractionSource.emit(press) }

                    val success = tryAwaitRelease()

                    val release =
                        if (success) {
                            PressInteraction.Release(press)
                        } else {
                            PressInteraction.Cancel(press)
                        }

                    scope.launch { internalInteractionSource.emit(release) }
                },
                onTap = { onClick() },
            )
        }
}
