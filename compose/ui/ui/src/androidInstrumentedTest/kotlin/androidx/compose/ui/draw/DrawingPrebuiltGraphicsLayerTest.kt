/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.SubcompositionReusableContentHost
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class DrawingPrebuiltGraphicsLayerTest {

    @get:Rule val rule = createComposeRule()

    private val size = 2
    private val sizeDp = with(rule.density) { size.toDp() }
    private val expectedSize = IntSize(size, size)

    private var layer: GraphicsLayer? = null
    private var context: GraphicsContext? = null
    private var drawPrebuiltLayer by mutableStateOf(false)

    @After
    fun releaseLayer() {
        rule.runOnUiThread {
            layer?.let { context!!.releaseGraphicsLayer(it) }
            layer = null
        }
    }

    @Test
    fun continueDrawingPrebuiltLayer() {
        rule.setContent {
            if (!drawPrebuiltLayer) {
                ColoredBox()
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle { drawPrebuiltLayer = true }

        rule.onNodeWithTag(LayerDrawingBoxTag).captureToImage().assertPixels(expectedSize) {
            Color.Red
        }
    }

    @Test
    fun sizeIsCorrect() {
        rule.setContent { ColoredBox() }

        rule.runOnIdle { assertThat(layer!!.size).isEqualTo(IntSize(size, size)) }
    }

    @Test
    fun drawingWithAlpha() {
        rule.setContent {
            if (!drawPrebuiltLayer) {
                ColoredBox()
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle {
            drawPrebuiltLayer = true
            layer!!.alpha = 0.5f
        }

        rule.onNodeWithTag(LayerDrawingBoxTag).captureToImage().assertPixels(expectedSize) {
            Color.Red.copy(alpha = 0.5f).compositeOver(Color.White)
        }
    }

    @Test
    fun keepComposingTheNodeWeTookLayerFrom() {
        if (Build.VERSION.SDK_INT == 28) {
            // there is a bug on 28: b/329262831
            return
        }
        var color by mutableStateOf(Color.Blue)

        rule.setContent {
            Column {
                ColoredBox(color = { color })
                if (drawPrebuiltLayer) {
                    LayerDrawingBox()
                }
            }
        }

        rule.runOnIdle { drawPrebuiltLayer = true }

        rule.onNodeWithTag(ColoredBoxTag).captureToImage().assertPixels(expectedSize) { Color.Blue }
        rule.onNodeWithTag(LayerDrawingBoxTag).captureToImage().assertPixels(expectedSize) {
            Color.Blue
        }

        rule.runOnUiThread { color = Color.Green }

        rule.onNodeWithTag(ColoredBoxTag).captureToImage().assertPixels(expectedSize) {
            Color.Green
        }
        rule.onNodeWithTag(LayerDrawingBoxTag).captureToImage().assertPixels(expectedSize) {
            Color.Green
        }
    }

    @Test
    fun keepDrawingTheLayerWePreviouslyPlacedWith() {
        // even that we don't place it anymore, it still holds the content we can continue drawing
        rule.setContent {
            Column {
                if (!drawPrebuiltLayer) {
                    val layer = obtainLayer()
                    Canvas(
                        Modifier.layout { measurable, _ ->
                            val placeable = measurable.measure(Constraints.fixed(size, size))
                            layout(placeable.width, placeable.height) {
                                placeable.placeWithLayer(0, 0, layer)
                            }
                        }
                    ) {
                        drawRect(Color.Red)
                    }
                } else {
                    LayerDrawingBox()
                }
            }
        }

        rule.runOnIdle { drawPrebuiltLayer = true }

        rule.onNodeWithTag(LayerDrawingBoxTag).captureToImage().assertPixels(expectedSize) {
            Color.Red
        }
    }

    @Test
    fun drawNestedLayers_drawLayer() {
        rule.setContent {
            if (!drawPrebuiltLayer) {
                Box(Modifier.drawIntoLayer()) {
                    Canvas(Modifier.size(sizeDp).drawIntoLayer(rememberGraphicsLayer())) {
                        drawRect(Color.Red)
                    }
                }
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle { drawPrebuiltLayer = true }

        rule.onNodeWithTag(LayerDrawingBoxTag).captureToImage().assertPixels(expectedSize) {
            Color.Red
        }
    }

    @Test
    fun keepDrawingNestedLayers_drawLayer_deeper() {
        rule.setContent {
            if (!drawPrebuiltLayer) {
                Box(Modifier.drawIntoLayer()) {
                    Box(Modifier.drawIntoLayer(rememberGraphicsLayer())) {
                        Canvas(Modifier.size(sizeDp).drawIntoLayer(rememberGraphicsLayer())) {
                            drawRect(Color.Red)
                        }
                    }
                }
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle { drawPrebuiltLayer = true }

        rule.onNodeWithTag(LayerDrawingBoxTag).captureToImage().assertPixels(expectedSize) {
            Color.Red
        }
    }

    @Test
    fun keepDrawingNestedLayers_graphicsLayerModifier() {
        // TODO remove this after we start using new layers on P
        Assume.assumeTrue(Build.VERSION.SDK_INT != Build.VERSION_CODES.P)
        rule.setContent {
            if (!drawPrebuiltLayer) {
                Box(Modifier.drawIntoLayer()) {
                    Box(Modifier.graphicsLayer()) {
                        Canvas(Modifier.size(sizeDp).graphicsLayer()) { drawRect(Color.Red) }
                    }
                }
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle { drawPrebuiltLayer = true }

        rule.onNodeWithTag(LayerDrawingBoxTag).captureToImage().assertPixels(expectedSize) {
            Color.Red
        }
    }

    @Test
    fun keepDrawingNestedLayers_deactivatedGraphicsLayerModifierScheduledForInvalidation() {
        val counter = mutableStateOf(0)
        // TODO remove this after we start using new layers on P
        Assume.assumeTrue(Build.VERSION.SDK_INT != Build.VERSION_CODES.P)
        rule.setContent {
            SubcompositionReusableContentHost(active = !drawPrebuiltLayer) {
                Box(Modifier.drawIntoLayer().graphicsLayer()) {
                    Canvas(Modifier.size(sizeDp)) {
                        counter.value
                        drawRect(Color.Red)
                    }
                }
            }
            if (drawPrebuiltLayer) {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle {
            drawPrebuiltLayer = true

            // changing the counter to trigger the layer invalidation. the invalidation should
            // be ignored in the end as we will release the layer before it will be drawn
            counter.value++
        }

        rule.onNodeWithTag(LayerDrawingBoxTag).captureToImage().assertPixels(expectedSize) {
            Color.Red
        }
    }

    @Test
    fun keepDrawingLayerFromANodeScheduledForInvalidation() {
        val counter = mutableStateOf(0)
        rule.setContent {
            if (!drawPrebuiltLayer) {
                ColoredBox(
                    color = {
                        counter.value
                        Color.Red
                    }
                )
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle {
            drawPrebuiltLayer = true

            // changing the counter to trigger the layer invalidation. the invalidation should
            // be ignored in the end as we will release the layer before it will be drawn
            counter.value++
        }

        rule.onNodeWithTag(LayerDrawingBoxTag).captureToImage().assertPixels(expectedSize) {
            Color.Red
        }
    }

    @Test
    fun updateLayerProperties() {
        rule.setContent {
            if (!drawPrebuiltLayer) {
                ColoredBox()
            } else {
                LayerDrawingBox()
            }
        }

        rule.runOnIdle {
            drawPrebuiltLayer = true
            layer!!.alpha = 1f
        }

        rule.runOnIdle { layer!!.alpha = 0.5f }

        rule.onNodeWithTag(LayerDrawingBoxTag).captureToImage().assertPixels(expectedSize) {
            Color.Red.copy(alpha = 0.5f).compositeOver(Color.White)
        }
    }

    @Test
    fun invalidatingNotPlacedAnymoreChildIsNotCorruptingTheLayerContent() {
        var shouldPlace by mutableStateOf(true)
        var color by mutableStateOf(Color.Red)
        rule.setContent {
            Column {
                val layer = obtainLayer()
                Canvas(
                    Modifier.layout { measurable, _ ->
                        val placeable = measurable.measure(Constraints.fixed(size, size))
                        layout(placeable.width, placeable.height) {
                            if (shouldPlace) {
                                placeable.placeWithLayer(0, 0, layer)
                            }
                        }
                    }
                ) {
                    drawRect(color)
                }
                LayerDrawingBox()
            }
        }

        rule.runOnIdle {
            shouldPlace = false
            // changing the color shouldn't affect the layer as we don't place with it anymore
            color = Color.Green
        }

        rule.onNodeWithTag(LayerDrawingBoxTag).captureToImage().assertPixels(expectedSize) {
            Color.Red
        }
    }

    @Composable
    private fun ColoredBox(modifier: Modifier = Modifier, color: () -> Color = { Color.Red }) {
        Canvas(modifier.size(sizeDp).testTag(ColoredBoxTag).drawIntoLayer()) { drawRect(color()) }
    }

    @Composable
    private fun obtainLayer(): GraphicsLayer {
        context = LocalGraphicsContext.current
        return layer ?: context!!.createGraphicsLayer().also { layer = it }
    }

    @Composable
    private fun Modifier.drawIntoLayer(layer: GraphicsLayer = obtainLayer()): Modifier {
        return drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
        }
    }

    @Composable
    private fun LayerDrawingBox() {
        Canvas(Modifier.size(sizeDp).testTag(LayerDrawingBoxTag)) {
            drawRect(Color.White)
            layer?.let { drawLayer(it) }
        }
    }
}

private val LayerDrawingBoxTag = "LayerDrawingBoxTag"
private val ColoredBoxTag = "RedBoxTag"
