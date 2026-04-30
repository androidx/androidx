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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import platform.UIKit.UIDevice

internal actual fun PlatformSelectionHandleShape(
    density: Density,
    cursor: Rect,
    isStartHandler: Boolean,
): SelectionHandleShape {
    return DefaultSelectionHandleShape(
        density = density,
        cursor = cursor,
        isStartHandler = isStartHandler,
        lineWidth = iosHandleStyle.stemWidth,
        circleRadius = iosHandleStyle.dotDiameter / 2,
    )
}
