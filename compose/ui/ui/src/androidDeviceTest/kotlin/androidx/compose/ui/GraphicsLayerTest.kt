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

import android.content.Context
import android.os.Build
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.RenderNodeApi23
import androidx.compose.ui.platform.RenderNodeApi29
import androidx.compose.ui.platform.ViewLayer
import androidx.compose.ui.platform.ViewLayerContainer
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class GraphicsLayerTest {
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

    @Test
    fun testLayerCameraDistance() {
        val targetCameraDistance = 15f

        var cameraDistanceApplied = false
        activity.runOnUiThread {
            // Verify that the camera distance parameters are consumed properly across API levels.
            // camera distance on the View API assumes Dp however, the compose API consumes pixels
            // Additionally RenderNode consumed the negative value of the camera distance.
            // Ensure that each implementation of camera distance consumes positive pixel values
            // properly. Layer implementations backed by View should be compatible on all
            // API versions
            cameraDistanceApplied =
                when (Build.VERSION.SDK_INT) {
                    // Use public RenderNode API
                    in Build.VERSION_CODES.Q..Int.MAX_VALUE ->
                        rule.verifyRenderNode29CameraDistance(targetCameraDistance) &&
                            rule.verifyViewLayerCameraDistance(targetCameraDistance)
                    // Cannot access private APIs on P
                    Build.VERSION_CODES.P ->
                        rule.verifyViewLayerCameraDistance(targetCameraDistance)
                    // Use stub access to framework RenderNode API
                    in Build.VERSION_CODES.M..Int.MAX_VALUE ->
                        rule.verifyRenderNode23CameraDistance(targetCameraDistance) &&
                            rule.verifyViewLayerCameraDistance(targetCameraDistance)
                    // No RenderNodes, use Views instead
                    else -> rule.verifyViewLayerCameraDistance(targetCameraDistance)
                }
        }
        rule.runOnIdle { assertTrue(cameraDistanceApplied) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun layerModifier_scaleDraw() {
        rule.setContent {
            FixedSize(size = 30, modifier = Modifier.background(Color.Blue)) {
                FixedSize(
                    size = 20,
                    modifier = AlignTopLeft.padding(5).scale(0.5f).background(Color.Red),
                ) {}
            }
        }
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.Red, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun layerModifier_scaleChange() {
        val scale = mutableStateOf(1f)
        val layerModifier =
            Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
        rule.setContent {
            FixedSize(size = 30, modifier = Modifier.background(Color.Blue)) {
                FixedSize(
                    size = 10,
                    modifier = Modifier.padding(10).then(layerModifier).background(Color.Red),
                ) {}
            }
        }
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.Red, size = 10)

        rule.runOnIdle { scale.value = 2f }

        rule.onRoot().captureToImage().asAndroidBitmap().apply {
            assertRect(Color.Red, size = 20, centerX = 15, centerY = 15)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun layerModifier_noClip() {
        val triangleShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ) =
                    Outline.Generic(
                        Path().apply {
                            moveTo(size.width / 2f, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                    )
            }
        rule.setContent {
            FixedSize(size = 30) {
                FixedSize(
                    size = 10,
                    modifier =
                        Modifier.padding(10)
                            .graphicsLayer(shape = triangleShape)
                            .drawBehind {
                                drawRect(
                                    Color.Blue,
                                    topLeft = Offset(-10f, -10f),
                                    size = Size(30.0f, 30.0f),
                                )
                            }
                            .background(Color.Red),
                ) {}
            }
        }
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.Red, size = 10)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testInvalidationMultipleLayers() {
        val innerColor = mutableStateOf(Color.Red)
        rule.setContent {
            val content: @Composable () -> Unit = remember {
                @Composable {
                    FixedSize(
                        size = 10,
                        modifier = Modifier.graphicsLayer().padding(10).background(innerColor.value),
                    ) {}
                }
            }
            FixedSize(size = 30, modifier = Modifier.graphicsLayer().background(Color.Blue)) {
                FixedSize(size = 30, modifier = Modifier.graphicsLayer(), content = content)
            }
        }
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.Red, size = 10)

        rule.runOnIdle { innerColor.value = Color.White }

        validateSquareColors(outerColor = Color.Blue, innerColor = Color.White, size = 10)
    }

    @Test
    fun detachChildWithLayer() {
        rule.setContent { FixedSize(10, Modifier.graphicsLayer()) { FixedSize(8) } }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun drawOnLayerMove() {
        val offset = mutableStateOf(10)
        rule.setContent {
            val yellowSquare =
                @Composable { FixedSize(10, Modifier.graphicsLayer().background(Color.Yellow)) {} }
            Layout(modifier = Modifier.background(Color.Red), content = yellowSquare) {
                measurables,
                _ ->
                val childConstraints = Constraints.fixed(10, 10)
                val p = measurables[0].measure(childConstraints)
                layout(30, 30) { p.place(offset.value, offset.value) }
            }
        }

        validateSquareColors(outerColor = Color.Red, innerColor = Color.Yellow, size = 10)

        rule.runOnIdle { offset.value = 5 }

        rule.waitForIdle()

        rule.onRoot().captureToImage().asAndroidBitmap().apply {
            // just test that it is red around the Yellow
            assertRect(Color.Red, size = 20, centerX = 10, centerY = 10, holeSize = 10)
            // now test that it is red in the lower-right
            assertRect(Color.Red, size = 10, centerX = 25, centerY = 25)
            assertRect(Color.Yellow, size = 10, centerX = 10, centerY = 10)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun drawOnLayerPropertyChange() {
        val offset = mutableStateOf(0f)
        rule.setContent {
            FixedSize(30, Modifier.background(Color.Red)) {
                FixedSize(
                    10,
                    Modifier.padding(10)
                        .graphicsLayer {
                            translationX = offset.value
                            translationY = offset.value
                        }
                        .background(Color.Yellow),
                ) {}
            }
        }

        validateSquareColors(outerColor = Color.Red, innerColor = Color.Yellow, size = 10)

        // Wait until the translation affects the screenshot. Give it 4 frames
        rule.runOnUiThread {
            activity.window.decorView.postOnAnimation(
                object : Runnable {
                    override fun run() {
                        activity.window.decorView.postOnAnimation(this)
                    }
                }
            )
            offset.value = -5f
        }

        rule.waitForIdle()

        rule.onRoot().captureToImage().asAndroidBitmap().apply {
            // just test that it is red around the Yellow
            assertRect(Color.Red, size = 20, centerX = 10, centerY = 10, holeSize = 10)
            // now test that it is red in the lower-right
            assertRect(Color.Red, size = 10, centerX = 25, centerY = 25)
            assertRect(Color.Yellow, size = 10, centerX = 10, centerY = 10)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun invalidateParentLayer() {
        var color by mutableStateOf(Color.Red)
        rule.setContent {
            FixedSize(
                size = 10,
                modifier =
                    Modifier.background(color = color)
                        .then(Modifier.padding(10).graphicsLayer().background(Color.White)),
            )
        }

        validateSquareColors(outerColor = Color.Red, innerColor = Color.White, size = 10)
        color = Color.Blue
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.White, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun invalidateParentLayerZIndex() {
        var zIndex by mutableStateOf(0f)
        rule.setContent {
            with(LocalDensity.current) {
                FixedSize(size = 30, modifier = Modifier.background(color = Color.Blue)) {
                    FixedSize(
                        size = 10,
                        modifier =
                            Modifier.graphicsLayer()
                                .zIndex(zIndex)
                                .padding(10.toDp())
                                .background(Color.White),
                    )
                    FixedSize(
                        size = 10,
                        modifier =
                            Modifier.graphicsLayer()
                                .zIndex(0f)
                                .padding(10.toDp())
                                .background(Color.Yellow),
                    )
                }
            }
        }

        validateSquareColors(outerColor = Color.Blue, innerColor = Color.Yellow, size = 10)
        zIndex = 1f
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.White, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun changedLayerChild() {
        var showInner by mutableStateOf(true)
        rule.setContent {
            FixedSize(
                size = 10,
                modifier =
                    Modifier.background(Color.Blue)
                        .padding(10)
                        .graphicsLayer()
                        .then(if (showInner) Modifier.background(Color.White) else Modifier),
            )
        }
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.White, size = 10)
        showInner = false
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.Blue, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun layoutUsesPlaceWithLayer() {
        val yellow = Color(0xFFFFFF00)
        val red = Color(0xFF800000)

        rule.setContent {
            Layout(
                content = {
                    AtLeastSize(size = 10, modifier = Modifier.drawBehind { drawRect(red) })
                },
                modifier = Modifier.drawBehind { drawRect(yellow) },
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(30, 30) { placeable.placeWithLayer(10, 10) }
            }
        }

        validateSquareColors(outerColor = yellow, innerColor = red, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun layoutUsesPlaceWithLayerWithScale() {
        val yellow = Color(0xFFFFFF00)
        val red = Color(0xFF800000)

        rule.setContent {
            Layout(
                content = {
                    AtLeastSize(size = 20, modifier = Modifier.drawBehind { drawRect(red) })
                },
                modifier = Modifier.drawBehind { drawRect(yellow) },
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(30, 30) {
                    placeable.placeWithLayer(5, 5) {
                        scaleX = 0.5f
                        scaleY = 0.5f
                    }
                }
            }
        }

        validateSquareColors(outerColor = yellow, innerColor = red, size = 10)
    }

    @Test
    fun layoutMovesPlacedWithLayerChild_noInvalidations() {
        var parentInvalidationCount = 0
        var childInvalidationCount = 0
        var offset by mutableStateOf(0)

        rule.setContent {
            Layout(
                content = {
                    AtLeastSize(
                        size = 20,
                        modifier = Modifier.drawBehind { childInvalidationCount++ },
                    )
                },
                modifier =
                    Modifier.drawWithContent {
                        drawContent()
                        parentInvalidationCount++
                    },
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(30, 30) { placeable.placeWithLayer(offset, offset) }
            }
        }

        rule.waitForIdle()
        assertEquals(1, parentInvalidationCount)
        assertEquals(1, childInvalidationCount)

        rule.waitForIdle()
        offset = 10

        rule.waitForIdle()
        assertEquals(1, parentInvalidationCount)
        assertEquals(1, childInvalidationCount)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun invalidateDescendants() {
        var color = Color.White
        rule.setContent {
            FixedSize(30, Modifier.background(Color.Blue)) {
                FixedSize(30, Modifier.graphicsLayer()) {
                    with(LocalDensity.current) {
                        Canvas(Modifier.requiredSize(10.toDp())) { drawRect(color) }
                    }
                }
            }
        }

        validateSquareColors(outerColor = Color.Blue, innerColor = Color.White, size = 10)

        color = Color.Yellow

        rule.runOnIdle {
            val view = rule.findAndroidComposeView() as AndroidComposeView
            view.invalidateDescendants()
        }
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.Yellow, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawnInCorrectLayer() {
        var outerColor by mutableStateOf(Color.Blue)
        var innerColor by mutableStateOf(Color.White)
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(30.toDp())
                        .drawBehind { drawRect(outerColor) }
                        .padding(10.toDp())
                        .clipToBounds()
                        .drawBehind {
                            // clipped by the layer
                            drawRect(innerColor, Offset(-10f, -10f), Size(30f, 30f))
                        }
                        .size(10.toDp())
                )
            }
        }

        validateSquareColors(outerColor = Color.Blue, innerColor = Color.White, size = 10)

        // changing the inner color should only affect the inner layer
        innerColor = Color.Yellow

        validateSquareColors(outerColor = Color.Blue, innerColor = Color.Yellow, size = 10)

        // changing the outer color should only affect the outer layer
        outerColor = Color.Red

        validateSquareColors(outerColor = Color.Red, innerColor = Color.Yellow, size = 10)
    }

    @Test
    fun attachingLayerDoesNotCauseRelayout() {
        lateinit var root: RequestLayoutTrackingFrameLayout
        lateinit var composeView: ComposeView
        var showLayer by mutableStateOf(false)

        rule.runOnUiThread {
            root = RequestLayoutTrackingFrameLayout(activity)
            composeView = ComposeView(activity)

            activity.setContentView(root)
            root.addView(composeView)
            composeView.setContent {
                val modifier = if (showLayer) Modifier.graphicsLayer() else Modifier
                Box(modifier)
            }
        }

        rule.runOnIdle {
            Truth.assertThat(root.requestLayoutCalled).isTrue()
            root.requestLayoutCalled = false
            showLayer = true
        }

        rule.runOnIdle { Truth.assertThat(root.requestLayoutCalled).isFalse() }
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun AndroidComposeTestRule<*, TestActivity>.verifyRenderNode29CameraDistance(
        cameraDistance: Float
    ): Boolean =
        RenderNodeApi29(
                createAndroidComposeView(Executors.newFixedThreadPool(3).asCoroutineDispatcher())
            )
            .apply { this.cameraDistance = cameraDistance }
            .dumpRenderNodeData()
            .cameraDistance == cameraDistance

    @RequiresApi(Build.VERSION_CODES.M)
    private fun AndroidComposeTestRule<*, TestActivity>.verifyRenderNode23CameraDistance(
        cameraDistance: Float
    ): Boolean =
        RenderNodeApi23(
                createAndroidComposeView(Executors.newFixedThreadPool(3).asCoroutineDispatcher())
            )
            .apply { this.cameraDistance = cameraDistance }
            .dumpRenderNodeData()
            .cameraDistance == -cameraDistance

    private fun AndroidComposeTestRule<*, TestActivity>.verifyViewLayerCameraDistance(
        cameraDistance: Float
    ): Boolean {
        val layer =
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
                    scope.layoutDirection = LayoutDirection.Ltr
                    scope.graphicsDensity = Density(1f)
                    updateLayerProperties(scope)
                }
        return layer.cameraDistance == cameraDistance * layer.resources.displayMetrics.densityDpi
    }
}

private class RequestLayoutTrackingFrameLayout(context: Context) : FrameLayout(context) {
    var requestLayoutCalled = false

    override fun requestLayout() {
        super.requestLayout()
        requestLayoutCalled = true
    }
}
