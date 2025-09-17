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

package androidx.compose.ui.test.actions

import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.expectAssertionError
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.em
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class LinkClickTest {
    @get:Rule val rule = createComposeRule()

    private val noTextFoundMessage = "Failed to click the link.\n Reason: No text found on node."

    @Test
    fun mergedTreeLookup_oneLink_clicked() {
        val url = "url_to_open"
        var clickedUrl: String? = null

        rule.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides
                    object : UriHandler {
                        override fun openUri(uri: String) {
                            clickedUrl = uri
                        }
                    }
            ) {
                BasicText(
                    buildAnnotatedString { withLink(LinkAnnotation.Url(url)) { append("link") } }
                )
            }
        }

        rule.onNodeWithText("link").performFirstLinkClick { true }
        rule.runOnIdle { assertThat(clickedUrl).isEqualTo(url) }
    }

    @Test
    fun unmergedTreeLookup_oneLink_clicked() {
        val url = "url_to_open"
        var clickedUrl: String? = null

        rule.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides
                    object : UriHandler {
                        override fun openUri(uri: String) {
                            clickedUrl = uri
                        }
                    }
            ) {
                BasicText(
                    buildAnnotatedString { withLink(LinkAnnotation.Url(url)) { append("link") } }
                )
            }
        }

        rule.onNodeWithText("link", useUnmergedTree = true).performFirstLinkClick { true }
        rule.runOnIdle { assertThat(clickedUrl).isEqualTo(url) }
    }

    @Test
    fun unmergedTreeLookup_oneLink_inMergedBox_clicked() {
        val url = "url_to_open"
        var clickedUrl: String? = null

        rule.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides
                    object : UriHandler {
                        override fun openUri(uri: String) {
                            clickedUrl = uri
                        }
                    }
            ) {
                Box(Modifier.semantics(mergeDescendants = true) {}) {
                    BasicText(
                        buildAnnotatedString {
                            withLink(LinkAnnotation.Url(url)) { append("link") }
                        }
                    )
                }
            }
        }

        rule.onNodeWithText("link", useUnmergedTree = true).performFirstLinkClick { true }
        rule.runOnIdle { assertThat(clickedUrl).isEqualTo(url) }
    }

    @Test
    fun multipleLinks_first_clicked() {
        val url1 = "url_to_open"
        val url2 = "another_url_to_open"
        var clickedUrl: String? = null

        rule.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides
                    object : UriHandler {
                        override fun openUri(uri: String) {
                            clickedUrl = uri
                        }
                    }
            ) {
                BasicText(
                    buildAnnotatedString {
                        withLink(LinkAnnotation.Url(url1)) { append("link") }
                        withLink(LinkAnnotation.Url(url2)) { append("text") }
                    }
                )
            }
        }

        rule.onNodeWithText("link", substring = true).performFirstLinkClick { true }
        rule.runOnIdle { assertThat(clickedUrl).isEqualTo(url1) }
    }

    @Test
    fun inlineContentAndLink_link_clicked() {
        val url = "url_to_open"
        var clickedUrl: String? = null

        val inlinePlaceholder = Placeholder(1.em, 1.em, PlaceholderVerticalAlign.Center)

        rule.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides
                    object : UriHandler {
                        override fun openUri(uri: String) {
                            clickedUrl = uri
                        }
                    }
            ) {
                BasicText(
                    buildAnnotatedString {
                        append("text")
                        appendInlineContent("inline")
                        withLink(LinkAnnotation.Url(url)) { append("link") }
                    },
                    inlineContent =
                        mapOf(
                            "inline" to
                                InlineTextContent(inlinePlaceholder) {
                                    Box(Modifier.clickable {}.fillMaxSize().background(Color.Red))
                                }
                        ),
                )
            }
        }

        rule.onNodeWithText("link", substring = true).performFirstLinkClick { true }
        rule.runOnIdle { assertThat(clickedUrl).isEqualTo(url) }
    }

    @Test
    fun mergedTreeLookup_multipleTexts_inMergedBox_firstLink_clicked() {
        val url1 = "url_to_open"
        val url2 = "another_url_to_open"
        var clickedUrl: String? = null

        rule.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides
                    object : UriHandler {
                        override fun openUri(uri: String) {
                            clickedUrl = uri
                        }
                    }
            ) {
                Column(Modifier.semantics(mergeDescendants = true) { testTag = "tag" }) {
                    BasicText(
                        buildAnnotatedString {
                            withLink(LinkAnnotation.Url(url1)) { append("link") }
                        }
                    )
                    BasicText(
                        buildAnnotatedString {
                            withLink(LinkAnnotation.Url(url2)) { append("link") }
                        }
                    )
                }
            }
        }

        rule.onNodeWithTag("tag").performFirstLinkClick { true }
        rule.runOnIdle { assertThat(clickedUrl).isEqualTo(url1) }
    }

    @Test
    fun unmergedTreeLookup_multipleTexts_inMergedBox_firstLink_notFound() {
        rule.setContent {
            Column(Modifier.semantics(mergeDescendants = true) { testTag = "tag" }) {
                BasicText(
                    buildAnnotatedString { withLink(LinkAnnotation.Url("url1")) { append("abc") } }
                )
                BasicText(
                    buildAnnotatedString { withLink(LinkAnnotation.Url("url2")) { append("def") } }
                )
            }
        }

        // In case of unmerged lookup, we should be looking for a text node directly
        expectAssertionError(expectedMessage = noTextFoundMessage) {
            rule.onNodeWithTag("tag", useUnmergedTree = true).performFirstLinkClick { true }
        }
    }

    @Test
    fun mergedTreeLookup_multipleTexts_inUnmergedBox_firstLink_notFound() {
        rule.setContent {
            Column(Modifier.semantics(mergeDescendants = false) { testTag = "tag" }) {
                BasicText(
                    buildAnnotatedString { withLink(LinkAnnotation.Url("url1")) { append("abc") } }
                )
                BasicText(
                    buildAnnotatedString { withLink(LinkAnnotation.Url("url2")) { append("def") } }
                )
            }
        }

        // In case of unmerged lookup, we should be looking for a text node directly
        expectAssertionError(expectedMessage = noTextFoundMessage) {
            rule.onNodeWithTag("tag").performFirstLinkClick { true }
        }
    }

    @Test
    fun unmergedTreeLookup_multipleTexts_inUnmergedBox_firstLink_notFound() {
        rule.setContent {
            Column(Modifier.semantics(mergeDescendants = false) { testTag = "tag" }) {
                BasicText(
                    buildAnnotatedString { withLink(LinkAnnotation.Url("url1")) { append("abc") } }
                )
                BasicText(
                    buildAnnotatedString { withLink(LinkAnnotation.Url("url2")) { append("def") } }
                )
            }
        }

        // In case of unmerged lookup, we should be looking for a text node directly
        expectAssertionError(expectedMessage = noTextFoundMessage) {
            rule.onNodeWithTag("tag", useUnmergedTree = true).performFirstLinkClick { true }
        }
    }

    @Test
    fun performFirstLinkClick_executesCallbackOnMainThread() {
        val url = "url_to_open"
        var clickedUrl: String? = null
        val wasOnMainThread = AtomicBoolean(false)

        rule.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides
                    object : UriHandler {
                        override fun openUri(uri: String) {
                            val isOnMainThread =
                                Thread.currentThread() == Looper.getMainLooper().thread
                            wasOnMainThread.set(isOnMainThread)
                            clickedUrl = uri
                        }
                    }
            ) {
                BasicText(
                    buildAnnotatedString { withLink(LinkAnnotation.Url(url)) { append("link") } }
                )
            }
        }

        rule.onNodeWithText("link", useUnmergedTree = true).performFirstLinkClick { true }
        rule.waitForIdle()
        assertThat(clickedUrl).isEqualTo(url)
        assertTrue(wasOnMainThread.get(), "The UriHandler was not invoked on the main thread.")
    }
}
