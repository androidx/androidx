/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.draw

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlurTest {

    @get:Rule val rule = createComposeRule()

    @Test
    @SmallTest
    fun testNoopBlur() {
        // If we are blurring with a 0 pixel radius and we are not clipping
        // the blurred result, this should return the default Modifier
        assertEquals(Modifier.blur(0.dp, BlurredEdgeTreatment.Unbounded), Modifier)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    private fun testBlurEdgeTreatment(
        blurredEdgeTreatment: BlurredEdgeTreatment,
        shape: Shape,
        clip: Boolean,
        tileMode: TileMode,
    ) {
        val blurTag = "blurTag"
        val graphicsLayerTag = "graphicsLayerTag"
        rule.setContent {
            val density = LocalDensity.current.density
            val size = (100 / density).dp
            val blurRadius = (4 / density).dp
            val padding = (2 / density).dp
            val drawBlock: DrawScope.() -> Unit = {
                val blurRadiusPx = blurRadius.toPx()
                inset(blurRadiusPx, blurRadiusPx) { drawRect(Color.Blue) }
            }
            val boxModifierChain = Modifier.size(size).background(Color.Black).padding(padding)
            Row {
                // Compare usages of Modifier.blur vs configuring the RenderEffect on a
                // graphicsLayer directly. The provided BlurredEdgeTreatment usage on Modifier.blur
                // should match the corresponding clip and TileMode configuration on the
                // usage of graphicsLayer
                Box(
                    Modifier.testTag(blurTag)
                        .then(boxModifierChain)
                        .blur(blurRadius, blurredEdgeTreatment)
                        .drawBehind(drawBlock)
                )
                Box(
                    Modifier.testTag(graphicsLayerTag)
                        .then(boxModifierChain)
                        .graphicsLayer {
                            val blurRadiusPx = blurRadius.toPx()
                            this.renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, tileMode)
                            this.clip = clip
                            this.shape = shape
                        }
                        .drawBehind(drawBlock)
                )
            }
        }

        val blurBuffer: IntArray
        val graphicsLayerBuffer: IntArray

        val blurPixelMap =
            rule.onNodeWithTag(blurTag).captureToImage().apply {
                blurBuffer = IntArray(width * height)
                toPixelMap(buffer = blurBuffer)
            }
        val graphicsLayerMap =
            rule.onNodeWithTag(graphicsLayerTag).captureToImage().apply {
                graphicsLayerBuffer = IntArray(width * height)
                toPixelMap(buffer = graphicsLayerBuffer)
            }

        assertEquals(blurPixelMap.width, graphicsLayerMap.width)
        assertEquals(blurPixelMap.height, graphicsLayerMap.height)

        MSSIMMatcher(threshold = 0.99)
            .compareBitmaps(
                blurBuffer,
                graphicsLayerBuffer,
                blurPixelMap.width,
                blurPixelMap.height,
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    @MediumTest
    fun testRectBoundedBlur() {
        // Any bounded edge treatment should clip the underlying graphicsLayer and use
        // TileMode.Clamp in the corresponding BlurEffect
        testBlurEdgeTreatment(BlurredEdgeTreatment.Rectangle, RectangleShape, true, TileMode.Clamp)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    @MediumTest
    fun testUnboundedBlur() {
        // Any unbounded edge treatment should not clip the underlying graphicsLayer and use
        // TileMode.Decal in the corresponding BlurEffect
        testBlurEdgeTreatment(BlurredEdgeTreatment.Unbounded, RectangleShape, false, TileMode.Decal)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    @MediumTest
    fun testCircleBoundedBlur() {
        testBlurEdgeTreatment(BlurredEdgeTreatment(CircleShape), CircleShape, true, TileMode.Clamp)
    }

    @Test
    @SmallTest
    fun testRectangleBlurredEdgeTreatmentHasShape() {
        assertNotNull(BlurredEdgeTreatment.Rectangle.shape)
    }

    @Test
    @SmallTest
    fun testUnboundedBlurredEdgeTreatmentDoesNotHaveShape() {
        assertNull(BlurredEdgeTreatment.Unbounded.shape)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    @MediumTest
    fun testProgressiveBlurVertical() {
        val tag = "progressiveBlur"
        setSplitContent(tag) {
            // Radii resolved through the BlurScope density so the end radius is exactly 20px.
            radius = BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.toDp())
        }
        val map = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
        // Top rows: gradient intensity ~0 -> per-pixel radius < 1px -> exact source colors.
        for (y in 0..2) map.assertSharpBoundaryAt(y)
        // Bottom rows: radius ~20px -> boundary pixels are a black/white mix.
        for (y in map.height - 3 until map.height) map.assertBlurredBoundaryAt(y)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    @MediumTest
    fun testProgressiveBlurMultiStopRegion() {
        // Verifies zero-region stops [(0, 0), (0.5, 0), (1, 20px)].
        // The first half stays pixel-exact; the blur ramps in over the second half.
        val tag = "progressiveBlurMultiStop"
        setSplitContent(tag) {
            radius =
                BlurRadiusSpec.verticalGradient(
                    listOf(BlurStop(0f, 0.dp), BlurStop(0.5f, 0.dp), BlurStop(1f, 20.toDp()))
                )
        }
        val map = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
        // Inside the zero region (y = 0.25 * height): sharp.
        map.assertSharpBoundaryAt(map.height / 4)
        // Near the bottom (radius ~18px): blurred.
        map.assertBlurredBoundaryAt(map.height - 5)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    @MediumTest
    fun testProgressiveBlurRadial() {
        val tag = "progressiveBlurRadial"
        setSplitContent(tag) {
            radius = BlurRadiusSpec.radialGradient(startRadius = 0.dp, endRadius = 20.toDp())
        }
        val map = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
        // At the gradient center (surface center, on the color boundary) the radius is ~0: sharp.
        map.assertSharpBoundaryAt(map.height / 2)
        // On the boundary near the bottom edge (~90% of the default falloff radius): blurred.
        map.assertBlurredBoundaryAt(map.height - 5)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    @MediumTest
    fun testProgressiveBlurMaskIntensityScalesMaxRadius() {
        // Verifies the mask's alpha channel is a 0..1 intensity scaled by maxRadius.
        // Full intensity over a 20px maxRadius must blur wider than over a 2px maxRadius.
        val fullIntensityShader =
            Api33Impl.createRuntimeShader(
                "half4 main(float2 coord) { return half4(0.0, 0.0, 0.0, 1.0); }"
            )
        rule.setContent {
            val density = LocalDensity.current
            val sizeDp = with(density) { TEST_SIZE_PX.toDp() }
            val narrowRadius = with(density) { 2.toDp() }
            val wideRadius = with(density) { 20.toDp() }
            Column {
                Box(
                    Modifier.testTag("narrow")
                        .size(sizeDp)
                        .blur {
                            radius = BlurRadiusSpec.shader(narrowRadius) { fullIntensityShader }
                        }
                        .drawBehind { drawSplitColors() }
                )
                Box(
                    Modifier.testTag("wide")
                        .size(sizeDp)
                        .blur { radius = BlurRadiusSpec.shader(wideRadius) { fullIntensityShader } }
                        .drawBehind { drawSplitColors() }
                )
            }
        }

        // Blur spread: count of intermediate-valued pixels along a horizontal scanline.
        fun spreadOf(tag: String): Int {
            val map = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
            val y = map.height / 2
            var count = 0
            for (x in 0 until map.width) if (map[x, y].isIntermediate()) count++
            return count
        }

        val narrow = spreadOf("narrow")
        val wide = spreadOf("wide")
        assertTrue("2px-alpha shader must blur boundary pixels but spread was $narrow", narrow >= 1)
        assertTrue(
            "20px-alpha spread ($wide) must be > 2x the 2px-alpha spread ($narrow)",
            wide > narrow * 2,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    @MediumTest
    fun testProgressiveBlurUnboundedBleedsOutsideContentBounds() {
        // Verifies Unbounded/Decal treatment bleeds blurred content outside original bounds.
        // Non-transparent bleed appears as non-black pixels outside the box.
        // Decal alpha fade appears as reduced luminance.
        val map = captureBleedContent(BlurredEdgeTreatment.Unbounded)
        val boxTop = (map.height - BLEED_BOX_PX) / 2
        val boxBottom = boxTop + BLEED_BOX_PX // first row below the box
        val boxLeft = (map.width - BLEED_BOX_PX) / 2

        var bleed = 0
        for (y in boxBottom until boxBottom + 10) {
            for (x in boxLeft until boxLeft + BLEED_BOX_PX) {
                if (map[x, y].red > 0.02f) bleed++
            }
        }
        assertTrue("expected blurred content below the box's original bottom edge", bleed > 0)

        // Decal edge fade: the box's outermost blurred row samples transparent black past the
        // edge without renormalizing, so it is dimmer than the fully-interior content.
        val interior = map[map.width / 2, boxTop + BLEED_BOX_PX / 2].red
        val edge = map[map.width / 2, boxBottom - 1].red
        assertTrue(
            "expected decal fade at the box edge (edge=$edge, interior=$interior)",
            edge < interior - 0.1f,
        )
        assertTrue("faded edge row must still be visible, was $edge", edge > 0.02f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    @MediumTest
    fun testProgressiveBlurRectangleDoesNotBleedOutsideContentBounds() {
        // Verifies bounded treatment clips the blur layer.
        // Pixels outside the box remain exactly the parent background.
        val map = captureBleedContent(BlurredEdgeTreatment.Rectangle)
        val boxTop = (map.height - BLEED_BOX_PX) / 2
        val boxLeft = (map.width - BLEED_BOX_PX) / 2
        val boxBottom = boxTop + BLEED_BOX_PX
        val boxRight = boxLeft + BLEED_BOX_PX

        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                val inside = x in boxLeft until boxRight && y in boxTop until boxBottom
                if (!inside) {
                    assertEquals(
                        "expected untouched background outside the box at ($x, $y)",
                        Color.Black.toArgb(),
                        map[x, y].toArgb(),
                    )
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    @MediumTest
    fun testProgressiveBlurRadiusChangeRepaints() {
        // Verifies state-backed radius changes trigger pixel repaints.
        // This validates layer-property updates that skip content re-recording.
        var endRadiusPx by mutableStateOf(0)
        val tag = "repaint"
        rule.setContent {
            val sizeDp = with(LocalDensity.current) { TEST_SIZE_PX.toDp() }
            Box(
                Modifier.testTag(tag)
                    .size(sizeDp)
                    .blur {
                        radius =
                            BlurRadiusSpec.verticalGradient(
                                startRadius = 0.dp,
                                endRadius = endRadiusPx.toDp(),
                            )
                    }
                    .drawBehind { drawSplitColors() }
            )
        }
        rule.waitForIdle()

        val before = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
        // Zero radius everywhere: the boundary is sharp top to bottom.
        for (y in intArrayOf(0, before.height / 2, before.height - 1)) {
            before.assertSharpBoundaryAt(y)
        }

        rule.runOnIdle { endRadiusPx = 20 }
        rule.waitForIdle()

        val after = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
        // Still sharp at the top (start radius 0), now blurred at the bottom.
        after.assertSharpBoundaryAt(0)
        for (y in after.height - 3 until after.height) after.assertBlurredBoundaryAt(y)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    @MediumTest
    fun testProgressiveBlurRadiusChangeDoesNotReRecordContent() {
        // Verifies animating blur radius skips content re-recording.
        var endRadius by mutableStateOf(8.dp)
        var contentDraws = 0
        rule.setContent {
            Box(
                Modifier.size(100.dp)
                    .blur {
                        radius =
                            BlurRadiusSpec.verticalGradient(
                                startRadius = 0.dp,
                                endRadius = endRadius,
                            )
                    }
                    .drawBehind { contentDraws++ }
            )
        }
        rule.waitForIdle()
        val baseline = contentDraws

        rule.runOnIdle { endRadius = 24.dp }
        rule.waitForIdle()

        assertEquals(baseline, contentDraws)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    @MediumTest
    fun testProgressiveBlurContentChangeReRecordsContent() {
        // Verifies genuine content changes force layer content re-recording.
        var color by mutableStateOf(Color.Blue)
        var contentDraws = 0
        rule.setContent {
            Box(
                Modifier.size(100.dp)
                    .blur {
                        radius =
                            BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 12.dp)
                    }
                    .drawBehind {
                        contentDraws++
                        drawRect(color)
                    }
            )
        }
        rule.waitForIdle()
        val baseline = contentDraws

        rule.runOnIdle { color = Color.Red }
        rule.waitForIdle()

        assertTrue(contentDraws > baseline)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    @MediumTest
    fun testProgressiveBlurRadiusAnimatedToZeroMatchesUnblurred() {
        // Verifies animating radius to zero fully clears the blur.
        // The zero configuration must resolve to an identity effect.
        var endRadius by mutableStateOf(20.dp)
        rule.setContent {
            Column {
                Box(
                    Modifier.testTag("blurred").size(100.dp).background(Color.Blue).blur {
                        radius =
                            BlurRadiusSpec.verticalGradient(
                                startRadius = 0.dp,
                                endRadius = endRadius,
                            )
                    }
                ) {
                    // Contrasting inner edge so a lingering blur would be visible.
                    Box(Modifier.size(50.dp).background(Color.Red))
                }
                Box(Modifier.testTag("plain").size(100.dp).background(Color.Blue)) {
                    Box(Modifier.size(50.dp).background(Color.Red))
                }
            }
        }
        rule.waitForIdle()

        rule.runOnIdle { endRadius = 0.dp }
        rule.waitForIdle()

        val blurred = rule.onNodeWithTag("blurred").captureToImage().toPixelMap()
        val plain = rule.onNodeWithTag("plain").captureToImage().toPixelMap()
        assertEquals(blurred.width, plain.width)
        assertEquals(blurred.height, plain.height)
        var diffs = 0
        for (y in 0 until blurred.height) {
            for (x in 0 until blurred.width) {
                if (blurred[x, y] != plain[x, y]) diffs++
            }
        }
        assertEquals("radius animated to zero must render identical to unblurred content", 0, diffs)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    @MediumTest
    fun testProgressiveBlurHoistedShaderUniformAnimation() {
        // Verifies hoisted-shader uniform animations.
        // A mask-based radius must never compare equal between frames.
        // This ensures the platform effect re-creates with fresh uniforms on state change.
        val tag = "hoistedShader"
        var intensity by mutableStateOf(0f)
        val runtimeShader =
            Api33Impl.createRuntimeShader(
                """
                uniform float intensity;
                half4 main(float2 coord) { return half4(0.0, 0.0, 0.0, intensity); }
                """
                    .trimIndent()
            )
        val hoistedRadius = BlurRadiusSpec.shader(16.dp) { runtimeShader }
        rule.setContent {
            Box(
                Modifier.testTag(tag).size(100.dp).background(Color.Blue).blur {
                    // State read inside the block: re-runs the block per change, mutating the
                    // hoisted shader before the effect for this draw is built.
                    Api33Impl.setFloatUniform(runtimeShader, "intensity", intensity)
                    radius = hoistedRadius
                }
            ) {
                Box(Modifier.size(50.dp).background(Color.Red))
            }
        }
        rule.waitForIdle()
        val before = rule.onNodeWithTag(tag).captureToImage().toPixelMap()

        rule.runOnIdle { intensity = 1f }
        rule.waitForIdle()
        val after = rule.onNodeWithTag(tag).captureToImage().toPixelMap()

        assertEquals(before.width, after.width)
        assertEquals(before.height, after.height)
        var diffs = 0
        for (y in 0 until before.height) {
            for (x in 0 until before.width) {
                if (before[x, y] != after[x, y]) diffs++
            }
        }
        assertTrue(
            "mutating the hoisted shader's uniform must change the rendered output",
            diffs > 0,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    @MediumTest
    fun testProgressiveBlurStopPaddingEquivalence() {
        // Verifies fixed-size shader tail padding.
        // A 3-stop gradient must render identically to the same gradient padded to 5 stops.
        val threeStops = listOf(BlurStop(0f, 0.dp), BlurStop(0.5f, 4.dp), BlurStop(1f, 12.dp))
        val fiveStops =
            listOf(
                BlurStop(0f, 0.dp),
                BlurStop(0.5f, 4.dp),
                BlurStop(1f, 12.dp),
                BlurStop(1f, 12.dp),
                BlurStop(1f, 12.dp),
            )
        rule.setContent {
            Column {
                Box(
                    Modifier.testTag("three").size(100.dp).background(Color.Blue).blur {
                        radius = BlurRadiusSpec.verticalGradient(threeStops)
                    }
                ) {
                    // Contrasting inner edge so the blurred result depends on the gradient.
                    Box(Modifier.size(50.dp).background(Color.Red))
                }
                Box(
                    Modifier.testTag("five").size(100.dp).background(Color.Blue).blur {
                        radius = BlurRadiusSpec.verticalGradient(fiveStops)
                    }
                ) {
                    Box(Modifier.size(50.dp).background(Color.Red))
                }
            }
        }

        val three = rule.onNodeWithTag("three").captureToImage().toPixelMap()
        val five = rule.onNodeWithTag("five").captureToImage().toPixelMap()
        assertEquals(three.width, five.width)
        assertEquals(three.height, five.height)
        var diffs = 0
        for (y in 0 until three.height) {
            for (x in 0 until three.width) {
                if (three[x, y] != five[x, y]) diffs++
            }
        }
        assertEquals("padded 5-stop must match 3-stop pixel-for-pixel", 0, diffs)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    @MediumTest
    fun testModifierBlurWithRadiusSpec() {
        val spec = BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp)
        rule.setContent {
            val sizeDp = with(LocalDensity.current) { TEST_SIZE_PX.toDp() }
            Column {
                Box(
                    Modifier.testTag("direct").size(sizeDp).blur(spec).drawBehind {
                        drawSplitColors()
                    }
                )
                Box(
                    Modifier.testTag("block")
                        .size(sizeDp)
                        .blur { radius = spec }
                        .drawBehind { drawSplitColors() }
                )
            }
        }

        val direct = rule.onNodeWithTag("direct").captureToImage().toPixelMap()
        val block = rule.onNodeWithTag("block").captureToImage().toPixelMap()
        assertEquals(direct.width, block.width)
        assertEquals(direct.height, block.height)
        for (y in 0 until direct.height) {
            for (x in 0 until direct.width) {
                assertEquals(
                    "pixel ($x, $y) must match between direct overload and block overload",
                    direct[x, y],
                    block[x, y],
                )
            }
        }
    }

    /** Hard-edged split content: left half black, right half white. */
    private fun DrawScope.drawSplitColors() {
        drawRect(Color.Black, size = Size(size.width / 2f, size.height))
        drawRect(
            Color.White,
            topLeft = Offset(size.width / 2f, 0f),
            size = Size(size.width / 2f, size.height),
        )
    }

    /** Sets a square of split content blurred with [block]. */
    private fun setSplitContent(tag: String, block: BlurScope.() -> Unit) {
        rule.setContent {
            val sizeDp = with(LocalDensity.current) { TEST_SIZE_PX.toDp() }
            Box(Modifier.testTag(tag).size(sizeDp).blur(block).drawBehind { drawSplitColors() })
        }
    }

    /**
     * Captures a black parent holding a centered white box. Blurs the box with a vertical 0 to 12px
     * gradient and [edgeTreatment].
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    private fun captureBleedContent(edgeTreatment: BlurredEdgeTreatment): PixelMap {
        val tag = "bleedParent"
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.testTag(tag).size(TEST_SIZE_PX.toDp()).background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(BLEED_BOX_PX.toDp())
                            .blur {
                                radius =
                                    BlurRadiusSpec.verticalGradient(
                                        startRadius = 0.dp,
                                        endRadius = 12.toDp(),
                                    )
                                this.edgeTreatment = edgeTreatment
                            }
                            .background(Color.White)
                    )
                }
            }
        }
        return rule.onNodeWithTag(tag).captureToImage().toPixelMap()
    }

    /**
     * Returns true if the pixel is an intermediate grayscale value. Intermediate means meaningfully
     * away from source colors (a blur mix).
     */
    private fun Color.isIntermediate(): Boolean = red > 0.02f && red < 0.98f

    /**
     * Asserts the split boundary is sharp at row [y]. Pixels adjacent to the boundary must exactly
     * match the source colors.
     */
    private fun PixelMap.assertSharpBoundaryAt(y: Int) {
        val boundary = width / 2
        assertEquals(
            "expected exact black just left of the boundary at y=$y",
            Color.Black.toArgb(),
            this[boundary - 1, y].toArgb(),
        )
        assertEquals(
            "expected exact white just right of the boundary at y=$y",
            Color.White.toArgb(),
            this[boundary, y].toArgb(),
        )
    }

    /** Asserts the split boundary is blurred at row [y]: boundary pixels are intermediate. */
    private fun PixelMap.assertBlurredBoundaryAt(y: Int) {
        val boundary = width / 2
        assertTrue(
            "expected a blur mix just left of the boundary at y=$y, was ${this[boundary - 1, y]}",
            this[boundary - 1, y].isIntermediate(),
        )
        assertTrue(
            "expected a blur mix just right of the boundary at y=$y, was ${this[boundary, y]}",
            this[boundary, y].isIntermediate(),
        )
    }

    companion object {
        /** Side length of the pixel-test surfaces, in px. */
        private const val TEST_SIZE_PX = 100
        /** Side length of the inner box in the bleed tests, in px. */
        private const val BLEED_BOX_PX = 40
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object Api33Impl {
    fun createRuntimeShader(skiaString: String): android.graphics.Shader {
        return android.graphics.RuntimeShader(skiaString)
    }

    fun setFloatUniform(shader: android.graphics.Shader, name: String, value: Float) {
        if (shader is android.graphics.RuntimeShader) {
            shader.setFloatUniform(name, value)
        }
    }
}
