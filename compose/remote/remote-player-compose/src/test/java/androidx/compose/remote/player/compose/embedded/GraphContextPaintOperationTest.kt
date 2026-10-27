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

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.remote.core.operations.ColorConstant
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.ImageAttribute
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.TextMeasure
import androidx.compose.remote.core.operations.layout.CanvasOperations
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.modifiers.RippleModifierOperation
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GraphContextPaintOperationTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun colorAttributeEvaluatesThroughPaintContextInGraphContext() {
        val sourceColorId = 1
        val doc =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(ColorConstant(sourceColorId, 0xFFFF8000.toInt()))
                operations.add(ColorAttribute(10, sourceColorId, ColorAttribute.COLOR_RED))
                operations.add(ColorAttribute(11, sourceColorId, ColorAttribute.COLOR_GREEN))
                operations.add(ColorAttribute(12, sourceColorId, ColorAttribute.COLOR_BLUE))
                operations.add(ColorAttribute(13, sourceColorId, ColorAttribute.COLOR_ALPHA))
                operations.add(NamedVariable(10, NamedVariable.FLOAT_TYPE, "USER:red"))
                operations.add(NamedVariable(11, NamedVariable.FLOAT_TYPE, "USER:green"))
                operations.add(NamedVariable(12, NamedVariable.FLOAT_TYPE, "USER:blue"))
                operations.add(NamedVariable(13, NamedVariable.FLOAT_TYPE, "USER:alpha"))
            }

        val state = RcPlayerState(doc)
        assertThat(state.floatState("red").value).isWithin(0.01f).of(1.0f)
        assertThat(state.floatState("green").value).isWithin(0.01f).of(128f / 255f)
        assertThat(state.floatState("blue").value).isWithin(0.01f).of(0.0f)
        assertThat(state.floatState("alpha").value).isWithin(0.01f).of(1.0f)
    }

    @Test
    fun textMeasureEvaluatesThroughPaintContextInGraphContext() {
        val textId = 5
        val doc =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(TextData(textId, "Hello World"))
                operations.add(
                    PaintData().apply { mPaintData = PaintBundle().apply { setTextSize(48f) } }
                )
                operations.add(TextMeasure(20, textId, TextMeasure.MEASURE_WIDTH))
                operations.add(TextMeasure(21, textId, TextMeasure.MEASURE_HEIGHT))
                operations.add(NamedVariable(20, NamedVariable.FLOAT_TYPE, "USER:width"))
                operations.add(NamedVariable(21, NamedVariable.FLOAT_TYPE, "USER:height"))
            }

        val state = RcPlayerState(doc)
        // Before canvas execution records a stream paint, TextMeasure falls back to the default
        // 12px paint size and produces a finite positive width rather than NaN/0.
        val defaultWidth = state.floatState("width").value
        assertThat(defaultWidth.isNaN()).isFalse()
        assertThat(defaultWidth).isGreaterThan(0f)
        assertThat(state.floatState("height").value).isGreaterThan(0f)

        // Rendering the canvas stream applies the 48px PaintData to the TextMeasure op.
        rule.setContent { RcPlayer(state = state, modifier = Modifier.size(200.dp, 80.dp)) }
        rule.onRoot().captureToImage()

        val largeWidth = state.floatState("width").value
        assertThat(largeWidth).isGreaterThan(defaultWidth)
    }

    @Test
    fun imageAttributeEvaluatesBitmapDataDimensions() {
        val imageId = 7
        val doc =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(
                    BitmapData(
                        imageId,
                        BitmapData.TYPE_PNG,
                        320,
                        BitmapData.ENCODING_URL,
                        240,
                        "camera/current".toByteArray(),
                    )
                )
                operations.add(ImageAttribute(30, imageId, ImageAttribute.IMAGE_WIDTH, null))
                operations.add(ImageAttribute(31, imageId, ImageAttribute.IMAGE_HEIGHT, null))
                operations.add(NamedVariable(30, NamedVariable.FLOAT_TYPE, "USER:width"))
                operations.add(NamedVariable(31, NamedVariable.FLOAT_TYPE, "USER:height"))
            }

        val state = RcPlayerState(doc)
        assertThat(state.floatState("width").value).isEqualTo(320f)
        assertThat(state.floatState("height").value).isEqualTo(240f)
    }

    @Test
    fun imageAttributeUpdatesZeroDimensionUrlBitmapFromLoadedAndOverriddenImage() {
        val state = RcPlayerState(urlBitmapDocument(width = 0, height = 0))
        val loaded =
            mutableStateOf<Drawable?>(
                BitmapDrawable(
                    Resources.getSystem(),
                    Bitmap.createBitmap(240, 160, Bitmap.Config.ARGB_8888),
                )
            )
        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) {
                RcPlayer(state = state, imageLoader = { loaded })
            }
        }
        rule.waitForIdle()

        assertThat(state.floatState("width").value).isEqualTo(240f)
        assertThat(state.floatState("height").value).isEqualTo(160f)

        // Overriding the bitmap updates the dimensions reported by ImageAttribute.
        rule.runOnIdle {
            loaded.value = null
            state.bitmapState("avatar").value =
                Bitmap.createBitmap(480, 360, Bitmap.Config.ARGB_8888)
        }
        rule.waitForIdle()

        assertThat(state.floatState("width").value).isEqualTo(480f)
        assertThat(state.floatState("height").value).isEqualTo(360f)
    }

    @Test
    fun imageAttributeUpdatesDefault1x1UrlBitmapFromLoadedImage() {
        val state = RcPlayerState(urlBitmapDocument(width = 1, height = 1))
        val loaded =
            mutableStateOf<Drawable?>(
                BitmapDrawable(
                    Resources.getSystem(),
                    Bitmap.createBitmap(240, 160, Bitmap.Config.ARGB_8888),
                )
            )
        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) {
                RcPlayer(state = state, imageLoader = { loaded })
            }
        }
        rule.waitForIdle()

        assertThat(state.floatState("width").value).isEqualTo(240f)
        assertThat(state.floatState("height").value).isEqualTo(160f)
    }

    /** A URL bitmap named `USER:avatar` with placeholder dimensions and its width and height. */
    private fun urlBitmapDocument(width: Int, height: Int): CoreDocument {
        val imageId = 8
        return CoreDocument(RemoteClock.SYSTEM).apply {
            operations.add(
                BitmapData(
                    imageId,
                    BitmapData.TYPE_PNG,
                    width.toShort(),
                    BitmapData.ENCODING_URL,
                    height.toShort(),
                    "https://example.com/avatar.png".toByteArray(),
                )
            )
            operations.add(NamedVariable(imageId, NamedVariable.IMAGE_TYPE, "USER:avatar"))
            operations.add(ImageAttribute(32, imageId, ImageAttribute.IMAGE_WIDTH, null))
            operations.add(ImageAttribute(33, imageId, ImageAttribute.IMAGE_HEIGHT, null))
            operations.add(NamedVariable(32, NamedVariable.FLOAT_TYPE, "USER:width"))
            operations.add(NamedVariable(33, NamedVariable.FLOAT_TYPE, "USER:height"))
        }
    }

    @Test
    fun resolveBitmapReturnsNullOnUnresolvableUrlWithoutThrowing() {
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
        val doc =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(badUrlBitmap)
                operations.add(NamedVariable(imageId, NamedVariable.IMAGE_TYPE, "USER:img"))
            }

        val state = RcPlayerState(doc)
        assertThat(state.bitmapState("img").value).isNull()

        val ctx =
            state.inspectableElements.first { it.name == "remoteContext" }.value
                as AndroidRemoteContext
        // Declared metadata remains intact in the object store.
        assertThat(ctx.mRemoteComposeState.getObject(imageId)).isSameInstanceAs(badUrlBitmap)
    }

    @Test
    fun resolveBitmapThroughGraphContextDecodesIntoStore() {
        val imageId = 11
        val inlineBitmap =
            BitmapData(
                imageId,
                BitmapData.TYPE_RAW8888,
                2,
                BitmapData.ENCODING_INLINE,
                2,
                ByteArray(16) { 0xFF.toByte() },
            )
        val doc =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(inlineBitmap)
                operations.add(
                    PaintData().apply {
                        mPaintData = PaintBundle().apply { setTextureShader(imageId, 0, 0, 0, 0) }
                    }
                )
                operations.add(DrawRect(0f, 0f, 2f, 2f))
            }

        val playerState = RcPlayerState(doc)
        val ctx =
            playerState.inspectableElements.first { it.name == "remoteContext" }.value
                as AndroidRemoteContext
        // Before drawing, inline BitmapData is registered lazily without decoding into the store.
        assertThat(ctx.mRemoteComposeState.containsId(imageId)).isFalse()

        // Drawing resolves the texture shader through GraphContext and persists the decoded Bitmap
        // into the backing store.
        rule.setContent { RcPlayer(state = playerState, modifier = Modifier.size(10.dp)) }
        rule.onRoot().captureToImage()

        val resolved = ctx.mRemoteComposeState.getFromId(imageId) as? Bitmap
        assertThat(resolved).isNotNull()
        assertThat(resolved!!.width).isEqualTo(2)
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
        NamedVariable.apply(wire, 60, NamedVariable.FLOAT_TYPE, "USER:sampleWidth")
        NamedVariable.apply(wire, 61, NamedVariable.FLOAT_TYPE, "USER:imageWidth")
        buffer.addContainerEnd()
        buffer.addContainerEnd()

        val document = CoreDocument()
        document.initFromBuffer(buffer)
        val playerState = RcPlayerState(document)

        assertThat(playerState.floatState("sampleWidth").value).isGreaterThan(0f)
        assertThat(playerState.floatState("imageWidth").value).isEqualTo(64f)

        val ctx =
            playerState.inspectableElements.first { it.name == "remoteContext" }.value
                as AndroidRemoteContext
        val registered = ctx.mRemoteComposeState.getObject(42) as? BitmapData
        assertThat(registered).isNotNull()
        assertThat(registered!!.width).isEqualTo(64)
        assertThat(registered.height).isEqualTo(48)
    }

    @Test
    fun buildFontVariationSettingsPreservesWeightAndStyleWhenNotExplicit() {
        val document = buildWriterDocument {
            val wdthOnlyId = addText("WdthOnly")
            val explicitWghtId = addText("ExplicitWght")
            root {
                startColumn(RecordingModifier().width(240f).height(160f), 0, 0)
                startTextComponent(
                    RecordingModifier().width(240f).height(60f),
                    wdthOnlyId,
                    -1,
                    0xFF000000.toInt(),
                    -1,
                    18f,
                    -1f,
                    -1f,
                    1, // italic
                    700f, // FontWeight.Bold
                    "device:sans-serif",
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
                    arrayOf("wdth"),
                    floatArrayOf(110f),
                    false,
                    0,
                )
                endTextComponent()
                startTextComponent(
                    RecordingModifier().width(240f).height(60f),
                    explicitWghtId,
                    -1,
                    0xFF000000.toInt(),
                    -1,
                    18f,
                    -1f,
                    -1f,
                    0, // normal
                    700f, // FontWeight.Bold
                    "device:sans-serif",
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
                    arrayOf("wght"),
                    floatArrayOf(550f),
                    false,
                    0,
                )
                endTextComponent()
                endColumn()
            }
        }

        rule.setContent { Box(Modifier.size(240.dp, 160.dp)) { RcPlayer(document = document) } }
        rule.waitForIdle()

        // Only "wdth" is in fontAxis -> "wght" (from FontWeight.Bold) and "ital" (from Italic) are
        // appended so variable-font resolution does not revert to 400/upright.
        val wdthOnlyFont =
            (rule.textLayoutResultOf("WdthOnly").layoutInput.style.fontFamily as FontListFontFamily)
                .fonts
                .single() as AndroidFont
        val byTag =
            wdthOnlyFont.variationSettings.settings.associate {
                it.axisName to it.toVariationValue(null)
            }
        assertThat(byTag["wdth"]).isEqualTo(110f)
        assertThat(byTag["wght"]).isEqualTo(700f)
        assertThat(byTag["ital"]).isEqualTo(1f)

        // Explicit "wght" in fontAxis is preserved and not overwritten by fontWeight.
        val explicitWghtFont =
            (rule.textLayoutResultOf("ExplicitWght").layoutInput.style.fontFamily
                    as FontListFontFamily)
                .fonts
                .single() as AndroidFont
        val explicitByTag =
            explicitWghtFont.variationSettings.settings.associate {
                it.axisName to it.toVariationValue(null)
            }
        assertThat(explicitByTag["wght"]).isEqualTo(550f)
        assertThat(explicitByTag.containsKey("ital")).isFalse()
    }

    @Test
    fun resolveCoreTextLineHeightUsesEffectiveFontSizeAndAutosizeRelativeEm() {
        val document = buildWriterDocument {
            val defaultId = addText("DefaultLineHeight")
            val fixedId = addText("FixedLineHeight")
            val autosizeId = addText("AutosizeLineHeight")
            root {
                startColumn(RecordingModifier().width(300f).height(300f), 0, 0)
                // Default multiplier=1f, add=0f -> Unspecified
                startTextComponent(
                    RecordingModifier().width(280f).height(80f),
                    defaultId,
                    -1,
                    0xFF000000.toInt(),
                    -1,
                    40f,
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
                    null,
                    null,
                    false,
                    0,
                )
                endTextComponent()
                // Fixed font size (40px at density 2 -> 20sp) * 1.5 multiplier = 30sp
                startTextComponent(
                    RecordingModifier().width(280f).height(80f),
                    fixedId,
                    -1,
                    0xFF000000.toInt(),
                    -1,
                    40f,
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
                    1.5f,
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
                endTextComponent()
                // Autosize with multiplier and no additive offset -> relative em
                startTextComponent(
                    RecordingModifier().width(280f).height(80f),
                    autosizeId,
                    -1,
                    0xFF000000.toInt(),
                    -1,
                    36f,
                    8f,
                    60f,
                    0,
                    400f,
                    null,
                    CoreText.TEXT_ALIGN_START,
                    CoreText.OVERFLOW_CLIP,
                    1,
                    0f,
                    0f,
                    1.25f,
                    0,
                    0,
                    0,
                    false,
                    false,
                    null,
                    null,
                    true,
                    0,
                )
                endTextComponent()
                endColumn()
            }
        }

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 1f)) {
                Box(Modifier.size(300.dp, 300.dp)) { RcPlayer(document = document) }
            }
        }
        rule.waitForIdle()

        assertThat(rule.textLayoutResultOf("DefaultLineHeight").layoutInput.style.lineHeight)
            .isEqualTo(TextUnit.Unspecified)
        assertThat(rule.textLayoutResultOf("FixedLineHeight").layoutInput.style.lineHeight)
            .isEqualTo(30.sp)
        assertThat(rule.textLayoutResultOf("AutosizeLineHeight").layoutInput.style.lineHeight)
            .isEqualTo(1.25.em)
    }

    @Test
    fun standaloneRippleModifierHandlesTouchWithoutSpuriousClickSemantics() {
        val doc = buildWriterDocument {
            val labelId = addText("rippleOnly")
            root {
                startBox(
                    RecordingModifier().width(48f).height(48f).then { w ->
                        val wire = w.buffer.buffer
                        RippleModifierOperation.apply(wire)
                        CoreSemantics().apply { mContentDescriptionId = labelId }.write(wire)
                    },
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                endBox()
            }
        }

        rule.setContent { RcPlayer(document = doc, modifier = Modifier.size(100.dp)) }
        rule.waitForIdle()

        val node = rule.onNodeWithContentDescription("rippleOnly")
        assertThat(SemanticsActions.OnClick in node.fetchSemanticsNode().config).isFalse()

        node.performTouchInput {
            down(center)
            up()
        }
        rule.waitForIdle()
    }
}
