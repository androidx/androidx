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

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.player.compose.embedded.draw.ComposeLocalPaint
import androidx.compose.remote.player.compose.embedded.resources.documentFontTypeface
import androidx.compose.remote.player.compose.embedded.resources.resolveFontFamily
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Fonts embedded in the document (`FontData`) in the embedded player's text paths. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerDocumentFontTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    private var fontId = 0
    private lateinit var context: RemoteContext

    private fun setUpPlayer(trustedContent: Boolean = true) {
        val font =
            requireNotNull(javaClass.getResourceAsStream("/font/karla_regular.ttf")).use {
                it.readBytes()
            }
        val document = buildDocument {
            fontId = writer.addFont(font)
            writer.drawCircle(1f, 1f, 1f)
        }
        rule.setContent {
            val state = rememberRcPlayerState(document)
            context = state.remoteContext
            Box(modifier = Modifier.size(100.dp)) {
                if (trustedContent) {
                    RcPlayer(state = state, trustedContent = true)
                } else {
                    RcPlayer(state = state)
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun canvasTextIgnoresDocumentFontByDefault() {
        setUpPlayer(trustedContent = false)
        val paint =
            ComposeLocalPaint().apply {
                fontFamily = fontId
                isTypefaceSet = true
                textSize = 40f
            }

        val nativePaint = paint.toNativeTextPaint(context)

        assertThat(documentFontTypeface(context, fontId, 400, false)).isNull()
        val default = Paint().apply { textSize = 40f }
        assertThat(nativePaint.measureText(SAMPLE)).isEqualTo(default.measureText(SAMPLE))
    }

    @Test
    fun layoutTextIgnoresDocumentFontByDefault() {
        setUpPlayer(trustedContent = false)
        val family =
            resolveFontFamily(
                fontFamilyType = fontId,
                fontName = null,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Normal,
                fontAxis = null,
                fontAxisValues = null,
                context = context,
            )

        assertThat(family).isEqualTo(FontFamily.SansSerif)
    }

    @Test
    fun canvasTextUsesTheDocumentFont() {
        setUpPlayer(trustedContent = true)
        val paint =
            ComposeLocalPaint().apply {
                fontFamily = fontId
                isTypefaceSet = true
                textSize = 40f
            }

        val nativePaint = paint.toNativeTextPaint(context)

        assertThat(nativePaint.typeface).isNotEqualTo(Typeface.DEFAULT)
        val default = Paint().apply { textSize = 40f }
        assertThat(nativePaint.measureText(SAMPLE)).isNotEqualTo(default.measureText(SAMPLE))
    }

    @Test
    fun layoutTextUsesTheDocumentFont() {
        setUpPlayer(trustedContent = true)
        val family =
            resolveFontFamily(
                fontFamilyType = fontId,
                fontName = null,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Normal,
                fontAxis = null,
                fontAxisValues = null,
                context = context,
            )

        // Without the document font this falls back to sans-serif.
        assertThat(family).isNotEqualTo(FontFamily.SansSerif)
        assertThat(family).isNotEqualTo(FontFamily.Default)
    }

    @Test
    fun ordinaryFontIdsAreNotDocumentFonts() {
        setUpPlayer(trustedContent = true)
        assertThat(documentFontTypeface(context, 1, 400, false)).isNull()
    }

    @Test
    fun corruptFontDataFallsBackWithoutThrowing() {
        setUpPlayer(trustedContent = true)
        val badFontId = 999
        context.loadFont(badFontId, byteArrayOf(0x01, 0x02, 0x03))

        assertThat(documentFontTypeface(context, badFontId, 400, false)).isNull()
    }

    @Test
    fun customFontFamilyNameResolvesNormally() {
        setUpPlayer(trustedContent = true)
        val customNameId = 500
        context.loadText(customNameId, "monospace")

        val family =
            resolveFontFamily(
                fontFamilyType = customNameId,
                fontName = context.getText(customNameId),
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Normal,
                fontAxis = null,
                fontAxisValues = null,
                context = context,
            )

        assertThat(family).isEqualTo(FontFamily.Monospace)
    }

    private companion object {
        const val SAMPLE = "Hamburgefonstiv"
    }
}
