/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.ui.core.Semantics
import androidx.ui.core.gesture.PressReleasedGestureDetector
import androidx.ui.core.semantics.SemanticsAction
import androidx.ui.core.semantics.SemanticsActionType
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer

/**
 * Combines [PressReleasedGestureDetector] and [Semantics] for the clickable
 * components like Button.
 *
 * @param onClick will be called when user clicked on the button. The button will not be
 *  clickable when it is null.
 * @param consumeDownOnStart true means [PressReleasedGestureDetector] should consume
 *  down events. Provide false if you have some visual feedback like Ripples,
 *  as it will consume this events instead.
 */
@Composable
fun Clickable(
    onClick: (() -> Unit)? = null,
    consumeDownOnStart: Boolean = false,
    @Children children: @Composable() () -> Unit
) {
    Semantics(
        button = true,
        enabled = (onClick != null),
        actions = if (onClick != null) {
            // TODO(ryanmentley): The unnecessary generic type specification works around an IR bug
            listOf<SemanticsAction<*>>(SemanticsAction(SemanticsActionType.Tap, onClick))
        } else {
            emptyList<SemanticsAction<*>>()
        }
    ) {
        PressReleasedGestureDetector(
            onRelease = onClick,
            consumeDownOnStart = consumeDownOnStart
        ) {
            children()
        }
    }
}