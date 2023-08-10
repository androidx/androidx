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

package androidx.compose.desktop.examples.popupexample

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CursorDropdownMenu
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JButton

@Composable
fun WindowScope.Content(
    windowState: WindowState,
    trayState: TrayState,
) {
    val dialogState = remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
        color = Color(55, 55, 55)
    ) {
        Column {
            Row(
                modifier = Modifier.background(color = Color(75, 75, 75))
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(start = 20.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WindowDraggableArea(
                    modifier = Modifier.weight(1f)
                ) {
                    TextBox(text = AppState.wndTitle.value)
                }
                Row {
                    Button(
                        color = Color(210, 210, 210),
                        modifier = Modifier.size(16.dp, 16.dp),
                        onClick = {
                            windowState.placement = WindowPlacement.Fullscreen
                        }
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Button(
                        color = Color(232, 182, 109),
                        modifier = Modifier.size(16.dp, 16.dp),
                        onClick = {
                            windowState.isMinimized = true
                        }
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Button(
                        color = Color(150, 232, 150),
                        modifier = Modifier.size(16.dp, 16.dp),
                        onClick = {
                            windowState.placement = WindowPlacement.Maximized
                        }
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Button(
                        onClick = AppState::closeMainWindow,
                        color = Color(232, 100, 100),
                        modifier = Modifier.size(16.dp, 16.dp),
                    )
                }
            }
            Row {
                Column(modifier = Modifier.padding(start = 30.dp, top = 30.dp)) {
                    Button("Show Popup", { AppState.popupState.value = true }, Color(232, 182, 109))
                    Spacer(modifier = Modifier.height(30.dp))
                    Button("Open dialog", { dialogState.value = true })
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        text = "New window...",
                        onClick = AppState::openSecondaryWindow,
                        color = Color(26, 198, 188)
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        text = "Send notification",
                        onClick = {
                            val message = "There should be your message."
                            when (AppState.notificationType.value) {
                                NotificationType.Notify -> trayState.sendNotification(
                                    Notification("Notification.", message)
                                )
                                NotificationType.Warn -> trayState.sendNotification(
                                    Notification("Warning.", message, Notification.Type.Warning)
                                )
                                NotificationType.Error -> trayState.sendNotification(
                                    Notification("Error.", message, Notification.Type.Error)
                                )
                            }
                        },
                        color = Color(196, 136, 255)
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button("Increment amount", { AppState.amount.value++ }, Color(150, 232, 150))
                    Spacer(modifier = Modifier.height(30.dp))
                    Button("Exit app", AppState::closeAll, Color(232, 100, 100))
                    Spacer(modifier = Modifier.height(30.dp))
                    SwingActionButton("JButton", { AppState.amount.value++ })
                }
                Column(
                    modifier = Modifier.padding(start = 30.dp, top = 50.dp, end = 30.dp)
                        .background(color = Color(255, 255, 255, 10))
                        .fillMaxWidth()
                ) {
                    Row {
                        ContextMenu()
                        Spacer(modifier = Modifier.width(30.dp))
                        TextFieldWithSuggestions()
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    Column(modifier = Modifier.padding(start = 20.dp)) {
                        RadioButton(
                            text = "- common dialog",
                            value = DialogType.Common,
                            state = AppState.dialogType
                        )
                        RadioButton(
                            text = "- window dialog",
                            value = DialogType.Window,
                            state = AppState.dialogType
                        )
                        RadioButton(
                            text = "- alert dialog",
                            value = DialogType.Alert,
                            state = AppState.dialogType
                        )
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    CheckBox(
                        text = "- undecorated",
                        state = AppState.undecorated,
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Column(modifier = Modifier.padding(start = 20.dp)) {
                        RadioButton(
                            text = "- notify",
                            value = NotificationType.Notify,
                            state = AppState.notificationType
                        )
                        RadioButton(
                            text = "- warn",
                            value = NotificationType.Warn,
                            state = AppState.notificationType
                        )
                        RadioButton(
                            text = "- error",
                            value = NotificationType.Error,
                            state = AppState.notificationType
                        )
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    Row(modifier = Modifier.padding(start = 20.dp)) {
                        TextBox(text = "Amount: ${AppState.amount.value}")
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier.background(color = Color(32, 32, 32))
                .fillMaxWidth()
                .height(30.dp)
        ) {
            Row(modifier = Modifier.padding(start = 20.dp)) {
                TextBox(
                    text = "Size: ${windowState.size}   Location: ${windowState.position}"
                )
            }
        }
    }

    PopupSample(
        displayed = AppState.popupState.value,
        onDismiss = {
            AppState.popupState.value = false
            println("Popup is dismissed.")
        }
    )
    if (AppState.popupState.value) {
        // To make sure the popup is displayed on the top.
        Box(
            Modifier.fillMaxSize().background(color = Color(0, 0, 0, 200))
        )
    }

    if (dialogState.value) {
        val dismiss = {
            dialogState.value = false
            println("Dialog window is dismissed.")
        }
        @OptIn(ExperimentalMaterialApi::class)
        when (AppState.dialogType.value) {
            DialogType.Common -> Dialog(
                onDismissRequest = dismiss
            ) {
                DialogContent(
                    amount = AppState.amount,
                    onClose = dismiss
                )
            }
            DialogType.Window -> DialogWindow(
                onCloseRequest = dismiss
            ) {
                WindowContent(
                    amount = AppState.amount,
                    onClose = dismiss
                )
            }
            DialogType.Alert -> AlertDialog(
                onDismissRequest = dismiss,
                confirmButton = {
                    Button(text = "OK", onClick = { AppState.amount.value++ })
                },
                dismissButton = {
                    Button(text = "Cancel", onClick = dismiss)
                },
                title = {
                    TextBox(text = "Alert Dialog")
                },
                text = {
                    println("CompositionLocal value is ${LocalTest.current}.")
                    TextBox(text = "Increment amount?")
                    DisposableEffect(Unit) {
                        onDispose {
                            println("onDispose inside AlertDialog is called.")
                        }
                    }
                },
                shape = RoundedCornerShape(0.dp),
                backgroundColor = Color(70, 70, 70)
            )
        }
    }
}

@Composable
fun PopupSample(displayed: Boolean, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (displayed) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, 50),
                focusable = true,
                onDismissRequest = onDismiss
            ) {
                println("CompositionLocal value is ${LocalTest.current}.")
                PopupContent(onDismiss)
                DisposableEffect(Unit) {
                    onDispose {
                        println("onDispose inside Popup is called.")
                    }
                }
            }
        }
    }
}

@Composable
fun PopupContent(onDismiss: () -> Unit) {
    Box(
        Modifier.size(300.dp, 150.dp).background(color = Color(65, 65, 65)),
        contentAlignment = Alignment.Center
    ) {
        Column {
            TextBox(text = "Popup demo.")
            Spacer(modifier = Modifier.height(30.dp))
            Button("Dismiss", { onDismiss.invoke() })
        }
    }
}

@Composable
fun DialogContent(modifier: Modifier = Modifier, amount: MutableState<Int>, onClose: () -> Unit) {
    Box(
        modifier.background(color = Color(55, 55, 55)).padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column {
            TextBox(text = "Increment amount?")
            Spacer(modifier = Modifier.height(30.dp))
            Row {
                Button(text = "Yes", onClick = { amount.value++ }, modifier = Modifier.width(100.dp))
                Spacer(modifier = Modifier.width(30.dp))
                Button(text = "Close", onClick = { onClose.invoke() }, modifier = Modifier.width(100.dp))
            }
        }
    }
}

@Composable
fun WindowContent(amount: MutableState<Int>, onClose: () -> Unit) {
    DialogContent(
        modifier = Modifier.fillMaxSize(),
        amount, onClose
    )
}

@Composable
fun Button(
    text: String = "",
    onClick: () -> Unit = {},
    color: Color = Color(10, 162, 232),
    modifier: Modifier = Modifier.width(200.dp)
) {
    @OptIn(ExperimentalFoundationApi::class)
    TooltipArea(
        tooltip = {
            Surface(
                color = Color(210, 210, 210),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(text = "Tooltip: [$text]", modifier = Modifier.padding(10.dp))
            }
        }
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = color
            ),
            modifier = modifier
        ) {
            Text(text = text)
        }
    }
}

@Composable
fun TextBox(text: String = "", modifier: Modifier = Modifier.height(30.dp)) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(200, 200, 200)
        )
    }
}

@Composable
@OptIn(
    ExperimentalFoundationApi::class
)
fun ContextMenu() {
    val items = listOf("Item A", "Item B", "Item C", "Item D", "Item E", "Item F")
    val showMenu = remember { mutableStateOf(false) }
    val selectedIndex = remember { mutableStateOf(0) }
    TooltipArea(
        delayMillis = 100,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopStart,
            alignment = Alignment.TopEnd
        ),
        tooltip = {
            Surface(
                color = Color(210, 210, 210),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(text = "Tooltip: [ContextMenu]", modifier = Modifier.padding(10.dp))
            }
        }
    ) {
        Surface(
            color = Color(255, 255, 255, 40),
            shape = RoundedCornerShape(4.dp)
        ) {
            TextBox(
                text = "Selected: ${items[selectedIndex.value]}",
                modifier = Modifier
                    .height(35.dp)
                    .padding(start = 4.dp, end = 4.dp)
                    .clickable(onClick = { showMenu.value = true })
            )
            CursorDropdownMenu(
                expanded = showMenu.value,
                onDismissRequest = { showMenu.value = false }
            ) {
                items.forEachIndexed { index, name ->
                    DropdownMenuItem(
                        onClick = {
                            selectedIndex.value = index
                            showMenu.value = false
                        }
                    ) {
                        Text(text = name)
                    }
                }
            }
        }
    }
}

