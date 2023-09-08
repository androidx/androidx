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

package androidx.compose.foundation.text.selection.gestures

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.gestures.util.TextFieldSelectionAsserter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class TextFieldSelectionGesturesRtlTest : TextFieldSelectionGesturesTest() {

    override val pointerAreaTag = "testTag"

    private val textContent = "בבבבב\nבבבבב בבבבב בבבבב\nבבבבב"

    override val word = "בבבבב"
    override val textFieldValue = mutableStateOf(TextFieldValue(textContent))
    override var textDirection: ResolvedTextDirection = ResolvedTextDirection.Rtl

    override lateinit var asserter: TextFieldSelectionAsserter

    @Composable
    override fun Content() {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = { textFieldValue.value = it },
                textStyle = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(pointerAreaTag),
            )
        }
    }

    override fun characterPosition(offset: Int): Offset {
        val textLayoutResult = rule.onNodeWithTag(pointerAreaTag).fetchTextLayoutResult()
        return textLayoutResult.getBoundingBox(offset).centerRight.nudge(HorizontalDirection.END)
    }
}
