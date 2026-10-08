/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Common tests for [border] that are run against every type of [Outline]. Non-parameterized tests
 * for specific use cases should go in [AdvancedBorderTest] instead.
 */
@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(Parameterized::class)
class BorderTest(val shape: Shape, val isNewBorderImplementationEnabled: Boolean) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "shape={0}, isNewBorderImplementationEnabled={1}")
        fun initParameters(): Array<Any> {
            val shapes =
                arrayOf(
                    // Outline.Rectangle
                    namedShape("Rectangle", RectangleShape),
                    // Outline.Rounded, simple (all corner radii equal)
                    namedShape("Circle", CircleShape),
                    // Outline.Rounded, simple (all corner radii equal)
                    namedShape("Rounded", RoundedCornerShape(5.0f)),
                    // Outline.Rounded, non-simple (different corner radii)
                    namedShape(
                        "NonSimpleRounded",
                        RoundedCornerShape(
                            topStartPercent = 5,
                            topEndPercent = 10,
                            bottomEndPercent = 15,
                            bottomStartPercent = 20,
                        ),
                    ),
                    // Outline.Generic
                    namedShape(
                        "Generic",
                        GenericShape { size, _ -> addRect(Rect(Offset.Zero, size)) },
                    ),
                )
            val flags = arrayOf(true, false)
            val result = mutableListOf<Array<Any>>()
            for (shape in shapes) {
                for (flag in flags) {
                    result.add(arrayOf(shape, flag))
                }
            }
            return result.toTypedArray()
        }

        private fun namedShape(name: String, shape: Shape): Shape =
            object : Shape by shape {
                override fun toString(): String = name
            }
    }

    private var previousFlagValue: Boolean? = null

    @get:Rule val rule = createComposeRule()

    @Before
    fun setUpFlag() {
        previousFlagValue = ComposeFoundationFlags.isNewBorderImplementationEnabled
        ComposeFoundationFlags.isNewBorderImplementationEnabled = isNewBorderImplementationEnabled
    }

    @After
    fun tearDownFlag() {
        previousFlagValue?.let {
            ComposeFoundationFlags.isNewBorderImplementationEnabled = it
        }
    }

    val testTag = "BorderParent"

    private val rtlAwareShape =
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density,
            ) =
                if (layoutDirection == LayoutDirection.Ltr) {
                    Outline.Rectangle(Rect(0f, 1f, 0f, 1f))
                } else {
                    shape.createOutline(size, layoutDirection, density)
                }
        }

    @Test
    fun border_color() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(10.0f.toDp(), Color.Red), shape)
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSize = Size(20f, 20f),
            shapeColor = Color.Blue,
            antiAliasingGap = 3.0f,
        )
    }

    @Test
    fun border_brush() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(10.0f.toDp(), SolidColor(Color.Red)), shape)
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSize = Size(20f, 20f),
            shapeColor = Color.Blue,
            antiAliasingGap = 3.0f,
        )
    }

    // RuntimeShader not available below Tiramisu
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun border_shaderBrush() {
        // Not supported for generic shapes with the old border
        Assume.assumeTrue(ComposeFoundationFlags.isNewBorderImplementationEnabled)
        // Shader that just draws red
        val brush =
            ShaderBrush(
                RuntimeShader("half4 main(float2 coord) { return half4(1.0, 0.0, 0.0, 1.0); }")
            )
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(10.0f.toDp(), brush), shape)
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSize = Size(20f, 20f),
            shapeColor = Color.Blue,
            antiAliasingGap = 3.0f,
        )
    }

    @Test
    fun border_biggerThanLayout_fills() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(1500.0f.toDp(), Color.Red), shape)
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.White,
            shapeColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            antiAliasingGap = 2.0f,
        )
    }

    @Test
    fun border_lessThanZero_doesNothing() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(-5.0f.toDp(), Color.Red), shape)
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.White,
            shapeColor = Color.Blue,
            shape = shape,
            backgroundShape = shape,
            antiAliasingGap = 2.0f,
        )
    }

    @Test
    fun border_zeroSizeLayout_drawsNothing() {
        rule.setContent {
            SemanticParent {
                Box(Modifier.size(40.0f.toDp(), 40.0f.toDp()).background(Color.White)) {
                    Box(
                        Modifier.size(0.0f.toDp(), 40.0f.toDp())
                            .border(BorderStroke(4.0f.toDp(), Color.Red), shape)
                    ) {}
                }
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.White,
            shapeColor = Color.White,
            shape = RectangleShape,
            antiAliasingGap = 1.0f,
        )
    }

    @Test
    fun border_rtl_initially() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(10.0f.toDp(), Color.Red), rtlAwareShape)
                ) {}
            }
        }
        rule
            .onNodeWithTag(testTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.Red,
                shape = shape,
                backgroundShape = shape,
                shapeSize = Size(20f, 20f),
                shapeColor = Color.Blue,
                antiAliasingGap = 3.0f,
            )
    }

    @Test
    fun border_rtl_after_switch() {
        val direction = mutableStateOf(LayoutDirection.Ltr)
        rule.setContent {
            SemanticParent {
                CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                    Box(
                        Modifier.size(40.0f.toDp(), 40.0f.toDp())
                            .background(color = Color.Blue)
                            .border(BorderStroke(10.0f.toDp(), Color.Red), rtlAwareShape)
                    ) {}
                }
            }
        }

        rule.runOnIdle { direction.value = LayoutDirection.Rtl }
        rule
            .onNodeWithTag(testTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.Red,
                shape = shape,
                backgroundShape = shape,
                shapeSize = Size(20f, 20f),
                shapeColor = Color.Blue,
                antiAliasingGap = 3.0f,
            )
    }

    @Composable
    fun SemanticParent(content: @Composable Density.() -> Unit) {
        Box { Box(modifier = Modifier.testTag(testTag)) { LocalDensity.current.content() } }
    }
}
