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

package androidx.compose.mpp.demo

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.SecureTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.mpp.demo.textfield.ClearFocusBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
private val enabledNativeTextInputOptions = PlatformImeOptions {
    usingNativeTextInput(true)
}

val NativeTextInputTextFields = Screen.Selection(
    title = "Native Text Input Examples",
    screens = listOf(
        Screen.Example("Native Text Input Fullscreen BTF1") { NativeTextInputFullscreenBtf1() },
        Screen.Example("Native Text Input Fullscreen BTF2") { NativeTextInputFullscreenBtf2() },
        Screen.Example("BTF1 Comparison") { Btf1NativeTextInputComparison() },
        Screen.Example("BTF2 Comparison") { Btf2NativeTextInputComparison() },
        Screen.Example("Brush") { Brush() },
        Screen.Example("InteractionSource focus border") { InteractionSourceFocusBorder() },
        Screen.Example("TextFieldDecorator / DecorationBox") { TextFieldDecoratorDecorationBox() },
        Screen.Example("ScrollState") { ScrollState() },
        Screen.Example("GraphicsLayer") { GraphicsLayer() },
        Screen.Example("Appearance modifiers") { AppearanceModifiers() },
        Screen.Example("Secure input") { SecureInput() },
    )
)

@Composable
private fun NativeTextInputFullscreenBtf1() {
    val textState = remember {
        mutableStateOf(
            buildString {
                repeat(100) {
                    appendLine("Text line $it")
                }
            }
        )
    }
    ClearFocusBox {
        TextField(
            textState.value, { textState.value = it },
            Modifier.fillMaxSize().padding(vertical = 40.dp),
            keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions)
        )
    }
}

@Composable
private fun NativeTextInputFullscreenBtf2() {
    val state = rememberTextFieldState(
        buildString {
            repeat(100) {
                appendLine("Text line $it")
            }
        }
    )
    ClearFocusBox {
        TextField(
            state,
            Modifier.fillMaxSize().padding(vertical = 40.dp).background(Color.LightGray),
            keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions)
        )
    }
}

@Composable
private fun Btf1NativeTextInputComparison() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        val modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
            .padding(4.dp)

        var text1 by remember { mutableStateOf("BTF1 without Native Text Input") }
        BasicTextField(
            value = text1,
            onValueChange = { text1 = it },
            modifier = modifier,
            decorationBox = { inner ->
                Box {
                    if (text1.isEmpty()) Text("BTF1 without Native Text Input", color = Color.Gray)
                    inner()
                }
            }
        )
        Spacer(Modifier.height(16.dp))

        var text2 by remember { mutableStateOf("BTF1 with Native Text Input") }
        BasicTextField(
            value = text2,
            onValueChange = { text2 = it },
            modifier = modifier,
            keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
            decorationBox = { inner ->
                Box {
                    if (text2.isEmpty()) Text("BTF1 with Native Text Input", color = Color.Gray)
                    inner()
                }
            }
        )
        Spacer(Modifier.height(16.dp))

        var text3 by remember { mutableStateOf("BTF1 with two lines\nwithout Native Text Input") }
        BasicTextField(
            value = text3,
            onValueChange = { text3 = it },
            modifier = modifier,
            decorationBox = { inner ->
                Box {
                    if (text3.isEmpty()) Text("BTF1 with two lines without Native Text Input", color = Color.Gray)
                    inner()
                }
            }
        )
        Spacer(Modifier.height(16.dp))

        val state4 = rememberTextFieldState("BTF2 with two lines\nwith Native Text Input")
        BasicTextField(
            state = state4,
            modifier = modifier,
            keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
            //TODO: v.mazunin: uncomment after compile compose with Kotlin later than 2.3.20-RC1
//            decorator = { inner ->
//                Box {
//                    if (state4.text.isEmpty()) Text("BTF2 with two lines with Native Text Input", color = Color.Gray)
//                    inner()
//                }
//            }
        )
    }
}

