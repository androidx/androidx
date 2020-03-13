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

package androidx.ui.foundation

import android.os.Build
import androidx.compose.Composable
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.TestTag
import androidx.ui.core.onPositioned
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Stack
import androidx.ui.semantics.Semantics
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.test.setContentAndCollectSizes
import androidx.ui.unit.Density
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.center
import androidx.ui.unit.dp
import androidx.ui.unit.px
import androidx.ui.unit.toOffset
import androidx.ui.unit.toRect
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(JUnit4::class)
class CanvasTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    val testTag = "CanvasParent"

    @Test
    fun canvas_noSize_emptyCanvas() {
        composeTestRule.setContentAndCollectSizes {
            Canvas(modifier = Modifier.None) {
                drawRect(size.toRect(), Paint())
            }
        }
            .assertHeightEqualsTo(0.dp)
            .assertWidthEqualsTo(0.dp)
    }

    @Test
    fun canvas_exactSizes() {
        var canvasSize: IntPxSize? = null
        composeTestRule.setContentAndCollectSizes {
            SemanticParent {
                Canvas(modifier = LayoutSize(100.dp) +
                        onPositioned { position -> canvasSize = position.size }) {
                    drawRect(size.toRect(), Paint().apply { color = Color.Red })
                }
            }
        }

        with(composeTestRule.density) {
            Truth.assertThat(canvasSize!!.width.value).isEqualTo(100.dp.toIntPx().value)
            Truth.assertThat(canvasSize!!.height.value).isEqualTo(100.dp.toIntPx().value)
        }

        val bitmap = findByTag(testTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Red,
            shapeColor = Color.Red,
            shape = RectangleShape
        )
    }

    @Test
    fun canvas_exactSizes_drawCircle() {
        var canvasSize: IntPxSize? = null
        composeTestRule.setContentAndCollectSizes {
            SemanticParent {
                Canvas(modifier = LayoutSize(100.dp) +
                        onPositioned { position -> canvasSize = position.size }) {
                    drawRect(size.toRect(), Paint().apply { color = Color.Red })
                    drawCircle(
                        size.center().toOffset(),
                        10f,
                        Paint().apply { color = Color.Blue })
                }
            }
        }

        with(composeTestRule.density) {
            Truth.assertThat(canvasSize!!.width.value).isEqualTo(100.dp.toIntPx().value)
            Truth.assertThat(canvasSize!!.height.value).isEqualTo(100.dp.toIntPx().value)
        }

        val bitmap = findByTag(testTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Red,
            shapeColor = Color.Blue,
            shape = CircleShape,
            shapeSizeX = 20.px,
            shapeSizeY = 20.px,
            shapeOverlapPixelCount = 2.px
        )
    }

    @Composable
    fun SemanticParent(children: @Composable Density.() -> Unit) {
        Stack {
            TestTag(tag = testTag) {
                Semantics(container = true) {
                    Box {
                        DensityAmbient.current.children()
                    }
                }
            }
        }
    }
}