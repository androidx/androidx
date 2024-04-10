/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.PlatformMagnifierFactory
import androidx.compose.foundation.isPlatformMagnifierSupported
import androidx.compose.foundation.magnifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

internal actual fun isCopyKeyEvent(keyEvent: KeyEvent): Boolean =
    false //TODO implement copy key event for iPad

internal actual fun Modifier.selectionMagnifier(manager: SelectionManager): Modifier {
    if (!isPlatformMagnifierSupported()) {
        return this
    }

    return composed {
        val density = LocalDensity.current
        var magnifierSize by remember { mutableStateOf(IntSize.Zero) }
        val color = LocalTextSelectionColors.current

        magnifier(
            sourceCenter = {
                // Don't animate position as it is automatically animated by the framework
                calculateSelectionMagnifierCenterAndroid(manager, magnifierSize)
            },
            onSizeChanged = { size ->
                magnifierSize = with(density) {
                    IntSize(size.width.roundToPx(), size.height.roundToPx())
                }
            },
            color = color.handleColor, // align magnifier border color with selection handleColor
            platformMagnifierFactory = PlatformMagnifierFactory.getForCurrentPlatform()
        )
    }
}