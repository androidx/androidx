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

package androidx.ui.foundation.demos

import android.app.Activity
import android.os.Bundle
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.Dp
import androidx.ui.core.Text
import androidx.ui.core.TextField
import androidx.ui.core.dp
import androidx.ui.core.setContent
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.Popup
import androidx.ui.foundation.VerticalScroller
import androidx.ui.foundation.disposeActivityComposition
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.input.EditorModel
import androidx.ui.input.EditorStyle
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.layout.Alignment
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.FlexRow
import androidx.ui.layout.FlexSize
import androidx.ui.layout.HeightSpacer
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Wrap
import androidx.ui.text.ParagraphStyle
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextAlign

class PopupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val exampleIndex = +state { 0 }
            val totalExamples = 7

            Column(mainAxisSize = FlexSize.Wrap) {
                FlexRow(
                    mainAxisSize = FlexSize.Expand,
                    mainAxisAlignment = MainAxisAlignment.SpaceBetween
                ) {
                    inflexible {
                        ClickableTextWithBackground(
                            text = "Prev",
                            color = Color.Cyan,
                            onClick = {
                                if (exampleIndex.value == 0) {
                                    exampleIndex.value = totalExamples
                                }

                                exampleIndex.value = (exampleIndex.value - 1) % totalExamples
                            },
                            padding = EdgeInsets(20.dp)
                        )
                    }

                    expanded(flex = 1f) {
                        Container(
                            alignment = Alignment.Center,
                            expanded = true,
                            constraints = DpConstraints(maxWidth = 300.dp)
                        ) {
                            val description: String = {
                                when (exampleIndex.value) {
                                    0 -> "Toggle a simple popup"
                                    1 -> "Different content for the popup"
                                    2 -> "Popup's behavior when the parent's size or position " +
                                            "changes"
                                    3 -> "Aligning the popup inside a parent"
                                    4 -> "[bug] Undesired visual effect caused by" +
                                            " having a new size content displayed at the old" +
                                            " position, until the new one is calculated"
                                    5 -> "[bug] The popup is not aligning to its " +
                                            "parent when the parent is inside a Scroller"
                                    6 -> "[bug] The popup is not repositioned " +
                                            "when the parent is moved by the keyboard"
                                    else -> "Demo description here"
                                }
                            }.invoke()

                            Text(
                                text = description,
                                paragraphStyle = ParagraphStyle(textAlign = TextAlign.Center)
                            )
                        }
                    }

                    inflexible {
                        ClickableTextWithBackground(
                            text = "Next",
                            color = Color.Cyan,
                            onClick = {
                                exampleIndex.value = (exampleIndex.value + 1) % totalExamples
                            },
                            padding = EdgeInsets(20.dp)
                        )
                    }
                }

                when (exampleIndex.value) {
                    0 -> PopupToggle()
                    1 -> PopupWithChangingContent()
                    2 -> PopupWithChangingParent()
                    3 -> PopupAlignmentDemo()
                    4 -> PopupWithChangingSize()
                    5 -> PopupInsideScroller()
                    6 -> PopupOnKeyboardUp()
                }
            }
        }
    }

    // TODO(b/140396932): Replace with Activity.disposeComposition() when it will be working
    //  properly
    override fun onDestroy() {
        disposeActivityComposition(this)
        super.onDestroy()
    }
}

