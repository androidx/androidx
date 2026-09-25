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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.remote.core.operations.layout.modifiers.RippleModifierOperation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
internal fun Modifier.ripple(
    @Suppress("UNUSED_PARAMETER") op: RippleModifierOperation,
    hasClickModifier: Boolean = false,
): Modifier {
    // In remote-core, ClickModifierOperation and MultiClickModifier animate a ripple on click by
    // default, and ClickModifier in the embedded player preserves Compose's default ripple on
    // Modifier.clickable / combinedClickable. When a click modifier is present on the same
    // component, a separate RippleModifierOperation is redundant.
    if (hasClickModifier) return this

    // For a standalone RippleModifierOperation (no click modifier), attach ripple indication and
    // emit press interactions directly from pointer input rather than using a no-op clickable {}
    // that would expose a spurious accessibility click action.
    val interactionSource = remember { MutableInteractionSource() }
    return this.indication(interactionSource, ripple()).pointerInput(interactionSource) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val press = PressInteraction.Press(down.position)
            interactionSource.tryEmit(press)
            val up = waitForUpOrCancellation()
            if (up != null) {
                interactionSource.tryEmit(PressInteraction.Release(press))
            } else {
                interactionSource.tryEmit(PressInteraction.Cancel(press))
            }
        }
    }
}
