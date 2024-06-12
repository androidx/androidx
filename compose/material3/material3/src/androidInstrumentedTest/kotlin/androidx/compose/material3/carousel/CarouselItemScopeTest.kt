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

package androidx.compose.material3.carousel

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.GOLDEN_MATERIAL3
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.setMaterialContent
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
class CarouselItemScopeTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    val testTag = "ItemTag"

    @Test
    fun mask_fullyUnmaskedShouldMatchSize() {
        rule.setMaterialContent(lightColorScheme()) {
            val scope =
                createCarouselItemScope(
                    size = 100.dp,
                    minSize = 10.dp,
                    maxSize = 100.dp,
                    maskRect = DpRect(0.dp, 0.dp, 100.dp, 100.dp)
                )
            with(scope) {
                Box(
                    modifier =
                        Modifier.testTag(testTag)
                            .size(100.dp)
                            .maskClip(shape = RoundedCornerShape(28.dp))
                            .background(Color.Red)
                )
            }
        }

        assertCarouselAgainstGolden("mask_fullyUnmaskedShouldMatchSize")
    }

    @Test
    fun mask_halfMaksedShouldIntersectSize() {
        rule.setMaterialContent(lightColorScheme()) {
            val scope =
                createCarouselItemScope(
                    size = 50.dp,
                    minSize = 10.dp,
                    maxSize = 100.dp,
                    maskRect = DpRect(25.dp, 25.dp, 75.dp, 75.dp)
                )
            with(scope) {
                Box(
                    modifier =
                        Modifier.testTag(testTag)
                            .size(100.dp)
                            .maskClip(shape = RoundedCornerShape(10.dp))
                            .background(Color.Red)
                )
            }
        }

        assertCarouselAgainstGolden("mask_halfMaskedShouldIntersectSize")
    }

    @Test
    fun mask_genericMaskedPathShouldIntersectSize() {
        val ovalPathShape = GenericShape { size, _ ->
            addOval(Rect(0f, 0f, size.width, size.height))
        }
        rule.setMaterialContent(lightColorScheme()) {
            val scope =
                createCarouselItemScope(
                    size = 50.dp,
                    minSize = 10.dp,
                    maxSize = 100.dp,
                    maskRect = DpRect(25.dp, 0.dp, 75.dp, 100.dp)
                )
            with(scope) {
                Box(
                    modifier =
                        Modifier.testTag(testTag)
                            .size(100.dp)
                            .maskClip(shape = ovalPathShape)
                            .background(Color.Red)
                )
            }
        }

        assertCarouselAgainstGolden("mask_genericMaskedPathShouldIntersectSize")
    }

    @Test
    fun mask_squareMaskShouldIntersectSize() {
        rule.setMaterialContent(lightColorScheme()) {
            val scope =
                createCarouselItemScope(
                    size = 50.dp,
                    minSize = 10.dp,
                    maxSize = 100.dp,
                    maskRect = DpRect(25.dp, 0.dp, 75.dp, 100.dp)
                )
            with(scope) {
                Box(
                    modifier =
                        Modifier.testTag(testTag)
                            .size(100.dp)
                            .maskClip(shape = RoundedCornerShape(0.dp))
                            .background(Color.Red)
                )
            }
        }

        assertCarouselAgainstGolden("mask_squareMaskShouldIntersectSize")
    }

    @Test
    fun maskBorder_fullyUnmaskedShouldMatchSize() {
        rule.setMaterialContent(lightColorScheme()) {
            val scope =
                createCarouselItemScope(
                    size = 100.dp,
                    minSize = 10.dp,
                    maxSize = 100.dp,
                    maskRect = DpRect(0.dp, 0.dp, 100.dp, 100.dp)
                )
            with(scope) {
                Box(
                    modifier =
                        Modifier.testTag(testTag)
                            .size(100.dp)
                            .maskClip(shape = RoundedCornerShape(10.dp))
                            .maskBorder(
                                border = BorderStroke(5.dp, Color.Blue),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .background(Color.Red)
                )
            }
        }

        assertCarouselAgainstGolden("maskBorder_fullyUnmaskedShouldMatchSize")
    }

    @Test
    fun maskBorder_triangleMaskShouldIntersectSize() {
        val triangle = GenericShape { size, _ ->
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        rule.setMaterialContent(lightColorScheme()) {
            val scope =
                createCarouselItemScope(
                    size = 100.dp,
                    minSize = 10.dp,
                    maxSize = 100.dp,
                    maskRect = DpRect(25.dp, 25.dp, 75.dp, 75.dp)
                )
            with(scope) {
                Box(
                    modifier =
                        Modifier.testTag(testTag)
                            .size(100.dp)
                            .maskClip(shape = triangle)
                            .maskBorder(border = BorderStroke(5.dp, Color.Blue), shape = triangle)
                            .background(Color.Red)
                )
            }
        }

        assertCarouselAgainstGolden("maskBorder_triangleMaskShouldIntersectSize")
    }

    private fun createCarouselItemScope(
        size: Dp,
        minSize: Dp,
        maxSize: Dp,
        maskRect: DpRect
    ): CarouselItemScope {
        return CarouselItemScopeImpl(
            CarouselItemDrawInfoImpl().apply {
                with(rule.density) {
                    sizeState = size.toPx()
                    minSizeState = minSize.toPx()
                    maxSizeState = maxSize.toPx()
                    maskRectState = maskRect.toRect()
                }
            }
        )
    }

    private fun assertCarouselAgainstGolden(goldenIdentifier: String) {
        rule
            .onNodeWithTag(testTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "carousel_$goldenIdentifier")
    }
}
