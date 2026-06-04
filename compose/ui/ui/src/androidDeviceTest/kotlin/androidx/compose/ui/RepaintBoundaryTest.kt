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
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RepaintBoundaryTest {
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
    fun recomposeNestedRepaintBoundariesColorChange() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        composeSquaresWithNestedRepaintBoundaries(model)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        val yellow = Color(0xFFFFFF00)
        rule.runOnIdle { model.innerColor = yellow }

        validateSquareColors(outerColor = blue, innerColor = yellow, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeNestedRepaintBoundariesSizeChange() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        composeSquaresWithNestedRepaintBoundaries(model)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)
        rule.runOnIdle { model.size = 20 }

        validateSquareColors(outerColor = blue, innerColor = white, size = 20)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeRepaintBoundariesMove() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        val offset = mutableStateOf(10)
        composeMovingSquaresWithRepaintBoundary(model, offset)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        rule.runOnIdle { offset.value = 20 }

        validateSquareColors(outerColor = blue, innerColor = white, offset = 10, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeMove() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        val offset = mutableStateOf(10)
        composeMovingSquares(model, offset)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        rule.runOnIdle { offset.value = 20 }

        validateSquareColors(outerColor = blue, innerColor = white, offset = 10, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeSizeTest() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        composeSquares(model)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        rule.runOnIdle { model.size = 20 }
        validateSquareColors(outerColor = blue, innerColor = white, size = 20)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleSquareColorAndSizeTest() {
        val green = Color(0xFF00FF00)
        val model = SquareModel(size = 20, outerColor = green, innerColor = green)

        rule.runOnUiThread {
            activity.setContent {
                Padding(
                    size = (model.size * 3),
                    modifier = Modifier.fillColor(model, isInner = false),
                ) {}
            }
        }
        validateSquareColors(outerColor = green, innerColor = green, size = 20)

        rule.runOnIdle { model.size = 30 }
        validateSquareColors(outerColor = green, innerColor = green, size = 30)

        val blue = Color(0xFF0000FF)

        rule.runOnIdle {
            model.innerColor = blue
            model.outerColor = blue
        }
        validateSquareColors(outerColor = blue, innerColor = blue, size = 30)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun moveRootLayoutRedrawsLeafRepaintBoundary() {
        val offset = mutableStateOf(0)
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    modifier = Modifier.fillColor(Color.Green),
                    content = {
                        AtLeastSize(size = 10) {
                            AtLeastSize(
                                size = 10,
                                modifier = Modifier.graphicsLayer().fillColor(Color.Cyan),
                            ) {}
                        }
                    },
                ) { measurables, constraints ->
                    layout(width = 20, height = 20) {
                        measurables.first().measure(constraints).place(offset.value, offset.value)
                    }
                }
            }
        }

        rule.waitForIdle()
        rule.onRoot().captureToImage().asAndroidBitmap().apply {
            assertRect(Color.Cyan, size = 10, centerX = 5, centerY = 5)
            assertRect(Color.Green, size = 10, centerX = 15, centerY = 15)
        }

        rule.runOnIdle { offset.value = 10 }

        rule.onRoot().captureToImage().asAndroidBitmap().apply {
            assertRect(Color.Green, size = 10, centerX = 5, centerY = 5)
            assertRect(Color.Cyan, size = 10, centerX = 15, centerY = 15)
        }
    }

    // When a LayoutNode is removed, but it contains a layout that is being updated, the
    // layout should not be remeasured.
    @Test
    fun disappearingLayoutNode() {
        var size by mutableStateOf(10f)
        var notShownCount = 0
        var measureCount = 0

        rule.setContent {
            Box(Modifier.background(Color.Red)) {
                val animatedSize by animateFloatAsState(size)
                if (animatedSize == 10f) {
                    Layout(modifier = Modifier.background(Color.Cyan), content = {}) { _, _ ->
                        @Suppress("KotlinConstantConditions")
                        if (animatedSize != 10f) {
                            measureCount++
                        }
                        val sizePx = animatedSize.roundToInt()
                        layout(sizePx, sizePx) {}
                    }
                } else {
                    notShownCount++
                }
            }
        }

        rule.runOnIdle {
            assertEquals(0, notShownCount)
            assertEquals(0, measureCount)
            size = 20f
        }

        rule.runOnIdle {
            assertEquals(0, measureCount)
            assertTrue(notShownCount > 0)
        }
    }

    @Test
    fun reattachingViewKeepsRootNodePlaced() {
        lateinit var container1: FrameLayout
        lateinit var container2: ComposeView

        var drawCount = 0

        rule.runOnUiThread {
            val activity = rule.activity
            container1 = FrameLayout(activity)
            container2 = ComposeView(activity)
            activity.setContentView(container1)
            container1.addView(container2)
            container2.setContent { FixedSize(10, Modifier.drawBehind { drawCount++ }) }
        }

        rule.runOnIdle {
            container1.removeView(container2)
            drawCount = 0
        }

        rule.runOnIdle {
            assertEquals(0, drawCount)
            container1.addView(container2)
        }

        // draw modifier will be redrawn if the root node is placed
        rule.runOnIdle { assertEquals(1, drawCount) }
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

    private fun Modifier.fillColor(color: Color): Modifier = drawBehind { drawRect(color) }

    private fun Modifier.fillColor(squareModel: SquareModel, isInner: Boolean): Modifier =
        drawBehind {
            drawRect(if (isInner) squareModel.innerColor else squareModel.outerColor)
        }

    private fun composeSquares(model: SquareModel) {
        rule.runOnUiThread {
            activity.setContent {
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
    }

    private fun composeSquaresWithNestedRepaintBoundaries(model: SquareModel) {
        rule.runOnUiThread {
            activity.setContent {
                Padding(
                    size = model.size,
                    modifier = Modifier.fillColor(model, isInner = false).graphicsLayer(),
                ) {
                    AtLeastSize(
                        size = model.size,
                        modifier = Modifier.graphicsLayer().fillColor(model, isInner = true),
                    ) {}
                }
            }
        }
    }

    private fun composeMovingSquaresWithRepaintBoundary(model: SquareModel, offset: State<Int>) {
        rule.runOnUiThread {
            activity.setContent {
                Position(
                    size = model.size * 3,
                    offset = offset,
                    modifier = Modifier.fillColor(model, isInner = false),
                ) {
                    AtLeastSize(
                        size = model.size,
                        modifier = Modifier.graphicsLayer().fillColor(model, isInner = true),
                    ) {}
                }
            }
        }
    }

    private fun composeMovingSquares(model: SquareModel, offset: State<Int>) {
        rule.runOnUiThread {
            activity.setContent {
                Position(
                    size = model.size * 3,
                    offset = offset,
                    modifier = Modifier.fillColor(model, isInner = false),
                ) {
                    AtLeastSize(
                        size = model.size,
                        modifier = Modifier.fillColor(model, isInner = true),
                    ) {}
                }
            }
        }
    }

    @Composable
    private fun Position(
        size: Int,
        offset: State<Int>,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Layout(modifier = modifier, content = content) { measurables, constraints ->
            val placeables = measurables.map { m -> m.measure(constraints) }
            layout(size, size) {
                placeables.forEach { child -> child.place(offset.value, offset.value) }
            }
        }
    }
}
