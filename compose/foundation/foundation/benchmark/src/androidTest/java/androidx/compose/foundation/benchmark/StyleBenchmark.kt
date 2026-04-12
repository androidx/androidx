/*
 * Copyright 2026 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.benchmark

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.MutableStyleState
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.StyleScope
import androidx.compose.foundation.style.disabled
import androidx.compose.foundation.style.focused
import androidx.compose.foundation.style.hovered
import androidx.compose.foundation.style.pressed
import androidx.compose.foundation.style.styleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class StyleBenchmark(val isStyle: Boolean) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "isStyle={0}")
        fun initParameters(): Array<Any> = arrayOf(true, false)
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun basic_box_border_change() {
        textFlag(isStyle) {
            benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
                { BasicBoxTestCase(isStyle) },
                assertOneRecomposition = false,
                requireRecomposition = false,
            )
        }
    }

    @Test
    fun basic_box() {
        textFlag(isStyle) { benchmarkRule.benchmarkToFirstPixel { BasicBoxTestCase(isStyle) } }
    }

    @Test
    fun input_state_basic_box() {
        textFlag(isStyle) { benchmarkRule.benchmarkToFirstPixel { InputStateTestCase(isStyle) } }
    }

    @Test
    fun basic_text() {
        textFlag(isStyle) { benchmarkRule.benchmarkToFirstPixel { BasicTextTestCase(isStyle) } }
    }

    @Test
    fun basic_text_provided_color() {
        textFlag(isStyle) {
            benchmarkRule.benchmarkToFirstPixel { BasicTextProvidedColorTestCase(isStyle) }
        }
    }

    @Test
    fun button() {
        textFlag(isStyle) { benchmarkRule.benchmarkToFirstPixel { ButtonTestCase(isStyle) } }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private inline fun textFlag(isStyle: Boolean, block: () -> Unit) {
    val previous = ComposeFoundationFlags.isInheritedTextStyleEnabled
    ComposeFoundationFlags.isInheritedTextStyleEnabled = isStyle
    try {
        block()
    } finally {
        ComposeFoundationFlags.isInheritedTextStyleEnabled = previous
    }
}

@OptIn(ExperimentalFoundationApi::class)
class BasicTextTestCase(val isStyle: Boolean) : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        BasicText("Hello World")
        BasicText("This is")
        BasicText("Some random")
        BasicText("Text that")
        BasicText("I wrote")
    }
}

class BasicTextProvidedColorTestCase(val isStyle: Boolean) : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        if (isStyle) {
            StyleVersion()
        } else {
            NonStyleVersion()
        }
    }

    @Composable
    fun StyleVersion() {
        Box(Modifier.styleable(null) { contentColor(Color.Blue) }) { BasicText("Hello World") }
    }

    @Composable
    fun NonStyleVersion() {
        Box {
            CompositionLocalProvider(LocalContentColor provides Color.Blue) { Text("Hello World") }
        }
    }
}

class ButtonTestCase(val isStyle: Boolean) : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        if (isStyle) {
            StyleButton(onClick = {}) { StyleVersion() }
        } else {
            Button(onClick = {}) { NonStyleVersion() }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable (() -> Unit)) {
        ComposeStyleTheme { content() }
    }

    @Composable
    fun StyleVersion() {
        BasicText("Hello World")
    }

    @Composable
    fun NonStyleVersion() {
        CompositionLocalProvider(LocalContentColor provides Color.Blue) { Text("Hello World") }
    }
}

class BasicBoxTestCase(val isStyle: Boolean) : LayeredComposeTestCase(), ToggleableTestCase {

    private val borderColor = mutableStateOf(Color.Blue)
    private val style = Style {
        externalPadding(10.dp)
        border(1.dp, borderColor.value)
        background(Color.Red)
        contentPadding(9.dp)
        size(100.dp)
    }

    @Composable
    override fun MeasuredContent() {
        if (isStyle) {
            Box(Modifier.styleable(null, style))
        } else {
            Box(
                Modifier.padding(10.dp)
                    .border(1.dp, borderColor.value)
                    .background(Color.Red)
                    .padding(10.dp)
                    .size(100.dp)
            )
        }
    }

    override fun toggleState() {
        if (borderColor.value == Color.Blue) {
            borderColor.value = Color.Red
        } else {
            borderColor.value = Color.Blue
        }
    }
}

class InputStateTestCase(val isStyle: Boolean) : LayeredComposeTestCase() {
    val style = Style {
        border(3.dp, Color.Black)
        background(Color.White)
        size(100.dp)
        hovered {
            borderColor(Color.Blue)
            background(Color.Blue)
        }
        focused {
            borderColor(Color.Red)
            background(Color.Green)
        }
        pressed {
            borderColor(Color.Green)
            background(Color.Red)
        }
    }

    @Composable
    override fun MeasuredContent() {
        if (isStyle) {
            StyleVersion()
        } else {
            NonStyleVersion()
        }
    }

    @Composable
    fun StyleVersion() {
        val interactionSource = remember { MutableInteractionSource() }
        val styleState = remember(interactionSource) { MutableStyleState(interactionSource) }
        Box(Modifier.styleable(styleState, style).interactions(interactionSource))
    }

    @Composable
    fun NonStyleVersion() {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()
        val isPressed by interactionSource.collectIsPressedAsState()
        val isHovered by interactionSource.collectIsHoveredAsState()

        val backgroundColor =
            when {
                isPressed -> Color.Red
                isFocused -> Color.Green
                isHovered -> Color.Blue
                else -> Color.White
            }

        val borderColor =
            when {
                isPressed -> Color.Green
                isFocused -> Color.Red
                isHovered -> Color.Blue
                else -> Color.Black
            }

        Box(
            Modifier.interactions(interactionSource)
                .border(3.dp, borderColor)
                .background(backgroundColor)
                .size(100.dp)
        )
    }
}

fun Modifier.interactions(interactionSource: MutableInteractionSource) =
    this then InteractionElement(interactionSource)

data class InteractionElement(private val interactionSource: MutableInteractionSource) :
    ModifierNodeElement<InteractionNode>() {
    override fun create() = InteractionNode(interactionSource)

    override fun update(node: InteractionNode) {
        node.interactionSource = interactionSource
    }
}

class InteractionNode(var interactionSource: MutableInteractionSource) :
    Modifier.Node(), PointerInputModifierNode, FocusEventModifierNode {
    private var previousPress: PressInteraction.Press? = null
    private var previousHover: HoverInteraction.Enter? = null
    private var previousFocus: FocusInteraction.Focus? = null

    override fun onCancelPointerInput() {}

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pass != PointerEventPass.Main) return
        val interactionSource = this@InteractionNode.interactionSource

        when (pointerEvent.type) {
            PointerEventType.Press -> {
                val press = PressInteraction.Press(Offset.Zero)
                previousPress = press
                interactionSource.tryEmit(press)
            }
            PointerEventType.Release -> {
                previousPress?.let { interactionSource.tryEmit(PressInteraction.Release(it)) }
                previousPress = null
            }
            PointerEventType.Enter -> {
                val enter = HoverInteraction.Enter()
                previousHover = enter
                interactionSource.tryEmit(enter)
            }

            PointerEventType.Exit -> {
                previousHover?.let { interactionSource.tryEmit(HoverInteraction.Exit(it)) }
                previousHover = null
            }
            else -> {}
        }
    }

    override fun onFocusEvent(focusState: FocusState) {
        when (focusState.isFocused) {
            true -> {
                val focus = FocusInteraction.Focus()
                previousFocus = focus
                interactionSource.tryEmit(focus)
            }
            false -> {
                previousFocus?.let { interactionSource.tryEmit(it) }
                previousFocus = null
            }
        }
    }
}

@Composable
fun StyleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: Style = Style,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = remember(interactionSource) { MutableStyleState(interactionSource) }
    styleState.isEnabled = enabled
    Row(
        modifier
            .clickable(onClick = onClick, indication = null, interactionSource = interactionSource)
            .styleable(styleState, buttonStyle, style),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

private val buttonStyle = Style {
    contentPadding(8.dp, 24.dp)
    background(colors.primary)
    shape(shapes.extraLarge)
    clip()
    minWidth(58.dp)
    minHeight(40.dp)
    textStyle(typography.labelLarge)
    contentColor(colors.onPrimary)
    disabled {
        animate {
            background(Color.Transparent)
            contentColor(colors.onSecondary)
        }
    }
}

val StyleScope.colors: ColorScheme
    get() = LocalColorScheme.currentValue
val StyleScope.typography: Typography
    get() = LocalTypography.currentValue
val StyleScope.shapes: Shapes
    get() = LocalShapes.currentValue

@Composable
fun ComposeStyleTheme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography) {
        CompositionLocalProvider(
            LocalColorScheme provides MaterialTheme.colorScheme,
            LocalTypography provides MaterialTheme.typography,
            LocalShapes provides MaterialTheme.shapes,
        ) {
            content()
        }
    }
}

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

private val LightColorScheme =
    lightColorScheme(primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40)

val Typography =
    androidx.compose.material3.Typography(
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            )
    )

val LocalColorScheme = staticCompositionLocalOf { LightColorScheme }
val LocalTypography = staticCompositionLocalOf { Typography }
val LocalShapes = staticCompositionLocalOf { Shapes() }
