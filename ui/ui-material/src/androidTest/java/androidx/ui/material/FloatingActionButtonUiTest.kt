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
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.painting.Image
import androidx.ui.painting.ImageConfig
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class FloatingActionButtonUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun defaultFabHasSizeFromSpec() {
        composeTestRule
            .setMaterialContentAndTestSizes {
                FloatingActionButton(icon = createImage())
            }
            .assertIsSquareWithSize(56.dp)
    }

    @Test
    fun extendedFabHasHeightFromSpec() {
        val size = composeTestRule
            .setMaterialContentAndCollectPixelSize {
                FloatingActionButton(icon = createImage(), text = "Extended")
            }
        withDensity(composeTestRule.density) {
            Truth.assertThat(size.width.round().value).isAtLeast(48.dp.toIntPx().value)
            Truth.assertThat(size.height.round()).isEqualTo(48.dp.toIntPx())
        }
    }

    private fun createImage() = withDensity(composeTestRule.density) {
        val size = 24.dp.toIntPx().value
        Image(size, size, ImageConfig.Argb8888)
    }
}