@Composable
private fun Btf2NativeTextInputComparison() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        val modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
            .padding(4.dp)

        val state1 = rememberTextFieldState("BTF2 without Native Text Input")
        BasicTextField(
            state = state1,
            modifier = modifier,
            //TODO: v.mazunin: uncomment after compile compose with Kotlin later than 2.3.20-RC1
//            decorator = { inner ->
//                Box {
//                    if (state1.text.isEmpty()) Text("BTF2 without Native Text Input", color = Color.Gray)
//                    inner()
//                }
//            }
        )
        Spacer(Modifier.height(16.dp))

        val state2 = rememberTextFieldState("BTF2 with Native Text Input")
        BasicTextField(
            state = state2,
            modifier = modifier,
            keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
            //TODO: v.mazunin: uncomment after compile compose with Kotlin later than 2.3.20-RC1
//            decorator = { inner ->
//                Box {
//                    if (state2.text.isEmpty()) Text("BTF2 with Native Text Input", color = Color.Gray)
//                    inner()
//                }
//            }
        )
        Spacer(Modifier.height(16.dp))

        val state3 = rememberTextFieldState("BTF2 with two lines\nwithout Native Text Input")
        BasicTextField(
            state = state3,
            modifier = modifier,
            //TODO: v.mazunin: uncomment after compile compose with Kotlin later than 2.3.20-RC1
//            decorator = { inner ->
//                Box {
//                    if (state3.text.isEmpty()) Text("BTF2 with two lines without Native Text Input", color = Color.Gray)
//                    inner()
//                }
//            }
        )
        Spacer(Modifier.height(16.dp))

        val state4 = rememberTextFieldState("BTF2 with two lines\nwith Native Text Input")
        BasicTextField(
            state = state4,
            modifier = modifier,
            keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
            //TODO: v.mazunin: uncomment after compile compose with Kotlin later than 2.3.20-RC1
//            decorator = { inner ->
//                Box {
//                    if (state4.text.isEmpty()) Text("BTF2 with two lines with Native Text Input", color = Color.Gray)
//                    inner()
//                }
//            }
        )
    }
}

@Composable
private fun Brush() {
    ClearFocusBox {
        Column {
            var text by remember { mutableStateOf("Native Text Input BasicTextField 1 with a long text") }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                Modifier.fillMaxWidth().height(56.dp),
                keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
                textStyle = TextStyle(
                    brush = Brush.linearGradient(listOf(Color.Magenta, Color.Cyan)),
                    fontSize = 18.sp
                )
            )
            Box(modifier = Modifier.height(16.dp))

            val text2 = rememberTextFieldState("Native Text Input BasicTextField 2 with a long text")
            BasicTextField(
                state = text2,
                Modifier.fillMaxWidth().height(56.dp),
                keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
                textStyle = TextStyle(
                    brush = Brush.linearGradient(listOf(Color.Magenta, Color.Cyan)),
                    fontSize = 18.sp
                )
            )
            Box(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InteractionSourceFocusBorder() {
    Column {
        var text by remember { mutableStateOf("Native Text Input BasicTextField 1 with a long text") }
        val interaction1 = remember { MutableInteractionSource() }
        val focused1 by interaction1.collectIsFocusedAsState()
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            interactionSource = interaction1,
            modifier = Modifier
                .border(2.dp, if (focused1) Color.Cyan else Color.Gray, RoundedCornerShape(8.dp))
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),

        )
        Box(modifier = Modifier.height(16.dp))
        val state = rememberTextFieldState("Native Text Input BasicTextField 2 with a long text")
        val interaction = remember { MutableInteractionSource() }
        val focused by interaction.collectIsFocusedAsState()
        BasicTextField(
            state = state,
            interactionSource = interaction,
            modifier = Modifier
                .border(2.dp, if (focused) Color.Cyan else Color.Gray, RoundedCornerShape(8.dp))
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
        )
    }
}

@Composable
private fun TextFieldDecoratorDecorationBox() {
    ClearFocusBox {
        Column {
            var text by remember { mutableStateOf("Native Text Input BasicTextField 1 with a long text") }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                    .padding(4.dp),
                keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
                decorationBox = { inner ->
                    Row(
                        Modifier
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (text.isEmpty()) Text("Search…", color = Color.Gray)
                            inner()
                        }
                        Text("${text.length}", color = Color.Gray)
                    }
                }
            )

            Box(Modifier.height(16.dp))

            val state2 = rememberTextFieldState("Native Text Input BasicTextField 2 with a long text")
            BasicTextField(
                state = state2,
                modifier = Modifier.fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                    .padding(4.dp),
                keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
                //TODO: v.mazunin: uncomment after compile compose with Kotlin later than 2.3.20-RC1
//                decorator = { inner ->
//                    Row(
//                        Modifier
//                            .background(Color(0xFF121212), RoundedCornerShape(10.dp))
//                            .padding(8.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
//                        Spacer(Modifier.width(8.dp))
//                        Box(Modifier.weight(1f)) {
//                            if (state2.text.isEmpty()) Text("Search…", color = Color.Gray)
//                            inner()
//                        }
//                        Text("${state2.text.length}", color = Color.Gray)
//                    }
//                }
            )
        }
    }
}

