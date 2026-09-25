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

import androidx.collection.mutableIntObjectMapOf
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.remote.core.operations.ImageAttribute
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.TextMeasure
import androidx.compose.remote.core.operations.layout.CanvasOperations
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GraphContextPaintOperationTest {

    @Test
    fun colorAttributeEvaluatesThroughPaintContextInGraphContext() {
        val state = SnapshotRemoteComposeState()
        val sourceColorId = 1
        // ARGB: Alpha=255, Red=255, Green=128, Blue=0
        state.overrideColor(sourceColorId, 0xFFFF8000.toInt())

        val redOp = ColorAttribute(10, sourceColorId, ColorAttribute.COLOR_RED)
        val greenOp = ColorAttribute(11, sourceColorId, ColorAttribute.COLOR_GREEN)
        val blueOp = ColorAttribute(12, sourceColorId, ColorAttribute.COLOR_BLUE)
        val alphaOp = ColorAttribute(13, sourceColorId, ColorAttribute.COLOR_ALPHA)

        val computedOps =
            mutableIntObjectMapOf<Operation>().apply {
                put(10, redOp)
                put(11, greenOp)
                put(12, blueOp)
                put(13, alphaOp)
            }

        val graphContext =
            GraphContext(
                realState = state,
                computedOps = computedOps,
                timeMillis = mutableFloatStateOf(0f),
                clock = RemoteClock.SYSTEM,
            )

        assertThat(graphContext.getFloat(10)).isWithin(0.01f).of(1.0f)
        assertThat(graphContext.getFloat(11)).isWithin(0.01f).of(128f / 255f)
        assertThat(graphContext.getFloat(12)).isWithin(0.01f).of(0.0f)
        assertThat(graphContext.getFloat(13)).isWithin(0.01f).of(1.0f)
    }

    @Test
    fun textMeasureEvaluatesThroughPaintContextInGraphContext() {
        val state = SnapshotRemoteComposeState()
        val textId = 5
        state.cacheData(textId, "Hello World")

        val widthOp = TextMeasure(20, textId, TextMeasure.MEASURE_WIDTH)
        val heightOp = TextMeasure(21, textId, TextMeasure.MEASURE_HEIGHT)
        val computedOps =
            mutableIntObjectMapOf<Operation>().apply {
                put(20, widthOp)
                put(21, heightOp)
            }

        val graphContext =
            GraphContext(
                realState = state,
                computedOps = computedOps,
                timeMillis = mutableFloatStateOf(0f),
                clock = RemoteClock.SYSTEM,
            )

        // Default ComposeLocalPaint has textSize = NaN; toNativeTextPaint preserves Paint's 12px
        // default so TextMeasure produces a finite non-negative width instead of NaN/0.
        val defaultWidth = graphContext.getFloat(20)
        assertThat(defaultWidth.isNaN()).isFalse()
        assertThat(defaultWidth).isGreaterThan(0f)

        val largePaint =
            ComposeLocalPaint().apply {
                textSize = 48f
                isTextSizeSet = true
            }
        graphContext.setTextMeasurePaint(20, largePaint)
        val largeWidth = graphContext.getFloat(20)
        assertThat(largeWidth).isGreaterThan(defaultWidth)
    }

    @Test
    fun imageAttributeEvaluatesBitmapDataDimensions() {
        val state = SnapshotRemoteComposeState()
        val imageId = 7
        val bitmapData =
            BitmapData(
                imageId,
                BitmapData.TYPE_PNG,
                320,
                BitmapData.ENCODING_URL,
                240,
                "camera/current".toByteArray(),
            )
        state.updateObject(imageId, bitmapData)

        val widthOp = ImageAttribute(30, imageId, ImageAttribute.IMAGE_WIDTH, null)
        val heightOp = ImageAttribute(31, imageId, ImageAttribute.IMAGE_HEIGHT, null)

        val computedOps =
            mutableIntObjectMapOf<Operation>().apply {
                put(30, widthOp)
                put(31, heightOp)
            }

        val graphContext =
            GraphContext(
                realState = state,
                computedOps = computedOps,
                timeMillis = mutableFloatStateOf(0f),
                clock = RemoteClock.SYSTEM,
            )

        assertThat(graphContext.getFloat(30)).isEqualTo(320f)
        assertThat(graphContext.getFloat(31)).isEqualTo(240f)
    }

    @Test
    fun resolveBitmapReturnsNullOnUnresolvableUrlWithoutThrowing() {
        val ctx = AndroidRemoteContext()
        val state = SnapshotRemoteComposeState()
        ctx.mRemoteComposeState = state
        val imageId = 9
        val badUrlBitmap =
            BitmapData(
                imageId,
                BitmapData.TYPE_PNG,
                100,
                BitmapData.ENCODING_URL,
                80,
                "camera/current".toByteArray(),
            )
        ctx.putObject(imageId, badUrlBitmap)

        val resolved = resolveBitmap(ctx, imageId)
        assertThat(resolved).isNull()
        // Declared metadata remains intact in the object store.
        assertThat(state.getObject(imageId)).isSameInstanceAs(badUrlBitmap)
    }

    @Test
    fun canvasBitmapDataIsRegisteredDuringApplyOperationsWithoutBitmaps() {
        val buffer = RemoteComposeBuffer()
        val wire = buffer.buffer
        BoxLayout.apply(wire, 1, 0, 0, 0)
        CanvasOperations.apply(wire)
        BitmapData.apply(
            wire,
            42,
            BitmapData.TYPE_PNG_8888,
            64,
            BitmapData.ENCODING_INLINE,
            48,
            ByteArray(0),
        )
        TextData.apply(wire, 50, "Sample")
        TextMeasure.apply(wire, 60, 50, TextMeasure.MEASURE_WIDTH)
        ImageAttribute.apply(wire, 61, 42, ImageAttribute.IMAGE_WIDTH, null)
        buffer.addContainerEnd()
        buffer.addContainerEnd()

        val document = CoreDocument()
        document.initFromBuffer(buffer)
        val preprocessed = preprocessDocument(document)

        assertThat(preprocessed.computedOpIndex.containsKey(60)).isTrue()
        assertThat(preprocessed.computedOpIndex.containsKey(61)).isTrue()

        val ctx = initializePlayerRemoteContext(document, RemoteClock.SYSTEM, preprocessed)
        val registered = ctx.mRemoteComposeState.getObject(42) as? BitmapData
        assertThat(registered).isNotNull()
        assertThat(registered!!.width).isEqualTo(64)
        assertThat(registered.height).isEqualTo(48)
    }

    @Test
    fun buildFontVariationSettingsPreservesWeightAndStyleWhenNotExplicit() {
        val state = SnapshotRemoteComposeState()
        state.cacheData(1, "wdth")
        state.cacheData(2, "wght")
        val graphContext =
            GraphContext(
                realState = state,
                computedOps = mutableIntObjectMapOf(),
                timeMillis = mutableFloatStateOf(0f),
                clock = RemoteClock.SYSTEM,
            )

        // Only "wdth" is in fontAxis -> "wght" (from FontWeight.Bold) and "ital" (from Italic) are
        // appended so variable-font resolution does not revert to 400/upright.
        val settingsWithWdthOnly =
            buildFontVariationSettings(
                fontAxis = intArrayOf(1),
                fontAxisValues = floatArrayOf(110f),
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                context = graphContext,
            )
        assertThat(settingsWithWdthOnly).isNotNull()
        val byTag =
            settingsWithWdthOnly!!.settings.associate { it.axisName to it.toVariationValue(null) }
        assertThat(byTag["wdth"]).isEqualTo(110f)
        assertThat(byTag["wght"]).isEqualTo(700f)
        assertThat(byTag["ital"]).isEqualTo(1f)

        // Explicit "wght" in fontAxis is preserved and not overwritten by fontWeight.
        val settingsWithExplicitWght =
            buildFontVariationSettings(
                fontAxis = intArrayOf(2),
                fontAxisValues = floatArrayOf(550f),
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Normal,
                context = graphContext,
            )
        assertThat(settingsWithExplicitWght).isNotNull()
        val explicitByTag =
            settingsWithExplicitWght!!.settings.associate {
                it.axisName to it.toVariationValue(null)
            }
        assertThat(explicitByTag["wght"]).isEqualTo(550f)
        assertThat(explicitByTag.containsKey("ital")).isFalse()
    }

    @Test
    fun resolveCoreTextLineHeightUsesEffectiveFontSizeAndAutosizeRelativeEm() {
        val density = Density(density = 2f, fontScale = 1f)

        // Default multiplier=1f, add=0f -> Unspecified
        assertThat(
                resolveCoreTextLineHeight(
                    fontSize = 40f,
                    lineHeightMultiplier = 1f,
                    lineHeightAdd = 0f,
                    autosize = false,
                    density = density,
                )
            )
            .isEqualTo(TextUnit.Unspecified)

        // Fixed font size (e.g. from PaintBundle = 40px at density 2 -> 20sp) * 1.5 multiplier =
        // 30sp
        assertThat(
                resolveCoreTextLineHeight(
                    fontSize = 40f,
                    lineHeightMultiplier = 1.5f,
                    lineHeightAdd = 0f,
                    autosize = false,
                    density = density,
                )
            )
            .isEqualTo(30.sp)

        // Autosize with multiplier and no additive offset -> relative em so lineHeight scales with
        // the auto-sized font size.
        assertThat(
                resolveCoreTextLineHeight(
                    fontSize = 36f,
                    lineHeightMultiplier = 1.25f,
                    lineHeightAdd = 0f,
                    autosize = true,
                    density = density,
                )
            )
            .isEqualTo(1.25.em)
    }
}
