/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.input

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.matchers.assertThat
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class BasicTextFieldLayoutPhaseToggleTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var state: TextFieldState

    private val fontSize = 20.sp
    private val textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY)

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun fontWeightChange_reflectsOnView() {
        state = TextFieldState("abc")
        var fontWeight by mutableStateOf(FontWeight.Normal)
        rule.setContent {
            BasicTextField(
                state = state,
                textStyle = TextStyle(fontWeight = fontWeight),
                modifier = Modifier.background(Color.White)
            )
        }

        val firstBitmap = rule.onNode(hasSetTextAction()).captureToImage().asAndroidBitmap()
        val firstTextLayoutResult = rule.onNode(hasSetTextAction()).fetchTextLayoutResult()

        assertThat(firstTextLayoutResult.layoutInput.style.fontWeight).isEqualTo(FontWeight.Normal)

        fontWeight = FontWeight.Bold

        val secondBitmap = rule.onNode(hasSetTextAction()).captureToImage().asAndroidBitmap()
        val secondTextLayoutResult = rule.onNode(hasSetTextAction()).fetchTextLayoutResult()

        assertThat(secondTextLayoutResult.layoutInput.style.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(firstBitmap).isNotEqualToBitmap(secondBitmap)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textAlignChange_reflectsOnView() {
        state = TextFieldState("abc")
        var textAlign by mutableStateOf(TextAlign.End)
        rule.setContent {
            BasicTextField(
                state = state,
                textStyle = textStyle.copy(textAlign = textAlign),
                modifier = Modifier.background(Color.White)
            )
        }

        val firstBitmap = rule.onNode(hasSetTextAction()).captureToImage().asAndroidBitmap()
        val firstTextLayoutResult = rule.onNode(hasSetTextAction()).fetchTextLayoutResult()

        assertThat(firstTextLayoutResult.layoutInput.style.textAlign).isEqualTo(TextAlign.End)

        textAlign = TextAlign.Start

        val secondBitmap = rule.onNode(hasSetTextAction()).captureToImage().asAndroidBitmap()
        val secondTextLayoutResult = rule.onNode(hasSetTextAction()).fetchTextLayoutResult()

        assertThat(secondTextLayoutResult.layoutInput.style.textAlign).isEqualTo(TextAlign.Start)
        assertThat(firstBitmap).isNotEqualToBitmap(secondBitmap)
    }
}
