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

package androidx.compose.foundation.text

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.unit.sp
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class TextLinkStyleTest {
    @get:Rule
    val rule = createComposeRule()

    val fontSize = 20.sp
    val tag = "Text field test tag"

    @Test
    fun defaultLinkStyle_color() {
        rule.setContent {
            TestText(buildAnnotatedString {
                withAnnotation(LinkAnnotation.Url("example.com")) { append("link") }
            })
        }

        with(rule.onNodeWithTag(tag).captureToImage()) {
            assertContainsColor(Color.Black)
        }
    }

    @Test
    fun customLinkStyle_color() {
        rule.setContent {
            CompositionLocalProvider(LocalTextLinkStyle provides SpanStyle(color = Color.Red)) {
                TestText(buildAnnotatedString {
                    withAnnotation(LinkAnnotation.Url("example.com")) { append("link") }
                })
            }
        }

        with(rule.onNodeWithTag(tag).captureToImage()) {
            assertContainsColor(Color.Red)
            assertDoesNotContainColor(Color.Black)
        }
    }

    @Test
    fun customLinkStyle() {
        val expectedStyle = SpanStyle(
            color = Color.Red,
            textDecoration = TextDecoration.LineThrough,
            fontSize = fontSize * 2
        )
        lateinit var spansStyle: SpanStyle
        rule.setContent {
            CompositionLocalProvider(LocalTextLinkStyle provides expectedStyle) {
                TestText(buildAnnotatedString {
                    append("text ")
                    withAnnotation(LinkAnnotation.Url("example.com")) { append("link") }
                    append(" text")
                }, onTextLayout = {
                    spansStyle = it.layoutInput.text.spanStyles.first().item
                })
            }
        }

        assertThat(spansStyle).isEqualTo(expectedStyle)
    }

    @Composable
    private fun TestText(
        text: AnnotatedString,
        onTextLayout: ((TextLayoutResult) -> Unit)? = null
    ) {
        val testTextStyle = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY)
        BasicText(
            text = text,
            modifier = Modifier.testTag(tag),
            style = testTextStyle,
            onTextLayout = onTextLayout
        )
    }
}
