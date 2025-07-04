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

package androidx.compose.material

import android.os.Build
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class TextLinkStylesScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun urlLink() {
        val url = "https://developer.android.com/jetpack/compose"
        val customLinkStyles = TextLinkStyles(style = SpanStyle(color = Color.Red))
        rule.setMaterialContent {
            val textWithLink = buildAnnotatedString {
                append("Text - ")
                withLink(LinkAnnotation.Url(url = url)) { append("Link") }
                append(" - Text - ")
                withLink(LinkAnnotation.Url(url = url, styles = customLinkStyles)) {
                    append("Custom style link")
                }
            }
            Text(textWithLink, modifier = Modifier.testTag(TAG))
        }

        rule
            .onNodeWithTag(TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "textLinkStyles_urlLink")
    }

    @Test
    fun clickableLink() {
        val customLinkStyles = TextLinkStyles(style = SpanStyle(color = Color.Red))
        rule.setMaterialContent {
            val textWithLink = buildAnnotatedString {
                append("Text - ")
                withLink(LinkAnnotation.Clickable(tag = "link1", linkInteractionListener = {})) {
                    append("Link")
                }
                append(" - Text - ")
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "link2",
                        linkInteractionListener = {},
                        styles = customLinkStyles,
                    )
                ) {
                    append("Custom style link")
                }
            }
            Text(textWithLink, modifier = Modifier.testTag(TAG))
        }

        rule
            .onNodeWithTag(TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "textLinkStyles_clickableLink")
    }
}

private const val TAG = "LinkedText"