@Composable
fun TextFieldWithSuggestions() {
    Surface(
        color = Color(255, 255, 255, 40),
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = Modifier.size(200.dp, 35.dp).padding(5.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val text = remember { mutableStateOf("") }
            val words = remember { listOf("Hi!", "walking", "are", "home", "world") }
            val showMenu = remember { mutableStateOf(false) }
            BasicTextField(
                textStyle = TextStyle.Default.copy(color = Color(200, 200, 200)),
                value = text.value,
                singleLine = true,
                onValueChange = {
                    text.value = it
                    if (text.value.isNotEmpty())
                        showMenu.value = true
                    else
                        showMenu.value = false
                },
                modifier = Modifier.height(14.dp),
            )
            DropdownMenu(
                expanded = showMenu.value,
                onDismissRequest = {},
                focusable = false
            ) {
                words.forEach { name ->
                    DropdownMenuItem(onClick = { text.value += name }) {
                        Text(text = name)
                    }
                }
            }
        }
    }
}

@Composable
fun CheckBox(text: String, state: MutableState<Boolean>) {
    Row {
        Box(
            modifier = Modifier.height(35.dp),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = state.value,
                onCheckedChange = {
                    state.value = !state.value
                },
                modifier = Modifier.padding(start = 20.dp, bottom = 5.dp)
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        TextBox(text = text)
    }
}

@Composable
fun <T> RadioButton(text: String, value: T, state: MutableState<T>) {
    Row(
        modifier = Modifier.height(35.dp).padding(start = 20.dp, bottom = 5.dp),
    ) {
        RadioButton(
            selected = value == state.value,
            onClick = {
                state.value = value
            }
        )
        Spacer(modifier = Modifier.width(5.dp))
        TextBox(text = text)
    }
}

@Composable
fun SwingActionButton(text: String, action: (() -> Unit)? = null) {
    SwingPanel(
        background = Color(55, 55, 55),
        modifier = Modifier.size(200.dp, 35.dp),
        factory = {
            JButton(text).apply {
                addActionListener(object : ActionListener {
                    public override fun actionPerformed(e: ActionEvent) {
                        action?.invoke()
                    }
                })
            }
        },
        update = { component ->
            component.setText("$text:${AppState.amount.value}")
        }
    )
}

@Composable
fun ApplicationScope.SecondaryWindow(onCloseRequest: () -> Unit) = Window(
    onCloseRequest = onCloseRequest,
    state = rememberWindowState(size = DpSize(400.dp, 200.dp)),
    undecorated = AppState.undecorated.value,
) {
    WindowContent(
        amount = AppState.amount,
        onClose = onCloseRequest
    )
    DisposableEffect(Unit) {
        onDispose {
            println("Dispose composition")
        }
    }
}
