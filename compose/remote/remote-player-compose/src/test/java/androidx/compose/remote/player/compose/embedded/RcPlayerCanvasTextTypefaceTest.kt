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

import android.graphics.Typeface
import androidx.collection.emptyIntObjectMap
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.paint.PaintPathEffects
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.player.core.platform.FontInstance
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerCanvasTextTypefaceTest {

    @get:Rule val rule = RcPlayerTestRule()

    private class CustomTestTypefaceResolver(val expectedTypeface: Typeface) : TypefaceResolver {
        var resolveCalled = false
        var appliedTags: Array<String>? = null
        var appliedValues: FloatArray? = null
        val appliedHistory = mutableListOf<Map<String, Float>>()

        override fun resolve(
            fontType: Int,
            weight: Int,
            italic: Boolean,
            fallbackTypeface: Typeface?,
            fallbackWeight: Int,
            fallbackItalic: Boolean,
        ): FontInstance {
            resolveCalled = true
            return object : FontInstance {
                override fun getTypeface(): Typeface = expectedTypeface

                override fun applyVariationSettings(
                    tags: Array<String>,
                    values: FloatArray,
                ): Typeface {
                    appliedTags = tags
                    appliedValues = values
                    appliedHistory.add(tags.indices.associate { tags[it] to values[it] })
                    return expectedTypeface
                }

                override fun setOnLoadedListener(listener: Runnable) {}
            }
        }

        override fun resolve(
            fontName: String,
            weight: Int,
            italic: Boolean,
            fallbackTypeface: Typeface?,
            fallbackWeight: Int,
            fallbackItalic: Boolean,
        ): FontInstance {
            resolveCalled = true
            return object : FontInstance {
                override fun getTypeface(): Typeface = expectedTypeface

                override fun applyVariationSettings(
                    tags: Array<String>,
                    values: FloatArray,
                ): Typeface {
                    appliedTags = tags
                    appliedValues = values
                    appliedHistory.add(tags.indices.associate { tags[it] to values[it] })
                    return expectedTypeface
                }

                override fun setOnLoadedListener(listener: Runnable) {}
            }
        }
    }

    @Test
    fun toNativeTextPaintUsesContextTypefaceResolver() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val context =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { setTypefaceResolver(testResolver) }

        val paint = ComposeLocalPaint().apply { textSize = 24f }

        val nativePaint = paint.toNativeTextPaint(context)

        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(nativePaint.typeface).isEqualTo(testTypeface)
    }

    @Test
    fun toNativeTextPaintWithGraphContextUsesConfiguredTypefaceResolver() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val playerContext =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { setTypefaceResolver(testResolver) }
        val snapshotState = SnapshotRemoteComposeState()
        val graphContext =
            GraphContext(
                    snapshotState,
                    emptyIntObjectMap(),
                    mutableFloatStateOf(0f),
                    RemoteClock.SYSTEM,
                )
                .apply { setTypefaceResolver(playerContext.typefaceResolver) }

        val paint = ComposeLocalPaint().apply { textSize = 24f }
        val nativePaint = paint.toNativeTextPaint(graphContext)

        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(nativePaint.typeface).isEqualTo(testTypeface)
    }

    @Test
    fun toTextStyleUsesContextTypefaceResolver() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val context =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { setTypefaceResolver(testResolver) }

        val paint = ComposeLocalPaint().apply { textSize = 24f }
        val textStyle = paint.toTextStyle(Density(1f), context)

        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(textStyle.fontFamily).isNotNull()
    }

    @Test
    fun toTextStylePreservesFontWeightAndStyleWhenFontAxisIsSet() {
        val context = AndroidRemoteContext(RemoteClock.SYSTEM)
        context.loadText(10, "device:sans-serif")
        context.loadText(20, "wdth")

        val bundle =
            PaintBundle().apply {
                setTextStyle(10, 700, true)
                setTextAxis(intArrayOf(20), floatArrayOf(85f))
                setTextSize(18f)
            }
        val paint = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paint, context)

        val textStyle = paint.toTextStyle(Density(1f), context)
        assertThat(textStyle.fontWeight).isEqualTo(FontWeight(700))
        assertThat(textStyle.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(textStyle.fontFamily).isNotNull()
    }

    @Test
    fun toDrawStylePopulatesStrokeMiterAndPathEffectFromPaintBundle() {
        val context = AndroidRemoteContext(RemoteClock.SYSTEM)
        val bundle =
            PaintBundle().apply {
                setStyle(PaintBundle.STYLE_STROKE)
                setStrokeWidth(6f)
                setStrokeMiter(8.5f)
                setPathEffect(PaintPathEffects.dash(0f, 10f, 5f))
            }
        val paint = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paint, context)

        val style = paint.toDrawStyle()
        assertThat(style).isInstanceOf(Stroke::class.java)
        val stroke = style as Stroke
        assertThat(stroke.width).isEqualTo(6f)
        assertThat(stroke.miter).isEqualTo(8.5f)
        assertThat(stroke.pathEffect).isNotNull()
    }

    @Test
    fun customTypefaceResolverAppliesFontVariationSettings() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val context =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { setTypefaceResolver(testResolver) }
        context.loadText(20, "wght")
        context.loadText(21, "wdth")

        resolveFontFamily(
            fontFamilyType = 0,
            fontName = "default",
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal,
            fontAxis = intArrayOf(20, 21),
            fontAxisValues = floatArrayOf(100f, 150f),
            context = context,
            typefaceResolver = testResolver,
        )

        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(testResolver.appliedTags).asList().containsExactly("wght", "wdth").inOrder()
        assertThat(testResolver.appliedValues)
            .usingExactEquality()
            .containsExactly(100f, 150f)
            .inOrder()
    }

    @Test
    fun toNativeTextPaintAppliesFontVariationSettingsViaResolver() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val context =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { setTypefaceResolver(testResolver) }
        context.loadText(20, "wght")
        context.loadText(21, "wdth")

        val bundle =
            PaintBundle().apply {
                setTextStyle(PaintBundle.FONT_TYPE_SANS_SERIF, 500, false)
                setTextAxis(intArrayOf(20, 21), floatArrayOf(650f, 90f))
                setTextSize(24f)
            }
        val paint = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paint, context)

        val nativePaint = paint.toNativeTextPaint(context)

        assertThat(nativePaint.typeface).isEqualTo(testTypeface)
        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(testResolver.appliedTags).asList().containsExactly("wght", "wdth").inOrder()
        assertThat(testResolver.appliedValues)
            .usingExactEquality()
            .containsExactly(650f, 90f)
            .inOrder()
    }

    @Test
    fun customTypefaceResolverAppliesTnumAndPnumSettings() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val context =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { setTypefaceResolver(testResolver) }
        context.loadText(20, "wght")
        context.loadText(21, "tnum")
        context.loadText(22, "pnum")

        resolveFontFamily(
            fontFamilyType = 0,
            fontName = "default",
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal,
            fontAxis = intArrayOf(20, 21),
            fontAxisValues = floatArrayOf(500f, 1f),
            context = context,
            typefaceResolver = testResolver,
        )

        assertThat(testResolver.appliedTags).asList().containsExactly("wght").inOrder()
        assertThat(testResolver.appliedValues).usingExactEquality().containsExactly(500f).inOrder()

        val bundle =
            PaintBundle().apply {
                setTextStyle(PaintBundle.FONT_TYPE_SANS_SERIF, 400, false)
                setTextAxis(intArrayOf(20, 22), floatArrayOf(500f, 1f))
                setTextSize(24f)
            }
        val paint = ComposeLocalPaint()
        updatePaintFromBundle(bundle, paint, context)
        val nativePaint = paint.toNativeTextPaint(context)
        val textStyle = paint.toTextStyle(Density(1f), context)

        assertThat(testResolver.appliedTags).asList().containsExactly("wght").inOrder()
        assertThat(testResolver.appliedValues).usingExactEquality().containsExactly(500f).inOrder()
        assertThat(nativePaint.fontFeatureSettings).isEqualTo("'pnum' 1")
        assertThat(textStyle.fontFeatureSettings).isEqualTo("'pnum' 1")
    }

    @Test
    fun remoteTextWithTnumAndPnumAppliesSettingsThroughPlayer() {
        val testResolver = CustomTestTypefaceResolver(Typeface.SANS_SERIF)

        rule.setRemoteContent(typefaceResolver = testResolver) {
            RemoteColumn {
                RemoteText(
                    text = "tnum: 11111".rs,
                    style =
                        RemoteTextStyle(
                            fontSize = 18.rsp,
                            fontFeatureSettings = "tnum",
                            fontVariationSettings =
                                FontVariation.Settings(FontVariation.weight(500)),
                        ),
                )
                RemoteText(
                    text = "pnum: 11111".rs,
                    style =
                        RemoteTextStyle(
                            fontSize = 18.rsp,
                            fontFeatureSettings = "pnum",
                            fontVariationSettings =
                                FontVariation.Settings(FontVariation.width(110f)),
                        ),
                )
            }
        }

        rule.onNodeWithText("tnum: 11111").assertIsDisplayed()
        rule.onNodeWithText("pnum: 11111").assertIsDisplayed()
        val tnumLayout = mutableListOf<TextLayoutResult>()
        val pnumLayout = mutableListOf<TextLayoutResult>()
        rule
            .onNodeWithText("tnum: 11111")
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(tnumLayout)
        rule
            .onNodeWithText("pnum: 11111")
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(pnumLayout)
        assertThat(tnumLayout.single().layoutInput.style.fontFeatureSettings).isEqualTo("'tnum' 1")
        assertThat(pnumLayout.single().layoutInput.style.fontFeatureSettings).isEqualTo("'pnum' 1")
        assertThat(tnumLayout.single().size.width).isGreaterThan(pnumLayout.single().size.width)
        assertThat(testResolver.appliedHistory)
            .containsAtLeast(mapOf("wght" to 500f), mapOf("wdth" to 110f))
    }
}
