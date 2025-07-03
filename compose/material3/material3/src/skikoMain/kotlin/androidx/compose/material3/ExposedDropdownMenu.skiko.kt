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

package androidx.compose.material3

import androidx.compose.material3.internal.rememberAccessibilityServiceState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.window.PopupProperties

internal actual class WindowBoundsCalculator(private val windowInfo: WindowInfo) {
    actual fun getVisibleWindowBounds(): IntRect = windowInfo.containerSize.toIntRect()
}

@Composable
internal actual fun platformWindowBoundsCalculator(): WindowBoundsCalculator {
    val windowInfo = LocalWindowInfo.current
    return remember(windowInfo) { WindowBoundsCalculator(windowInfo) }
}

@Composable
internal actual fun OnPlatformWindowBoundsChange(block: () -> Unit) {
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.containerSize }
            .collect { block() }
    }
}

@Composable
internal actual fun popupPropertiesForAnchorType(
    anchorType: ExposedDropdownMenuAnchorType,
    alwaysFocusable: Boolean,
): PopupProperties {
    val a11yServicesEnabled by rememberAccessibilityServiceState()

    // If typing on the IME is required, the menu should not be focusable
    // in order to prevent stealing focus from the input method.
    val imeRequired =
        anchorType == ExposedDropdownMenuAnchorType.PrimaryEditable ||
            (anchorType == ExposedDropdownMenuAnchorType.SecondaryEditable && !a11yServicesEnabled)
    return PopupProperties(
        focusable = !imeRequired || alwaysFocusable
    )
}
