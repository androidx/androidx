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

package androidx.compose.foundation.style

import androidx.compose.animation.core.Spring.StiffnessHigh
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.Test
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationStyleApi::class)
class StyleUxTaskTests {
    @get:Rule val rule = createComposeRule(effectContext = StandardTestDispatcher())

    @Test
    fun task1() = task {
        val boxStyle = Style {
            border(3.dp, Color.Magenta)
            contentPadding(10.dp)
        }
        Box(modifier = Modifier.styleable(null, boxStyle)) { Text("Box with style") }
    }

    @Test
    fun task2() = task {
        Box(modifier = Modifier.styleable { background(Color.Red) }) { Text("Red box") }
    }

    @Test
    fun task3() = task {
        Box(
            modifier =
                Modifier.styleable {
                    contentPadding(20.dp)
                    size(100.dp)
                }
        ) {
            Text("Padded box")
        }
    }

    @Test
    fun task4() = task {
        Box(
            modifier =
                Modifier.styleable {
                    shape(CircleShape)
                    border(2.dp, Color.Blue)
                    background(Color.LightGray)
                    size(100.dp)
                }
        )
    }

    @Test
    fun task5() = task {
        Box(
            modifier =
                Modifier.styleable {
                    contentPadding(12.dp)
                    shape(RoundedCornerShape(10.dp))
                    background(Color.Magenta)
                    border(1.dp, Color.Black)
                    size(150.dp)
                }
        )
    }

    @Test
    fun task6() = task {
        Column(
            modifier =
                Modifier.height(200.dp).styleable {
                    background(Color.Gray)
                    contentPadding(16.dp)
                },
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text("Top")
            Text("Bottom")
        }
    }

