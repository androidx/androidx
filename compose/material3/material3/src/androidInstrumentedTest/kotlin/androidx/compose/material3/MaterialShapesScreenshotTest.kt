/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class MaterialShapesScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.TopStart)
    private val wrapperTestTag = "materialShapesWrapper"

    @Test
    fun morphShape_start() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Button(
                    onClick = {},
                    modifier = Modifier.requiredSize(56.dp),
                    shape = morphShape(progress = 0f)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertIndicatorAgainstGolden("morphShape_start")
    }

    @Test
    fun morphShape_mid() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Button(
                    onClick = {},
                    modifier = Modifier.requiredSize(56.dp),
                    shape = morphShape(progress = 0.5f)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertIndicatorAgainstGolden("morphShape_mid")
    }

    @Test
    fun morphShape_end() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Button(
                    onClick = {},
                    modifier = Modifier.requiredSize(56.dp),
                    shape = morphShape(progress = 1f)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }
        assertIndicatorAgainstGolden("morphShape_end")
    }

    @Test
    fun materialShapes_allShapes() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                LazyVerticalGrid(
                    columns = GridCells.FixedSize(64.dp),
                    contentPadding = PaddingValues(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    allShapes().forEach { polygon ->
                        item {
                            Spacer(
                                modifier =
                                    Modifier.requiredSize(56.dp)
                                        .clip(polygon.toShape())
                                        .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
        assertIndicatorAgainstGolden("materialShapes_allShapes")
    }

    private fun allShapes(): List<RoundedPolygon> {
        return listOf(
            MaterialShapes.Circle,
            MaterialShapes.Square,
            MaterialShapes.Slanted,
            MaterialShapes.Arch,
            MaterialShapes.Fan,
            MaterialShapes.Arrow,
            MaterialShapes.SemiCircle,
            MaterialShapes.Oval,
            MaterialShapes.Pill,
            MaterialShapes.Triangle,
            MaterialShapes.Diamond,
            MaterialShapes.ClamShell,
            MaterialShapes.Pentagon,
            MaterialShapes.Gem,
            MaterialShapes.Sunny,
            MaterialShapes.VerySunny,
            MaterialShapes.Cookie4Sided,
            MaterialShapes.Cookie6Sided,
            MaterialShapes.Cookie7Sided,
            MaterialShapes.Cookie9Sided,
            MaterialShapes.Cookie12Sided,
            MaterialShapes.Ghostish,
            MaterialShapes.Clover4Leaf,
            MaterialShapes.Clover8Leaf,
            MaterialShapes.Burst,
            MaterialShapes.SoftBurst,
            MaterialShapes.Boom,
            MaterialShapes.SoftBoom,
            MaterialShapes.Flower,
            MaterialShapes.Puffy,
            MaterialShapes.PuffyDiamond,
            MaterialShapes.PixelCircle,
            MaterialShapes.PixelTriangle,
            MaterialShapes.Bun,
            MaterialShapes.Heart
        )
    }

    private fun morphShape(progress: Float): Shape {
        val morph = Morph(MaterialShapes.Diamond, MaterialShapes.Cookie12Sided)
        return object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val matrix = Matrix()
                matrix.scale(size.width, size.height)
                val path = morph.toPath(progress)
                path.transform(matrix)
                return Outline.Generic(path)
            }
        }
    }

    private fun assertIndicatorAgainstGolden(goldenName: String) {
        rule
            .onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