@Composable
fun PopupToggle() {
    val showPopup = +state { true }

    if (showPopup.value) {
        Popup(alignment = Alignment.Center) {
            Wrap {
                DrawShape(CircleShape, Color.Green)
                Container(width = 70.dp, height = 70.dp) {
                    Text(
                        text = "This is a popup!",
                        paragraphStyle = ParagraphStyle(textAlign = TextAlign.Center)
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

@Composable
fun PopupWithChangingContent() {
    Container {
        Column {
            val heightSize = 120.dp
            val widthSize = 160.dp
            val popupContentState = +state { 0 }
            val totalContentExamples = 2
            val popupCounter = +state { 0 }

            ColoredContainer(
                height = heightSize,
                width = widthSize,
                color = Color.Gray
            ) {
                Popup(Alignment.Center) {
                    when (popupContentState.value % totalContentExamples) {
                        0 -> ClickableTextWithBackground(
                            text = "Counter : ${popupCounter.value}",
                            color = Color.Green,
                            onClick = {
                                popupCounter.value += 1
                            }
                        )
                        1 -> Container(
                            width = 60.dp,
                            height = 40.dp
                        ) {
                            DrawShape(CircleShape, Color.Blue)
                        }
                    }
                }
            }

            HeightSpacer(10.dp)
            ClickableTextWithBackground(
                text = "Change content",
                color = Color.Cyan,
                onClick = {
                    popupContentState.value += 1
                }
            )
        }
    }
}

@Composable
fun PopupWithChangingParent() {
    val containerWidth = 400.dp
    val containerHeight = 200.dp
    val parentAlignment = +state { Alignment.TopLeft }
    val parentWidth = +state { 80.dp }
    val parentHeight = +state { 60.dp }
    val parentSizeChanged = +state { false }

    Column {
        Container(
            height = containerHeight,
            width = containerWidth,
            alignment = parentAlignment.value
        ) {
            ColoredContainer(
                width = parentWidth.value,
                height = parentHeight.value,
                color = Color.Blue
            ) {
                Popup(Alignment.BottomCenter) {
                    ColoredContainer(color = Color.Green) {
                        Text("Popup")
                    }
                }
            }
        }
        HeightSpacer(10.dp)
        ClickableTextWithBackground(
            text = "Change parent's position",
            color = Color.Cyan,
            onClick = {
                parentAlignment.value =
                    if (parentAlignment.value == Alignment.TopLeft)
                        Alignment.TopRight
                    else
                        Alignment.TopLeft
            }
        )
        HeightSpacer(10.dp)
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
fun PopupAlignmentDemo() {
    Container(alignment = Alignment.Center) {
        val heightSize = 200.dp
        val widthSize = 400.dp
        val counter = +state { 0 }
        val popupAlignment = +state { Alignment.TopLeft }

        Column {
            ColoredContainer(
                height = heightSize,
                width = widthSize,
                color = Color(0xFFFF0000.toInt()),
                alignment = Alignment.BottomCenter
            ) {
                Popup(popupAlignment.value) {
                    ClickableTextWithBackground(
                        text = "Click to change alignment",
                        color = Color.White,
                        onClick = {
                            counter.value += 1
                            when (counter.value % 9) {
                                0 -> popupAlignment.value = Alignment.TopLeft
                                1 -> popupAlignment.value = Alignment.TopCenter
                                2 -> popupAlignment.value = Alignment.TopRight
                                3 -> popupAlignment.value = Alignment.CenterRight
                                4 -> popupAlignment.value = Alignment.BottomRight
                                5 -> popupAlignment.value = Alignment.BottomCenter
                                6 -> popupAlignment.value = Alignment.BottomLeft
                                7 -> popupAlignment.value = Alignment.CenterLeft
                                8 -> popupAlignment.value = Alignment.Center
                            }
                        }
                    )
                }
            }

            HeightSpacer(10.dp)
            ColoredContainer(color = Color.White) {
                Text("Alignment: " + popupAlignment.value.toString())
            }
        }
    }
}

@Composable
fun PopupWithChangingSize() {
    Container {
        Column {
            val showPopup = +state { true }
            val heightSize = 120.dp
            val widthSize = 160.dp
            val rectangleState = +state { 0 }

            HeightSpacer(15.dp)
            ColoredContainer(
                height = heightSize,
                width = widthSize,
                color = Color.Magenta
            ) {
                if (showPopup.value) {
                    Popup(Alignment.Center) {
                        when (rectangleState.value % 4) {
                            0 -> ColoredContainer(
                                width = 30.dp,
                                height = 30.dp,
                                color = Color.Gray
                            ) {}
                            1 -> ColoredContainer(
                                width = 100.dp,
                                height = 100.dp,
                                color = Color.Gray
                            ) {}
                            2 -> ColoredContainer(
                                width = 30.dp,
                                height = 90.dp,
                                color = Color.Gray
                            ) {}
                            3 -> ColoredContainer(
                                width = 90.dp,
                                height = 30.dp,
                                color = Color.Gray
                            ) {}
                        }
                    }
                }
            }
            HeightSpacer(25.dp)
            ClickableTextWithBackground(
                text = "Change size",
                color = Color.Cyan,
                onClick = {
                    rectangleState.value += 1
                }
            )
        }
    }
}

@Composable
fun PopupInsideScroller() {
    val heightSize = 400.dp
    val widthSize = 200.dp
    Container(width = widthSize, height = heightSize) {
        VerticalScroller {
            Column {
                ColoredContainer(
                    width = 80.dp,
                    height = 160.dp,
                    color = Color(0xFF00FF00.toInt())
                ) {
                    Popup(alignment = Alignment.Center) {
                        ClickableTextWithBackground(text = "Centered", color = Color.Cyan)
                    }
                }

                for (i in 0..30) {
                    Text("Scroll #$i")
                }
            }
        }
    }
}

@Composable
fun PopupOnKeyboardUp() {
    Container {
        Column {
            val widthSize = 190.dp
            val heightSize = 120.dp

            HeightSpacer(350.dp)
            Text("Start typing in the EditText below the parent(Red rectangle)")
            ColoredContainer(
                height = heightSize,
                width = widthSize,
                color = Color.Red
            ) {
                Popup(Alignment.Center) {
                    ColoredContainer(color = Color.Green) {
                        Text("Popup")
                    }
                }
            }

            EditLine(initialText = "Continue typing...", color = Color.Gray)

            HeightSpacer(24.dp)
        }
    }
}

@Composable
fun ClickableTextWithBackground(
    text: String,
    color: Color,
    onClick: (() -> Unit)? = null,
    padding: EdgeInsets = EdgeInsets(0.dp)
) {
    Wrap {
        DrawShape(RectangleShape, color)
        Clickable(onClick = onClick) {
            Container(padding = padding) {
                Text(text)
            }
        }
    }
}

@Composable
fun ColoredContainer(
    width: Dp? = null,
    height: Dp? = null,
    color: Color,
    expanded: Boolean = false,
    alignment: Alignment = Alignment.Center,
    constraints: DpConstraints = DpConstraints(),
    children: @Composable() () -> Unit
) {
    Container(
        width = width,
        height = height,
        alignment = alignment,
        expanded = expanded,
        constraints = constraints
    ) {
        DrawShape(RectangleShape, color)
        children()
    }
}

@Composable
fun EditLine(
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    initialText: String = "",
    color: Color = Color.White
) {
    val state = +state { EditorModel(text = initialText) }
    Wrap {
        DrawShape(RectangleShape, color)
        TextField(
            value = state.value,
            keyboardType = keyboardType,
            imeAction = imeAction,
            onValueChange = { state.value = it },
            editorStyle = EditorStyle(textStyle = TextStyle())
        )
    }
}