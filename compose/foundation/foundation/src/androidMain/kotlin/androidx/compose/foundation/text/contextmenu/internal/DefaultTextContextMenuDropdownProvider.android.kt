/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.contextmenu.internal

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.contextmenu.ContextMenuColumnBuilder
import androidx.compose.foundation.contextmenu.ContextMenuPopupPositionProvider
import androidx.compose.foundation.contextmenu.ContextMenuScope
import androidx.compose.foundation.contextmenu.ContextMenuSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSeparator
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuTextClassificationItem
import androidx.compose.foundation.text.contextmenu.internal.TextClassificationHelperApi28.sendLegacyIntent
import androidx.compose.foundation.text.contextmenu.internal.TextClassificationHelperApi28.sendPendingIntent
import androidx.compose.foundation.text.contextmenu.internal.TextContextMenuHelperApi28.textClassificationItem
import androidx.compose.foundation.text.contextmenu.provider.BasicTextContextMenuProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.ProvideBasicTextContextMenu
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.basicTextContextMenuProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

// TODO(grantapher) Consider making public.
@Composable
internal fun ProvideDefaultTextContextMenuDropdown(content: @Composable () -> Unit) {
    ProvideBasicTextContextMenu(
        providableCompositionLocal = LocalTextContextMenuDropdownProvider,
        contextMenu = { session, dataProvider, anchorLayoutCoordinates ->
            OpenContextMenu(session, dataProvider, anchorLayoutCoordinates)
        },
        content = content,
    )
}

@Composable
internal fun ProvideDefaultTextContextMenuDropdown(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    ProvideBasicTextContextMenu(
        modifier = modifier,
        providableCompositionLocal = LocalTextContextMenuDropdownProvider,
        contextMenu = { session, dataProvider, anchorLayoutCoordinates ->
            OpenContextMenu(session, dataProvider, anchorLayoutCoordinates)
        },
        content = content,
    )
}

@Composable
internal fun defaultTextContextMenuDropdown(): BasicTextContextMenuProvider =
    basicTextContextMenuProvider { session, dataProvider, anchorLayoutCoordinates ->
        OpenContextMenu(session, dataProvider, anchorLayoutCoordinates)
    }

private val DefaultPopupProperties = PopupProperties(focusable = true)

@Composable
private fun OpenContextMenu(
    session: TextContextMenuSession,
    dataProvider: TextContextMenuDataProvider,
    anchorLayoutCoordinates: () -> LayoutCoordinates,
) {
    val popupPositionProvider =
        remember(dataProvider) {
            MaintainWindowPositionPopupPositionProvider(
                ContextMenuPopupPositionProvider({
                    dataProvider.position(anchorLayoutCoordinates()).round()
                })
            )
        }

    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = { session.close() },
        properties = DefaultPopupProperties,
    ) {
        val data by remember(dataProvider) { derivedStateOf(dataProvider::data) }
        DefaultTextContextMenuDropdown(session, data)
    }
}

@Composable
private fun DefaultTextContextMenuDropdown(
    session: TextContextMenuSession,
    data: TextContextMenuData,
) {
    val context =
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            LocalContext.current
        } else {
            null
        }
    ContextMenuColumnBuilder {
        data.components.fastForEach { component ->
            when (component) {
                is TextContextMenuItem ->
                    item(
                        label = { component.label },
                        leadingIcon =
                            if (component.leadingIcon == Resources.ID_NULL) {
                                null
                            } else {
                                { color -> IconBox(component.leadingIcon, color) }
                            },
                        onClick = { component.onClick(session) },
                    )
                is TextContextMenuTextClassificationItem ->
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        textClassificationItem(context, component)
                    }
                is TextContextMenuSeparator -> separator()
            }
        }
    }
}

// Lift of relevant M3 Icon parts.
@Composable
private fun IconBox(@DrawableRes resId: Int, tint: Color) {
    val context = LocalContext.current
    val drawableResourceId =
        remember(context, resId) {
            context
                .obtainStyledAttributes(intArrayOf(resId))
                .getResourceId(/* index= */ 0, /* defValue= */ -1)
        }
    if (drawableResourceId == -1) return

    val painter = painterResource(drawableResourceId)
    val colorFilter = remember(tint) { if (tint.isUnspecified) null else ColorFilter.tint(tint) }
    Box(
        Modifier.size(ContextMenuSpec.IconSize)
            .paint(painter, colorFilter = colorFilter, contentScale = ContentScale.Fit)
    )
}

/**
 * Delegates to the [popupPositionProvider], but re-uses the previous calculated position if the
 * only change is the `anchorBounds` in the window. This ensures that anchor layout movement such as
 * scrolls do not cause the popup to move, but other relevant layout changes do move the popup.
 *
 * We do want to re-calculate a new position for any `windowSize`, `layoutDirection`, and
 * `popupContentSize` changes since they may make the previous popup position un-viable.
 */
// TODO(grantapher) Consider making public.
private class MaintainWindowPositionPopupPositionProvider(
    val popupPositionProvider: PopupPositionProvider
) : PopupPositionProvider {
    var previousWindowSize: IntSize? = null
    var previousLayoutDirection: LayoutDirection? = null
    var previousPopupContentSize: IntSize? = null

    var previousPosition: IntOffset? = null

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val position = previousPosition
        if (
            position != null &&
                previousWindowSize == windowSize &&
                previousLayoutDirection == layoutDirection &&
                previousPopupContentSize == popupContentSize
        ) {
            return position
        }

        val newPosition =
            popupPositionProvider.calculatePosition(
                anchorBounds,
                windowSize,
                layoutDirection,
                popupContentSize,
            )

        previousWindowSize = windowSize
        previousLayoutDirection = layoutDirection
        previousPopupContentSize = popupContentSize
        previousPosition = newPosition
        return newPosition
    }
}

@RequiresApi(28)
private object TextContextMenuHelperApi28 {
    @Suppress("DEPRECATION")
    fun ContextMenuScope.textClassificationItem(
        context: Context?,
        component: TextContextMenuTextClassificationItem,
    ) {
        if (context == null) return
        val index = component.index
        val textClassification = component.textClassification
        if (index < 0) {
            item(
                label = { textClassification.label.toString() },
                leadingIcon = textClassification.icon?.let { icon -> { color -> IconBox(icon) } },
                onClick = { sendLegacyIntent(context, textClassification) },
            )
        } else {
            val action = textClassification.actions[index]
            val isPrimary = index == 0
            item(
                label = { action.title.toString() },
                leadingIcon =
                    if (isPrimary || action.shouldShowIcon()) {
                        { IconBox(action.icon) }
                    } else {
                        null
                    },
                onClick = { sendPendingIntent(action.actionIntent) },
            )
        }
    }

    @Composable
    private fun IconBox(icon: Icon) {
        val context = LocalContext.current
        val drawable = remember(icon, context) { icon.loadDrawable(context) } ?: return
        IconBox(drawable)
    }

    @Composable
    private fun IconBox(drawable: Drawable) {
        Box(
            Modifier.size(ContextMenuSpec.IconSize).drawBehind {
                drawIntoCanvas { canvas ->
                    drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                    drawable.draw(canvas.nativeCanvas)
                }
            }
        )
    }
}
