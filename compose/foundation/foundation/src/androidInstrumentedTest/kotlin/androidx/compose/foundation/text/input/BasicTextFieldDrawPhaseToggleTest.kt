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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class BasicTextFieldDrawPhaseToggleTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var state: TextFieldState

    private val fontSize = 20.sp
    private val textStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY)

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun colorChange_reflectsOnView() {
        state = TextFieldState("abc")
        var color by mutableStateOf(Color.Red)
        rule.setContent {
            BasicTextField(
                state = state,
                textStyle = textStyle.copy(color = color),
                modifier = Modifier.background(Color.White)
            )
        }

        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertContainsColor(Color.Red)
            .assertContainsColor(Color.White)
            .assertDoesNotContainColor(Color.Blue)

        color = Color.Blue

        rule
            .onNode(hasSetTextAction())
            .captureToImage()
            .assertContainsColor(Color.Blue)
            .assertContainsColor(Color.White)
            .assertDoesNotContainColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun brushChange_reflectsOnView() {
        state = TextFieldState("abc")
        var brush by
            mutableStateOf(
                Brush.linearGradient(listOf(Color.Red, Color.Blue), end = Offset(20f, 20f))
            )
        rule.setContent {
            BasicTextField(
                state = state,
                textStyle = textStyle.copy(brush = brush),
                // use brush also for background to get rid of weird antialiasing edges
                modifier = Modifier.background(brush)
            )
        }

        rule.onNode(hasSetTextAction()).captureToImage().assertPixelConsistency { color ->
            // gradient should not contain any discernible level of green channel
            color.green <= 0.02f
        }

        brush = Brush.linearGradient(listOf(Color.Red, Color.Green), end = Offset(20f, 20f))

        rule.onNode(hasSetTextAction()).captureToImage().assertPixelConsistency { color ->
            // gradient should not contain any discernible level of blue channel
            color.blue <= 0.02f
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun shadowChange_reflectsOnView() {
        state = TextFieldState("abc")
        var shadow by mutableStateOf<Shadow?>(null)
        rule.setContent {
            BasicTextField(
                state = state,
                textStyle = textStyle.copy(color = Color.White, shadow = shadow),
                modifier = Modifier.background(Color.White)
            )
        }

        rule.onNode(hasSetTextAction()).captureToImage().assertPixelConsistency { color ->
            color == Color.White
        }

        shadow = Shadow(blurRadius = 8f)

        val pixelMap = rule.onNode(hasSetTextAction()).captureToImage().toPixelMap()
        for (x in 0 until pixelMap.width) {
            for (y in 0 until pixelMap.height) {
                if (pixelMap[x, y] != Color.White) return // everything is fine, end the test
            }
        }
        throw AssertionError("Could not detect a Shadow in the view")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textDecorationChange_reflectsOnView() {
        state = TextFieldState("abc")
        var textDecoration by mutableStateOf(TextDecoration.None)
        rule.setContent {
            BasicTextField(
                state = state,
                textStyle = textStyle.copy(textDecoration = textDecoration),
                modifier = Modifier.background(Color.White)
            )
        }

        val initialPixelMap = rule.onNode(hasSetTextAction()).captureToImage().toPixelMap()

        textDecoration = TextDecoration.Underline

        val underlinedPixelMap = rule.onNode(hasSetTextAction()).captureToImage().toPixelMap()

        assertThat(initialPixelMap.width to initialPixelMap.height)
            .isEqualTo(underlinedPixelMap.width to underlinedPixelMap.height)

        // They should not be the same due to underline.
        assertThat(initialPixelMap.buffer).isNotEqualTo(underlinedPixelMap.buffer)
    }
}

/**
 * Instead of looking for an exact match of pixel values, this assertion provides the ability to
 * judge each pixel individually to whether it fits a predefined filter.
 */
internal inline fun ImageBitmap.assertPixelConsistency(filter: (color: Color) -> Boolean) {
    val pixel = toPixelMap()
    for (x in 0 until width) {
        for (y in 0 until height) {
            val pxColor = pixel[x, y]
            if (!filter(pxColor)) {
                throw AssertionError(
                    "Pixel at [$x, $y] with the value of [$pxColor] is unexpected!"
                )
            }
        }
    }
}
