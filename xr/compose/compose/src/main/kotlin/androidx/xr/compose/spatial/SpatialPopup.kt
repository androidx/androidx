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

package androidx.xr.compose.spatial

import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.xr.compose.platform.LocalSpatialCapabilities

/**
 * A composable that creates a panel in 3D space to hoist Popup based composables.
 *
 * @param alignment the alignment of the popup relative to its parent.
 * @param offset An offset from the original aligned position of the popup. Offset respects the
 *   Ltr/Rtl context, thus in Ltr it will be added to the original aligned position and in Rtl it
 *   will be subtracted from it.
 * @param onDismissRequest callback invoked when the user requests to dismiss the popup (e.g., by
 *   clicking outside).
 * @param elevation the elevation value of the SpatialPopUp.
 * @param properties [PopupProperties] configuration properties for further customization of this
 *   popup's behavior.
 * @param content the composable content to be displayed within the popup.
 */
@Composable
public fun SpatialPopup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    onDismissRequest: (() -> Unit)? = null,
    elevation: Dp = SpatialElevationLevel.Level3,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit,
) {
    val movableContent = remember { movableContentOf(content) }
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        LayoutSpatialPopup(
            alignment = alignment,
            offset = offset,
            onDismissRequest = onDismissRequest,
            properties = properties,
            elevation = elevation,
            content = movableContent,
        )
    } else {
        Popup(
            alignment = alignment,
            offset = offset,
            onDismissRequest = onDismissRequest,
            properties = properties,
            content = movableContent,
        )
    }
}

/**
 * Composable that creates a panel in 3d space to hoist Popup based composables.
 *
 * @param alignment The alignment relative to the parent.
 * @param offset An offset from the original aligned position of the popup. Offset respects the
 *   Ltr/Rtl context, thus in Ltr it will be added to the original aligned position and in Rtl it
 *   will be subtracted from it.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param elevation the elevation value of the SpatialPopUp.`
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param content The content to be displayed inside the popup.
 */
@Composable
private fun LayoutSpatialPopup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    onDismissRequest: (() -> Unit)? = null,
    elevation: Dp = SpatialElevationLevel.Level3,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit,
) {
    val popupPositioner =
        remember(alignment, offset) { AlignmentOffsetPositionProvider(alignment, offset) }
    LayoutSpatialPopup(
        popupPositionProvider = popupPositioner,
        onDismissRequest = onDismissRequest,
        properties = properties,
        elevation = elevation,
        content = content,
    )
}

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned using a custom [popupPositionProvider].
 *
 * @param popupPositionProvider Provides the screen position of the popup.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param elevation the elevation value of the SpatialPopUp.
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param content The content to be displayed inside the popup.
 */
@Composable
private fun LayoutSpatialPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)? = null,
    elevation: Dp = SpatialElevationLevel.Level3,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit,
) {
    val restingLevel by remember { mutableStateOf(elevation) }
    var contentSize: IntSize by remember { mutableStateOf(IntSize.Zero) }
    var parentLayoutDirection = LocalLayoutDirection.current
    var anchorBounds by remember { mutableStateOf(IntRect.Zero) }
    val fullScreenRect = getWindowVisibleDisplayFrame()
    val windowSize = IntSize(fullScreenRect.width(), fullScreenRect.height())

    val popupOffset by remember {
        derivedStateOf {
            popupPositionProvider.calculatePosition(
                anchorBounds,
                windowSize,
                parentLayoutDirection,
                contentSize,
            )
        }
    }

    BackHandler(enabled = properties.dismissOnBackPress) { onDismissRequest?.invoke() }

    // The coordinates should be re-calculated on every layout to properly retrieve the absolute
    // bounds for popup content offset calculation.
    Layout(
        content = {},
        modifier =
            Modifier.onGloballyPositioned { childCoordinates ->
                val parentCoordinates = childCoordinates.parentLayoutCoordinates!!
                val layoutSize = parentCoordinates.size
                val position = parentCoordinates.positionInWindow()
                val layoutPosition =
                    IntOffset(position.x.fastRoundToInt(), position.y.fastRoundToInt())

                anchorBounds = IntRect(layoutPosition, layoutSize)
            },
    ) { _, _ ->
        parentLayoutDirection = layoutDirection
        layout(0, 0) {}
    }

    ElevatedPanel(
        elevation = restingLevel,
        contentSize = contentSize,
        contentOffset = Offset(popupOffset.x.toFloat(), popupOffset.y.toFloat()),
    ) {
        Box(
            Modifier.onClickOutside(
                    enabled = properties.dismissOnClickOutside,
                    onClickOutside = { onDismissRequest?.invoke() },
                )
                .constrainTo(
                    Constraints(
                        minWidth = 0,
                        maxWidth = Constraints.Infinity,
                        minHeight = 0,
                        maxHeight = Constraints.Infinity,
                    )
                )
                .onSizeChanged { contentSize = it }
        ) {
            content()
        }
    }
}

// Get the visible display Rect for the current window of the device
@Composable
private fun getWindowVisibleDisplayFrame(): Rect {
    return Rect().apply { LocalView.current.getWindowVisibleDisplayFrame(this) }
}

/** Calculates the position of a [Popup] on a screen. */
@Immutable
private interface PopupPositionProvider {
    /**
     * Calculates the position of a [Popup] on screen.
     *
     * The window size is useful in cases where the popup is meant to be positioned next to its
     * anchor instead of inside of it. The size can be used to calculate available space around the
     * parent to find a spot with enough clearance (e.g. when implementing a dropdown). Note that
     * positioning the popup outside of the window bounds might prevent it from being visible.
     *
     * @param anchorBounds the bounds of the anchor layout relative to the window.
     * @param windowSize The size of the window containing the anchor layout.
     * @param layoutDirection The layout direction of the anchor layout.
     * @param popupContentSize The size of the popup's content.
     * @return The window relative position where the popup should be positioned.
     */
    fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset
}

/** Calculates the position of a [Popup] on screen. */
private class AlignmentOffsetPositionProvider(val alignment: Alignment, val offset: IntOffset) :
    PopupPositionProvider {

    /**
     * Calculates the position of a [Popup] on screen.
     *
     * The window size is useful in cases where the popup is meant to be positioned next to its
     * anchor instead of inside of it. The size can be used to calculate available space around the
     * parent to find a spot with enough clearance (e.g. when implementing a dropdown). Note that
     * positioning the popup outside of the window bounds might prevent it from being visible.
     *
     * @param anchorBounds The window relative bounds of the layout which this popup is anchored to.
     * @param windowSize The size of the window containing the anchor layout.
     * @param layoutDirection The layout direction of the anchor layout.
     * @param popupContentSize The size of the popup's content.
     * @return The window relative position where the popup should be positioned.
     */
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchorAlignmentPoint = alignment.align(IntSize.Zero, anchorBounds.size, layoutDirection)
        // Note the negative sign. Popup alignment point contributes negative offset.
        val popupAlignmentPoint = -alignment.align(IntSize.Zero, popupContentSize, layoutDirection)
        val resolvedUserOffset =
            IntOffset(offset.x * (if (layoutDirection == LayoutDirection.Ltr) 1 else -1), offset.y)

        return anchorBounds.topLeft +
            anchorAlignmentPoint +
            popupAlignmentPoint +
            resolvedUserOffset
    }
}
