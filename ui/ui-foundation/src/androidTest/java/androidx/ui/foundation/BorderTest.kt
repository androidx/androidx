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

import androidx.ui.core.TestTag
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shape
import androidx.ui.graphics.SolidColor
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Stack
import androidx.ui.semantics.Semantics
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.unit.Density
import androidx.ui.unit.px
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
            RectangleShape, CircleShape, RoundedCornerShape(5.px)
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
                    modifier = LayoutSize(40.px.toDp(), 40.px.toDp()) +
                            DrawBackground(Color.Blue, shape) +
                            DrawBorder(Border(10.px.toDp(), Color.Red), shape)

                ) {}
            }
        }
        val bitmap = findByTag(testTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSizeX = 20.px,
            shapeSizeY = 20.px,
            shapeColor = Color.Blue,
            shapeOverlapPixelCount = 2.px
        )
    }

    @Test
    fun border_brush() {
        composeTestRule.setContent {
            SemanticParent {
                Stack(
                    modifier = LayoutSize(40.px.toDp(), 40.px.toDp()) +
                            DrawBackground(Color.Blue, shape) +
                            DrawBorder(Border(10.px.toDp(), SolidColor(Color.Red)), shape)
                ) {}
            }
        }
        val bitmap = findByTag(testTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSizeX = 20.px,
            shapeSizeY = 20.px,
            shapeColor = Color.Blue,
            shapeOverlapPixelCount = 2.px
        )
    }

    @Composable
    fun SemanticParent(children: @Composable Density.() -> Unit) {
        Stack {
            TestTag(tag = testTag) {
                Semantics(container = true) {
                    Container {
                        DensityAmbient.current.children()
                    }
                }
            }
        }
    }
}