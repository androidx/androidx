/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.text.JPopupTextMenu
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItemWithComposableLeadingIcon
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition
import java.awt.Component
import java.awt.MouseInfo
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

/**
 * Representation of a context menu that is suitable for light themes of the application.
 */
val LightDefaultContextMenuRepresentation = DefaultContextMenuRepresentation(
    backgroundColor = Color.White,
    textColor = Color.Black,
    itemHoverColor = Color.Black.copy(alpha = 0.04f)
)

/**
 * Representation of a context menu that is suitable for dark themes of the application.
 */
val DarkDefaultContextMenuRepresentation = DefaultContextMenuRepresentation(
    backgroundColor = Color(0xFF121212), // like surface in darkColors
    textColor = Color.White,
    itemHoverColor = Color.White.copy(alpha = 0.04f)
)

/**
 * Custom representation of a context menu that allows to specify different colors.
 *
 * @param backgroundColor Color of a context menu background.
 * @param textColor Color of the text in a context menu
 * @param itemHoverColor Color of an item background when we hover it.
 */
class DefaultContextMenuRepresentation(
    private val backgroundColor: Color,
    private val textColor: Color,
    private val itemHoverColor: Color,
    private val disabledTextColor: Color = textColor.copy(alpha = 0.38f),
) : ContextMenuRepresentation {
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status
        if (status !is ContextMenuState.Status.Open) return

        val session = remember(state) {
            object : TextContextMenuSession {
                override fun close() {
                    state.status = ContextMenuState.Status.Closed
                }
            }
        }
        val components by remember {
            derivedStateOf {
                items().map {
                    TextContextMenuItemWithComposableLeadingIcon(
                        key = it,
                        label = it.label,
                        enabled = it.enabled,
                        onClick = {
                            session.close()
                            it.onClick()
                        }
                    )
                }
            }
        }

        if (components.isEmpty()) {
            SideEffect { session.close() }
        } else {
            val colors = remember(backgroundColor, textColor, itemHoverColor, disabledTextColor) {
                ContextMenuColors(
                    backgroundColor = backgroundColor,
                    textColor = textColor,
                    iconColor = textColor,
                    disabledTextColor = disabledTextColor,
                    disabledIconColor = disabledTextColor,
                    hoverColor = itemHoverColor,
                )
            }
            DefaultOpenContextMenu(
                session = session,
                components = components,
                popupPositionProvider = rememberPopupPositionProviderAtPosition(status.rect.center),
                colors = colors,
            )
        }
    }
}

/**
 * [ContextMenuRepresentation] that uses [JPopupMenu] to show a context menu for [ContextMenuArea].
 *
 * You can use it by overriding [LocalContextMenuRepresentation] on the top level of your application.
 *
 * See also [JPopupTextMenu] that allows more specific customization for the text context menu.
 *
 * @param owner The root component that owns a context menu. Usually it is [ComposeWindow] or [ComposePanel].
 * @param createMenu Describes how to create [JPopupMenu] from list of [ContextMenuItem]
 */
@ExperimentalFoundationApi
class JPopupContextMenuRepresentation(
    private val owner: Component,
    private val createMenu: (List<ContextMenuItem>) -> JPopupMenu = { items ->
        JPopupMenu().apply {
            for (item in items) {
                add(
                    JMenuItem(item.label).apply {
                        addActionListener { item.onClick() }
                    }
                )
            }
        }
    },
) : ContextMenuRepresentation {
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val isOpen = state.status is ContextMenuState.Status.Open
        if (isOpen) {
            val menu = remember {
                createMenu(items()).apply {
                    addPopupMenuListener(object : PopupMenuListener {
                        override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) = Unit

                        override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
                            state.status = ContextMenuState.Status.Closed
                        }

                        override fun popupMenuCanceled(e: PopupMenuEvent?) = Unit
                    })
                }
            }

            DisposableEffect(Unit) {
                val mousePosition = MouseInfo.getPointerInfo().location
                SwingUtilities.convertPointFromScreen(mousePosition, owner)
                menu.show(owner, mousePosition.x, mousePosition.y)
                onDispose {
                    menu.isVisible = false
                }
            }
        }
    }
}