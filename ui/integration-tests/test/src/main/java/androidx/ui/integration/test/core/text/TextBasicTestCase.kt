/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.integration.test.core.text

import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutWidth
import androidx.ui.test.ComposeTestCase
import androidx.ui.integration.test.RandomTextGenerator
import androidx.ui.layout.LayoutAlign
import androidx.ui.text.TextStyle

/**
 * The benchmark test case for [Text], where the input is a plain string.
 */
class TextBasicTestCase(
    textLength: Int,
    randomTextGenerator: RandomTextGenerator
) : ComposeTestCase {

    /**
     * Text render has a word cache in the underlying system. To get a proper metric of its
     * performance, the cache needs to be disabled, which unfortunately is not doable right now.
     * Here is a workaround which generates a new string when setupContentInternal is called.
     * Notice that this function is called whenever a new ViewTree(and of course the text composable)
     * is recreated. This helps to make sure that the text composable created later won't benefit
     * from the previous result.
     */
    private val text = randomTextGenerator.nextParagraph(textLength)

    @Composable
    override fun emitContent() {
        Text(
            text = text, style = TextStyle(color = Color.Black, fontSize = 8.sp),
            modifier = LayoutAlign.Center + LayoutWidth(160.dp)
        )
    }
}