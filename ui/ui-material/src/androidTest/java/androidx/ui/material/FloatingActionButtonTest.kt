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
import androidx.ui.core.Modifier
import androidx.ui.core.TestTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.foundation.shape.corner.CutCornerShape
import androidx.ui.graphics.Color
import androidx.ui.layout.Stack
import androidx.ui.layout.preferredSize
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite
import androidx.ui.test.assertSemanticsIsEqualTo
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.createFullSemantics
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import androidx.ui.test.runOnIdleCompose
import androidx.ui.unit.dp
import androidx.ui.unit.round
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class FloatingActionButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultButtonSemantics = createFullSemantics(
        isEnabled = true
    )

    @Test
    fun fabDefaultSemantics() {
        composeTestRule.setMaterialContent {
            Stack {
                TestTag(tag = "myButton") {
                    FloatingActionButton(onClick = {}) {
                        Icon(Icons.Filled.Favorite)
                    }
                }
            }
        }

        findByTag("myButton")
            .assertSemanticsIsEqualTo(defaultButtonSemantics)
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
            assertThat(size.height.round()).isEqualTo(48.dp.toIntPx())
            assertThat(size.width.round().value).isAtLeast(48.dp.toIntPx().value)
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
            assertThat(size.width.round()).isEqualTo(48.dp.toIntPx())
            assertThat(size.height.round()).isEqualTo(48.dp.toIntPx())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun fab_shapeAndColorFromThemeIsUsed() {
        val themeShape = CutCornerShape(4.dp)
        val realShape = CutCornerShape(50)
        var surface = Color.Transparent
        var primary = Color.Transparent
        composeTestRule.setMaterialContent {
            Stack {
                surface = MaterialTheme.colors.surface
                primary = MaterialTheme.colors.primary
                Providers(ShapesAmbient provides Shapes(small = themeShape)) {
                    TestTag(tag = "myButton") {
                        FloatingActionButton(onClick = {}, elevation = 0.dp) {
                            Box(Modifier.preferredSize(10.dp, 10.dp))
                        }
                    }
                }
            }
        }

        findByTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = realShape,
                shapeColor = primary,
                backgroundColor = surface,
                shapeOverlapPixelCount = with(composeTestRule.density) { 1.dp.toPx() }
            )
    }
}
