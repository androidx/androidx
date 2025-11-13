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

package androidx.compose.ui.interop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.findNodeWithLabel
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UIContextMenuConfiguration
import platform.UIKit.UIContextMenuInteraction
import platform.UIKit.UIContextMenuInteractionAnimatingProtocol
import platform.UIKit.UIContextMenuInteractionDelegateProtocol
import platform.UIKit.UIMenu

internal class InteropUIMenuTest {

    @Test
    fun testUIMenuDismissByTapOnComposeView() = runUIKitInstrumentedTestWithInterop(
        ignoreIf = !available(OS.Ios to OSVersion(16)),
        ignoreNotes = "The test does not receive touches on iOS < 15 when the context menu opened"
    ) { overlay ->
        var isMenuOpen: () -> Boolean = { false }

        setContent {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(Color.Green)
                        .testTag("Box")
                )
                UIKitView(
                    factory = {
                        val button = ContextMenuButton()
                        isMenuOpen = { button.isMenuOpen }
                        button
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("MenuButton"),
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        findNodeWithTag("MenuButton")
            .tap()

        delay(800)

        assertTrue(isMenuOpen())

        findNodeWithTag("Box")
            .tap()

        delay(800)

        assertFalse(isMenuOpen())
    }

    @Test
    fun testUIMenuEmbeddedInComposeViewDismissByTapOnOtherComposeView() = runUIKitInstrumentedTestWithInterop(
        ignoreIf = !available(OS.Ios to OSVersion(16)),
        ignoreNotes = "The test does not receive touches on iOS < 15 when the context menu opened"
    ) { overlay ->
        var isMenuOpen: () -> Boolean = { false }

        setContent {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(Color.Green)
                        .testTag("Box")
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    UIKitView(
                        factory = {
                            val button = ContextMenuButton()
                            isMenuOpen = { button.isMenuOpen }
                            button
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("MenuButton"),
                        properties = UIKitInteropProperties(placedAsOverlay = overlay)
                    )
                }
            }
        }

        findNodeWithTag("MenuButton")
            .tap()

        delay(800)

        assertTrue(isMenuOpen())

        findNodeWithTag("Box")
            .tap()

        delay(800)

        assertFalse(isMenuOpen())
    }

    @Test
    fun testUIMenuDismissByTapOnUIButton() = runUIKitInstrumentedTestWithInterop(
        ignoreIf = !available(OS.Ios to OSVersion(16)),
        ignoreNotes = "The test does not receive touches on iOS < 15 when the context menu opened"
    ) { overlay ->
        var isMenuOpen: () -> Boolean = { false }

        setContent {
            Column {
                UIKitView(
                    factory = {
                        val button = UIButton()
                        button.backgroundColor = UIColor.yellowColor
                        button
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .testTag("UIButton"),
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
                UIKitView(
                    factory = {
                        val button = ContextMenuButton()
                        isMenuOpen = { button.isMenuOpen }
                        button
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("MenuButton"),
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        findNodeWithTag("MenuButton")
            .tap()

        delay(800)

        assertTrue(isMenuOpen())

        findNodeWithTag("UIButton")
            .tap()

        delay(800)

        assertFalse(isMenuOpen())
    }

    @Test
    fun testUIMenuDismissByTapOnUIAction() = runUIKitInstrumentedTestWithInterop(
        ignoreIf = !available(OS.Ios to OSVersion(16)),
        ignoreNotes = "The test does not receive touches on iOS < 15 when the context menu opened"
    ) { overlay ->
        var isMenuOpen: () -> Boolean = { false }

        setContent {
            UIKitView(
                factory = {
                    val button = ContextMenuButton()
                    isMenuOpen = { button.isMenuOpen }
                    button
                },
                modifier = Modifier.fillMaxWidth().height(400.dp).testTag("MenuButton"),
                properties = UIKitInteropProperties(placedAsOverlay = overlay)
            )
        }

        findNodeWithTag("MenuButton")
            .tap()

        delay(800)

        assertTrue(isMenuOpen())

        findNodeWithLabel("MenuItem2")
            .tap()

        delay(800)

        assertFalse(isMenuOpen())
    }
}

@OptIn(ExperimentalForeignApi::class)
private class ContextMenuButton(
    var isMenuOpen: Boolean = false,
): UIButton(frame = CGRectZero.readValue()), UIContextMenuInteractionDelegateProtocol {
    init {
        backgroundColor = UIColor.redColor
        showsMenuAsPrimaryAction = true
        menu = UIMenu()
    }

    override fun contextMenuInteraction(
        interaction: UIContextMenuInteraction,
        configurationForMenuAtLocation: CValue<CGPoint>
    ): UIContextMenuConfiguration {
        isMenuOpen = true
        return UIContextMenuConfiguration.configurationWithIdentifier(
            identifier = null,
            previewProvider = null,
            actionProvider = {
                UIMenu.menuWithChildren(
                    listOf(
                        UIAction.actionWithTitle("MenuItem1", null, null) {},
                        UIAction.actionWithTitle("MenuItem2", null, null) {},
                        UIAction.actionWithTitle("MenuItem3", null, null) {},
                    )
                )
            }
        )
    }

    override fun contextMenuInteraction(
        interaction: UIContextMenuInteraction,
        willEndForConfiguration: UIContextMenuConfiguration,
        animator: UIContextMenuInteractionAnimatingProtocol?
    ) {
        isMenuOpen = false
    }
}