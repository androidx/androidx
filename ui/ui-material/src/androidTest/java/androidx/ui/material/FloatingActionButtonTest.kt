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

package androidx.ui.material

import android.os.Build
import androidx.compose.Providers
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.boundsInRoot
import androidx.ui.core.onPositioned
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.foundation.shape.corner.CutCornerShape
import androidx.ui.graphics.Color
import androidx.ui.layout.Stack
import androidx.ui.layout.preferredSize
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite
import androidx.ui.test.assertIsEnabled
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import androidx.ui.test.runOnIdleCompose
import androidx.ui.unit.center
import androidx.ui.unit.dp
import androidx.ui.unit.height
import androidx.ui.unit.toPx
import androidx.ui.unit.width
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.roundToInt

@MediumTest
@RunWith(JUnit4::class)
class FloatingActionButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun fabDefaultSemantics() {
        composeTestRule.setMaterialContent {
            Stack {
                FloatingActionButton(modifier = Modifier.testTag("myButton"), onClick = {}) {
                    Icon(Icons.Filled.Favorite)
                }
            }
        }

        findByTag("myButton")
            .assertIsEnabled()
    }

    @Test
    fun extendedFab_findByTextAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "myButton"

        composeTestRule.setMaterialContent {
            Stack {
                ExtendedFloatingActionButton(text = { Text(text) }, onClick = onClick)
            }
        }

        findByText(text)
            .doClick()

        runOnIdleCompose {
            assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun defaultFabHasSizeFromSpec() {
        composeTestRule
            .setMaterialContentAndCollectSizes {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Filled.Favorite)
                }
            }
            .assertIsSquareWithSize(56.dp)
    }

    @Test
    fun extendedFab_longText_HasHeightFromSpec() {
        val size = composeTestRule
            .setMaterialContentAndGetPixelSize {
                ExtendedFloatingActionButton(
                    text = { Text("Extended FAB Text") },
                    icon = { Icon(Icons.Filled.Favorite) },
                    onClick = {}
                )
            }
        with(composeTestRule.density) {
            assertThat(size.height.roundToInt().toFloat()).isEqualTo(48.dp.toIntPx().value)
            assertThat(size.width.roundToInt().toFloat()).isAtLeast(48.dp.toIntPx().value)
        }
    }

    @Test
    fun extendedFab_shortText_HasMinimumSizeFromSpec() {
        val size = composeTestRule
            .setMaterialContentAndGetPixelSize {
                ExtendedFloatingActionButton(
                    text = { Text(".") },
                    onClick = {}
                )
            }
        with(composeTestRule.density) {
            assertThat(size.width.roundToInt().toFloat()).isEqualTo(48.dp.toIntPx().value)
            assertThat(size.height.roundToInt().toFloat()).isEqualTo(48.dp.toIntPx().value)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun fab_shapeAndColorFromThemeIsUsed() {
        val themeShape = CutCornerShape(4.dp)
        val realShape = CutCornerShape(50)
        var surface = Color.Transparent
        var fabColor = Color.Transparent
        composeTestRule.setMaterialContent {
            Stack {
                surface = MaterialTheme.colors.surface
                fabColor = MaterialTheme.colors.secondary
                Providers(ShapesAmbient provides Shapes(small = themeShape)) {
                    FloatingActionButton(
                        modifier = Modifier.testTag("myButton"),
                        onClick = {},
                        elevation = 0.dp
                    ) {
                        Box(Modifier.preferredSize(10.dp, 10.dp))
                    }
                }
            }
        }

        findByTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = realShape,
                shapeColor = fabColor,
                backgroundColor = surface,
                shapeOverlapPixelCount = with(composeTestRule.density) { 1.dp.toPx() }
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun extendedFab_shapeAndColorFromThemeIsUsed() {
        val themeShape = CutCornerShape(4.dp)
        val realShape = CutCornerShape(50)
        var surface = Color.Transparent
        var fabColor = Color.Transparent
        composeTestRule.setMaterialContent {
            Stack {
                surface = MaterialTheme.colors.surface
                fabColor = MaterialTheme.colors.secondary
                Providers(ShapesAmbient provides Shapes(small = themeShape)) {
                    ExtendedFloatingActionButton(
                        modifier = Modifier.testTag("myButton"),
                        onClick = {},
                        elevation = 0.dp,
                        text = { Box(Modifier.preferredSize(10.dp, 50.dp)) }
                    )
                }
            }
        }

        findByTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = realShape,
                shapeColor = fabColor,
                backgroundColor = surface,
                shapeOverlapPixelCount = with(composeTestRule.density) { 1.dp.toPx() }
            )
    }

    @Test
    fun contentIsWrappedAndCentered() {
        var buttonCoordinates: LayoutCoordinates? = null
        var contentCoordinates: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Stack {
                FloatingActionButton({}, Modifier.onPositioned { buttonCoordinates = it }) {
                    Box(Modifier.preferredSize(2.dp)
                        .onPositioned { contentCoordinates = it }
                    )
                }
            }
        }

        runOnIdleCompose {
            val buttonBounds = buttonCoordinates!!.boundsInRoot
            val contentBounds = contentCoordinates!!.boundsInRoot
            assertThat(contentBounds.width).isLessThan(buttonBounds.width)
            assertThat(contentBounds.height).isLessThan(buttonBounds.height)
            with(composeTestRule.density) {
                assertThat(contentBounds.width).isEqualTo(2.dp.toIntPx().value.toFloat())
                assertThat(contentBounds.height).isEqualTo(2.dp.toIntPx().value.toFloat())
            }
            assertWithinOnePixel(buttonBounds.center(), contentBounds.center())
        }
    }

    @Test
    fun extendedFabTextIsWrappedAndCentered() {
        var buttonCoordinates: LayoutCoordinates? = null
        var contentCoordinates: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Stack {
                ExtendedFloatingActionButton(
                    text = {
                        Box(Modifier.preferredSize(2.dp)
                            .onPositioned { contentCoordinates = it }
                        )
                    },
                    onClick = {},
                    modifier = Modifier.onPositioned { buttonCoordinates = it }
                )
            }
        }

        runOnIdleCompose {
            val buttonBounds = buttonCoordinates!!.boundsInRoot
            val contentBounds = contentCoordinates!!.boundsInRoot
            assertThat(contentBounds.width).isLessThan(buttonBounds.width)
            assertThat(contentBounds.height).isLessThan(buttonBounds.height)
            with(composeTestRule.density) {
                assertThat(contentBounds.width).isEqualTo(2.dp.toIntPx().value.toFloat())
                assertThat(contentBounds.height).isEqualTo(2.dp.toIntPx().value.toFloat())
            }
            assertWithinOnePixel(buttonBounds.center(), contentBounds.center())
        }
    }

    @Test
    fun extendedFabTextAndIconArePositionedCorrectly() {
        var buttonCoordinates: LayoutCoordinates? = null
        var textCoordinates: LayoutCoordinates? = null
        var iconCoordinates: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Stack {
                ExtendedFloatingActionButton(
                    text = {
                        Box(Modifier.preferredSize(2.dp)
                            .onPositioned { textCoordinates = it }
                        )
                    },
                    icon = {
                        Box(Modifier.preferredSize(10.dp)
                            .onPositioned { iconCoordinates = it }
                        )
                    },
                    onClick = {},
                    modifier = Modifier.onPositioned { buttonCoordinates = it }
                )
            }
        }

        runOnIdleCompose {
            val buttonBounds = buttonCoordinates!!.boundsInRoot
            val textBounds = textCoordinates!!.boundsInRoot
            val iconBounds = iconCoordinates!!.boundsInRoot
            with(composeTestRule.density) {
                assertThat(textBounds.width).isEqualTo(2.dp.toIntPx().value.toFloat())
                assertThat(textBounds.height).isEqualTo(2.dp.toIntPx().value.toFloat())
                assertThat(iconBounds.width).isEqualTo(10.dp.toIntPx().value.toFloat())
                assertThat(iconBounds.height).isEqualTo(10.dp.toIntPx().value.toFloat())

                assertWithinOnePixel(buttonBounds.center().y, iconBounds.center().y)
                assertWithinOnePixel(buttonBounds.center().y, textBounds.center().y)
                val halfPadding = 6.dp.toIntPx().toPx().value
                assertWithinOnePixel(
                    iconBounds.center().x + iconBounds.width / 2 + halfPadding,
                    textBounds.center().x - textBounds.width / 2 - halfPadding
                )
            }
        }
    }
}
