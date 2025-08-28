/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.SuppressLint
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.internal.DropdownMenuPositionProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
actual fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    offset: DpOffset,
    scrollState: ScrollState,
    properties: PopupProperties,
    shape: Shape,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    border: BorderStroke?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = expanded

    if (expandedState.currentState || expandedState.targetState) {
        val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }
        val density = LocalDensity.current
        val popupPositionProvider =
            remember(offset, density) {
                DropdownMenuPositionProvider(offset, density) { parentBounds, menuBounds ->
                    transformOriginState.value = calculateTransformOrigin(parentBounds, menuBounds)
                }
            }

        Popup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = popupPositionProvider,
            properties = properties,
        ) {
            DropdownMenuContent(
                expandedState = expandedState,
                transformOriginState = transformOriginState,
                scrollState = scrollState,
                shape = shape,
                containerColor = containerColor,
                tonalElevation = tonalElevation,
                shadowElevation = shadowElevation,
                border = border,
                modifier = modifier,
                content = content,
            )
        }
    }
}

@Deprecated(
    level = DeprecationLevel.HIDDEN,
    replaceWith =
        ReplaceWith(
            expression =
                "DropdownMenu(\n" +
                    "    expanded = expanded,\n" +
                    "    onDismissRequest = onDismissRequest,\n" +
                    "    modifier = modifier,\n" +
                    "    offset = offset,\n" +
                    "    scrollState = scrollState,\n" +
                    "    properties = properties,\n" +
                    "    shape = MenuDefaults.shape,\n" +
                    "    containerColor = MenuDefaults.containerColor,\n" +
                    "    tonalElevation = MenuDefaults.TonalElevation,\n" +
                    "    shadowElevation = MenuDefaults.ShadowElevation,\n" +
                    "    border = null,\n" +
                    "    content = content,\n" +
                    ")"
        ),
    message =
        "Maintained for binary compatibility. Use overload with parameters for shape, " +
            "color, elevation, and border.",
)
@Composable
@SuppressLint("ComposableNaming")
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) =
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        shape = MenuDefaults.shape,
        containerColor = MenuDefaults.containerColor,
        tonalElevation = MenuDefaults.TonalElevation,
        shadowElevation = MenuDefaults.ShadowElevation,
        border = null,
        content = content,
    )

@Deprecated(
    level = DeprecationLevel.HIDDEN,
    replaceWith =
        ReplaceWith(
            expression =
                "DropdownMenu(expanded,onDismissRequest, modifier, offset, " +
                    "rememberScrollState(), properties, content)",
            "androidx.compose.foundation.rememberScrollState",
        ),
    message = "Replaced by a DropdownMenu function with a ScrollState parameter",
)
@Composable
@SuppressLint("ComposableNaming")
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) =
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = rememberScrollState(),
        properties = properties,
        content = content,
    )

@Composable
actual fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    enabled: Boolean,
    colors: MenuItemColors,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource?,
) {
    DropdownMenuItemContent(
        text = text,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )
}

internal actual val DefaultMenuProperties = PopupProperties(focusable = true)
