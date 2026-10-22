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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Fonts embedded in the document (`FontData`) in the embedded player's text paths. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RcPlayerDocumentFontTest {

    @get:Rule val rule = RcPlayerTestRule()

    private fun loadKarlaBytes(): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/font/karla_regular.ttf")).use {
            it.readBytes()
        }

    @Test
    fun canvasTextIgnoresDocumentFontByDefault() {
        val fontBytes = loadKarlaBytes()
        val docWithEmbeddedFont = canvasFontDocument(fontBytes) { embeddedId -> embeddedId }
        val docWithDefaultFont = canvasFontDocument(fontBytes) { PaintBundle.FONT_TYPE_DEFAULT }

        var currentDocument by mutableStateOf(docWithEmbeddedFont)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(200.dp, 100.dp)) {
                    RcPlayer(document = currentDocument, trustedContent = false)
                }
            }
        }
        val embeddedPixels = rule.captureRootPixels(200, 100)

        currentDocument = docWithDefaultFont
        rule.waitForIdle()
        val defaultPixels = rule.captureRootPixels(200, 100)

        assertThat(embeddedPixels).isEqualTo(defaultPixels)
    }

    @Test
    fun layoutTextIgnoresDocumentFontByDefault() {
        val doc = layoutFontDocument(loadKarlaBytes()) { embeddedId -> embeddedId }
        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                RcPlayer(document = doc, trustedContent = false)
            }
        }
        rule.waitForIdle()

        assertThat(fetchFontFamily(SAMPLE)).isEqualTo(FontFamily.SansSerif)
    }

    @Test
    fun canvasTextUsesTheDocumentFont() {
        val fontBytes = loadKarlaBytes()
        val docWithEmbeddedFont = canvasFontDocument(fontBytes) { embeddedId -> embeddedId }
        val docWithDefaultFont = canvasFontDocument(fontBytes) { PaintBundle.FONT_TYPE_DEFAULT }

        var currentDocument by mutableStateOf(docWithEmbeddedFont)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(200.dp, 100.dp)) {
                    RcPlayer(document = currentDocument, trustedContent = true)
                }
            }
        }
        val embeddedPixels = rule.captureRootPixels(200, 100)

        currentDocument = docWithDefaultFont
        rule.waitForIdle()
        val defaultPixels = rule.captureRootPixels(200, 100)

        assertThat(embeddedPixels).isNotEqualTo(defaultPixels)
    }

    @Test
    fun layoutTextUsesTheDocumentFont() {
        val doc = layoutFontDocument(loadKarlaBytes()) { embeddedId -> embeddedId }
        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                RcPlayer(document = doc, trustedContent = true)
            }
        }
        rule.waitForIdle()

        val family = fetchFontFamily(SAMPLE)
        assertThat(family).isNotEqualTo(FontFamily.SansSerif)
        assertThat(family).isNotEqualTo(FontFamily.Default)
    }

    @Test
    fun ordinaryFontIdsAreNotDocumentFonts() {
        val doc = layoutFontDocument(loadKarlaBytes()) { PaintBundle.FONT_TYPE_SERIF }
        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                RcPlayer(document = doc, trustedContent = true)
            }
        }
        rule.waitForIdle()

        assertThat(fetchFontFamily(SAMPLE)).isEqualTo(FontFamily.Serif)
    }

    @Test
    fun corruptFontDataFallsBackWithoutThrowing() {
        val doc = layoutFontDocument(byteArrayOf(0x01, 0x02, 0x03)) { embeddedId -> embeddedId }
        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                RcPlayer(document = doc, trustedContent = true)
            }
        }
        rule.waitForIdle()

        assertThat(fetchFontFamily(SAMPLE)).isEqualTo(FontFamily.SansSerif)
    }

    @Test
    fun customFontFamilyNameResolvesNormally() {
        val doc = layoutFontDocument(loadKarlaBytes()) { addText("monospace") }
        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                RcPlayer(document = doc, trustedContent = true)
            }
        }
        rule.waitForIdle()

        assertThat(fetchFontFamily(SAMPLE)).isEqualTo(FontFamily.Monospace)
    }

    private fun fetchFontFamily(text: String): FontFamily? {
        val results = mutableListOf<TextLayoutResult>()
        rule
            .onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        return results.single().layoutInput.style.fontFamily
    }

    private fun canvasFontDocument(
        fontBytes: ByteArray,
        fontFamilyIdProvider: RemoteComposeContextAndroid.(Int) -> Int,
    ): CoreDocument = buildDocument {
        val embeddedId = writer.addFont(fontBytes)
        val fontFamilyId = fontFamilyIdProvider(embeddedId)
        val textId = addText(SAMPLE)
        val pathId = addPathString("M 5 60 L 195 60")
        writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFFFFFF.toInt()) })
        drawRect(0f, 0f, 200f, 100f)
        writer.buffer.addPaint(
            PaintBundle().apply {
                setColor(0xFF000000.toInt())
                setTextSize(40f)
                setTextStyle(fontFamilyId, 400, false)
            }
        )
        drawTextOnPath(textId, pathId, 0f, 0f)
    }

    private fun layoutFontDocument(
        fontBytes: ByteArray? = null,
        fontFamilyIdProvider: RemoteComposeContextAndroid.(Int) -> Int,
    ): CoreDocument {
        var resolvedFontFamilyId = 0
        val document = buildDocument {
            val embeddedId = if (fontBytes != null) writer.addFont(fontBytes) else -1
            resolvedFontFamilyId = fontFamilyIdProvider(embeddedId)
            val textId = addText(SAMPLE)
            root {
                writer.buffer.addTextComponentStart(
                    -1,
                    -1,
                    textId,
                    -1,
                    0xFF000000.toInt(),
                    -1,
                    24f,
                    -1f,
                    -1f,
                    0,
                    400f,
                    resolvedFontFamilyId,
                    CoreText.TEXT_ALIGN_START,
                    CoreText.OVERFLOW_CLIP,
                    1,
                    0f,
                    0f,
                    1f,
                    0,
                    0,
                    0,
                    false,
                    false,
                    null,
                    null,
                    false,
                    0,
                )
                writer.buffer.addContentStart()
                endTextComponent()
            }
        }
        val coreText = document.rootLayoutComponent!!.mList.first { it is CoreText } as CoreText
        coreText.mPaint.setTextStyle(resolvedFontFamilyId, 400, false)
        return document
    }

    private companion object {
        const val SAMPLE = "Hamburgefonstiv"
    }
}
