/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * Properties used to customize the behavior of a [Popup].
 *
 * @property focusable Whether the popup is focusable. When true, the popup will receive IME
 * events and key presses, such as when the back button is pressed.
 * @property dismissOnBackPress Whether the popup can be dismissed by pressing the back button
 * on Android or escape key on desktop.
 * If true, pressing the back button will call onDismissRequest. Note that [focusable] must be
 * set to true in order to receive key events such as the back button - if the popup is not
 * focusable then this property does nothing.
 * @property dismissOnClickOutside Whether the popup can be dismissed by clicking outside the
 * popup's bounds. If true, clicking outside the popup will call onDismissRequest.
 * @property clippingEnabled Whether to allow the popup window to extend beyond the bounds of the
 * screen. By default the window is clipped to the screen boundaries. Setting this to false will
 * allow windows to be accurately positioned.
 * The default value is true.
 */
@Immutable
expect class PopupProperties(
    focusable: Boolean = false,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    clippingEnabled: Boolean = true,
) {
    val focusable: Boolean
    val dismissOnBackPress: Boolean
    val dismissOnClickOutside: Boolean
    val clippingEnabled: Boolean
}

/**
 * Calculates the position of a [Popup] on screen.
 */
@Immutable
interface PopupPositionProvider {
    /**
     * Calculates the position of a [Popup] on screen.
     *
     * The window size is useful in cases where the popup is meant to be positioned next to its
     * anchor instead of inside of it. The size can be used to calculate available space
     * around the parent to find a spot with enough clearance (e.g. when implementing a dropdown).
     * Note that positioning the popup outside of the window bounds might prevent it from being
     * visible.
     *
     * @param anchorBounds The window relative bounds of the layout which this popup is anchored to.
     * @param windowSize The size of the window containing the anchor layout.
     * @param layoutDirection The layout direction of the anchor layout.
     * @param popupContentSize The size of the popup's content.
     *
     * @return The window relative position where the popup should be positioned.
     */
    fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset
}

internal class AlignmentOffsetPositionProvider(
    val alignment: Alignment,
    val offset: IntOffset
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // TODO: Decide which is the best way to round to result without reimplementing Alignment.align

        val anchorAlignmentPoint = alignment.align(
            IntSize.Zero,
            anchorBounds.size,
            layoutDirection
        )
        // Note the negative sign. Popup alignment point contributes negative offset.
        val popupAlignmentPoint = -alignment.align(
            IntSize.Zero,
            popupContentSize,
            layoutDirection
        )
        val resolvedUserOffset = IntOffset(
            offset.x * (if (layoutDirection == LayoutDirection.Ltr) 1 else -1),
            offset.y
        )

        return anchorBounds.topLeft +
            anchorAlignmentPoint +
            popupAlignmentPoint +
            resolvedUserOffset
    }
}

/**
 * Opens a popup with the given content.
 *
 * A popup is a floating container that appears on top of the current activity.
 * It is especially useful for non-modal UI surfaces that remain hidden until they
 * are needed, for example floating menus like Cut/Copy/Paste.
 *
 * The popup is positioned relative to its parent, using the [alignment] and [offset].
 * The popup is visible as long as it is part of the composition hierarchy.
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param alignment The alignment relative to the parent.
 * @param offset An offset from the original aligned position of the popup. Offset respects the
 * Ltr/Rtl context, thus in Ltr it will be added to the original aligned position and in Rtl it
 * will be subtracted from it.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param content The content to be displayed inside the popup.
 */
@Composable
expect fun Popup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = @OptIn(ExperimentalComposeUiApi::class) PopupProperties(),
    content: @Composable () -> Unit
)

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned using a custom [popupPositionProvider].
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param popupPositionProvider Provides the screen position of the popup.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param content The content to be displayed inside the popup.
 */
@Composable
expect fun Popup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = @OptIn(ExperimentalComposeUiApi::class) PopupProperties(),
    content: @Composable () -> Unit
)
