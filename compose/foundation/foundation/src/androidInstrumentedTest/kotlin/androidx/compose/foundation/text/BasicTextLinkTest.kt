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

import android.os.Build
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.MouseInjectionScope
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getFirstLinkBounds
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkAnnotation.Url
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class BasicTextLinkTest {
    @get:Rule val rule = createComposeRule()

    private val fontSize = 20.sp
    private val focusRequester = FocusRequester()
    private lateinit var focusManager: FocusManager
    private var openedUri: String? = null
    private var layoutResult: TextLayoutResult? = null
    private val uriHandler =
        object : UriHandler {
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

        rule.onAllNodes(hasClickAction(), useUnmergedTree = true)[2].assertIsFocused()
    }

    @Test
    fun multipleLinks_middleGetsFocus() {
        setupContent { TextWithLinks() }

        rule.runOnIdle {
            focusRequester.requestFocus()
            focusManager.moveFocus(FocusDirection.Previous)
            focusManager.moveFocus(FocusDirection.Previous)
        }

        rule.onAllNodes(hasClickAction(), useUnmergedTree = true)[2].assertIsNotFocused()
        rule.onAllNodes(hasClickAction(), useUnmergedTree = true)[1].assertIsFocused()
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

        rule.onAllNodes(hasClickAction(), useUnmergedTree = true)[2].assertIsNotFocused()
        rule.onAllNodes(hasClickAction(), useUnmergedTree = true)[1].assertIsNotFocused()
        rule.onAllNodes(hasClickAction(), useUnmergedTree = true)[0].assertIsFocused()
    }

    @Test
    fun multipleLinks_onClick_insideFirstLink_opensFirstUrl() {
        setupContent { TextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule.onFirstText().performTouchInputOnFirstLink({ (it.item as? Url)?.url == Url1 }) {
            click(it)
        }

        rule.runOnIdle { assertThat(openedUri).isEqualTo(Url1) }
    }

    @Test
    fun multipleLinks_onClick_insideSecondLink_opensSecondUrl() {
        setupContent { TextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule.onFirstText().performTouchInputOnFirstLink({ (it.item as? Url)?.url == Url2 }) {
            click(it)
        }
        rule.runOnIdle { assertThat(openedUri).isEqualTo(Url2) }
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

        rule.runOnIdle { assertThat(openedUri).isNull() }
    }

    @Test
    fun link_spansTwoLines_onClick_opensSecondUrl() {
        setupContent { TextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule.onFirstText().performTouchInput {
            val boundingBox = layoutResult!!.getBoundingBox(24)
            click(boundingBox.center)
        }

        rule.runOnIdle { assertThat(openedUri).isEqualTo(Url2) }
    }

    @Test
    fun rtlText_onClick_insideFirstLink_opensFirstUrl() {
        setupContent { RtlTextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            .performTouchInputOnFirstLink({ (it.item as? Url)?.url == Url1 }) { click(it) }

        rule.runOnIdle { assertThat(openedUri).isEqualTo(Url1) }
    }

    @Test
    fun rtlText_onClick_insideSecondLink_opensSecondUrl() {
        setupContent { RtlTextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            .performTouchInputOnFirstLink({ (it.item as? Url)?.url == Url2 }) { click(it) }

        rule.runOnIdle { assertThat(openedUri).isEqualTo(Url2) }
    }

    @Test
    fun rtlText_onClick_outsideLink_doNothing() {
        setupContent { RtlTextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)).performTouchInput {
            val boundingBox = layoutResult!!.getBoundingBox(35)
            click(boundingBox.center)
        }

        rule.runOnIdle { assertThat(openedUri).isEqualTo(null) }
    }

    @Test
    fun rtlText_onClick_inBetweenLinks_doNothing() {
        setupContent { RtlTextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)).performTouchInput {
            val boundingBox = layoutResult!!.getBoundingBox(20)
            click(boundingBox.center)
        }

        rule.runOnIdle { assertThat(openedUri).isEqualTo(null) }
    }

    @Test
    fun bidiText_onClick_insideLink_opensUrl() {
        setupContent { BidiTextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            .performTouchInputOnFirstLink({ (it.item as? Url)?.url == Url1 }) { click(it) }

        rule.runOnIdle { assertThat(openedUri).isEqualTo(Url1) }
    }

    @Test
    fun bidiText_onClick_outsideLink_doNothing() {
        setupContent { BidiTextWithLinks() }

        rule.runOnIdle { assertThat(layoutResult).isNotNull() }
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)).performTouchInput {
            val boundingBox = layoutResult!!.getBoundingBox(2)
            click(boundingBox.center)
        }

        rule.runOnIdle { assertThat(openedUri).isEqualTo(null) }
    }

    @Test
    fun link_andInlineContent_onClick_opensUrl() {
        setupContent {
            /** +--------------------+ | link text [ ] text | +--------------------+ */
            val text = buildAnnotatedString {
                withLink(Url(Url1)) { append("link") }
                append(" text ")
                appendInlineContent("box")
                append(" text")
            }
            val inlineTextContent =
                InlineTextContent(
                    placeholder = Placeholder(fontSize, fontSize, PlaceholderVerticalAlign.Center)
                ) {
                    Box(modifier = Modifier.fillMaxSize().testTag("box"))
                }
            BasicText(text = text, inlineContent = mapOf("box" to inlineTextContent))
        }

        rule.onNodeWithText("text", substring = true).performTouchInputOnFirstLink({
            (it.item as? Url)?.url == Url1
        }) {
            click(it)
        }

        rule.onNodeWithTag("box", useUnmergedTree = true).assertExists()
        rule.runOnIdle { assertThat(openedUri).isEqualTo(Url1) }
    }

    @Test
    fun link_withTranslatedString() {
        val originalText = buildAnnotatedString {
            append("text ")
            withLink(Url(Url1)) { append("link") }
        }
        setupContent { BasicText(originalText) }

        // set translated string
        val node = rule.onFirstText().fetchSemanticsNode()
        rule.runOnUiThread {
            val translatedText = buildAnnotatedString { append("text") }
            node.config[SemanticsActions.SetTextSubstitution].action?.invoke(translatedText)
        }
        rule.waitForIdle()

        rule.runOnUiThread {
            // show the translated text
            node.config[SemanticsActions.ShowTextSubstitution].action?.invoke(true)
        }
        rule.waitForIdle()

        rule.onFirstText().performClick()
        rule.runOnIdle { assertThat(openedUri).isEqualTo(null) }
    }

    @Test
    fun updateColor_insideAnnotation_retainsFocusCorrectly() {
        setupContent {
            Column {
                // initial focus
                Box(Modifier.testTag("box").size(10.dp).focusRequester(focusRequester).focusable())

                val color = remember { mutableStateOf(Color.Red) }
                BasicText(
                    buildAnnotatedString {
                        withLink(Url(Url1)) {
                            withStyle(SpanStyle(color = color.value)) { append("link") }
                        }
                    },
                    modifier =
                        Modifier.onFocusChanged {
                            color.value = if (it.hasFocus) Color.Green else Color.Red
                        }
                )
            }
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
            focusManager.moveFocus(FocusDirection.Down)
        }

        rule.onNodeWithTag("box").assertIsNotFocused()
        rule.onNode(hasClickAction(), useUnmergedTree = true).assertIsFocused()
    }

    @Test
    fun linkMeasure_withExceededMaxConstraintSize_doesNotCrash() {
        val textWithLink = buildAnnotatedString {
            withLink(Url("link")) { append("text ".repeat(25_000)) }
        }

        expectError<IllegalArgumentException>(expectError = false) {
            setupContent {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    BasicText(text = textWithLink, modifier = Modifier.width(100.dp))
                }
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_styleFromAnnotationUsed() {
        val textWithLink = buildAnnotatedString {
            withLink(Url("link", TextLinkStyles(style = SpanStyle(color = Color.Red)))) {
                append("text")
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule.onNodeWithText("text").captureToImage().assertContainsColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_withinOtherStyle_styleFromAnnotationUsed() {
        val textWithLink = buildAnnotatedString {
            withStyle(SpanStyle(background = Color.Red)) {
                withLink(Url("link", TextLinkStyles(SpanStyle(color = Color.Green)))) {
                    append("text")
                }
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNodeWithText("text")
            .captureToImage()
            .assertContainsColor(Color.Green)
            .assertContainsColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_onHover_hoveredStyleUsed() {
        val textWithLink = buildAnnotatedString {
            withLink(
                Url(
                    "link",
                    TextLinkStyles(
                        style = SpanStyle(background = Color.Red),
                        hoveredStyle = SpanStyle(background = Color.Green)
                    )
                )
            ) {
                append("text")
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNodeWithText("text")
            .performMouseInputOnFirstLink { moveTo(it) }
            .captureToImage()
            .assertContainsColor(Color.Green)
            .assertDoesNotContainColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_withinOtherStyle_onHover_hoveredStyleUsed() {
        val textWithLink = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Blue)) {
                withLink(
                    Url(
                        "link",
                        TextLinkStyles(
                            style = SpanStyle(background = Color.Red),
                            hoveredStyle = SpanStyle(background = Color.Green)
                        )
                    )
                ) {
                    append("text")
                }
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNodeWithText("text")
            .performMouseInputOnFirstLink { moveTo(it) }
            .captureToImage()
            .assertContainsColor(Color.Green)
            .assertDoesNotContainColor(Color.Red)
            .assertContainsColor(Color.Blue)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_onHover_hoveredStyle_mergedIntoNormal() {
        val textWithLink = buildAnnotatedString {
            withLink(
                Url(
                    "link",
                    TextLinkStyles(
                        style = SpanStyle(color = Color.Red),
                        hoveredStyle = SpanStyle(background = Color.Green)
                    )
                )
            ) {
                append("text")
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNodeWithText("text")
            .performMouseInputOnFirstLink { moveTo(it) }
            .captureToImage()
            .assertContainsColor(Color.Green)
            .assertContainsColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_onFocus_focusedStyleUsed() {
        val textWithLink = buildAnnotatedString {
            withLink(
                Url(
                    "link",
                    TextLinkStyles(
                        style = SpanStyle(background = Color.Red),
                        focusedStyle = SpanStyle(background = Color.Blue)
                    )
                )
            ) {
                append("text")
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNode(hasClickAction(), useUnmergedTree = true)
            .requestFocus()
            .captureToImage()
            .assertContainsColor(Color.Blue)
            .assertDoesNotContainColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_onFocus_restoresOriginalStyle_afterFocusLost() {
        val textWithLink = buildAnnotatedString {
            withLink(
                Url("link", TextLinkStyles(focusedStyle = SpanStyle(background = Color.Blue)))
            ) {
                append("text")
            }
        }
        setupContent {
            Column {
                Box(Modifier.size(10.dp).focusable().testTag("box"))
                BasicText(text = textWithLink, style = TextStyle(color = Color.White))
            }
        }

        // link captures focus
        rule
            .onNode(hasClickAction(), useUnmergedTree = true)
            .requestFocus()
            .captureToImage()
            .assertContainsColor(Color.Blue)

        // link loses focus
        rule.onNodeWithTag("box").requestFocus()

        // verify link restores its unfocused style
        rule
            .onNode(hasClickAction(), useUnmergedTree = true)
            .captureToImage()
            .assertDoesNotContainColor(Color.Blue)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_withinOtherStyle_onFocus_focusedStyleUsed() {
        val textWithLink = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Green)) {
                withLink(
                    Url(
                        "link",
                        TextLinkStyles(
                            style = SpanStyle(background = Color.Red),
                            focusedStyle = SpanStyle(background = Color.Blue)
                        )
                    )
                ) {
                    append("text")
                }
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNode(hasClickAction(), useUnmergedTree = true)
            .requestFocus()
            .captureToImage()
            .assertContainsColor(Color.Blue)
            .assertDoesNotContainColor(Color.Red)
            .assertContainsColor(Color.Green)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_onFocus_focusedStyle_mergedIntoNormal() {
        val textWithLink = buildAnnotatedString {
            withLink(
                Url(
                    "link",
                    TextLinkStyles(
                        style = SpanStyle(color = Color.Red),
                        focusedStyle = SpanStyle(background = Color.Blue)
                    )
                )
            ) {
                append("text")
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNode(hasClickAction(), useUnmergedTree = true)
            .requestFocus()
            .captureToImage()
            .assertContainsColor(Color.Blue)
            .assertContainsColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_onFocus_onHovered_onHoveredStyleUsed() {
        val textWithLink = buildAnnotatedString {
            withLink(
                Url(
                    "link",
                    TextLinkStyles(
                        hoveredStyle = SpanStyle(background = Color.Green),
                        focusedStyle = SpanStyle(background = Color.Blue)
                    )
                )
            ) {
                append("text")
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNode(hasClickAction(), useUnmergedTree = true)
            .requestFocus()
            .performMouseInput { moveTo(this.center) }
            .captureToImage()
            .assertContainsColor(Color.Green)
            .assertDoesNotContainColor(Color.Blue)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_onHovered_onFocus_onHoveredStyleUsed() {
        val textWithLink = buildAnnotatedString {
            withLink(
                Url(
                    "link",
                    TextLinkStyles(
                        hoveredStyle = SpanStyle(background = Color.Green),
                        focusedStyle = SpanStyle(background = Color.Blue)
                    )
                )
            ) {
                append("text")
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNode(hasClickAction(), useUnmergedTree = true)
            .performMouseInput { moveTo(this.center) }
            .requestFocus()
            .captureToImage()
            .assertContainsColor(Color.Green)
            .assertDoesNotContainColor(Color.Blue)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_hoveredAndFocused_mergedStyle() {
        val textWithLink = buildAnnotatedString {
            withLink(
                Url(
                    "link",
                    TextLinkStyles(
                        hoveredStyle = SpanStyle(color = Color.Green),
                        focusedStyle = SpanStyle(background = Color.Blue)
                    )
                )
            ) {
                append("text")
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNode(hasClickAction(), useUnmergedTree = true)
            .performMouseInput { moveTo(this.center) }
            .requestFocus()
            .captureToImage()
            .assertContainsColor(Color.Green)
            .assertContainsColor(Color.Blue)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_onPressed_pressedStyleUsed() {
        val textWithLink = buildAnnotatedString {
            withLink(Url("link", TextLinkStyles(pressedStyle = SpanStyle(color = Color.Green)))) {
                append("text")
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNodeWithText("text")
            .performTouchInputOnFirstLink { longPress(it) }
            .captureToImage()
            .assertContainsColor(Color.Green)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_withOtherStyle_onPressed_pressedStyleUsed() {
        val textWithLink = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Green)) {
                withLink(
                    Url("link", TextLinkStyles(pressedStyle = SpanStyle(color = Color.Blue)))
                ) {
                    append("text")
                }
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNodeWithText("text")
            .performTouchInputOnFirstLink { longPress(it) }
            .captureToImage()
            .assertDoesNotContainColor(Color.Green)
            .assertContainsColor(Color.Blue)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_onPressed_whenHoveredAndFocused_pressedStyleUsed() {
        val textWithLink = buildAnnotatedString {
            withLink(
                Url(
                    "link",
                    TextLinkStyles(
                        focusedStyle = SpanStyle(color = Color.Red),
                        hoveredStyle = SpanStyle(color = Color.Green),
                        pressedStyle = SpanStyle(color = Color.Blue)
                    )
                )
            ) {
                append("text")
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNode(hasClickAction(), useUnmergedTree = true)
            .requestFocus()
            .performMouseInput { moveTo(this.center) }
            .performTouchInput { longPress(this.center) }
            .captureToImage()
            .assertContainsColor(Color.Blue)
            .assertDoesNotContainColor(Color.Red)
            .assertDoesNotContainColor(Color.Green)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_onPressed_whenFocused_mergedStyle() {
        val textWithLink = buildAnnotatedString {
            withLink(
                Url(
                    "link",
                    TextLinkStyles(
                        focusedStyle = SpanStyle(background = Color.Green),
                        pressedStyle = SpanStyle(color = Color.Blue)
                    )
                )
            ) {
                append("text")
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNode(hasClickAction(), useUnmergedTree = true)
            .requestFocus()
            .performTouchInput { longPress(this.center) }
            .captureToImage()
            .assertContainsColor(Color.Blue)
            .assertContainsColor(Color.Green)
    }

    @Test
    fun linkAnnotation_url_customInteractionListener_called() {
        var clickedUrlAnnotation: Url? = null
        setupContent {
            BasicText(
                buildAnnotatedString {
                    withLink(Url("qwerty") { clickedUrlAnnotation = it as Url }) { append("link") }
                }
            )
        }

        rule.onNode(hasClickAction(), useUnmergedTree = true).performClick()

        rule.runOnIdle {
            assertThat(openedUri).isNull()
            assertThat(clickedUrlAnnotation).isNotNull()
            assertThat(clickedUrlAnnotation!!.url).isEqualTo("qwerty")
        }
    }

    @Test
    fun linkAnnotation_clickable_customInteractionListener_called() {
        var clickedAnnotation: LinkAnnotation.Clickable? = null
        setupContent {
            BasicText(
                buildAnnotatedString {
                    withLink(
                        LinkAnnotation.Clickable("qwerty") {
                            clickedAnnotation = it as LinkAnnotation.Clickable
                        }
                    ) {
                        append("link")
                    }
                }
            )
        }

        rule.onNode(hasClickAction(), useUnmergedTree = true).performClick()

        rule.runOnIdle {
            assertThat(clickedAnnotation).isNotNull()
            assertThat(clickedAnnotation!!.tag).isEqualTo("qwerty")
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun link_doesNotReplaceNestedStyle_whenEmptyStyle() {
        val textWithLink = buildAnnotatedString {
            withLink(Url("link", TextLinkStyles(focusedStyle = SpanStyle(Color.Blue)))) {
                withStyle(SpanStyle(Color.Green)) { append("text") }
            }
        }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        rule
            .onNode(hasClickAction(), useUnmergedTree = true)
            .captureToImage()
            .assertDoesNotContainColor(Color.Blue)
            .assertContainsColor(Color.Green)

        rule
            .onNode(hasClickAction(), useUnmergedTree = true)
            .requestFocus()
            .captureToImage()
            .assertDoesNotContainColor(Color.Blue)
            .assertContainsColor(Color.Green)
    }

    @Test
    fun link_doesNotModifyAnnotations_whenEmptyStyles() {
        val textWithLink = buildAnnotatedString { withLink(Url("link")) { append("text") } }
        setupContent { BasicText(text = textWithLink, style = TextStyle(color = Color.White)) }

        val styles = rule.onNodeWithText("text").fetchTextLayoutResult().layoutInput.text.spanStyles

        assertThat(styles).isEmpty()
    }

    @Test
    fun links_displayedWithCorrectStyle_onFirstFrame() {
        val checked = mutableStateOf(false)
        var calledAfterChecked = false

        rule.setContent {
            calledAfterChecked = checked.value
            BasicText(
                buildAnnotatedString {
                    withLink(
                        LinkAnnotation.Clickable(
                            "tag",
                            TextLinkStyles(SpanStyle(color = Color.Green))
                        ) {}
                    ) {
                        append("Link")
                    }
                },
                style = TextStyle(Color.Red),
                onTextLayout = {
                    // When the flicker happens, the BasicText is first composed and drawn
                    // with the empty span styles. Only after the recomposition the link
                    // style is applied. After the fix the first composition happens with
                    // the correct link color and therefore the span styles will contain it
                    val colors = it.layoutInput.text.spanStyles.map { it.item.color }
                    assertThat(colors).isNotEmpty()
                    assertThat(colors.first()).isEqualTo(Color.Green)
                }
            )
        }

        checked.value = true
        rule.runOnIdle {
            // ensures that recomposition happened after a `checked` change
            assertThat(calledAfterChecked).isTrue()
        }
    }

    @Composable
    private fun TextWithLinks() =
        with(rule.density) {
            Column(Modifier.padding(87.dp)) {
                /**
                 * +-----------------------+ | text link text a long | | link text | | text link | |
                 * [ ] | +-----------------------+
                 */
                val style = TextStyle(fontSize = fontSize, fontFamily = TEST_FONT_FAMILY)

                val text = buildAnnotatedString {
                    append("text ")
                    withLink(Url(Url1)) { append("link ") }
                    append("text ")
                    withLink(Url(Url2)) { append("a long link ") }
                    append("text")
                }
                val widthDp = (fontSize * 22).toDp() // to fit text in the middle of the second link
                BasicText(
                    text = text,
                    modifier = Modifier.width(widthDp),
                    onTextLayout = { layoutResult = it },
                    style = style
                )

                BasicText(
                    buildAnnotatedString {
                        append("text ")
                        withLink(Url(Url3)) { append("link ") }
                    },
                    style = style
                )

                // initial focus
                Box(Modifier.size(10.dp).focusRequester(focusRequester).focusTarget())
            }
        }

    @Composable
    private fun BidiTextWithLinks() {
        val text = buildAnnotatedString {
            append("\u05D0\u05D1 \u05D2\u05D3")
            withLink(Url(Url1)) { append(" text ") }
            append("\u05D0\u05D1 \u05D2\u05D3 \u05D0\u05D1 \u05D2\u05D3")
        }
        BasicText(text, onTextLayout = { layoutResult = it })
    }

    @Composable
    private fun RtlTextWithLinks() {
        val text = buildAnnotatedString {
            withLink(Url(Url1)) { append("\u05D0\u05D1 \u05D2\u05D3 \u05D0\u05D1") }
            append(" \u05D0\u05D1 \u05D2\u05D3 \u05D0\u05D1 \u05D0\u05D1 \u05D2\u05D3")
            withLink(Url(Url2)) { append(" \u05D0\u05D1 \u05D2\u05D3 \u05D0\u05D1") }
            append("\u05D0\u05D1 \u05D2\u05D3")
        }
        BasicText(text, onTextLayout = { layoutResult = it })
    }

    private fun setupContent(content: @Composable () -> Unit) {
        val keyboardMockManager =
            object : InputModeManager {
                override val inputMode = InputMode.Keyboard

                override fun requestInputMode(inputMode: InputMode) = true
            }
        rule.setContent {
            focusManager = LocalFocusManager.current
            val viewConfiguration =
                DelegatedViewConfiguration(LocalViewConfiguration.current, DpSize.Zero)
            CompositionLocalProvider(
                LocalUriHandler provides uriHandler,
                LocalViewConfiguration provides viewConfiguration,
                LocalInputModeManager provides keyboardMockManager,
                content = content
            )
        }
    }

    private fun SemanticsNodeInteractionsProvider.onFirstText(): SemanticsNodeInteraction =
        onAllNodesWithText("text", substring = true)[0]

    private fun SemanticsNodeInteraction.performMouseInputOnFirstLink(
        predicate: (AnnotatedString.Range<LinkAnnotation>) -> Boolean = { true },
        block: MouseInjectionScope.(offsetInLink: Offset) -> Unit
    ): SemanticsNodeInteraction {
        val linkBounds = getFirstLinkBounds(predicate)
        return this.performMouseInput { block(linkBounds!!.center) }
    }

    private fun SemanticsNodeInteraction.performTouchInputOnFirstLink(
        predicate: (AnnotatedString.Range<LinkAnnotation>) -> Boolean = { true },
        block: TouchInjectionScope.(offsetInLink: Offset) -> Unit
    ): SemanticsNodeInteraction {
        val linkBounds = getFirstLinkBounds(predicate)
        return this.performTouchInput { block(linkBounds!!.center) }
    }
}

private class DelegatedViewConfiguration(
    delegate: ViewConfiguration,
    minimumTouchTargetSizeOverride: DpSize,
) : ViewConfiguration by delegate {
    override val minimumTouchTargetSize: DpSize = minimumTouchTargetSizeOverride
}
