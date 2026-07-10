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

package androidx.compose.ui

import android.os.Build
import android.view.View
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.RenderNodeApi23
import androidx.compose.ui.platform.RenderNodeApi29
import androidx.compose.ui.platform.ViewLayer
import androidx.compose.ui.platform.ViewLayerContainer
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DrawModifierTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())
    @get:Rule val excessiveAssertions = AndroidOwnerExtraAssertionsRule()
    private lateinit var activity: TestActivity
    private lateinit var density: Density

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        density = Density(activity)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleDrawTest() {
        val yellow = Color(0xFFFFFF00)
        val red = Color(0xFF800000)
        val model = SquareModel(outerColor = yellow, innerColor = red, size = 10)
        composeSquares(model)

        validateSquareColors(outerColor = yellow, innerColor = red, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O, maxSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleDrawTestLegacyFallback() {
        try {
            RenderNodeApi23.testFailCreateRenderNode = true
            val yellow = Color(0xFFFFFF00)
            val red = Color(0xFF800000)
            val model = SquareModel(outerColor = yellow, innerColor = red, size = 10)
            composeSquares(model)

            validateSquareColors(outerColor = yellow, innerColor = red, size = 10)
        } finally {
            RenderNodeApi23.testFailCreateRenderNode = false
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun nestedDrawTest() {
        val yellow = Color(0xFFFFFF00)
        val red = Color(0xFF800000)
        val model = SquareModel(outerColor = yellow, innerColor = red, size = 10)
        composeNestedSquares(model)

        validateSquareColors(outerColor = yellow, innerColor = red, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeDrawTest() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        composeSquares(model)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        val red = Color(0xFF800000)
        val yellow = Color(0xFFFFFF00)
        rule.runOnUiThread {
            model.outerColor = red
            model.innerColor = yellow
        }

        validateSquareColors(outerColor = red, innerColor = yellow, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun noPlaceNoDraw() {
        val green = Color(0xFF00FF00)
        val white = Color(0xFFFFFFFF)
        val model = SquareModel(size = 20, outerColor = green, innerColor = white)

        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        Padding(
                            size = (model.size * 3),
                            modifier = Modifier.fillColor(model, isInner = false),
                        ) {}
                        Padding(
                            size = model.size,
                            modifier = Modifier.fillColor(model, isInner = true),
                        ) {}
                    },
                    measurePolicy = { measurables, constraints ->
                        val placeables = measurables.map { it.measure(constraints) }
                        layout(placeables[0].width, placeables[0].height) {
                            placeables[0].place(0, 0)
                        }
                    },
                )
            }
        }
        validateSquareColors(outerColor = green, innerColor = green, size = 20)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawOrderWithChildren() {
        val green = Color(0xFF00FF00)
        val white = Color(0xFFFFFFFF)
        val model = SquareModel(size = 20, outerColor = green, innerColor = white)

        rule.runOnUiThread {
            activity.setContent {
                val contentDrawing =
                    object : DrawModifier {
                        override fun ContentDrawScope.draw() {
                            // Fill the space with the outerColor
                            drawRect(model.outerColor)
                            val offset = size.width / 3
                            // clip drawing to the inner rectangle
                            clipRect(offset, offset, offset * 2, offset * 2) {
                                this@draw.drawContent()

                                // Fill bottom half with innerColor -- should be clipped
                                drawRect(
                                    model.innerColor,
                                    topLeft = Offset(0f, size.height / 2f),
                                    size = Size(size.width, size.height / 2f),
                                )
                            }
                        }
                    }

                val paddingContent =
                    Modifier.drawBehind {
                        // Fill top half with innerColor -- should be clipped
                        drawRect(model.innerColor, size = Size(size.width, size.height / 2f))
                    }
                Padding(size = (model.size * 3), modifier = contentDrawing.then(paddingContent)) {}
            }
        }
        validateSquareColors(outerColor = green, innerColor = white, size = 20)
    }

    @Test
    fun testCompositingStrategyAuto() {
        var compositingApplied = false
        activity.runOnUiThread {
            compositingApplied =
                when (Build.VERSION.SDK_INT) {
                    // Use public RenderNode API
                    in Build.VERSION_CODES.Q..Int.MAX_VALUE ->
                        rule.verifyRenderNode29CompositingStrategy(
                            CompositingStrategy.Auto,
                            expectedCompositing = false,
                            expectedOverlappingRendering = true,
                        )
                    // Cannot access private APIs on P
                    Build.VERSION_CODES.P ->
                        rule.verifyViewLayerCompositingStrategy(
                            CompositingStrategy.Auto,
                            View.LAYER_TYPE_NONE,
                            true,
                        )
                    // Use stub access to framework RenderNode API
                    in Build.VERSION_CODES.M..Int.MAX_VALUE ->
                        rule.verifyRenderNode23CompositingStrategy(
                            CompositingStrategy.Auto,
                            expectedLayerType = View.LAYER_TYPE_NONE,
                            expectedOverlappingRendering = true,
                        )
                    // No RenderNodes, use Views instead
                    else ->
                        rule.verifyViewLayerCompositingStrategy(
                            CompositingStrategy.Auto,
                            View.LAYER_TYPE_NONE,
                            true,
                        )
                }
        }

        rule.waitForIdle()
        assertTrue(compositingApplied)
    }

    @Test
    fun testCompositingStrategyModulateAlpha() {
        var compositingApplied = false
        activity.runOnUiThread {
            compositingApplied =
                when (Build.VERSION.SDK_INT) {
                    // Use public RenderNode API
                    in Build.VERSION_CODES.Q..Int.MAX_VALUE ->
                        rule.verifyRenderNode29CompositingStrategy(
                            CompositingStrategy.ModulateAlpha,
                            expectedCompositing = false,
                            expectedOverlappingRendering = false,
                        )
                    // Cannot access private APIs on P
                    Build.VERSION_CODES.P ->
                        rule.verifyViewLayerCompositingStrategy(
                            CompositingStrategy.ModulateAlpha,
                            View.LAYER_TYPE_NONE,
                            false,
                        )
                    // Use stub access to framework RenderNode API
                    in Build.VERSION_CODES.M..Int.MAX_VALUE ->
                        rule.verifyRenderNode23CompositingStrategy(
                            CompositingStrategy.ModulateAlpha,
                            expectedLayerType = View.LAYER_TYPE_NONE,
                            expectedOverlappingRendering = false,
                        )
                    // No RenderNodes, use Views instead
                    else ->
                        rule.verifyViewLayerCompositingStrategy(
                            CompositingStrategy.ModulateAlpha,
                            View.LAYER_TYPE_NONE,
                            false,
                        )
                }
        }

        rule.waitForIdle()
        assertTrue(compositingApplied)
    }

    @Test
    fun testCompositingStrategyAlways() {
        var compositingApplied = false
        activity.runOnUiThread {
            compositingApplied =
                when (Build.VERSION.SDK_INT) {
                    // Use public RenderNode API
                    in Build.VERSION_CODES.Q..Int.MAX_VALUE ->
                        rule.verifyRenderNode29CompositingStrategy(
                            CompositingStrategy.Offscreen,
                            expectedCompositing = true,
                            expectedOverlappingRendering = true,
                        )
                    // Cannot access private APIs on P
                    Build.VERSION_CODES.P ->
                        rule.verifyViewLayerCompositingStrategy(
                            CompositingStrategy.Offscreen,
                            View.LAYER_TYPE_HARDWARE,
                            true,
                        )
                    // Use stub access to framework RenderNode API
                    in Build.VERSION_CODES.M..Int.MAX_VALUE ->
                        rule.verifyRenderNode23CompositingStrategy(
                            CompositingStrategy.Offscreen,
                            expectedLayerType = View.LAYER_TYPE_HARDWARE,
                            expectedOverlappingRendering = true,
                        )
                    // No RenderNodes, use Views instead
                    else ->
                        rule.verifyViewLayerCompositingStrategy(
                            CompositingStrategy.Offscreen,
                            View.LAYER_TYPE_HARDWARE,
                            true,
                        )
                }
        }

        rule.waitForIdle()
        assertTrue(compositingApplied)
    }

    @Test
    fun testDrawWithLayoutNotPlaced() {
        var drawn = false
        rule.setContent {
            Layout(
                content = { AtLeastSize(30, modifier = Modifier.drawBehind { drawn = true }) }
            ) { _, _ ->
                // don't measure or place the AtLeastSize
                layout(20, 20) {}
            }
        }

        rule.runOnIdle { assertFalse(drawn) }
    }

    @Test
    fun parentSizeForDrawIsProvidedWithoutPadding() {
        var drawSize = Size.Zero
        rule.setContent {
            val drawnContent = Modifier.drawBehind { drawSize = size }
            AtLeastSize(100, Modifier.padding(10).then(drawnContent)) {}
        }
        rule.runOnIdle {
            assertEquals(100.0f, drawSize.width)
            assertEquals(100.0f, drawSize.height)
        }
    }

    @Test
    fun parentSizeForDrawInsideRepaintBoundaryIsProvidedWithoutPadding() {
        var drawSize = Size.Zero
        rule.setContent {
            AtLeastSize(100, Modifier.padding(10).graphicsLayer().drawBehind { drawSize = size }) {}
        }
        rule.runOnIdle {
            assertEquals(100.0f, drawSize.width)
            assertEquals(100.0f, drawSize.height)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_drawPositioning() {
        val outerColor = Color.Blue
        val innerColor = Color.White
        rule.setContent {
            FixedSize(30, Modifier.background(outerColor)) {
                FixedSize(10, Modifier.padding(10).background(innerColor))
            }
        }
        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 10)
    }

    @Test
    fun drawModifier_testLayoutDirection() {
        val layoutDirection = Ref<LayoutDirection>()
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                FixedSize(
                    size = 50,
                    modifier = Modifier.drawBehind { layoutDirection.value = this.layoutDirection },
                )
            }
        }

        rule.runOnIdle { assertEquals(LayoutDirection.Rtl, layoutDirection.value) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_modelChangesOnRoot() {
        val model = SquareModel(innerColor = Color.White, outerColor = Color.Green)
        rule.setContent {
            FixedSize(30, Modifier.background(model, false)) {
                FixedSize(10, Modifier.padding(10).background(model, true))
            }
        }
        validateSquareColors(outerColor = Color.Green, innerColor = Color.White, size = 10)
        rule.runOnUiThread { model.innerColor = Color.Yellow }
        validateSquareColors(outerColor = Color.Green, innerColor = Color.Yellow, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_modelChangesOnRepaintBoundary() {
        val model = SquareModel(innerColor = Color.White, outerColor = Color.Green)
        rule.setContent {
            FixedSize(30, Modifier.background(Color.Green)) {
                FixedSize(10, Modifier.graphicsLayer().padding(10).background(model, true))
            }
        }
        validateSquareColors(outerColor = Color.Green, innerColor = Color.White, size = 10)
        rule.runOnUiThread { model.innerColor = Color.Yellow }
        validateSquareColors(outerColor = Color.Green, innerColor = Color.Yellow, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_oneModifier() {
        val outerColor = Color.Blue
        val innerColor = Color.White
        rule.setContent {
            val colorModifier =
                Modifier.drawBehind {
                    drawRect(outerColor)
                    drawRect(innerColor, topLeft = Offset(10f, 10f), size = Size(10f, 10f))
                }
            FixedSize(30, colorModifier)
        }

        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_nestedModifiers() {
        val outerColor = Color.Blue
        val innerColor = Color.White
        rule.setContent {
            FixedSize(30, Modifier.background(color = outerColor)) {
                Padding(10) { FixedSize(10, Modifier.background(color = innerColor)) }
            }
        }
        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_withLayoutModifier() {
        val outerColor = Color.Blue
        val innerColor = Color.White
        rule.setContent {
            FixedSize(30, Modifier.background(color = outerColor)) {
                FixedSize(size = 10, modifier = Modifier.padding(10).background(color = innerColor))
            }
        }
        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_withLayout() {
        val outerColor = Color.Blue
        val innerColor = Color.White
        rule.runOnUiThread {
            activity.setContent {
                val drawAndOffset =
                    Modifier.drawWithContent {
                        drawRect(outerColor)
                        translate(10f, 10f) { this@drawWithContent.drawContent() }
                    }
                FixedSize(30, drawAndOffset) {
                    FixedSize(size = 10, modifier = AlignTopLeft.background(innerColor))
                }
            }
        }
        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 10)
    }

    @Test
    fun doubleDraw() {
        val offset = mutableStateOf(0)
        var innerDrawCount = 0
        var outerDrawCount = 0
        rule.setContent {
            FixedSize(30, Modifier.drawBehind { outerDrawCount++ }.graphicsLayer()) {
                FixedSize(
                    10,
                    Modifier.drawBehind {
                        drawLine(
                            Color.Blue,
                            Offset(offset.value.toFloat(), 0f),
                            Offset(0f, offset.value.toFloat()),
                            strokeWidth = Stroke.HairlineWidth,
                        )
                        innerDrawCount++
                    },
                )
            }
        }

        rule.runOnIdle {
            offset.value = 10
            innerDrawCount = 0
            outerDrawCount = 0
        }
        rule.runOnIdle {
            assertEquals(1, innerDrawCount)
            assertEquals(0, outerDrawCount)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun validateSquareColors(
        outerColor: Color,
        innerColor: Color,
        size: Int,
        offset: Int = 0,
        totalSize: Int = size * 3,
    ) {
        rule.validateSquareColors(
            outerColor = outerColor,
            innerColor = innerColor,
            size = size,
            offset = offset,
            totalSize = totalSize,
        )
    }

    private fun Modifier.fillColor(squareModel: SquareModel, isInner: Boolean): Modifier =
        drawBehind {
            drawRect(if (isInner) squareModel.innerColor else squareModel.outerColor)
        }

    private fun composeSquares(model: SquareModel) {
        rule.setContent {
            Padding(
                size = model.size,
                modifier = Modifier.drawBehind { drawRect(model.outerColor) },
            ) {
                AtLeastSize(
                    size = model.size,
                    modifier = Modifier.drawBehind { drawRect(model.innerColor) },
                )
            }
        }
    }

    private fun composeNestedSquares(model: SquareModel) {
        rule.setContent {
            val fillColorModifier = Modifier.drawBehind { drawRect(model.innerColor) }
            val innerDrawWithContentModifier =
                Modifier.drawWithContent {
                    drawRect(model.outerColor)
                    val start = model.size.toFloat()
                    val end = start * 2
                    clipRect(start, start, end, end) { this@drawWithContent.drawContent() }
                }
            AtLeastSize(size = (model.size * 3), modifier = innerDrawWithContentModifier) {
                AtLeastSize(size = (model.size * 3), modifier = fillColorModifier)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun AndroidComposeTestRule<*, TestActivity>.verifyRenderNode29CompositingStrategy(
        compositingStrategy: CompositingStrategy,
        expectedCompositing: Boolean,
        expectedOverlappingRendering: Boolean,
    ): Boolean {
        val node =
            RenderNodeApi29(
                    createAndroidComposeView(
                        Executors.newFixedThreadPool(3).asCoroutineDispatcher()
                    )
                )
                .apply { this.compositingStrategy = compositingStrategy }
        return expectedCompositing == node.isUsingCompositingLayer() &&
            expectedOverlappingRendering == node.hasOverlappingRendering()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun AndroidComposeTestRule<*, TestActivity>.verifyRenderNode23CompositingStrategy(
        compositingStrategy: CompositingStrategy,
        expectedLayerType: Int,
        expectedOverlappingRendering: Boolean,
    ): Boolean {
        val node =
            RenderNodeApi23(
                    createAndroidComposeView(
                        Executors.newFixedThreadPool(3).asCoroutineDispatcher()
                    )
                )
                .apply { this.compositingStrategy = compositingStrategy }
        return expectedLayerType == node.getLayerType() &&
            expectedOverlappingRendering == node.hasOverlappingRendering()
    }

    private fun AndroidComposeTestRule<*, TestActivity>.verifyViewLayerCompositingStrategy(
        compositingStrategy: CompositingStrategy,
        expectedLayerType: Int,
        expectedOverlappingRendering: Boolean,
    ): Boolean {
        val view =
            ViewLayer(
                    createAndroidComposeView(
                        Executors.newFixedThreadPool(3).asCoroutineDispatcher()
                    ),
                    ViewLayerContainer(activity),
                    { _, _ -> },
                    {},
                )
                .apply {
                    val scope = ReusableGraphicsLayerScope()
                    scope.cameraDistance = cameraDistance
                    scope.compositingStrategy = compositingStrategy
                    scope.layoutDirection = LayoutDirection.Ltr
                    scope.graphicsDensity = Density(1f)
                    updateLayerProperties(scope)
                }
        return expectedLayerType == view.layerType &&
            expectedOverlappingRendering == view.hasOverlappingRendering()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun modifier_combinedModifiers() {
        rule.setContent {
            FixedSize(30, Modifier.background(Color.Blue)) {
                JustConstraints(LayoutAndDrawModifier(Color.White)) {}
            }
        }
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.White, size = 10)
    }
}

@Composable
private fun JustConstraints(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {}
    }
}

private class LayoutAndDrawModifier(val color: Color) : LayoutModifier, DrawModifier {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(Constraints.fixed(10, 10))
        return layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeRelative(
                (constraints.maxWidth - placeable.width) / 2,
                (constraints.maxHeight - placeable.height) / 2,
            )
        }
    }

    override fun ContentDrawScope.draw() {
        drawRect(color)
    }
}
