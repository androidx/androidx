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
import androidx.compose.emptyContent
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.TestTag
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.semantics.Semantics
import androidx.ui.test.assertPixels
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(Parameterized::class)
class ElevationOverlayTest(private val elevation: Dp, private val expectedOverlayColor: Color) {

    private val Tag = "Surface"
    private val SurfaceSize = IntPxSize(10.ipx, 10.ipx)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        // Mappings for elevation -> expected overlay color in dark theme
        fun initElevation(): Array<Any> = arrayOf(
            arrayOf(0.dp, Color(0xFF121212)),
            arrayOf(1.dp, Color(0xFF1E1E1E)),
            arrayOf(2.dp, Color(0xFF232323)),
            arrayOf(3.dp, Color(0xFF262626)),
            arrayOf(4.dp, Color(0xFF282828)),
            arrayOf(6.dp, Color(0xFF2B2B2B)),
            arrayOf(8.dp, Color(0xFF2E2E2E)),
            arrayOf(12.dp, Color(0xFF333333)),
            arrayOf(16.dp, Color(0xFF353535)),
            arrayOf(24.dp, Color(0xFF393939))
        )
    }

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun correctElevationOverlayInDarkTheme() {
        setupSurfaceForTesting(elevation, darkColorPalette())

        findByTag(Tag)
            .captureToBitmap()
            .assertPixels(SurfaceSize) {
                expectedOverlayColor
            }
    }

    @Test
    fun noChangesInLightTheme() {
        setupSurfaceForTesting(elevation, lightColorPalette())

        // No overlay should be applied in light theme
        val expectedSurfaceColor = Color.White

        findByTag(Tag)
            .captureToBitmap()
            .assertPixels(SurfaceSize) {
                expectedSurfaceColor
            }
    }

    private fun setupSurfaceForTesting(elevation: Dp, colorPalette: ColorPalette) {
        with(composeTestRule.density) {
            composeTestRule.setContent {
                MaterialTheme(colorPalette) {
                    Container {
                        Surface(elevation = elevation) {
                            // Make the surface size small so we compare less pixels
                            TestTag(Tag) {
                                Semantics(container = true) {
                                    Container(
                                        width = SurfaceSize.width.toDp(),
                                        height = SurfaceSize.height.toDp(),
                                        children = emptyContent()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