@Composable
private fun ScrollState() {
    ClearFocusBox {
        Column {
            var text by remember { mutableStateOf(("lots of text Native Text Input BTF1 \n").repeat(40)) }
            val scroll1 = rememberScrollState()
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .verticalScroll(scroll1),
                keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
                textStyle = TextStyle(color = Color.Black, fontSize = 14.sp)
            )

            Box(Modifier.height(16.dp))

            val state2 = rememberTextFieldState(("Lots of text Native Text Input BTF2\n").repeat(40))
            val scroll2 = rememberScrollState()
            BasicTextField(
                state = state2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .verticalScroll(scroll2),
                keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
                textStyle = TextStyle(color = Color.Black, fontSize = 14.sp)
            )
        }
    }
}

@Composable
private fun GraphicsLayer() {
    ClearFocusBox {
        Column {
            var text by remember { mutableStateOf("Native Text Input BasicTextField 1 with a long text") }
            val pulse1 by rememberInfiniteTransition().animateFloat(
                initialValue = 0.96f, targetValue = 1.04f,
                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse)
            )
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer { scaleX = pulse1; scaleY = pulse1 },
                keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
                textStyle = TextStyle(color = Color.Black, fontSize = 18.sp)
            )

            Box(Modifier.height(16.dp))

            val state2 = rememberTextFieldState("Native Text Input BasicTextField 2 with a long text")
            val pulse2 by rememberInfiniteTransition().animateFloat(
                0.96f, 1.04f, infiniteRepeatable(tween(600), RepeatMode.Reverse)
            )
            BasicTextField(
                state = state2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer { scaleX = pulse2; scaleY = pulse2 },
                keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
                textStyle = TextStyle(color = Color.Black, fontSize = 18.sp)
            )
        }
    }
}

@Composable
private fun AppearanceModifiers() {
    ClearFocusBox {
        Column {
            var text by remember { mutableStateOf("Native Text Input BasicTextField 1 with a long text") }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.Cyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFFFFE0B2), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
            )

            Box(Modifier.height(16.dp))

            val state2 = rememberTextFieldState("Native Text Input BasicTextField 2 with a long text")
            BasicTextField(
                state = state2,
                textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.Cyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFFFFE0B2), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                keyboardOptions = KeyboardOptions(platformImeOptions = enabledNativeTextInputOptions),
            )
        }
    }
}

@Composable
private fun SecureInput() {
    ClearFocusBox {
        Column {
            var text by remember { mutableStateOf("Native Text Input BasicTextField 1 with a long text") }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false,
                    platformImeOptions = enabledNativeTextInputOptions
                ),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
            )

            Box(Modifier.height(16.dp))

            val state2 = rememberTextFieldState("Native Text Input SecureTextField with a long text")
            SecureTextField(
                state = state2,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false,
                    platformImeOptions = enabledNativeTextInputOptions
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                textStyle = TextStyle(color = Color.Black, fontSize = 16.sp)
            )
        }
    }
}
