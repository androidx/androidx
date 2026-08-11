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

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.remote.core.operations.layout.modifiers.RippleModifierOperation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
internal fun Modifier.ripple(op: RippleModifierOperation): Modifier {
    // Provide ripple visual feedback via Modifier.indication rather than a no-op `clickable {}`.
    // A bare clickable would create a focusable, actionable accessibility node that announces and
    // does nothing; `indication` attaches the ripple without a spurious a11y action. A co-located
    // ClickModifier supplies the press interactions that actually drive the ripple.
    val interactionSource = remember { MutableInteractionSource() }
    return this.indication(interactionSource, ripple())
}
