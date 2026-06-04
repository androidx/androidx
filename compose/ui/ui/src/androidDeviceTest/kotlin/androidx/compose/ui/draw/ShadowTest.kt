/*
 * Copyright 2019 The Android Open Source Project
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

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.first
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ShadowTest {

    @get:Rule val rule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())
    private lateinit var activity: TestActivity

    private val rectShape =
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density,
            ) = Outline.Rectangle(size.toRect())
        }

    @Before
    fun setup() {
        activity = rule.activity
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun tearDown() {
        isDebugInspectorInfoEnabled = false
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shadowDrawn() {
        rule.setContent { ShadowContainer() }

        takeScreenShot(12).apply { hasShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shadowDrawnInsideRenderNode() {
        rule.setContent { ShadowContainer(modifier = Modifier.graphicsLayer()) }

        takeScreenShot(12).apply { hasShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromShadowToNoShadow() {
        val elevation = mutableStateOf(10.dp)
        rule.setContent { ShadowContainer(elevation = elevation) }
        takeScreenShot(12).apply { hasShadow() }
        rule.runOnUiThread { elevation.value = 0.dp }

        takeScreenShot(12).apply { hasNoShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switchFromNoShadowToShadowWithNestedRepaintBoundaries() {
        val elevation = mutableStateOf(0.dp)

        rule.setContent {
            ShadowContainer(modifier = Modifier.graphicsLayer(clip = true), elevation)
        }

        rule.runOnIdle { elevation.value = 12.dp }

        takeScreenShot(12).apply { hasShadow() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun opacityAppliedForTheShadow() {
        rule.setContent {
            AtLeastSize(size = 12, modifier = Modifier.background(Color.White)) {
                val elevation = with(LocalDensity.current) { 4.dp.toPx() }
                AtLeastSize(
                    size = 10,
                    modifier =
                        Modifier.graphicsLayer(
                            shadowElevation = elevation,
                            shape = rectShape,
                            alpha = 0.5f,
                        ),
                ) {}
            }
        }
        takeScreenShot(12).apply {
            val shadowColor = color(width / 2, height - 1)
            // assert the shadow is still visible
            assertNotEquals(shadowColor, Color.White)
            // but the shadow is not as dark as it would be without opacity.
            // Full opacity depends on the device, but is around 0.8 luminance.
            // At 50%, the luminance is over 0.9
            assertTrue(shadowColor.luminance() > 0.9f)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun colorsAppliedForTheShadow() {
        rule.setContent {
            AtLeastSize(size = 12, modifier = Modifier.background(Color.White)) {
                val elevation = with(LocalDensity.current) { 4.dp.toPx() }
                AtLeastSize(
                    size = 10,
                    modifier =
                        Modifier.graphicsLayer(
                            shadowElevation = elevation,
                            shape = rectShape,
                            ambientShadowColor = Color(0xFFFF00FF),
                            spotShadowColor = Color(0xFFFF00FF),
                        ),
                ) {}
            }
        }
        takeScreenShot(12).apply {
            val shadowColor = color(width / 2, height - 1)
            // assert the shadow is still visible
            assertNotEquals(shadowColor, Color.White)
            // The shadow should have a magenta hue
            assertTrue(shadowColor.red > shadowColor.green)
            assertTrue(shadowColor.blue > shadowColor.green)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun emitShadowLater() {
        val model = mutableStateOf(false)

        rule.setContent {
            AtLeastSize(size = 12, modifier = Modifier.background(Color.White)) {
                val shadow =
                    if (model.value) {
                        Modifier.shadow(8.dp, rectShape)
                    } else {
                        Modifier
                    }
                AtLeastSize(size = 10, modifier = shadow) {}
            }
        }

        rule.runOnIdle { model.value = true }

        takeScreenShot(12).apply { hasShadow() }
    }

    @Test
    fun testInspectorValue() {
        rule.runOnUiThread {
            val modifier = Modifier.shadow(4.0.dp).first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("shadow")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.asIterable())
                .containsExactly(
                    ValueElement("elevation", 4.0.dp),
                    ValueElement("shape", RectangleShape),
                    ValueElement("clip", true),
                    ValueElement("ambientColor", DefaultShadowColor),
                    ValueElement("spotColor", DefaultShadowColor),
                )
        }
    }

    @Test
    fun elevationWithinModifier() {
        val elevation = mutableStateOf(0f)
        val color = mutableStateOf(Color.Blue)
        val underColor = mutableStateOf(Color.Transparent)
        var drawCount = 0
        var elevationReadCount = 0
        val modifier =
            Modifier.graphicsLayer()
                .background(underColor)
                .drawBehind { drawCount++ }
                .graphicsLayer {
                    shadowElevation = elevation.value
                    elevationReadCount++
                }
                .background(color)

        rule.setContent { androidx.compose.ui.FixedSize(30, modifier) }

        rule.runOnIdle {
            elevationReadCount = 0
            drawCount = 0
            elevation.value = 1f
        }

        rule.runOnIdle {
            assertEquals(1, elevationReadCount)
            assertEquals(1, drawCount)
            elevation.value = 2f // elevation was already 1, so it doesn't need to enableZ again
        }

        rule.runOnIdle {
            assertEquals(1, drawCount)
            assertEquals(2, elevationReadCount)
            elevation.value = 0f // going to 0 doesn't trigger invalidation
        }

        rule.runOnIdle {
            assertEquals(1, drawCount)
            assertEquals(3, elevationReadCount)
            elevation.value = 1f // going to 1 won't invalidate because it was last drawn with Z
        }

        rule.runOnIdle {
            assertEquals(1, drawCount)
            assertEquals(4, elevationReadCount)

            elevation.value = 0f
            underColor.value = Color.Black
        }
    }

    @Composable
    private fun ShadowContainer(
        modifier: Modifier = Modifier,
        elevation: State<Dp> = mutableStateOf(8.dp),
    ) {
        AtLeastSize(size = 12, modifier = modifier.background(Color.White)) {
            AtLeastSize(
                size = 10,
                modifier = Modifier.shadow(elevation = elevation.value, shape = rectShape),
            ) {}
        }
    }

    private fun Bitmap.hasNoShadow() {
        assertColorsEqualWithTolerance(Color.White, color(width / 2, height - 1))
    }

    private fun Bitmap.hasShadow() {
        assertNotEquals(color(width / 2, height - 1), Color.White)
    }

    private fun Modifier.background(color: State<Color>) = drawBehind {
        if (color.value != Color.Transparent) {
            drawRect(color.value)
        }
    }

    // waitAndScreenShot() requires API level 26
    @RequiresApi(Build.VERSION_CODES.O)
    private fun takeScreenShot(width: Int, height: Int = width): Bitmap {
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
        return bitmap
    }

    private fun assertColorsEqualWithTolerance(
        expected: Color,
        actual: Color,
        tolerance: Float = 0.02f,
    ) {
        assertEquals(expected.red, actual.red, tolerance)
        assertEquals(expected.green, actual.green, tolerance)
        assertEquals(expected.blue, actual.blue, tolerance)
        assertEquals(expected.alpha, actual.alpha, tolerance)
    }
}
