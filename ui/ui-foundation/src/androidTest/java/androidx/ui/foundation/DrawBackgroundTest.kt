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

package androidx.ui.foundation

import android.os.Build
import androidx.compose.Composable
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.SolidColor
import androidx.ui.layout.Stack
import androidx.ui.layout.preferredSize
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.unit.Density
import androidx.ui.unit.px
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(JUnit4::class)
class DrawBackgroundTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val contentTag = "Content"

    @Test
    fun background_colorRect() {
        composeTestRule.setContent {
            SemanticParent {
                Box(
                    Modifier.preferredSize(40.px.toDp()).drawBackground(Color.Magenta),
                    gravity = ContentGravity.Center
                ) {
                    Box(Modifier.preferredSize(20.px.toDp()).drawBackground(Color.White))
                }
            }
        }
        val bitmap = findByTag(contentTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Magenta,
            shape = RectangleShape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.White
        )
    }

    @Test
    fun background_brushRect() {
        composeTestRule.setContent {
            SemanticParent {
                Box(
                    Modifier.preferredSize(40.px.toDp()).drawBackground(Color.Magenta),
                    gravity = ContentGravity.Center
                ) {
                    Box(
                        Modifier.preferredSize(20.px.toDp())
                            .drawBackground(SolidColor(Color.White))
                    )
                }
            }
        }
        val bitmap = findByTag(contentTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Magenta,
            shape = RectangleShape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.White
        )
    }

    @Test
    fun background_colorCircle() {
        composeTestRule.setContent {
            SemanticParent {
                Box(
                    Modifier.preferredSize(40.px.toDp())
                        .drawBackground(Color.Magenta)
                        .drawBackground(color = Color.White, shape = CircleShape)
                )
            }
        }
        val bitmap = findByTag(contentTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Magenta,
            shape = CircleShape,
            shapeColor = Color.White,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun background_brushCircle() {
        composeTestRule.setContent {
            SemanticParent {
                Box(
                    Modifier.preferredSize(40.px.toDp())
                        .drawBackground(Color.Magenta)
                        .drawBackground(
                            brush = SolidColor(Color.White),
                            shape = CircleShape
                        )
                )
            }
        }
        val bitmap = findByTag(contentTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Magenta,
            shape = CircleShape,
            shapeColor = Color.White,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Composable
    private fun SemanticParent(children: @Composable Density.() -> Unit) {
        Stack(Modifier.testTag(contentTag)) {
            DensityAmbient.current.children()
        }
    }
}