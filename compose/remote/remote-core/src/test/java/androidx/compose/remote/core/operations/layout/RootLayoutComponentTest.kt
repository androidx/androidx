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

package androidx.compose.remote.core.operations.layout

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.layout.managers.LayoutManager
import androidx.compose.remote.core.testing.HeadlessRemoteContext
import androidx.compose.remote.core.testing.SoftwareRasterPaintContext
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlin.test.Test

class RootLayoutComponentTest {

    @Test
    fun `resizing the document does not reset strings declared in the root`() {
        // Arrange: a string declared inside the root, displayed by a text component.
        val context = HeadlessRemoteContext()
        var textId = -1
        val document =
            createDocument(context) {
                root {
                    textId = addText("initial")
                    textComponent(
                        RecordingModifier().size(100, 40),
                        textId,
                        /* color= */ -0x1000000,
                        /* fontSize= */ 16f,
                        /* fontStyle= */ 0,
                        /* fontWeight= */ 400f,
                        /* fontFamily= */ null,
                        /* textAlign= */ 0,
                        /* overflow= */ 0,
                        /* maxLines= */ 1,
                    ) {}
                }
            }
        context.loadText(textId, "edited")
        document.paint(context, Theme.UNSPECIFIED)
        assertThat(context.getText(textId)).isEqualTo("edited")

        // Act: resize, which animates the root bounds over a few frames.
        context.mWidth = 300f
        context.mHeight = 300f
        context.paintContext = NoTextPaintContext(context, 300, 300)
        repeat(5) {
            context.currentTime += 16
            document.paint(context, Theme.UNSPECIFIED)
        }

        // Assert: the edited value survived the relayout.
        assertThat(context.getText(textId)).isEqualTo("edited")
    }

    private fun createDocument(
        context: RemoteContext,
        buildDocument: RemoteComposeWriter.() -> Unit,
    ): CoreDocument {
        val writer =
            RemoteComposeWriter(
                RcPlatformServices.None,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 200),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 200),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
            )
        writer.buildDocument()
        val document =
            CoreDocument().apply {
                initFromBuffer(
                    RemoteComposeBuffer.fromInputStream(
                        ByteArrayInputStream(writer.encodeToByteArray())
                    )
                )
            }
        context.setDocument(document)
        context.touchVersion = LayoutManager.FIX_TOUCH_EVENT
        context.setAnimationEnabled(true)
        context.paintContext = NoTextPaintContext(context, 200, 200)
        context.mWidth = 200f
        context.mHeight = 200f
        document.initializeContext(context)
        document.paint(context, Theme.UNSPECIFIED)
        return document
    }

    /**
     * Glyph rasterization needs AWT font support (libfreetype), which presubmit hosts don't have;
     * this test only cares about document state, not pixels.
     */
    private class NoTextPaintContext(context: RemoteContext, width: Int, height: Int) :
        SoftwareRasterPaintContext(context, width, height) {
        override fun drawTextRun(
            textID: Int,
            start: Int,
            end: Int,
            contextStart: Int,
            contextEnd: Int,
            x: Float,
            y: Float,
            rtl: Boolean,
        ) {}
    }
}
