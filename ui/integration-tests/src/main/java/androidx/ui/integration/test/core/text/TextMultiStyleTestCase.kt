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
import androidx.ui.foundation.Text
import androidx.ui.graphics.Color
import androidx.ui.integration.test.RandomTextGenerator
import androidx.ui.layout.preferredWidth
import androidx.ui.layout.wrapContentSize
import androidx.ui.test.ComposeTestCase
import androidx.ui.text.AnnotatedString
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.sp

/**
 * The benchmark test case for [Text], where the input is an [AnnotatedString] with [TextStyle]s
 * on it.
 */
class TextMultiStyleTestCase(
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
            text = text, color = Color.Black, fontSize = 8.sp,
            modifier = Modifier.wrapContentSize(Alignment.Center).preferredWidth(160.dp)
        )
    }
}