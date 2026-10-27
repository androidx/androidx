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

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font as NativeFont
import android.graphics.fonts.FontFamily as NativeFontFamily
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.provider.FontRequest
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Font caching in the embedded player: downloadable fonts are loaded once per family whatever the
 * variation axes, document fonts apply variation axes and reuse their typefaces, and continuous
 * axis values do not churn OpenType feature settings.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RcPlayerFontCacheTest {

    @get:Rule val rule = RcPlayerTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** A downloadable font loader that counts loads and records each provider request. */
    private class CountingFontLoader(private val typeface: Typeface?) : GmsFontLoader {
        val requests = mutableListOf<FontRequest>()
        var pending: CompletableDeferred<Unit>? = null

        override suspend fun loadFont(context: Context, fontRequest: FontRequest): Typeface? {
            requests.add(fontRequest)
            pending?.await()
            return typeface
        }
    }

    @Test
    fun animatedWeightOnDownloadableFontLoadsFamilyOnce() {
        val loader = CountingFontLoader(robotoFlexTypeface())
        val resolver =
            GmsFontTypefaceResolver(context = context, fontCertsResId = 123, fontLoader = loader)
        val state = RcPlayerState(canvasTextDocument(fontName = "google:Roboto Flex"))
        hostCanvas(state, resolver)

        val inkByWeight = mutableMapOf<Int, Int>()
        for (step in 0 until 40) {
            val weight = 100 + step * 20
            state.floatState(AXIS_VALUE).value = weight.toFloat()
            rule.waitForIdle()
            inkByWeight[weight] = inkPixels()
        }

        // One provider load for the family, without the per-frame axis values.
        assertThat(loader.requests).hasSize(1)
        assertThat(loader.requests.single().variationSettings).isNull()
        // The loaded font is drawn on every frame, with the weight axis applied.
        assertThat(inkByWeight.getValue(880)).isGreaterThan(inkByWeight.getValue(100))
    }

    @Test
    fun animatedWeightWhileDownloadIsPendingSharesOneLoad() {
        val loader =
            CountingFontLoader(robotoFlexTypeface()).apply { pending = CompletableDeferred() }
        val resolver =
            GmsFontTypefaceResolver(context = context, fontCertsResId = 123, fontLoader = loader)
        val state = RcPlayerState(canvasTextDocument(fontName = "google:Roboto Flex"))
        hostCanvas(state, resolver)

        for (weight in listOf(200f, 300f, 400f, 500f, 600f)) {
            state.floatState(AXIS_VALUE).value = weight
            rule.waitForIdle()
            rule.captureRootBitmap()
        }
        assertThat(loader.requests).hasSize(1)

        loader.pending!!.complete(Unit)
        rule.waitForIdle()
        val loadedInk = inkPixels()
        state.floatState(AXIS_VALUE).value = 700f
        rule.waitForIdle()

        assertThat(loader.requests).hasSize(1)
        assertThat(inkPixels()).isGreaterThan(loadedInk)
    }

    @Test
    fun downloadableFontLoadsIgnoreVariationSettings() = runBlocking {
        val loader = CountingFontLoader(Typeface.MONOSPACE)
        val resolver =
            GmsFontTypefaceResolver(context = context, fontCertsResId = 123, fontLoader = loader)

        repeat(200) { i ->
            val resolved =
                resolver.resolve(
                    RcTypefaceRequest(
                        fontFamily = "google:Roboto Flex",
                        weight = 400,
                        variationSettings = "'wght' ${100f + i * 4.5f}",
                    )
                )
            assertThat(resolved).isSameInstanceAs(Typeface.MONOSPACE)
        }

        assertThat(loader.requests).hasSize(1)
        assertThat(loader.requests.single().variationSettings).isNull()
    }

    @Test
    fun downloadableFontFailuresAreBounded() = runBlocking {
        val loader = CountingFontLoader(null)
        val resolver =
            GmsFontTypefaceResolver(context = context, fontCertsResId = 123, fontLoader = loader)
        fun request(i: Int) = RcTypefaceRequest(fontFamily = "google:Missing $i")

        val families = Limits.MAX_CACHE_ENTRIES + 5
        for (i in 0 until families) resolver.resolve(request(i))
        assertThat(loader.requests).hasSize(families)

        // A recent failure is remembered, so it is not retried every frame.
        resolver.resolve(request(families - 1))
        assertThat(loader.requests).hasSize(families)

        // The oldest failure has been evicted, so the cache does not grow without bound.
        resolver.resolve(request(0))
        assertThat(loader.requests).hasSize(families + 1)
    }

    @Test
    fun fractionalItalicAndUppercaseAxesAreNotFeatures() {
        val document =
            textComponentDocument(
                axes = arrayOf("ital", "GRAD", "tnum", "SMCP"),
                values = floatArrayOf(0.4f, 37.6f, 1f, 1f),
            )
        hostDocument(document)

        assertThat(rule.textLayoutResultOf(SAMPLE).layoutInput.style.fontFeatureSettings)
            .isEqualTo("'tnum' 1, 'SMCP' 1")
    }

    @Test
    fun animatedUppercaseAxisKeepsFeatureSettings() {
        var gradeVar = 0f
        val document = buildDocument {
            gradeVar = addNamedFloat(GRADE, 10.2f)
            textComponent(this, arrayOf("GRAD", "tnum"), floatArrayOf(gradeVar, 1f))
        }
        val state = RcPlayerState(document)
        rule.setContent {
            Box(Modifier.size(300.dp, 100.dp)) { RcPlayer(state = state) }
        }
        rule.waitForIdle()

        val featureSettings = mutableSetOf<String?>()
        for (grade in listOf(10.2f, 10.8f, 11.4f, 12.6f, 13.1f, 20.9f)) {
            state.floatState(GRADE).value = grade
            rule.waitForIdle()
            featureSettings += rule.textLayoutResultOf(SAMPLE).layoutInput.style.fontFeatureSettings
        }

        assertThat(featureSettings).containsExactly("'tnum' 1")
    }

    @Test
    fun documentFontAppliesVariationAxesOnCanvas() {
        val state =
            RcPlayerState(
                canvasTextDocument(fontBytes = dashFontBytes(), axis = "wdth", text = DASHES)
            )
        hostCanvas(state, resolver = null)

        state.floatState(AXIS_VALUE).value = 100f
        rule.waitForIdle()
        val narrowInk = inkPixels()
        state.floatState(AXIS_VALUE).value = 500f
        rule.waitForIdle()
        val wideInk = inkPixels()

        assertThat(narrowInk).isGreaterThan(0)
        assertThat(wideInk).isGreaterThan(narrowInk)
    }

    @Test
    fun documentFontAppliesVariationAxesInLayoutTextAndReusesFontFamily() {
        var fontId = 0
        val document = buildDocument {
            fontId = writer.addFont(dashFontBytes())
            val widthVar = addNamedFloat(AXIS_VALUE, 100f)
            textComponent(this, arrayOf("wdth"), floatArrayOf(widthVar), fontId, DASHES)
        }
        val coreText = document.rootLayoutComponent!!.mList.first { it is CoreText } as CoreText
        coreText.mPaint.setTextStyle(fontId, 400, false)
        val state = RcPlayerState(document)
        rule.setContent {
            Box(Modifier.size(300.dp, 100.dp)) { RcPlayer(state = state, trustedContent = true) }
        }
        rule.waitForIdle()

        val narrow = rule.textLayoutResultOf(DASHES)
        state.floatState(AXIS_VALUE).value = 500f
        rule.waitForIdle()
        val wide = rule.textLayoutResultOf(DASHES)
        state.floatState(AXIS_VALUE).value = 100f
        rule.waitForIdle()
        val narrowAgain = rule.textLayoutResultOf(DASHES)

        assertThat(wide.getLineRight(0)).isGreaterThan(narrow.getLineRight(0))
        // Returning to an earlier axis value reuses its font family, so the style is unchanged.
        assertThat(narrowAgain.layoutInput.style.fontFamily)
            .isSameInstanceAs(narrow.layoutInput.style.fontFamily)
        assertThat(narrowAgain.layoutInput.style).isEqualTo(narrow.layoutInput.style)
    }

    private fun hostCanvas(state: RcPlayerState, resolver: RcTypefaceResolver?) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    if (resolver != null) {
                        RcPlayer(state = state, typefaceResolver = resolver)
                    } else {
                        RcPlayer(state = state, trustedContent = true)
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun hostDocument(document: CoreDocument) {
        rule.setContent { Box(Modifier.size(300.dp, 100.dp)) { RcPlayer(document = document) } }
        rule.waitForIdle()
    }

    private fun inkPixels(): Int =
        rule.captureRootPixels(WIDTH, HEIGHT).count { it == 0xFF000000.toInt() }

    /**
     * Black canvas [text] on white whose [axis] follows the named float [AXIS_VALUE], using either
     * the named [fontName] or the embedded [fontBytes].
     */
    private fun canvasTextDocument(
        fontName: String? = null,
        fontBytes: ByteArray? = null,
        weight: Int = 100,
        axis: String = "wght",
        text: String = SAMPLE,
    ): CoreDocument = buildDocument {
        val fontId = if (fontBytes != null) writer.addFont(fontBytes) else addText(fontName!!)
        val weightVar = addNamedFloat(AXIS_VALUE, weight.toFloat())
        val textId = addText(text)
        val wghtId = addText(axis)
        writer.buffer.addPaint(PaintBundle().apply { setColor(0xFFFFFFFF.toInt()) })
        drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat())
        writer.buffer.addPaint(
            PaintBundle().apply {
                setColor(0xFF000000.toInt())
                setTextStyle(fontId, 400, false)
                setTextAxis(intArrayOf(wghtId), floatArrayOf(weightVar))
                setTextSize(32f)
            }
        )
        drawTextRun(textId, 0, text.length, 0, text.length, 5f, 50f, false)
    }

    private fun textComponentDocument(axes: Array<String>, values: FloatArray): CoreDocument =
        buildDocument {
            textComponent(this, axes, values)
        }

    private fun textComponent(
        context: RemoteComposeContextAndroid,
        axes: Array<String>,
        values: FloatArray,
        fontFamilyId: Int = -1,
        text: String = SAMPLE,
    ) =
        with(context) {
            val textId = addText(text)
            val axisIds = IntArray(axes.size) { addText(axes[it]) }
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
                    fontFamilyId,
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
                    axisIds,
                    values,
                    false,
                    0,
                )
                writer.buffer.addContentStart()
                endTextComponent()
            }
        }

    /**
     * A byte-for-byte copy of the variable font test fixture in
     * `core/core/src/androidTest/res/font/variable_width_dash_font.ttf`. It's a tiny (916 byte)
     * font that only maps '-', with a 'wdth' axis from 100 to 500, so changing the axis value
     * visibly changes the measured width of [DASHES].
     */
    private fun dashFontBytes(): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/font/variable_width_dash_font.ttf")).use {
            it.readBytes()
        }

    private fun robotoFlexBytes(): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/font/robotoflex_variable.ttf")).use {
            it.readBytes()
        }

    private fun robotoFlexTypeface(): Typeface {
        val bytes = robotoFlexBytes()
        val buffer = ByteBuffer.allocateDirect(bytes.size).put(bytes).apply { rewind() }
        val family = NativeFontFamily.Builder(NativeFont.Builder(buffer).build()).build()
        return Typeface.CustomFallbackBuilder(family).build()
    }

    private companion object {
        const val SAMPLE = "Hamburgefonstiv"
        // Only '-' is mapped by the dash font, see dashFontBytes.
        const val DASHES = "-----"
        const val AXIS_VALUE = "axisValue"
        const val GRADE = "grade"
        const val WIDTH = 300
        const val HEIGHT = 80
    }
}