    @Test
    fun task7() = interactiveTask {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier.width(200.dp).styleable {
                    background(Color.Blue)
                    contentPadding(16.dp)
                },
        ) {
            Text("Left", style = { contentColor(Color.White) })
            Text("Right", style = { contentColor(Color.White) })
        }
    }

    @Test
    fun task7_a() = interactiveTask {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier.width(200.dp).styleable {
                    background(Color.Blue)
                    contentPadding(16.dp)
                    contentColor(Color.White)
                },
        ) {
            Text("Left")
            Text("Right")
        }
    }

    @Test
    fun task8() = interactiveTask {
        val mis = remember { MutableInteractionSource() }
        val styleState = remember(mis) { MutableStyleState(mis) }
        Box(
            modifier =
                Modifier.clickable(interactionSource = mis, indication = null) {}
                    .styleable(styleState) {
                        background(Color.LightGray)
                        size(100.dp)
                        shape(CircleShape)
                        pressed { background(Color.Blue) }
                    },
            contentAlignment = Alignment.Center,
        ) {
            Text("Click Me")
        }
    }

    @Test
    fun task9() = interactiveTask {
        val mis = remember { MutableInteractionSource() }
        val styleState = remember(mis) { MutableStyleState(mis) }
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(500)
            focusRequester.requestFocus()
        }
        Box(
            modifier =
                Modifier.clickable(interactionSource = mis) {}
                    .focusRequester(focusRequester)
                    .focusable(interactionSource = mis)
                    .styleable(styleState) {
                        background(Color.White)
                        border(2.dp, Color.Black)
                        size(100.dp)
                        focused {
                            borderColor(Color.Red)
                            background(Color.Yellow)
                        }
                    }
        )
    }

    @Test
    fun task10() = interactiveTask {
        val mis = remember { MutableInteractionSource() }
        val styleState = remember(mis) { MutableStyleState(mis) }
        Box(
            modifier =
                Modifier.clickable(interactionSource = mis, indication = null) {}
                    .focusable(interactionSource = mis)
                    .hoverable(interactionSource = mis)
                    .styleable(styleState) {
                        background(Color.White)
                        border(2.dp, Color.Transparent)
                        size(100.dp)
                        pressed { animate { background(Color.Blue) } }
                        hovered { background(Color.Green) }
                        focused { border(2.dp, Color.Red) }
                        focused { hovered { border(6.dp, Color.Red) } }
                    }
        )
    }

    @Test
    fun task11() = interactiveTask {
        val mis = remember { MutableInteractionSource() }
        val styleState = remember(mis) { MutableStyleState(mis) }
        Box(
            modifier =
                Modifier.clickable(interactionSource = mis, indication = null) {}
                    .styleable(styleState) {
                        background(Color.LightGray)
                        size(100.dp)
                        pressed { animate { background(Color.Blue) } }
                    }
        )
    }

    @Test
    fun task12() = interactiveTask {
        val mis = remember { MutableInteractionSource() }
        val styleState = remember(mis) { MutableStyleState(mis) }
        Box(
            modifier =
                Modifier.clickable(interactionSource = mis, indication = null) {}
                    .styleable(styleState) {
                        background(Color.LightGray)
                        size(100.dp)
                        scaleX(1.0f)
                        scaleY(1.0f)
                        pressed {
                            animate(spec = spring(stiffness = StiffnessMediumLow)) {
                                background(Color.Cyan)
                                scaleX(0.9f)
                                scaleY(0.9f)
                            }
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            Text("Click Me")
        }
    }

    @Test
    fun task15() = interactiveTask {
        val focusRequester = remember { FocusRequester() }
        StyledButton(
            onClick = {},
            modifier = Modifier.focusRequester(focusRequester),
            style = {
                background(Color.Green)
                contentColor(Color.Black)
            },
        ) {
            Text("Green Button")
        }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    @Test
    fun task16() = interactiveTask {
        StyledButton(onClick = {}, style = { pressed { background(Color.Red) } }) {
            Text("I turn red when pressed (and 0.8 alpha)")
        }
    }

    @Test
    fun task17() {
        val sizeStyle = Style {
            size(100.dp)
            contentPadding(10.dp)
        }

        val colorStyle = Style {
            background(Color.Yellow)
            contentPadding(20.dp) // Should override the padding in sizeStyle
        }

        val mergedStyle = Style(sizeStyle, colorStyle)

        task { Box(modifier = Modifier.styleable(null, mergedStyle)) }
    }

    @Test
    fun task21() {
        var checked by mutableStateOf(true)
        var enabled by mutableStateOf(true)
        interactiveTask {
            Column {
                StyledSwitch(checked = checked, enabled = enabled, valueChange = { checked = it }) {
                    Text("Toggle: $checked")
                }
                StyledButton(onClick = { enabled = !enabled }) { Text("Toggle enabled") }
            }
        }
    }

    @Test // 468337359
    fun animation_state_nestingOrder() {
        interactiveTask {
            Column {
                StyledButton(
                    {},
                    style = { pressed { animate(tween(1000)) { background(Color.Green) } } },
                ) {
                    Text("Green Press Button - pressed/animate")
                }
                StyledButton(
                    {},
                    style = { animate(tween(1000)) { pressed { background(Color.Green) } } },
                ) {
                    Text("Green Press Button - animate/pressed")
                }
            }
        }
    }

    @Test // 479217116
    fun translation_in_animation_block() {
        interactiveTask {
            val interactionSource = remember { MutableInteractionSource() }
            val styleState = remember { MutableStyleState(interactionSource) }
            Box(
                modifier =
                    Modifier.clickable(interactionSource = interactionSource, indication = null) {}
                        .styleable(styleState) {
                            size(100.dp)
                            background(Color.Blue)
                            pressed { animate { translationY(100.dp.toPx()) } }
                        }
            )
        }
    }

    @Test // 481222410
    fun animating_font_weight() {
        interactiveTask {
            BaseStyleableButton(
                onClick = {},
                style = {
                    contentPadding(12.dp)
                    contentColor(Color(0xffADC7FF))
                    shape(RoundedCornerShape(50))
                    border(2.dp, Brush.linearGradient(listOf(Color(0xff217BFE), Color(0xffDCE2FF))))
                    fontWeight(FontWeight.Medium)
                    fontSize(14.sp)
                    pressed {
                        animate(tween(300)) {
                            contentColor(Color(0xff217BFE))
                            background(Color(0xffC7E4FF))
                            border(2.dp, Color(0xffC7E4FF))
                            fontWeight(FontWeight.Bold)
                        }
                    }
                },
            ) {
                Text("Press me!")
            }
        }
    }

    @Test
    fun rounded_corner_shape_border_and_background() {
        interactiveTask {
            BaseStyleableButton(
                onClick = {},
                style = {
                    contentPadding(12.dp)
                    shape(RoundedCornerShape(50))
                    border(10.dp, Color.Blue)
                    size(200.dp)
                    background(Color.Green)
                    contentColor(Color(0xffADC7FF))
                    fontSize(14.sp)
                    fontWeight(FontWeight.Medium)
                },
            ) {
                Text("Press me!")
            }
        }
    }

    @Test
    fun animating_background_press() {
        interactiveTask {
            BaseStyleableButton(
                onClick = {},
                style = {
                    contentPadding(12.dp)
                    shape(RoundedCornerShape(50))
                    border(1.dp, Color.Black)
                    background(Color.Blue)
                    contentColor(Color.White)

                    pressed { animate { background(Color.Green) } }
                },
            ) {
                Text("Press me!")
            }
        }
    }

    @Test
    fun style_modifier_equivalence_background() {
        interactiveTask {
            Column {
                BaseStyleableButton(onClick = {}, style = { background(Color.Blue) }) {
                    BasicText("Press me!", style = TextStyle(color = Color.White))
                }
                Spacer(modifier = Modifier.height(10.dp))
                BaseModifierButton(onClick = {}, background = SolidColor(Color.Blue)) {
                    BasicText("Press me!", style = whiteText)
                }
            }
        }
    }

    @Test
    fun style_modifier_equivalence_border() {
        interactiveTask {
            Column {
                BaseStyleableButton(onClick = {}, style = { border(5.dp, Color.Blue) }) {
                    BasicText("Press me!", style = blackText)
                }
                Spacer(modifier = Modifier.height(10.dp))
                BaseModifierButton(onClick = {}, border = BorderStroke(5.dp, Color.Blue)) {
                    BasicText("Press me!", style = blackText)
                }
            }
        }
    }

    @Test
    fun style_modifier_equivalence_border_background_contentPadding() {
        interactiveTask {
            Column {
                BaseStyleableButton(
                    onClick = {},
                    style = {
                        border(5.dp, Color.Blue)
                        background(Color.Green)
                        contentPadding(10.dp)
                    },
                ) {
                    BasicText("Press me!", style = blackText)
                }
                Spacer(modifier = Modifier.height(10.dp))
                BaseModifierButton(
                    onClick = {},
                    border = BorderStroke(5.dp, Color.Blue),
                    background = SolidColor(Color.Green),
                    contentPadding = PaddingValues(10.dp),
                ) {
                    BasicText("Press me!", style = blackText)
                }
            }
        }
    }

    @Test
    fun style_modifier_equivalence_externalPadding() {
        interactiveTask {
            Column {
                Box(modifier = Modifier.border(Dp.Hairline, Color.Black).padding(1.dp)) {
                    BaseStyleableButton(
                        onClick = {},
                        style = {
                            border(5.dp, Color.Blue)
                            background(Color.Green)
                            contentPadding(10.dp)
                            externalPadding(10.dp)
                        },
                    ) {
                        BasicText("Press me!", style = blackText)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.border(Dp.Hairline, Color.Black).padding(1.dp)) {
                    BaseModifierButton(
                        onClick = {},
                        border = BorderStroke(5.dp, Color.Blue),
                        background = SolidColor(Color.Green),
                        contentPadding = PaddingValues(10.dp),
                        externalPadding = PaddingValues(10.dp),
                    ) {
                        BasicText("Press me!", style = blackText)
                    }
                }
            }
        }
    }

    private fun task(content: @Composable () -> Unit) {
        rule.setContent(content)
    }

    private fun interactiveTask(isDone: Boolean = true, content: @Composable () -> Unit) {
        var done = isDone
        rule.setContent {
            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                Box(modifier = Modifier.border(1.dp, Color.Black).padding(20.dp)) { content() }
                if (!done) {
                    Box(
                        modifier =
                            Modifier.border(
                                    10.dp,
                                    color = Color.LightGray,
                                    RoundedCornerShape(15.dp),
                                )
                                .background(Color.Cyan, RoundedCornerShape(15.dp))
                                .padding(20.dp)
                                .clickable { done = true }
                    ) {
                        Text("Done")
                    }
                }
            }
        }
        rule.waitUntil(1000 * 60 * 2) { done }
    }
}

@ExperimentalFoundationStyleApi
@Composable
private fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: Style = Style,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    color: ColorProducer? = null,
    autoSize: TextAutoSize? = null,
) {
    BasicText(
        text = text,
        modifier = modifier.styleable(null, style),
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        color = color,
        autoSize = autoSize,
    )
}

private val pressShade = SolidColor(Color.Black.copy(alpha = 0.3f))
private val hoverShade = SolidColor(Color.Black.copy(alpha = 0.15f))

@OptIn(ExperimentalFoundationStyleApi::class)
private val baseButtonStyle = Style {
    shape(RoundedCornerShape(4.dp))
    background(Color.Blue)
    contentColor(Color.White)
    contentPadding(12.dp)
    border(1.dp, Color.Transparent)

    pressed { animate(spring(stiffness = StiffnessHigh)) { foreground(pressShade) } }
    hovered { animate(spring(stiffness = StiffnessMediumLow)) { foreground(hoverShade) } }
    disabled { animate { background(Color.Gray) } }
    focused { animate { borderColor(Color.Black) } }
}

@OptIn(ExperimentalFoundationStyleApi::class)
private val styledSwitchBaseStyle = Style {
    background(Color.Cyan)
    border(2.dp, Color.Black)
    contentPadding(10.dp)

    checked { animate { background(Color.Yellow) } }
    disabled { animate { background(Color.LightGray) } }
}

private val whiteText = TextStyle(color = Color.White)
private val blackText = TextStyle(color = Color.Black)

@ExperimentalFoundationStyleApi
@Composable
private fun StyledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: Style = Style,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    BaseStyleableButton(
        onClick = onClick,
        modifier = modifier,
        style = baseButtonStyle then style,
        enabled = enabled,
        interactionSource = interactionSource,
        content = content,
    )
}

@ExperimentalFoundationStyleApi
@Composable
private fun StyledSwitch(
    checked: Boolean,
    enabled: Boolean,
    valueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    style: Style = Style,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val styleState =
        rememberUpdatedStyleState(interactionSource) {
            it.isEnabled = enabled
            it.isChecked = checked
        }
    Row(
        modifier =
            modifier
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onValueChange = valueChange,
                )
                .focusable(enabled, interactionSource)
                .styleable(styleState, styledSwitchBaseStyle, style),
        content = content,
    )
}
