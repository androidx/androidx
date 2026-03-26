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

package androidx.compose.material3

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * [Material Design modal bottom sheet](https://m3.material.io/components/bottom-sheets/overview)
 *
 * Modal bottom sheets are used as an alternative to inline menus or simple dialogs on mobile,
 * especially when offering a long list of action items, or when items require longer descriptions
 * and icons. Like dialogs, modal bottom sheets appear in front of app content, disabling all other
 * app functionality when they appear, and remaining on screen until confirmed, dismissed, or a
 * required action has been taken.
 *
 * ![Bottom sheet
 * image](https://developer.android.com/images/reference/androidx/compose/material3/bottom_sheet.png)
 *
 * A simple example of a modal bottom sheet looks like this:
 *
 * @sample androidx.compose.material3.samples.ModalBottomSheetSample
 * @param onDismissRequest Executes when the user clicks outside of the bottom sheet, after sheet
 *   animates to [Hidden].
 * @param modifier Optional [Modifier] for the bottom sheet.
 * @param sheetState The state of the bottom sheet.
 * @param sheetMaxWidth [Dp] that defines what the maximum width the sheet will take. Pass in
 *   [Dp.Unspecified] for a sheet that spans the entire screen width.
 * @param sheetGesturesEnabled Whether the bottom sheet can be interacted with by gestures.
 * @param shape The shape of the bottom sheet.
 * @param containerColor The color used for the background of this bottom sheet
 * @param contentColor The preferred color for content inside this bottom sheet. Defaults to either
 *   the matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param scrimColor Color of the scrim that obscures content when the bottom sheet is open.
 * @param dragHandle Optional visual marker to swipe the bottom sheet.
 * @param contentWindowInsets callback which provides window insets to be passed to the bottom sheet
 *   content via [androidx.compose.foundation.layout.windowInsetsPadding]. [ModalBottomSheet] will
 *   pre-emptively consume top insets based on it's current offset. This keeps content outside of
 *   the expected window insets at any position.
 * @param properties [ModalBottomSheetProperties] for further customization of this modal bottom
 *   sheet's window behavior.
 * @param content The content to be displayed inside the bottom sheet.
 */
@Composable
@ExperimentalMaterial3Api
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val animateToDismiss: () -> Unit = {
        if (sheetState.confirmValueChange(Hidden)) {
            scope
                .launch { sheetState.hide() }
                .invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismissRequest()
                    }
                }
        }
    }

    val settleToDismiss: () -> Unit = {
        if (sheetState.currentValue == Expanded && sheetState.hasPartiallyExpandedState) {
            // Smoothly animate away predictive back transformations since we are not fully
            // dismissing. We don't need to do this in the else below because we want to
            // preserve the predictive back transformations (scale) during the hide animation.
            scope.launch { sheetState.partialExpand() }
        } else { // Is expanded without collapsed state or is collapsed.
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
        }
    }

    ModalBottomSheetDialog(
        properties = properties,
        contentColor = contentColor,
        onDismissRequest = settleToDismiss,
    ) {
        Box(modifier = Modifier.fillMaxSize().imePadding().semantics { isTraversalGroup = true }) {
            val sheetWindowInsets = remember(sheetState) { SheetWindowInsets(sheetState) }
            val isScrimVisible: Boolean by remember {
                derivedStateOf { sheetState.targetValue != Hidden }
            }
            val scrimAlpha by
                animateFloatAsState(
                    targetValue = if (isScrimVisible) 1f else 0f,
                    animationSpec = MotionSchemeKeyTokens.DefaultEffects.value(),
                    label = "ScrimAlphaAnimation",
                )
            Scrim(
                contentDescription = getString(Strings.CloseSheet),
                onClick = if (properties.shouldDismissOnClickOutside) animateToDismiss else null,
                alpha = { scrimAlpha },
                color = scrimColor,
            )
            BottomSheet(
                modifier = modifier.align(TopCenter).consumeWindowInsets(sheetWindowInsets),
                state = sheetState,
                onDismissRequest = onDismissRequest,
                maxWidth = sheetMaxWidth,
                gesturesEnabled = sheetGesturesEnabled,
                backHandlerEnabled = properties.shouldDismissOnBackPress,
                shape = shape,
                containerColor = containerColor,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                dragHandle = dragHandle,
                contentWindowInsets = contentWindowInsets,
                content = content,
            )
        }
    }
    if (sheetState.hasExpandedState) {
        LaunchedEffect(sheetState) { sheetState.show() }
    }
}

/**
 * Properties used to customize the behavior of a [ModalBottomSheet].
 *
 * @param shouldDismissOnBackPress Whether the modal bottom sheet can be dismissed by pressing the
 *   back button. If true, pressing the back button will call onDismissRequest.
 * @param shouldDismissOnClickOutside Whether the modal bottom sheet can be dismissed by clicking on
 *   the scrim.
 */
@Immutable
@ExperimentalMaterial3Api
expect class ModalBottomSheetProperties(
    shouldDismissOnBackPress: Boolean = true,
    shouldDismissOnClickOutside: Boolean = true,
) {
    val shouldDismissOnBackPress: Boolean
    val shouldDismissOnClickOutside: Boolean
}

/** Default values for [ModalBottomSheet] */
@Immutable
@ExperimentalMaterial3Api
expect object ModalBottomSheetDefaults {

    /** Properties used to customize the behavior of a [ModalBottomSheet]. */
    val properties: ModalBottomSheetProperties
}

/**
 * Create and [remember] a [SheetState] for [ModalBottomSheet].
 *
 * @param skipPartiallyExpanded Whether the partially expanded state, if the sheet is tall enough,
 *   should be skipped. If true, the sheet will always expand to the [Expanded] state and move to
 *   the [Hidden] state when hiding the sheet, either programmatically or by user interaction.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterial3Api
fun rememberModalBottomSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
) =
    rememberSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
        confirmValueChange = confirmValueChange,
        initialValue = Hidden,
    )

@Stable
@OptIn(ExperimentalMaterial3Api::class)
internal class SheetWindowInsets(private val state: SheetState) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int = 0

    override fun getTop(density: Density): Int {
        val offset = state.anchoredDraggableState.offset
        return if (offset.isNaN()) 0 else offset.toInt().coerceAtLeast(0)
    }

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int = 0

    override fun getBottom(density: Density): Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SheetWindowInsets) return false
        return state == other.state
    }

    override fun hashCode(): Int = state.hashCode()
}

/**
 * [Dialog]-like component providing default window behavior for [BottomSheet]. This implementation
 * explicitly provides a full-screen edge to edge layout.
 *
 * The dialog is visible as long as it is part of the composition hierarchy. In order to let the
 * user dismiss the Dialog, the implementation of onDismissRequest should contain a way to remove
 * the dialog from the composition hierarchy.
 *
 * You can add implement a custom [ModalBottomSheet] by leveraging this API alongside [BottomSheet],
 * [draggableAnchoredSheet], and [Scrim]:
 *
 * @sample androidx.compose.material3.samples.ManualModalBottomSheetSample
 * @param onDismissRequest Callback which executes when user tries to dismiss
 *   [ModalBottomSheetDialog].
 * @param contentColor The content color of this dialog. Used to inform the default behavior of the
 *   windows' system bars and content.
 * @param properties [ModalBottomSheetProperties] for further customization of this dialog.
 * @param content The content displayed in this [ModalBottomSheetDialog]. Usually [BottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal expect fun ModalBottomSheetDialog(
    onDismissRequest: () -> Unit = {},
    contentColor: Color = contentColorFor(BottomSheetDefaults.ContainerColor),
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable () -> Unit,
)
