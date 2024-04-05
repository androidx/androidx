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

package androidx.compose.material

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests text layout in text fields.
 */
@OptIn(ExperimentalTestApi::class)
class TextLayout {
    @Test
    fun getLineForOffsetReturnsLastLineWhenOffsetIsBeyondLast() = runComposeUiTest {
        lateinit var textLayoutResult: TextLayoutResult

        setContent {
            val text = remember { "X\n".repeat(100) }

            BasicTextField(
                value = text,
                onValueChange = {},
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier.fillMaxSize()
            )
        }

        assertEquals(100, textLayoutResult.getLineForOffset(Int.MAX_VALUE))
    }

    @Test
    fun getLineForVerticalPositionReturnsLastLineWhenPositionIsBeyondLastLine() = runComposeUiTest {
        lateinit var textLayoutResult: TextLayoutResult

        setContent {
            val text = remember { "Compose rules!\n".repeat(100) }

            BasicTextField(
                value = text,
                onValueChange = {},
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier.fillMaxSize()
            )
        }

        assertEquals(100, textLayoutResult.getLineForVerticalPosition(Float.MAX_VALUE))
    }
}