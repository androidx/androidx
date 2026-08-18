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

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerTextPropertiesTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    @Test
    fun testCoreTextDataReflectiveReading() {
        val coreText =
            CoreText(
                null,
                1,
                -1,
                0f,
                0f,
                100f,
                50f,
                10,
                0xFF0000FF.toInt(),
                -1,
                18f,
                12f,
                36f,
                0,
                400f,
                -1,
                CoreText.TEXT_ALIGN_START,
                CoreText.OVERFLOW_START_ELLIPSIS,
                3,
                0.5f,
                2f,
                1.2f,
                CoreText.BREAK_STRATEGY_HIGH_QUALITY,
                1,
                CoreText.JUSTIFICATION_MODE_INTER_WORD,
                true,
                false,
                null,
                null,
                true,
                0,
                -1,
            )

        val data = coreText.readDataReflection()
        assertThat(data.autosize).isTrue()
        assertThat(data.minFontSize).isEqualTo(12f)
        assertThat(data.maxFontSize).isEqualTo(36f)
        assertThat(data.lineBreakStrategy).isEqualTo(CoreText.BREAK_STRATEGY_HIGH_QUALITY)
        assertThat(data.hyphenationFrequency).isEqualTo(1)
        assertThat(data.justificationMode).isEqualTo(CoreText.JUSTIFICATION_MODE_INTER_WORD)
        assertThat(data.overflow).isEqualTo(CoreText.OVERFLOW_START_ELLIPSIS)
        assertThat(data.letterSpacing).isEqualTo(0.5f)
        assertThat(data.lineHeightAdd).isEqualTo(2f)
        assertThat(data.lineHeightMultiplier).isEqualTo(1.2f)
        assertThat(data.underline).isTrue()
        assertThat(data.strikethrough).isFalse()
    }

    @Test
    fun testRemoteTextRendersThroughPlayer() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteText(
                                text = "Hello Remote Properties".rs,
                                style = RemoteTextStyle(fontSize = 18.rsp),
                            )
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) }
            }

            rule.onNodeWithText("Hello Remote Properties").assertIsDisplayed()
        }
    }

    @Test
    fun testTextAlignJustifyWithoutJustificationModeMapsToStart() {
        val textAlignWithoutJustification =
            resolveTextAlign(CoreText.TEXT_ALIGN_JUSTIFY, CoreText.JUSTIFICATION_MODE_NONE)
        assertThat(textAlignWithoutJustification).isEqualTo(TextAlign.Start)

        val textAlignWithJustification =
            resolveTextAlign(CoreText.TEXT_ALIGN_JUSTIFY, CoreText.JUSTIFICATION_MODE_INTER_WORD)
        assertThat(textAlignWithJustification).isEqualTo(TextAlign.Justify)
    }
}
