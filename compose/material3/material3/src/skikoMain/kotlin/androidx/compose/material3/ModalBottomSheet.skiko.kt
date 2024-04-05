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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties


@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun ModalBottomSheetPopup(
    properties: ModalBottomSheetProperties,
    onDismissRequest: () -> Unit,
    windowInsets: WindowInsets,
    content: @Composable () -> Unit,
) {
    Popup(
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ) = IntOffset.Zero
        },
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            usePlatformInsets = false
        )
    ) {
        Box(Modifier.windowInsetsPadding(windowInsets)) {
            content()
        }
    }
}

/**
 * Properties used to customize the behavior of a [ModalBottomSheet].
 *
 * @param isFocusable Whether the modal bottom sheet is focusable. When true,
 * the modal bottom sheet will receive IME events and key presses, such as when
 * the back button is pressed.
 * @param shouldDismissOnBackPress Whether the modal bottom sheet can be dismissed by pressing
 * the back button. If true, pressing the back button will call onDismissRequest.
 * Note that [isFocusable] must be set to true in order to receive key events such as
 * the back button - if the modal bottom sheet is not focusable then this property does nothing.
 */
@ExperimentalMaterial3Api
actual class ModalBottomSheetProperties actual constructor(
    actual val isFocusable: Boolean,
    actual val shouldDismissOnBackPress: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModalBottomSheetProperties) return false

        if (isFocusable != other.isFocusable) return false
        if (shouldDismissOnBackPress != other.shouldDismissOnBackPress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 31 * isFocusable.hashCode()
        result = 31 * result + shouldDismissOnBackPress.hashCode()
        return result
    }
}

/**
 * Default values for [ModalBottomSheet]
 */
@Immutable
@ExperimentalMaterial3Api
actual object ModalBottomSheetDefaults {
    /**
     * Properties used to customize the behavior of a [ModalBottomSheet].
     *
     * @param isFocusable Whether the modal bottom sheet is focusable. When true,
     * the modal bottom sheet will receive IME events and key presses, such as when
     * the back button is pressed.
     * @param shouldDismissOnBackPress Whether the modal bottom sheet can be dismissed by pressing
     * the back button. If true, pressing the back button will call onDismissRequest.
     * Note that [isFocusable] must be set to true in order to receive key events such as
     * the back button - if the modal bottom sheet is not focusable then this property does nothing.
     */
    actual fun properties(
        isFocusable: Boolean,
        shouldDismissOnBackPress: Boolean
    ) = ModalBottomSheetProperties(isFocusable, shouldDismissOnBackPress)
}