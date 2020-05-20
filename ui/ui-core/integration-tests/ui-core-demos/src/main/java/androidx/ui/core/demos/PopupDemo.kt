/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.demos

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.DropDownAlignment
import androidx.ui.core.DropdownPopup
import androidx.ui.core.Modifier
import androidx.ui.core.Popup
import androidx.ui.foundation.TextField
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.Text
import androidx.ui.foundation.TextField
import androidx.ui.foundation.TextFieldValue
import androidx.ui.foundation.VerticalScroller
import androidx.ui.foundation.clickable
import androidx.ui.foundation.drawBackground
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.ColumnScope
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.text.style.TextAlign
import androidx.ui.unit.Dp
import androidx.ui.unit.dp

@Composable
fun PopupDemo() {
    val exampleIndex = state { 0 }
    val totalExamples = 9

    Column {
        Row(
            Modifier.fillMaxWidth().gravity(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            this@Column.ClickableTextWithBackground(
                text = "Prev",
                color = Color.Cyan,
                onClick = {
                    if (exampleIndex.value == 0) {
                        exampleIndex.value = totalExamples
                    }

                    exampleIndex.value = (exampleIndex.value - 1) % totalExamples
                },
                padding = 20.dp
            )

            Box(
                modifier = Modifier.weight(1f),
                gravity = ContentGravity.Center
            ) {
                val description: String = {
                    when (exampleIndex.value) {
                        0 -> "Toggle a simple popup"
                        1 -> "Different content for the popup"
                        2 -> "Popup's behavior when the parent's size or position " +
                                "changes"
                        3 -> "Aligning the popup below the parent"
                        4 -> "Aligning the popup inside a parent"
                        5 -> "Insert an email in the popup and then click outside to " +
                                "dismiss"
                        6 -> "[bug] Undesired visual effect caused by" +
                                " having a new size content displayed at the old" +
                                " position, until the new one is calculated"
                        7 -> "The popup is aligning to its parent when the parent is" +
                                " inside a Scroller"
                        8 -> "[bug] The popup is not repositioned " +
                                "when the parent is moved by the keyboard"
                        else -> "Demo description here"
                    }
                }.invoke()

                Text(
                    text = description,
                    textAlign = TextAlign.Center
                )
            }

            this@Column.ClickableTextWithBackground(
                text = "Next",
                color = Color.Cyan,
                onClick = {
                    exampleIndex.value = (exampleIndex.value + 1) % totalExamples
                },
                padding = 20.dp
            )
        }

        when (exampleIndex.value) {
            0 -> PopupToggle()
            1 -> PopupWithChangingContent()
            2 -> PopupWithChangingParent()
            3 -> PopupDropdownAlignment()
            4 -> PopupAlignmentDemo()
            5 -> PopupWithEditText()
            6 -> PopupWithChangingSize()
            7 -> PopupInsideScroller()
            8 -> PopupOnKeyboardUp()
        }
    }
}

@Composable
private fun ColumnScope.PopupToggle() {
    val showPopup = state { true }

    Column(Modifier.gravity(Alignment.CenterHorizontally)) {
        Box(Modifier.preferredSize(100.dp)) {
            if (showPopup.value) {
                Popup(alignment = Alignment.Center) {
                    Box(
                        Modifier.preferredSize(70.dp),
                        backgroundColor = Color.Green,
                        shape = CircleShape,
                        gravity = ContentGravity.Center
                    ) {
                        Text(
                            text = "This is a popup!",
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        ClickableTextWithBackground(
            text = "Toggle Popup",
            color = Color.Cyan,
            onClick = {
                showPopup.value = !showPopup.value
            }
        )
    }
}

@Composable
private fun ColumnScope.PopupWithChangingContent() {
    Column(Modifier.gravity(Alignment.CenterHorizontally)) {
        val heightSize = 120.dp
        val widthSize = 160.dp
        val popupContentState = state { 0 }
        val totalContentExamples = 2
        val popupCounter = state { 0 }

        Box(Modifier.preferredSize(widthSize, heightSize), backgroundColor = Color.Gray) {
            Popup(Alignment.Center) {
                when (popupContentState.value % totalContentExamples) {
                    0 -> ClickableTextWithBackground(
                        text = "Counter : ${popupCounter.value}",
                        color = Color.Green,
                        onClick = {
                            popupCounter.value += 1
                        }
                    )
                    1 -> Box(
                        Modifier.preferredSize(60.dp, 40.dp),
                        backgroundColor = Color.Blue,
                        shape = CircleShape
                    )
                }
            }
        }

        Spacer(Modifier.preferredHeight(10.dp))
        ClickableTextWithBackground(
            text = "Change content",
            color = Color.Cyan,
            onClick = {
                popupContentState.value += 1
            }
        )
    }
}

@Composable
private fun ColumnScope.PopupWithChangingParent() {
    val containerWidth = 400.dp
    val containerHeight = 200.dp
    val parentGravity = state { ContentGravity.TopStart }
    val parentWidth = state { 80.dp }
    val parentHeight = state { 60.dp }
    val parentSizeChanged = state { false }

    Column(Modifier.gravity(Alignment.CenterHorizontally)) {
        Box(
            Modifier.preferredSize(containerWidth, containerHeight),
            gravity = parentGravity.value
        ) {
            Box(
                Modifier.preferredSize(parentWidth.value, parentHeight.value),
                backgroundColor = Color.Blue
            ) {
                Popup(Alignment.BottomCenter) {
                    Text("Popup", modifier = Modifier.drawBackground(color = Color.Green))
                }
            }
        }
        Spacer(Modifier.preferredHeight(10.dp))
        ClickableTextWithBackground(
            text = "Change parent's position",
            color = Color.Cyan,
            onClick = {
                parentGravity.value =
                    if (parentGravity.value == ContentGravity.TopStart)
                        ContentGravity.TopEnd
                    else
                        ContentGravity.TopStart
            }
        )
        Spacer(Modifier.preferredHeight(10.dp))
        ClickableTextWithBackground(
            text = "Change parent's size",
            color = Color.Cyan,
            onClick = {
                if (parentSizeChanged.value) {
                    parentWidth.value = 80.dp
                    parentHeight.value = 60.dp
                } else {
                    parentWidth.value = 160.dp
                    parentHeight.value = 120.dp
                }
                parentSizeChanged.value = !parentSizeChanged.value
            }
        )
    }
}

@Composable
private fun ColumnScope.PopupDropdownAlignment() {
    Column(Modifier.gravity(Alignment.CenterHorizontally)) {
        val heightSize = 120.dp
        val widthSize = 160.dp
        val dropDownAlignment = state { DropDownAlignment.Start }

        ClickableTextWithBackground(
            text = "Change alignment",
            color = Color.Cyan,
            onClick = {
                dropDownAlignment.value =
                    if (dropDownAlignment.value == DropDownAlignment.Start) {
                        DropDownAlignment.End
                    } else {
                        DropDownAlignment.Start
                    }
            }
        )

        Spacer(Modifier.preferredHeight(10.dp))

        Box(Modifier.preferredSize(widthSize, heightSize), backgroundColor = Color.Gray) {
            DropdownPopup(dropDownAlignment = dropDownAlignment.value) {
                Box(Modifier.preferredSize(40.dp, 70.dp), backgroundColor = Color.Blue)
            }
        }
    }
}

@Composable
private fun ColumnScope.PopupAlignmentDemo() {
    Column(Modifier.gravity(Alignment.CenterHorizontally)) {
        val heightSize = 200.dp
        val widthSize = 400.dp
        val counter = state { 0 }
        val popupAlignment = state { Alignment.TopStart }
        Box(
            modifier = Modifier.preferredSize(widthSize, heightSize),
            backgroundColor = Color.Red,
            gravity = ContentGravity.BottomCenter
        ) {
            Popup(popupAlignment.value) {
                ClickableTextWithBackground(
                    text = "Click to change alignment",
                    color = Color.White,
                    onClick = {
                        counter.value += 1
                        when (counter.value % 9) {
                            0 -> popupAlignment.value = Alignment.TopStart
                            1 -> popupAlignment.value = Alignment.TopCenter
                            2 -> popupAlignment.value = Alignment.TopEnd
                            3 -> popupAlignment.value = Alignment.CenterEnd
                            4 -> popupAlignment.value = Alignment.BottomEnd
                            5 -> popupAlignment.value = Alignment.BottomCenter
                            6 -> popupAlignment.value = Alignment.BottomStart
                            7 -> popupAlignment.value = Alignment.CenterStart
                            8 -> popupAlignment.value = Alignment.Center
                        }
                    }
                )
            }
        }

        Spacer(Modifier.preferredHeight(10.dp))
        Text(
            modifier = Modifier.gravity(Alignment.CenterHorizontally)
                .drawBackground(color = Color.White),
            text = "Alignment : " + popupAlignment.value.toString()
        )
    }
}

@Composable
private fun ColumnScope.PopupWithEditText() {
    Column(Modifier.gravity(Alignment.CenterHorizontally)) {
        val widthSize = 190.dp
        val heightSize = 120.dp
        val editLineSize = 150.dp
        val showEmail = state {
            "Enter your email in the white rectangle and click outside"
        }
        val email = state { "email" }
        val showPopup = state { true }

        Text(text = showEmail.value)

        Box(
            modifier = Modifier.preferredSize(widthSize, heightSize)
                .gravity(Alignment.CenterHorizontally),
            backgroundColor = Color.Red
        ) {
            if (showPopup.value) {
                Popup(
                    alignment = Alignment.Center,
                    isFocusable = true,
                    onDismissRequest = {
                        showEmail.value = "You entered: " + email.value
                        showPopup.value = false
                    }
                ) {
                    EditLine(
                        modifier = Modifier.preferredWidth(editLineSize),
                        initialText = "",
                        color = Color.White,
                        onValueChange = {
                            email.value = it
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.PopupWithChangingSize() {
    Column(Modifier.gravity(Alignment.CenterHorizontally)) {
        val showPopup = state { true }
        val heightSize = 120.dp
        val widthSize = 160.dp
        val rectangleState = state { 0 }

        Spacer(Modifier.preferredHeight(15.dp))
        Box(
            modifier = Modifier.preferredSize(widthSize, heightSize),
            backgroundColor = Color.Magenta
        ) {
            if (showPopup.value) {
                Popup(Alignment.Center) {
                    val size = when (rectangleState.value % 4) {
                        0 -> Modifier.preferredSize(30.dp)
                        1 -> Modifier.preferredSize(100.dp)
                        2 -> Modifier.preferredSize(30.dp, 90.dp)
                        else -> Modifier.preferredSize(90.dp, 30.dp)
                    }
                    Box(modifier = size, backgroundColor = Color.Gray)
                }
            }
        }
        Spacer(Modifier.preferredHeight(25.dp))
        ClickableTextWithBackground(
            text = "Change size",
            color = Color.Cyan,
            onClick = {
                rectangleState.value += 1
            }
        )
    }
}

@Composable
private fun ColumnScope.PopupInsideScroller() {
    VerticalScroller(
        modifier = Modifier.preferredSize(200.dp, 400.dp).gravity(Alignment.CenterHorizontally)
    ) {
        Column(Modifier.fillMaxHeight()) {
            Box(
                modifier = Modifier.preferredSize(80.dp, 160.dp),
                backgroundColor = Color(0xFF00FF00)
            ) {
                Popup(alignment = Alignment.Center) {
                    ClickableTextWithBackground(text = "Centered", color = Color.Cyan)
                }
            }

            for (i in 0..30) {
                Text(text = "Scroll #$i", modifier = Modifier.gravity(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun PopupOnKeyboardUp() {
    Column {
        val widthSize = 190.dp
        val heightSize = 120.dp

        Spacer(Modifier.preferredHeight(350.dp))
        Text("Start typing in the EditText below the parent(Red rectangle)")
        Box(
            modifier = Modifier.preferredSize(widthSize, heightSize)
                .gravity(Alignment.CenterHorizontally),
            backgroundColor = Color.Red
        ) {
            Popup(Alignment.Center) {
                Box(backgroundColor = Color.Green) {
                    Text("Popup")
                }
            }
        }

        EditLine(initialText = "Continue typing...", color = Color.Gray)

        Spacer(Modifier.preferredHeight(24.dp))
    }
}

@Composable
private fun ColumnScope.ClickableTextWithBackground(
    text: String,
    color: Color,
    onClick: (() -> Unit)? = null,
    padding: Dp = 0.dp
) {
    Box(
        Modifier
            .gravity(Alignment.CenterHorizontally)
            .clickable(onClick = onClick ?: {}, enabled = onClick != null),
        backgroundColor = color,
        padding = padding
    ) {
        Text(text)
    }
}

@Composable
private fun EditLine(
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onValueChange: (String) -> Unit = {},
    initialText: String = "",
    color: Color = Color.White
) {
    val state = state { TextFieldValue(initialText) }
    TextField(
        value = state.value,
        modifier = modifier.drawBackground(color = color),
        keyboardType = keyboardType,
        imeAction = imeAction,
        onValueChange = {
            state.value = it
            onValueChange(it.text)
        }
    )
}