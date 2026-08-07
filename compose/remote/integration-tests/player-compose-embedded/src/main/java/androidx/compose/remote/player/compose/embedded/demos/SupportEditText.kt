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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.demos

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.remote.player.compose.embedded.CustomComposablePlugin
import androidx.compose.remote.player.compose.embedded.FloatProperty
import androidx.compose.remote.player.compose.embedded.IntProperty
import androidx.compose.remote.player.compose.embedded.RcCustomComponent
import androidx.compose.remote.player.compose.embedded.StringProperty
import androidx.compose.remote.player.compose.embedded.TextReturnProperty
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Data class representing extracted data and callbacks for a custom `EditText` component.
 *
 * All property getters ([text], [hint], [textColor], [textSize], [backgroundColor]) are backed by
 * Compose [State] for reactive recomposition.
 */
public data class SupportEditTextData(
    val textState: State<String> = mutableStateOf(""),
    val hintState: State<String> = mutableStateOf(""),
    val textColorState: State<Color> = mutableStateOf(Color.Unspecified),
    val textSizeState: State<TextUnit> = mutableStateOf(14.sp),
    val backgroundColorState: State<Color> = mutableStateOf(Color.Transparent),
    val config: String = "support:edit-text",
    val onTextChanged: (String) -> Unit = {},
) {
    /** Reactive text string. */
    public val text: String
        get() = textState.value

    /** Reactive hint text string. */
    public val hint: String
        get() = hintState.value

    /** Reactive text color [Color]. */
    public val textColor: Color
        get() = textColorState.value

    /** Reactive font size [TextUnit]. */
    public val textSize: TextUnit
        get() = textSizeState.value

    /** Reactive background color [Color]. */
    public val backgroundColor: Color
        get() = backgroundColorState.value

    public companion object {
        public val TEXT: StringProperty = StringProperty(1.toShort(), default = "")
        public val TEXT_COLOR: IntProperty = IntProperty(2.toShort(), default = 0)
        public val TEXT_SIZE: FloatProperty = FloatProperty(3.toShort(), default = 14f)
        public val BACKGROUND_COLOR: IntProperty = IntProperty(4.toShort(), default = 0)
        public val HINT: StringProperty = StringProperty(5.toShort(), default = "")
        public val RET_TEXT: TextReturnProperty = TextReturnProperty(6.toShort())
    }
}

/** Custom component plugin for rendering a custom `EditText` with Compose [BasicTextField]. */
@SuppressLint("RestrictedApiAndroidX")
public object SupportEditTextPlugin : CustomComposablePlugin<SupportEditTextData> {
    override val name: String = "support:edit-text"

    @Composable
    override fun extract(component: RcCustomComponent): SupportEditTextData? {

        val rawTextColorState = component.intState(SupportEditTextData.TEXT_COLOR)
        val textColorState = remember {
            derivedStateOf {
                val argb = rawTextColorState.value
                if (argb != 0) Color(argb) else Color.Unspecified
            }
        }

        val rawTextSizeState = component.floatState(SupportEditTextData.TEXT_SIZE)
        val textSizeState = remember { derivedStateOf { rawTextSizeState.value.sp } }

        val rawBgColorState = component.intState(SupportEditTextData.BACKGROUND_COLOR)
        val backgroundColorState = remember {
            derivedStateOf {
                val argb = rawBgColorState.value
                if (argb != 0) Color(argb) else Color.Transparent
            }
        }

        return SupportEditTextData(
            textState = component.textState(SupportEditTextData.TEXT),
            hintState = component.textState(SupportEditTextData.HINT),
            textColorState = textColorState,
            textSizeState = textSizeState,
            backgroundColorState = backgroundColorState,
            config = component.config,
            onTextChanged = component.returnTextHandler(SupportEditTextData.RET_TEXT),
        )
    }

    @Composable
    override fun Content(
        data: SupportEditTextData,
        component: RcCustomComponent,
        modifier: Modifier,
    ) {
        var textValue by remember(data.text) { mutableStateOf(data.text) }
        val textStyle = TextStyle(color = data.textColor, fontSize = data.textSize)

        Box(
            modifier = modifier.background(data.backgroundColor).fillMaxSize(),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = textValue,
                onValueChange = { newText ->
                    textValue = newText
                    data.onTextChanged(newText)
                },
                textStyle = textStyle,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { data.onTextChanged(textValue) }),
            )
            if (textValue.isEmpty() && data.hint.isNotEmpty()) {
                BasicText(
                    text = data.hint,
                    style = textStyle.copy(color = textStyle.color.copy(alpha = 0.5f)),
                )
            }
        }
    }
}
