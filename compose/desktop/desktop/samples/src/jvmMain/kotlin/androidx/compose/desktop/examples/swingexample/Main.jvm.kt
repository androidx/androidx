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

package androidx.compose.desktop.examples.swingexample

import java.awt.Color as awtColor
import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.JPopupTextMenu
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.launchApplication
import androidx.compose.ui.window.rememberWindowState
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.GridLayout
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.CTRL_DOWN_MASK
import java.awt.event.KeyEvent.META_DOWN_MASK
import java.net.URLEncoder
import java.nio.charset.Charset
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JPopupMenu.Separator
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.WindowConstants
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import org.jetbrains.skiko.hostOs

val globalClicks = mutableStateOf(0)

fun main() = SwingUtilities.invokeLater {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    SwingComposeWindow()
}

private typealias SaveableStateData = Map<String, List<Any?>>

private class GlobalSaveableStateRegistry(
    val saveableId: String,
) : SaveableStateRegistry by SaveableStateRegistry(
    restoredValues = map[saveableId],
    canBeSaved = { true }
) {
    fun save() { map[saveableId] = performSave() }
    companion object {
        private val map = mutableMapOf<String, SaveableStateData>()
    }
}

fun createGreenComposePanel() = ComposePanel().also {
    val saveableStateRegistry = GlobalSaveableStateRegistry("GREEN")
    it.background = awtColor(55, 155, 55)
    it.setContent {
        JPopupTextMenuProvider(it) {
            CompositionLocalProvider(
                LocalSaveableStateRegistry provides saveableStateRegistry,
            ) {
                ComposeContent(background = Color(55, 155, 55))
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                saveableStateRegistry.save()
                println("Dispose composition")
            }
        }
    }
}

fun createBlueComposePanel() = ComposePanel().also {
    val saveableStateRegistry = GlobalSaveableStateRegistry("BLUE")
    it.background = awtColor(55, 55, 155)
    it.setContent {
        CustomTextMenuProvider {
            CompositionLocalProvider(
                LocalSaveableStateRegistry provides saveableStateRegistry,
            ) {
                ComposeContent(background = Color(55, 55, 155))
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                saveableStateRegistry.save()
                println("Dispose composition")
            }
        }
    }
}

fun SwingComposeWindow() {
    var composePanel1: ComposePanel? = createGreenComposePanel()
    var composePanel2: ComposePanel? = createBlueComposePanel()

    val window = JFrame()
    window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    window.title = "SwingComposeWindow"

    val panel = JPanel()
    panel.layout = GridLayout(2, 1)
    window.contentPane.add(panel, BorderLayout.CENTER)

    window.contentPane.add(actionButton("WEST", { globalClicks.value++ }), BorderLayout.WEST)
    window.contentPane.add(
        actionButton(
            text = "GREEN",
            size = IntSize(40, 40),
            action = {
                if (composePanel1 != null) {
                    panel.remove(composePanel1)
                    composePanel1 = null
                } else {
                    composePanel1 = createGreenComposePanel()
                    panel.add(composePanel1, 0)
                }
                panel.revalidate()
                panel.repaint()
            }
        ),
        BorderLayout.NORTH
    )
    window.contentPane.add(
        actionButton(
            text = "BLUE",
            size = IntSize(40, 40),
            action = {
                if (composePanel2 != null) {
                    panel.remove(composePanel2)
                    composePanel2 = null
                } else {
                    composePanel2 = createBlueComposePanel()
                    panel.add(composePanel2)
                }
                panel.revalidate()
                panel.repaint()
            }
        ),
        BorderLayout.SOUTH
    )

    // addind ComposePanel on JFrame
    panel.add(composePanel1)
    panel.add(composePanel2)

    window.setSize(800, 600)
    window.isVisible = true
}

