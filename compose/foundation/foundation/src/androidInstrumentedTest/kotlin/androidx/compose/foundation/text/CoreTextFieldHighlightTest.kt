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

package androidx.compose.foundation.text

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.FillableData
import androidx.compose.ui.autofill.createFrom
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions.OnFillData
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CoreTextFieldHighlightTest {
    @get:Rule val rule = createComposeRule()
    val testFieldTag = "TextField"
    val defaultHighlightColor = autofillHighlightColor()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun assertAutofillHighlightColor_whenPerformTextAction() {
        val value = TextFieldValue("initial text")
        val textToAutofill = checkNotNull(FillableData.createFrom("foo"))

        val textFieldBackgroundColor = Color.White
        val expectedHighlightColor = blendColors(defaultHighlightColor, textFieldBackgroundColor)

        rule.setContent {
            CoreTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.testTag(testFieldTag).background(color = Color.White),
            )
        }

        rule.onNodeWithTag(testFieldTag).performTextAction(OnFillData, data = textToAutofill)
        rule.onNodeWithTag(testFieldTag).captureToImage().let { imageBitmap ->
            val pixelMap = imageBitmap.toPixelMap()
            val actualColor = pixelMap[imageBitmap.width / 2, imageBitmap.height / 2]

            assertEquals(
                "The center pixel's color does not match the highlight color.",
                expectedHighlightColor,
                actualColor,
            )
        }
    }
}

/**
 * Performs a semantics action that requires a FillableData argument, such as the internal
 * 'onFillableData' action.
 *
 * @param key The SemanticsPropertyKey for the action.
 * @param data The FillableData argument to pass to the action.
 */
internal fun SemanticsNodeInteraction.performTextAction(
    key: SemanticsPropertyKey<AccessibilityAction<(FillableData) -> Boolean>>,
    data: FillableData,
): SemanticsNodeInteraction {
    return performSemanticsAction(key) { it.invoke(data) }
}

/** Blends a semi-transparent [source] color over an opaque [destination] color. */
internal fun blendColors(source: Color, destination: Color): Color {
    val sourceAlpha = source.alpha
    val destAlpha = 1 - sourceAlpha

    val red = source.red * sourceAlpha + destination.red * destAlpha
    val green = source.green * sourceAlpha + destination.green * destAlpha
    val blue = source.blue * sourceAlpha + destination.blue * destAlpha

    return Color(red, green, blue)
}
