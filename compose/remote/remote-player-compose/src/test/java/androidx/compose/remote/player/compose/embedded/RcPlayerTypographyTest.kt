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

import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Typography behaviour of the embedded player: OpenType tag classification for combined
 * feature/variation settings, and bounded reuse of typefaces and font families when variation
 * values change.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RcPlayerTypographyTest {

    @get:Rule val rule = RcPlayerTestRule()

    private class CountingTypefaceResolver : RcTypefaceResolver {
        val requests = mutableListOf<RcTypefaceRequest>()

        override suspend fun resolve(request: RcTypefaceRequest): Typeface {
            requests.add(request)
            return Typeface.DEFAULT
        }
    }

    @Test
    fun italAndUppercaseTagsAreAppliedAsFeatures() {
        rule.setRemoteContent {
            RemoteText("Hi".rs, fontFeatureSettings = "'ital' 1, 'SMCP' 1, 'tnum' 1")
        }

        val style = textStyleOf("Hi")
        assertThat(style.fontFeatureSettings).contains("'ital' 1")
        assertThat(style.fontFeatureSettings).contains("'SMCP' 1")
        assertThat(style.fontFeatureSettings).contains("'tnum' 1")
        // ital and uppercase tags are also valid variation axes.
        val axes = variationAxesOf(style)
        assertThat(axes).containsAtLeast("ital", "SMCP")
        assertThat(axes).doesNotContain("tnum")
    }

    @Test
    fun registeredAxesAreNotAppliedAsFeatures() {
        rule.setRemoteContent {
            RemoteText(
                "Hi".rs,
                fontVariationSettings =
                    FontVariation.Settings(
                        FontVariation.weight(600),
                        FontVariation.Setting("wdth", 80f),
                    ),
            )
        }

        val style = textStyleOf("Hi")
        assertThat(style.fontFeatureSettings.orEmpty()).doesNotContain("wght")
        assertThat(style.fontFeatureSettings.orEmpty()).doesNotContain("wdth")
        assertThat(variationAxesOf(style)).containsAtLeast("wght", "wdth")
    }

    @Test
    fun malformedAxisTagInTextComponentIsIgnored() {
        val document = buildDocument {
            val textId = addText("Hi")
            root {
                startTextComponent(
                    RecordingModifier().width(100f).height(40f),
                    textId,
                    -1,
                    0xFF000000.toInt(),
                    -1,
                    18f,
                    -1f,
                    -1f,
                    0,
                    400f,
                    null,
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
                    arrayOf("WIDTH", "Wdt", "wght", "smcp"),
                    floatArrayOf(100f, 50f, 700f, 1f),
                    false,
                    0,
                )
                endTextComponent()
            }
        }

        hostDocument(document)

        val style = textStyleOf("Hi")
        assertThat(style.fontFeatureSettings).isEqualTo("'smcp' 1")
        assertThat(variationAxesOf(style)).containsExactly("wght")
    }

    @Test
    fun malformedAxisTagInCanvasTextIsIgnored() {
        val document = buildDocument {
            val textId = addText("Hi")
            writer.rcPaint.setColor(0xFF000000.toInt()).setTextSize(30f).commit()
            writer.rcPaint.setAxis(arrayOf("WIDTH", "wght"), floatArrayOf(1f, 700f)).commit()
            drawTextRun(textId, 0, 2, 0, 2, 10f, 50f, false)
        }

        hostDocument(document)

        // Canvas text is drawn rather than exposed as a text node; it must render instead of
        // crashing while applying the malformed axis.
        val bitmap = rule.captureRootBitmap()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        assertThat(pixels.any { it == 0xFF000000.toInt() }).isTrue()
    }

    @Test
    fun resolvedTypefacesAreEvictedBeyondCacheLimit() {
        val resolver = CountingTypefaceResolver()
        val density = mutableStateOf(Density(1f))
        rule.setRemoteContent(
            typefaceResolver = resolver,
            playComposableWrapper = { content ->
                CompositionLocalProvider(LocalDensity provides density.value) {
                    Box(Modifier.size(100.dp)) { content() }
                }
            },
        ) {
            RemoteColumn {
                for (i in 0 until 45) {
                    RemoteText(
                        "T$i".rs,
                        fontVariationSettings =
                            FontVariation.Settings(FontVariation.weight(100 + i * 10)),
                    )
                }
            }
        }
        assertThat(resolver.requests.map { it.variationSettings }.distinct()).hasSize(45)
        val firstPassCalls = resolver.requests.size

        density.value = Density(2f)
        rule.waitForIdle()

        // Re-measuring all 45 distinct requests cannot be served by a bounded cache.
        assertThat(resolver.requests.size).isGreaterThan(firstPassCalls)
    }

    @Test
    fun resolvedTypefacesAreReusedWithinCacheLimit() {
        val resolver = CountingTypefaceResolver()
        val density = mutableStateOf(Density(1f))
        rule.setRemoteContent(
            typefaceResolver = resolver,
            playComposableWrapper = { content ->
                CompositionLocalProvider(LocalDensity provides density.value) {
                    Box(Modifier.size(100.dp)) { content() }
                }
            },
        ) {
            RemoteColumn {
                for (i in 0 until 10) {
                    RemoteText(
                        "T$i".rs,
                        fontVariationSettings =
                            FontVariation.Settings(FontVariation.weight(100 + i * 10)),
                    )
                }
            }
        }
        assertThat(resolver.requests.map { it.variationSettings }.distinct()).hasSize(10)
        val firstPassCalls = resolver.requests.size

        density.value = Density(2f)
        rule.waitForIdle()

        assertThat(resolver.requests.size).isEqualTo(firstPassCalls)
    }

    @Test
    fun identicalVariationSettingsShareFontFamily() {
        val settings = FontVariation.Settings(FontVariation.weight(600), FontVariation.width(80f))
        rule.setRemoteContent {
            RemoteColumn {
                RemoteText("A".rs, fontVariationSettings = settings)
                RemoteText("B".rs, fontVariationSettings = settings)
            }
        }

        val familyA = textStyleOf("A").fontFamily
        val familyB = textStyleOf("B").fontFamily
        assertThat(variationAxesOf(textStyleOf("A"))).containsAtLeast("wght", "wdth")
        assertThat(familyA).isSameInstanceAs(familyB)
    }

    private fun hostDocument(document: CoreDocument) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.size(100.dp)) { RcPlayer(document = document) }
            }
        }
        rule.waitForIdle()
    }

    private fun textStyleOf(text: String): TextStyle {
        val results = mutableListOf<TextLayoutResult>()
        rule
            .onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        return results.single().layoutInput.style
    }

    @OptIn(ExperimentalTextApi::class)
    private fun variationAxesOf(style: TextStyle): List<String> {
        val family = style.fontFamily as? FontListFontFamily ?: return emptyList()
        return family.fonts.flatMap { font ->
            (font as? AndroidFont)?.variationSettings?.settings.orEmpty().map { it.axisName }
        }
    }
}
