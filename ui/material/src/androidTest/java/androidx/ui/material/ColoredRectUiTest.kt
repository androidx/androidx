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

import androidx.compose.composer
import androidx.test.filters.MediumTest
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color
import androidx.ui.layout.DpConstraints
import androidx.ui.test.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class ColoredRectUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val color = Color(0xFFFF0000.toInt())

    @Test
    fun coloredRect_fixedSizes() {
        val width = 40.dp
        val height = 71.dp
        composeTestRule
            .setMaterialContentAndTestSizes {
                ColoredRect(width = width, height = height, color = color)
            }
            .assertWidthEqualsTo(width)
            .assertHeightEqualsTo(height)
    }

    @Test
    fun coloredRect_expand_LimitedSizes() {
        val width = 40.dp
        val height = 71.dp
        composeTestRule
            .setMaterialContentAndTestSizes(
                parentConstraints = DpConstraints.tightConstraints(
                    width,
                    height
                )
            ) {
                ColoredRect(color = color)
            }
            .assertWidthEqualsTo(width)
            .assertHeightEqualsTo(height)
    }

    @Test
    fun coloredRect_expand_WholeScreenSizes() {
        val dm = composeTestRule.displayMetrics
        composeTestRule
            .setMaterialContentAndTestSizes {
                ColoredRect(color = color)
            }
            .assertWidthEqualsTo { dm.widthPixels.ipx }
            .assertHeightEqualsTo { dm.heightPixels.ipx }
    }
}