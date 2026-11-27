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

@file:Suppress("RestrictedApiAndroidX")
@file:OptIn(ExperimentalRemotePlayerApi::class)

package androidx.compose.remote.player.compose.embedded

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import java.io.ByteArrayInputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Screenshot tests for embedded [RcDocLayout] documents rendered in [RcPlayer]. */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RcPlayerRcDocScreenshotTest {

    @get:Rule val rule = RcPlayerTestRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    private fun buildBytes(
        width: Int,
        height: Int,
        block: RemoteComposeWriter.() -> Unit,
    ): ByteArray {
        val writer =
            RemoteComposeWriterAndroid(
                width,
                height,
                "EmbeddedScreenshotDoc",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            )
        writer.block()
        return writer.encodeToByteArray()
    }

    private fun RemoteComposeWriter.addLabel(
        textId: Int,
        color: Int = AndroidColor.WHITE,
        fontSize: Float = 24f,
    ) {
        textComponent(
            RecordingModifier(),
            textId,
            color,
            fontSize,
            0,
            600f,
            null,
            CoreText.TEXT_ALIGN_LEFT,
            CoreText.OVERFLOW_CLIP,
            1,
        ) {}
    }

    @Test
    fun embeddedDocuments_resolvedAndInlineWithVariableMapping() {
        val resolvedChildBytes =
            buildBytes(width = 360, height = 120) {
                val titleId = addNamedString("title", "Default")
                root {
                    box(
                        RecordingModifier()
                            .fillMaxSize()
                            .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                            .background(0xFF1D4ED8.toInt())
                            .padding(16),
                        BoxLayout.START,
                        BoxLayout.CENTER,
                    ) {
                        addLabel(titleId, color = AndroidColor.WHITE, fontSize = 26f)
                    }
                }
            }

        val inlineChildBytes =
            buildBytes(width = 360, height = 120) {
                val badgeId = addNamedString("badge", "Inline")
                root {
                    box(
                        RecordingModifier()
                            .fillMaxSize()
                            .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                            .background(0xFF047857.toInt())
                            .padding(16),
                        BoxLayout.START,
                        BoxLayout.CENTER,
                    ) {
                        addLabel(badgeId, color = AndroidColor.WHITE, fontSize = 26f)
                    }
                }
            }

        val hostBytes =
            buildBytes(width = 400, height = 340) {
                val headerId = addText("Parent Document")
                val resolvedTitle = addNamedString("resolvedTitle", "Resolved Child (Mapped)")
                val inlineBadge = addNamedString("inlineBadge", "Inline BinaryData Child")
                val inlineDocId = addEmbeddedDocument(inlineChildBytes)

                root {
                    column(
                        RecordingModifier()
                            .fillMaxSize()
                            .background(0xFF0F172A.toInt())
                            .padding(20),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        addLabel(headerId, color = AndroidColor.WHITE, fontSize = 28f)
                        box(RecordingModifier().size(1, 16))
                        val resolvedId =
                            rcDoc("screenshot://resolved", RecordingModifier().size(360, 110))
                        mapRcDocVariables(
                            resolvedId,
                            RemoteComposeWriter.MAP_FORWARD,
                            resolvedTitle,
                            "title",
                        )
                        box(RecordingModifier().size(1, 16))
                        val embeddedId = rcDoc(inlineDocId, RecordingModifier().size(360, 110))
                        mapRcDocVariables(
                            embeddedId,
                            RemoteComposeWriter.MAP_FORWARD,
                            inlineBadge,
                            "badge",
                        )
                    }
                }
            }

        val hostDoc =
            CoreDocument().apply {
                ByteArrayInputStream(hostBytes).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        rule.setContent {
            Box(modifier = Modifier.size(200.dp, 170.dp)) {
                RcPlayer(
                    document = hostDoc,
                    documentResolver =
                        RcDocumentResolver { url ->
                            if (url == "screenshot://resolved") resolvedChildBytes else null
                        },
                )
            }
        }

        rule.waitForIdle()
        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "RcPlayerRcDocScreenshotTest_embeddedDocuments",
            )
    }
}
