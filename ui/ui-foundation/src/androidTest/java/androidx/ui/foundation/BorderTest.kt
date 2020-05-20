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
import androidx.ui.core.testTag
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.Shape
import androidx.ui.graphics.SolidColor
import androidx.ui.layout.Stack
import androidx.ui.layout.preferredSize
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.unit.Density
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(Parameterized::class)
class BorderTest(val shape: Shape) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initShapes(): Array<Any> = arrayOf(
            RectangleShape, CircleShape, RoundedCornerShape(5.0f)
        )
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    val testTag = "BorderParent"

    @Test
    fun border_color() {
        composeTestRule.setContent {
            SemanticParent {
                Stack(
                    Modifier.preferredSize(40.0f.toDp(), 40.0f.toDp())
                        .drawBackground(Color.Blue, shape)
                        .drawBorder(Border(10.0f.toDp(), Color.Red), shape)

                ) {}
            }
        }
        val bitmap = findByTag(testTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.Blue,
            shapeOverlapPixelCount = 3.0f
        )
    }

    @Test
    fun border_brush() {
        composeTestRule.setContent {
            SemanticParent {
                Stack(
                    Modifier.preferredSize(40.0f.toDp(), 40.0f.toDp())
                        .drawBackground(Color.Blue, shape)
                        .drawBorder(
                            Border(10.0f.toDp(), SolidColor(Color.Red)),
                            shape
                        )
                ) {}
            }
        }
        val bitmap = findByTag(testTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.Blue,
            shapeOverlapPixelCount = 3.0f
        )
    }

    @Test
    fun border_biggerThanLayout_fills() {
        composeTestRule.setContent {
            SemanticParent {
                Stack(
                    Modifier.preferredSize(40.0f.toDp(), 40.0f.toDp())
                        .drawBackground(Color.Blue, shape)
                        .drawBorder(Border(1500.0f.toDp(), Color.Red), shape)
                ) {}
            }
        }
        val bitmap = findByTag(testTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.White,
            shapeColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun border_lessThanZero_doesNothing() {
        composeTestRule.setContent {
            SemanticParent {
                Stack(
                    Modifier.preferredSize(40.0f.toDp(), 40.0f.toDp())
                        .drawBackground(Color.Blue, shape)
                        .drawBorder(Border(-5.0f.toDp(), Color.Red), shape)
                ) {}
            }
        }
        val bitmap = findByTag(testTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.White,
            shapeColor = Color.Blue,
            shape = shape,
            backgroundShape = shape,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun border_zeroSizeLayout_drawsNothing() {
        composeTestRule.setContent {
            SemanticParent {
                Box(
                    Modifier.preferredSize(40.0f.toDp(), 40.0f.toDp()),
                    backgroundColor = Color.White
                ) {
                    Stack(
                        Modifier.preferredSize(0.0f.toDp(), 40.0f.toDp())
                            .drawBorder(Border(4.0f.toDp(), Color.Red), shape)
                    ) {}
                }
            }
        }
        val bitmap = findByTag(testTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.White,
            shapeColor = Color.White,
            shape = RectangleShape,
            shapeOverlapPixelCount = 1.0f
        )
    }

    @Composable
    fun SemanticParent(children: @Composable Density.() -> Unit) {
        Stack {
            Box(modifier = Modifier.testTag(testTag)) {
                DensityAmbient.current.children()
            }
        }
    }
}