/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.navigation

import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * Create a [ModalBottomSheetLayout] displaying content from a [BottomSheetNavigator].
 *
 * @param bottomSheetNavigator The navigator that manages the bottom sheet content.
 * @param modifier Optional [Modifier] for the entire component.
 * @param sheetShape The shape of the bottom sheet.
 * @param sheetElevation The elevation of the bottom sheet.
 * @param sheetBackgroundColor The background color of the bottom sheet.
 * @param sheetContentColor The preferred content color provided by the bottom sheet to its
 *   children. Defaults to the matching content color for [sheetBackgroundColor], or if that is not
 *   a color from the theme, this will keep the same content color set above the bottom sheet.
 * @param scrimColor The color of the scrim that is applied to the rest of the screen when the
 *   bottom sheet is visible. If the color passed is [Color.Unspecified], then a scrim will no
 *   longer be applied and the bottom sheet will not block interaction with the rest of the screen
 *   when visible.
 * @param content The content of rest of the screen.
 * @sample androidx.compose.material.navigation.samples.BottomSheetNavDemo
 * @see [ModalBottomSheetLayout]
 */
@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Suppress("MissingJvmstatic")
@Composable
// Keep defaults in sync with androidx.compose.material.ModalBottomSheetLayout
public fun ModalBottomSheetLayout(
    bottomSheetNavigator: BottomSheetNavigator,
    modifier: Modifier = Modifier,
    sheetShape: Shape = MaterialTheme.shapes.large,
    sheetElevation: Dp = ModalBottomSheetDefaults.Elevation,
    sheetBackgroundColor: Color = MaterialTheme.colors.surface,
    sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
    scrimColor: Color = ModalBottomSheetDefaults.scrimColor,
    content: @Composable () -> Unit,
) {
    ModalBottomSheetLayout(
        sheetState = bottomSheetNavigator.sheetState,
        sheetContent = bottomSheetNavigator.sheetContent,
        modifier = modifier,
        sheetShape = sheetShape,
        sheetElevation = sheetElevation,
        sheetBackgroundColor = sheetBackgroundColor,
        sheetContentColor = sheetContentColor,
        scrimColor = scrimColor,
        content = content,
    )
}

/**
 * Create a [ModalBottomSheetLayout] displaying content from a [BottomSheetNavigator].
 *
 * @param bottomSheetNavigator The navigator that manages the bottom sheet content.
 * @param modifier Optional [Modifier] for the entire component.
 * @param sheetGesturesEnabled Whether the bottom sheet can be interacted with by gestures.
 * @param sheetShape The shape of the bottom sheet.
 * @param sheetElevation The elevation of the bottom sheet.
 * @param sheetBackgroundColor The background color of the bottom sheet.
 * @param sheetContentColor The preferred content color provided by the bottom sheet to its
 *   children. Defaults to the matching content color for [sheetBackgroundColor], or if that is not
 *   a color from the theme, this will keep the same content color set above the bottom sheet.
 * @param scrimColor The color of the scrim that is applied to the rest of the screen when the
 *   bottom sheet is visible. If the color passed is [Color.Unspecified], then a scrim will no
 *   longer be applied and the bottom sheet will not block interaction with the rest of the screen
 *   when visible.
 * @param content The content of rest of the screen.
 * @sample androidx.compose.material.navigation.samples.BottomSheetNavDemo
 * @see [ModalBottomSheetLayout]
 */
@Suppress("MissingJvmstatic")
@Composable
// Keep defaults in sync with androidx.compose.material.ModalBottomSheetLayout
public fun ModalBottomSheetLayout(
    bottomSheetNavigator: BottomSheetNavigator,
    modifier: Modifier = Modifier,
    sheetGesturesEnabled: Boolean = true,
    sheetShape: Shape = MaterialTheme.shapes.large,
    sheetElevation: Dp = ModalBottomSheetDefaults.Elevation,
    sheetBackgroundColor: Color = MaterialTheme.colors.surface,
    sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
    scrimColor: Color = ModalBottomSheetDefaults.scrimColor,
    content: @Composable () -> Unit,
) {
    ModalBottomSheetLayout(
        sheetState = bottomSheetNavigator.sheetState,
        sheetContent = bottomSheetNavigator.sheetContent,
        modifier = modifier,
        sheetGesturesEnabled = sheetGesturesEnabled,
        sheetShape = sheetShape,
        sheetElevation = sheetElevation,
        sheetBackgroundColor = sheetBackgroundColor,
        sheetContentColor = sheetContentColor,
        scrimColor = scrimColor,
        content = content,
    )
}
