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

import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.uikit.LocalNativeTextInputContext
import androidx.compose.ui.unit.DpSize

@OptIn(InternalComposeUiApi::class)
@Composable
internal actual fun SelectionHandle(
    offsetProvider: OffsetProvider,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    minTouchTargetSize: DpSize,
    lineHeight: Float,
    modifier: Modifier
) {
    val nativeInputProvider = LocalNativeTextInputContext.current
    if (nativeInputProvider.usingNativeTextInput()) {
        return // iOS draws selection handles itself.
    }
    SkikoSelectionHandle(offsetProvider, isStartHandle, direction, handlesCrossed, minTouchTargetSize, lineHeight, modifier)
}
