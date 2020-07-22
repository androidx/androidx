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
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.compose.foundation.Text
import androidx.compose.ui.graphics.Color
import androidx.ui.integration.test.RandomTextGenerator
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.ui.test.ComposeTestCase
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.ui.unit.Dp
import androidx.ui.unit.TextUnit

/**
 * The benchmark test case for [Text], where the input is an [AnnotatedString] with [TextStyle]s
 * on it.
 */
class TextMultiStyleTestCase(
    private val width: Dp,
    private val fontSize: TextUnit,
    textLength: Int,
    styleCount: Int,
    randomTextGenerator: RandomTextGenerator
) : ComposeTestCase {

    /**
     * Trick to avoid the text word cache.
     * @see TextBasicTestCase.text
     */
    private val text = randomTextGenerator.nextAnnotatedString(
        length = textLength,
        styleCount = styleCount,
        hasMetricAffectingStyle = true
    )

    @Composable
    override fun emitContent() {
        Text(
            text = text, color = Color.Black, fontSize = fontSize,
            modifier = Modifier.wrapContentSize(Alignment.Center).preferredWidth(width)
        )
    }
}