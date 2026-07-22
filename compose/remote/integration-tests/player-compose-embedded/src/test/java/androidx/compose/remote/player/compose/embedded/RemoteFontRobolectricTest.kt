/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.remote.material3.RemoteText
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteFontRobolectricTest {

    @get:Rule val rule = RcPlayerTestRule()

    // TypefaceResolver is no longer used for standard/device fonts in this implementation path.
    // We verify standard behavior or specific prefixes if applicable.

    @Test
    fun testStandardFontResolution() {
        // Verify that standard/named fonts resolve without crashing.
        // Since we removed TypefaceResolver tracking from this specific path,
        // we mainly verify it routes to standard Compose FontFamily or doesn't throw.

        rule.setRemoteContent(autoUpdate = false) {
            RemoteText(
                text = "TestText".rs,
                fontFamily = RemoteFontFamily.Named("sans-serif"),
                modifier = RemoteModifier.size(100.rdp),
            )
        }

        rule.mainClock.advanceTimeBy(100)
        rule.waitForIdle()

        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText("TestText").performSemanticsAction(
            SemanticsActions.GetTextLayoutResult
        ) {
            it(textLayoutResults)
        }

        Assert.assertTrue(textLayoutResults.isNotEmpty())
        val fontFamily = textLayoutResults.first().layoutInput.style.fontFamily
        Assert.assertEquals(FontFamily.SansSerif, fontFamily)
    }

    @Test
    fun testDeviceFontResolution() {
        // Verify "device:" prefix routing.
        rule.setRemoteContent(autoUpdate = false) {
            RemoteText(
                text = "TestText".rs,
                fontFamily = RemoteFontFamily.Named("device:roboto-flex"),
                modifier = RemoteModifier.size(100.rdp),
            )
        }

        rule.mainClock.advanceTimeBy(100)
        rule.waitForIdle()

        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText("TestText").performSemanticsAction(
            SemanticsActions.GetTextLayoutResult
        ) {
            it(textLayoutResults)
        }

        Assert.assertTrue(textLayoutResults.isNotEmpty())
        val fontFamily = textLayoutResults.first().layoutInput.style.fontFamily
        val expectedFontFamily =
            FontFamily(
                Font(
                    DeviceFontFamilyName("roboto-flex"),
                    weight = FontWeight.Normal,
                    style = FontStyle.Normal,
                )
            )
        Assert.assertEquals(expectedFontFamily, fontFamily)
    }
}