fun actionButton(
    text: String,
    action: (() -> Unit)? = null,
    size: IntSize = IntSize(70, 70)
): JButton {
    val button = JButton(text)
    button.toolTipText = "Tooltip for $text button."
    button.preferredSize = Dimension(size.width, size.height)
    button.addActionListener { action?.invoke() }

    return button
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JPopupTextMenuProvider(owner: Component, content: @Composable () -> Unit) {
    val localization = LocalLocalization.current
    CompositionLocalProvider(
        LocalTextContextMenu provides JPopupTextMenu(owner) { textManager, items ->
            JPopupMenu().apply {
                textManager.cut?.also {
                    add(swingItem(localization.cut, java.awt.Color.RED, KeyEvent.VK_X, it))
                }
                textManager.copy?.also {
                    add(swingItem(localization.copy, java.awt.Color.GREEN, KeyEvent.VK_C, it))
                }
                textManager.paste?.also {
                    add(swingItem(localization.paste, java.awt.Color.BLUE, KeyEvent.VK_V, it))
                }
                textManager.selectAll?.also {
                    add(Separator())
                    add(swingItem(localization.selectAll, java.awt.Color.BLACK, KeyEvent.VK_A, it))
                }
                for (item in items) {
                    add(
                        JMenuItem(item.label).apply {
                            addActionListener { item.onClick() }
                        }
                    )
                }
            }
        },
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomTextMenuProvider(content: @Composable () -> Unit) {
    val textMenu = LocalTextContextMenu.current
    val uriHandler = LocalUriHandler.current
    CompositionLocalProvider(
        LocalTextContextMenu provides object : TextContextMenu {
            @Composable
            override fun Area(
                textManager: TextContextMenu.TextManager,
                state: ContextMenuState,
                content: @Composable () -> Unit
            ) {
                ContextMenuDataProvider({
                    val shortText = textManager.selectedText.crop()
                    if (shortText.isNotEmpty()) {
                        val encoded = URLEncoder.encode(shortText, Charset.defaultCharset())
                        listOf(ContextMenuItem("Search $shortText") {
                            uriHandler.openUri("https://google.com/search?q=$encoded")
                        })
                    } else {
                        emptyList()
                    }
                }) {
                    textMenu.Area(textManager, state, content = content)
                }
            }
        },
        content = content
    )
}

private fun AnnotatedString.crop() = if (length <= 5) toString() else "${take(5)}..."

private fun swingItem(
    label: String,
    color: java.awt.Color,
    key: Int,
    onClick: () -> Unit
) = JMenuItem(label).apply {
    icon = circleIcon(color)
    accelerator = KeyStroke.getKeyStroke(key, if (hostOs.isMacOS) META_DOWN_MASK else CTRL_DOWN_MASK)
    addActionListener { onClick() }
}

private fun circleIcon(color: java.awt.Color) = object : Icon {
    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        g.create().apply {
            this.color = color
            translate(16, 2)
            fillOval(0, 0, 16, 16)
        }
    }

    override fun getIconWidth() = 16

    override fun getIconHeight() = 16
}

@Composable
fun ComposeContent(background: Color = Color.White) {
    val rememberClicks = remember { mutableStateOf(0) }
    val rememberSaveableClicks = rememberSaveable { mutableStateOf(0) }
    Box(
        modifier = Modifier.fillMaxSize().background(color = background),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Row(
                modifier = Modifier.height(40.dp)
            ) {
                Button(
                    modifier = Modifier.height(35.dp).padding(top = 3.dp),
                    onClick = {
                        @OptIn(DelicateCoroutinesApi::class)
                        GlobalScope.launchApplication {
                            Window(
                                onCloseRequest = ::exitApplication,
                                state = rememberWindowState(size = DpSize(400.dp, 250.dp))
                            ) {
                                SecondWindowContent()
                            }
                        }
                    }
                ) {
                    Text("New window...", color = Color.White)
                }
                Spacer(modifier = Modifier.width(20.dp))
                SwingPanel(
                    modifier = Modifier.size(200.dp, 39.dp),
                    factory = {
                        actionButton(
                            text = "JComponent",
                            action = {
                                globalClicks.value++
                                rememberClicks.value++
                                rememberSaveableClicks.value++
                            }
                        )
                    },
                    background = background
                )
                Spacer(modifier = Modifier.width(20.dp))
                SwingPanel(
                    background = background,
                    modifier = Modifier.size(200.dp, 39.dp),
                    factory = { ComposableColoredPanel(Color.Red) }
                )
            }
            Spacer(modifier = Modifier.height(50.dp))
            Row {
                Counter("Global", globalClicks)
                Spacer(modifier = Modifier.width(25.dp))
                Counter("Remember", rememberClicks)
                Spacer(modifier = Modifier.width(25.dp))
                Counter("Saveable", rememberSaveableClicks)
                Spacer(modifier = Modifier.width(25.dp))
                Column(modifier = Modifier.width(200.dp)) {
                    SelectionContainer {
                        Column {
                            Text("Text1")
                            Text("Text2")
                        }
                    }
                    var text by remember { mutableStateOf("") }
                    TextField(text, { text = it })
                }
            }
        }
    }
}

fun ComposableColoredPanel(color: Color): Component {
    val composePanel = ComposePanel()

    // setting the content
    composePanel.setContent {
        Box(
            modifier = Modifier.fillMaxSize().background(color = color),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "ColoredPanel")
        }
    }

    return composePanel
}

@Composable
fun Counter(text: String, counter: MutableState<Int>) {
    Surface(
        modifier = Modifier.size(130.dp, 130.dp),
        color = Color(180, 180, 180),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier.height(30.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "${text}Clicks: ${counter.value}")
            }
            Spacer(modifier = Modifier.height(25.dp))
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { counter.value++ }) {
                    Text(text = "\uD83D\uDE80", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ApplicationScope.SecondWindowContent() {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Box(
                modifier = Modifier.height(30.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Second Window")
            }
            Spacer(modifier = Modifier.height(30.dp))
            Box(
                modifier = Modifier.height(30.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { exitApplication() }) {
                    Text("Close")
                }
            }
        }
    }
}
