/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.demos.text2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.demos.text.Language
import androidx.compose.foundation.demos.text.RainbowColors
import androidx.compose.foundation.demos.text.TagLine
import androidx.compose.foundation.demos.text.loremIpsum
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Checkbox
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusProperties.Companion.UnsetFocusRect
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private data class DecorationSettings(
    val isDecorated: Boolean = false,
    /** Changes the focusArea of TextField from cursor to entire decoration box */
    val focusOnDecoration: Boolean = false,
)

private val LocalDecorationSettings = compositionLocalOf { DecorationSettings() }

@Composable
fun ScrollableDemos() {
    var isDecorated by remember { mutableStateOf(false) }
    var focusOnDecoration by remember { mutableStateOf(false) }
    CompositionLocalProvider(
        value = LocalDecorationSettings provides DecorationSettings(isDecorated, focusOnDecoration)
    ) {
        LazyColumn(Modifier.padding(16.dp).windowInsetsPadding(WindowInsets.ime)) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isDecorated, onCheckedChange = { isDecorated = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Decorated")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = focusOnDecoration,
                            onCheckedChange = { focusOnDecoration = it },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Focus On Decoration")
                    }
                }
            }

            item {
                TagLine(tag = "SingleLine Horizontal Scroll")
                SingleLineHorizontalScrollableTextField()
            }

            item {
                TagLine(tag = "SingleLine Horizontal Scroll with newlines")
                SingleLineHorizontalScrollableTextFieldWithNewlines()
            }

            item {
                Box(
                    modifier =
                        Modifier.padding(vertical = 16.dp)
                            .height(120.dp)
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = RainbowColors,
                                    tileMode = TileMode.Mirror,
                                )
                            )
                ) {
                    Text("Left empty for demo purposes", Modifier.align(Alignment.Center))
                }
            }

            item {
                TagLine(tag = "MultiLine Vertical Scroll")
                MultiLineVerticalScrollableTextField()
            }

            item {
                TagLine(tag = "Hoisted ScrollState")
                HoistedHorizontalScroll()
            }

            item {
                TagLine(tag = "Shared Hoisted ScrollState")
                SharedHoistedScroll()
            }
        }
    }
}

@Composable
fun ScrollableDemosRtl() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ScrollableDemos()
    }
}

@Composable
fun SingleLineHorizontalScrollableTextField() {
    val layoutDirection = LocalLayoutDirection.current
    val language = if (layoutDirection == LayoutDirection.Ltr) Language.Latin else Language.Hebrew
    val state =
        remember(language) { TextFieldState(loremIpsum(wordCount = 100, language = language)) }
    BasicTextField(
        state = state,
        lineLimits = SingleLine,
        textStyle = TextStyle(fontSize = 24.sp),
        modifier = Modifier.padding(horizontal = 32.dp).focusRect(),
        decorator = simpleDecoration(),
    )
}

@Composable
fun SingleLineHorizontalScrollableTextFieldWithNewlines() {
    val layoutDirection = LocalLayoutDirection.current
    val language = if (layoutDirection == LayoutDirection.Ltr) Language.Latin else Language.Hebrew
    val state =
        remember(language) {
            TextFieldState(loremIpsum(wordCount = 20, language = language, separator = "\n"))
        }
    BasicTextField(
        state = state,
        lineLimits = SingleLine,
        textStyle = TextStyle(fontSize = 24.sp),
        modifier = Modifier.focusRect(),
        decorator = simpleDecoration(),
    )
}

@Composable
fun MultiLineVerticalScrollableTextField() {
    val layoutDirection = LocalLayoutDirection.current
    val language = if (layoutDirection == LayoutDirection.Ltr) Language.Latin else Language.Hebrew
    val state =
        remember(language) { TextFieldState(loremIpsum(wordCount = 200, language = language)) }
    BasicTextField(
        state = state,
        textStyle = TextStyle(fontSize = 24.sp),
        modifier = Modifier.heightIn(max = 200.dp).focusRect(),
        lineLimits = MultiLine(),
        decorator = simpleDecoration(),
    )
}

@Suppress("FrequentlyChangingValue")
@Composable
fun HoistedHorizontalScroll() {
    val layoutDirection = LocalLayoutDirection.current
    val language = if (layoutDirection == LayoutDirection.Ltr) Language.Latin else Language.Hebrew
    val state =
        remember(language) { TextFieldState(loremIpsum(wordCount = 20, language = language)) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    Column {
        Slider(
            value = scrollState.value.toFloat(),
            onValueChange = { coroutineScope.launch { scrollState.scrollTo(it.roundToInt()) } },
            valueRange = 0f..scrollState.maxValue.toFloat(),
        )
        BasicTextField(
            state = state,
            scrollState = scrollState,
            textStyle = TextStyle(fontSize = 24.sp),
            modifier = Modifier.height(200.dp).focusRect(),
            lineLimits = SingleLine,
            decorator = simpleDecoration(),
        )
    }
}

@Suppress("FrequentlyChangingValue")
@Composable
fun SharedHoistedScroll() {
    val layoutDirection = LocalLayoutDirection.current
    val language = if (layoutDirection == LayoutDirection.Ltr) Language.Latin else Language.Hebrew
    val state1 =
        remember(language) { TextFieldState(loremIpsum(wordCount = 20, language = language)) }
    val state2 =
        remember(language) { TextFieldState(loremIpsum(wordCount = 20, language = language)) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    Column {
        Slider(
            value = scrollState.value.toFloat(),
            onValueChange = { coroutineScope.launch { scrollState.scrollTo(it.roundToInt()) } },
            valueRange = 0f..scrollState.maxValue.toFloat(),
        )
        BasicTextField(
            state = state1,
            scrollState = scrollState,
            textStyle = TextStyle(fontSize = 24.sp),
            modifier = Modifier.fillMaxWidth().focusRect(),
            lineLimits = SingleLine,
            decorator = simpleDecoration(),
        )
        BasicTextField(
            state = state2,
            scrollState = scrollState,
            textStyle = TextStyle(fontSize = 24.sp),
            modifier = Modifier.fillMaxWidth().focusRect(),
            lineLimits = SingleLine,
            decorator = simpleDecoration(),
        )
    }
}

@Composable
fun simpleDecoration(): TextFieldDecorator {
    return remember {
        TextFieldDecorator {
            val isDecorated = LocalDecorationSettings.current.isDecorated
            if (isDecorated) {
                Box(
                    modifier =
                        Modifier.border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                ) {
                    it()
                }
            } else {
                it()
            }
        }
    }
}

fun Modifier.focusRect(): Modifier = composed {
    if (LocalDecorationSettings.current.focusOnDecoration) {
        focusProperties { focusRect = UnsetFocusRect }
    } else this
}
