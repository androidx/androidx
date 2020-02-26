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
package androidx.ui.material

import androidx.test.filters.LargeTest
import androidx.ui.foundation.Icon
import androidx.ui.graphics.Color
import androidx.ui.graphics.painter.ColorPainter
import androidx.ui.graphics.vector.VectorAssetBuilder
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Menu
import androidx.ui.test.createComposeRule
import androidx.ui.unit.dp
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class IconTest {
    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun materialIconSize_vector_dimensions() {
        val width = 24.dp
        val height = 24.dp
        val vector = Icons.Filled.Menu
        composeTestRule
            .setMaterialContentAndCollectSizes {
                Icon(vector)
            }
            .assertWidthEqualsTo(width)
            .assertHeightEqualsTo(height)
    }

    @Test
    fun customIconSize_vector_dimensions() {
        val width = 35.dp
        val height = 83.dp
        val vector = VectorAssetBuilder(defaultWidth = width, defaultHeight = height,
            viewportWidth = width.value, viewportHeight = height.value).build()
        composeTestRule
            .setMaterialContentAndCollectSizes {
                Icon(vector)
            }
            .assertWidthEqualsTo(width)
            .assertHeightEqualsTo(height)
    }

    @Test
    fun materialIconSize_painter_dimensions() {
        val width = 24.dp
        val height = 24.dp
        val painter = ColorPainter(Color.Red)
        composeTestRule
            .setMaterialContentAndCollectSizes {
                Icon(painter)
            }
            .assertWidthEqualsTo(width)
            .assertHeightEqualsTo(height)
    }

    @Ignore("TODO(b/149693776): currently we do not use intrinsic size for painters")
    @Test
    fun customIconSize_painter_dimensions() {
        val width = 35.dp
        val height = 83.dp
        val painter = ColorPainter(Color.Red)
        composeTestRule
            .setMaterialContentAndCollectSizes {
                Icon(painter)
            }
            .assertWidthEqualsTo(width)
            .assertHeightEqualsTo(height)
    }
}
