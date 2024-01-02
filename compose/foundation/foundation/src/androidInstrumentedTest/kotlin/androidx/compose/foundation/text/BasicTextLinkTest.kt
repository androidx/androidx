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

package androidx.compose.foundation.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTextApi::class)
@MediumTest
class BasicTextLinkTest {
    @get:Rule
    val rule = createComposeRule()

    private val fontSize = 20.sp
    private val focusRequester = FocusRequester()
    private lateinit var focusManager: FocusManager
    private var openedUri: String? = null
    private var layoutResult: TextLayoutResult? = null
    private val uriHandler = object : UriHandler {
        override fun openUri(uri: String) {
            openedUri = uri
        }
    }

    private val Url1 = "link1"
    private val Url2 = "link2"
    private val Url3 = "link3"

    @Before
    fun setup() {
        openedUri = null
    }

    @Test
    fun multipleLinks_lastGetsFocus() {
        setupContent { TextWithLinks() }

        rule.runOnIdle {
            focusRequester.requestFocus()
            focusManager.moveFocus(FocusDirection.Previous)
        }

        rule.onAllNodes(hasClickAction())[2].assertIsFocused()
    }

    @Test
    fun multipleLinks_middleGetsFocus() {
        setupContent { TextWithLinks() }

        rule.runOnIdle {
            focusRequester.requestFocus()
            focusManager.moveFocus(FocusDirection.Previous)
            focusManager.moveFocus(FocusDirection.Previous)
        }

        rule.onAllNodes(hasClickAction())[2].assertIsNotFocused()
        rule.onAllNodes(hasClickAction())[1].assertIsFocused()
    }

    @Test
    fun multipleLinks_firstGetsFocus() {
        setupContent { TextWithLinks() }

        rule.runOnIdle {
            focusRequester.requestFocus()
            focusManager.moveFocus(FocusDirection.Previous)
            focusManager.moveFocus(FocusDirection.Previous)
            focusManager.moveFocus(FocusDirection.Previous)
        }

        rule.onAllNodes(hasClickAction())[2].assertIsNotFocused()
        rule.onAllNodes(hasClickAction())[1].assertIsNotFocused()
        rule.onAllNodes(hasClickAction())[0].assertIsFocused()
    }

    @Test
    fun multipleLinks_onClick_insideFirstLink_opensFirstUrl() {
        setupContent { TextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule.onFirstText().performTouchInput {
            val boundingBox = layoutResult!!.getBoundingBox(7)
            click(boundingBox.center)
        }

        rule.runOnIdle {
            assertThat(openedUri).isEqualTo(Url1)
        }
    }

    @Test
    fun multipleLinks_onClick_insideSecondLink_opensSecondUrl() {
        setupContent { TextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule.onFirstText().performTouchInput {
            val boundingBox = layoutResult!!.getBoundingBox(20)
            click(boundingBox.center)
        }

        rule.runOnIdle {
            assertThat(openedUri).isEqualTo(Url2)
        }
    }

    @Test
    fun multipleLinks_onClick_outsideLinks_doNothing() {
        setupContent { TextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule.onFirstText().performTouchInput {
            val boundingBox = layoutResult!!.getBoundingBox(2)
            click(boundingBox.center)
        }

        assertThat(openedUri).isNull()
    }

    @Test
    fun multipleLinks_onClick_inBetweenLinks_doNothing() {
        setupContent { TextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule.onFirstText().performTouchInput {
            val boundingBox = layoutResult!!.getBoundingBox(12)
            click(boundingBox.center)
        }

        rule.runOnIdle {
            assertThat(openedUri).isNull()
        }
    }

    @Test
    fun link_spansTwoLines_onClick_opensSecondUrl() {
        setupContent { TextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule.onFirstText().performTouchInput {
            val boundingBox = layoutResult!!.getBoundingBox(24)
            click(boundingBox.center)
        }

        rule.runOnIdle {
            assertThat(openedUri).isEqualTo(Url2)
        }
    }

    @Test
    fun link_andInlineContent_onClick_opensUrl() {
        setupContent {
            /***
             * +--------------------+
             * | link text [ ] text |
             * +--------------------+
             */
            val text = buildAnnotatedString {
                withAnnotation(UrlAnnotation(Url1)) { append("link") }
                append(" text ")
                appendInlineContent("box")
                append(" text")
            }
            val inlineTextContent = InlineTextContent(
                placeholder = Placeholder(
                    fontSize,
                    fontSize,
                    PlaceholderVerticalAlign.Center
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("box")
                )
            }
            BasicText(
                text = text,
                inlineContent = mapOf("box" to inlineTextContent)
            )
        }

        rule.onAllNodes(hasClickAction())[0].performClick()

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            assertThat(openedUri).isEqualTo(Url1)
        }
    }

    @Composable
    private fun TextWithLinks() = with(rule.density) {
        Column {
            /***
             * +-----------------------+
             * | text link text a long |
             * | link text             |
             * | text link             |
             * | [ ]                   |
             * +-----------------------+
             */
            val style = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY)

            val text = buildAnnotatedString {
                append("text ")
                withAnnotation(UrlAnnotation(Url1)) {
                    append("link ")
                }
                append("text ")
                withAnnotation(UrlAnnotation(Url2)) {
                    append("a long link ")
                }
                append("text")
            }
            val widthDp = (fontSize * 22).toDp() // to fit text in the middle of the second link
            BasicText(
                text = text,
                modifier = Modifier.width(widthDp),
                onTextLayout = { layoutResult = it },
                style = style
            )

            BasicText(buildAnnotatedString {
                append("text ")
                withAnnotation(UrlAnnotation(Url3)) {
                    append("link ")
                }
            }, style = style)

            // initial focus
            Box(
                Modifier
                    .size(10.dp)
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
    }

    private fun setupContent(content: @Composable () -> Unit) {
        rule.setContent {
            focusManager = LocalFocusManager.current
            val viewConfiguration = DelegatedViewConfiguration(
                LocalViewConfiguration.current,
                DpSize.Zero
            )
            CompositionLocalProvider(
                LocalUriHandler provides uriHandler,
                LocalViewConfiguration provides viewConfiguration,
                content = content
            )
        }
    }

    private fun SemanticsNodeInteractionsProvider.onFirstText(): SemanticsNodeInteraction =
        onAllNodesWithText("text", substring = true)[0]
}

private class DelegatedViewConfiguration(
    delegate: ViewConfiguration,
    minimumTouchTargetSizeOverride: DpSize,
) : ViewConfiguration by delegate {
    override val minimumTouchTargetSize: DpSize = minimumTouchTargetSizeOverride
}
