/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.xr.glimmer

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.first
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.testutils.captureToImage
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class DepthEffectTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Before
    fun setup() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun tearDown() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun layer2IsDrawnOnTopOfLayer1() {
        val depthEffect =
            DepthEffect(
                layer1 = Shadow(radius = 50.dp, color = Color.Blue, spread = 50.dp),
                layer2 = Shadow(radius = 25.dp, color = Color.Red, spread = 25.dp),
            )
        rule.setGlimmerThemeContent {
            Box(
                Modifier.testTag("depthEffect")
                    .padding(100.dp)
                    .size(100.dp)
                    .depthEffect(depthEffect, RectangleShape)
            )
        }
        rule.onNodeWithTag("depthEffect").captureToImage().run {
            val map = toPixelMap()
            val center = map[width / 2, height / 2]
            val topMiddle = map[width / 2, height / 5]
            val middleLeft = map[width / 5, height / 2]
            // Center pixel should be red, as we draw layer2 (red) on top of layer1 (blue)
            assertThat(center).isEqualTo(Color.Red)
            // Outer pixels should be more blue than red, since layer1 has a larger radius and
            // spread
            assertThat(topMiddle.blue).isGreaterThan(topMiddle.red)
            assertThat(middleLeft.blue).isGreaterThan(middleLeft.red)
        }
    }

    @Test
    fun changeColor_redrawsShadow() {
        var layer2Color by mutableStateOf(Color.Red)
        rule.setGlimmerThemeContent {
            Box(
                Modifier.testTag("depthEffect")
                    .padding(100.dp)
                    .size(100.dp)
                    .depthEffect(
                        DepthEffect(
                            layer1 = Shadow(radius = 50.dp, color = Color.Blue, spread = 50.dp),
                            layer2 = Shadow(radius = 25.dp, color = layer2Color, spread = 25.dp),
                        ),
                        RectangleShape,
                    )
            )
        }
        rule.onNodeWithTag("depthEffect").captureToImage().run {
            val map = toPixelMap()
            val center = map[width / 2, height / 2]
            // Center pixel should be red, as we draw layer2 (red) on top of layer1 (blue)
            assertThat(center).isEqualTo(Color.Red)
        }

        rule.runOnIdle { layer2Color = Color.Green }

        rule.onNodeWithTag("depthEffect").captureToImage().run {
            val map = toPixelMap()
            val center = map[width / 2, height / 2]
            // Center pixel should be green, as layer2 has changed the color
            assertThat(center).isEqualTo(Color.Green)
        }
    }

    @Test
    fun changeShape_redrawsShadow() {
        var shape: Shape by mutableStateOf(RectangleShape)
        rule.setGlimmerThemeContent {
            Box(
                Modifier.testTag("depthEffect")
                    .padding(100.dp)
                    .size(100.dp)
                    .depthEffect(
                        DepthEffect(
                            layer1 = Shadow(radius = 50.dp, color = Color.Blue, spread = 50.dp),
                            layer2 = Shadow(radius = 25.dp, color = Color.Red, spread = 25.dp),
                        ),
                        shape,
                    )
            )
        }
        rule.onNodeWithTag("depthEffect").captureToImage().run {
            val map = toPixelMap()
            val topLeftCorner = map[width / 3, height / 3]
            // Top-left pixel should be more red than blue, as we draw layer2 on top of layer1
            assertThat(topLeftCorner.red).isGreaterThan(topLeftCorner.blue)
        }

        rule.runOnIdle {
            shape =
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
        }

        rule.onNodeWithTag("depthEffect").captureToImage().run {
            val map = toPixelMap()
            val topLeftCorner = map[width / 3, height / 3]
            // Top-left pixel should now be more blue than red because of the triangle shape
            assertThat(topLeftCorner.blue).isGreaterThan(topLeftCorner.red)
        }
    }

    @Test
    fun inspectorValue() {
        rule.runOnUiThread {
            val depthEffect = DepthEffect(Shadow(1.dp), Shadow(2.dp))
            val shape = RectangleShape
            val modifier = Modifier.depthEffect(depthEffect, shape).first() as InspectableValue

            assertThat(modifier.nameFallback).isEqualTo("depthEffect")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.asIterable())
                .containsExactly(
                    ValueElement("depthEffect", depthEffect),
                    ValueElement("shape", shape),
                )
        }
    }
}
