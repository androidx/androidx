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

import android.graphics.Bitmap
import androidx.compose.composer
import androidx.test.filters.MediumTest
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.layout.Wrap
import androidx.ui.painting.Image
import androidx.ui.painting.ImageConfig
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
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
    fun defaultFabHasSizeFromSpec() = withDensity(composeTestRule.density) {
        var size: PxSize? = null

        composeTestRule.setMaterialContent {
            Wrap {
                OnChildPositioned(onPositioned = { position ->
                    size = position.size
                }) {
                    FloatingActionButton(icon = createImage())
                }
            }
        }
        with(size!!) {
            val expectedSize = 56.dp.toIntPx()
            assertThat(width.round()).isEqualTo(expectedSize)
            assertThat(height.round()).isEqualTo(expectedSize)
        }
    }

    @Test
    fun extendedFabHasHeightFromSpec() = withDensity(composeTestRule.density) {
        var size: PxSize? = null

        composeTestRule.setMaterialContent {
            Wrap {
                OnChildPositioned(onPositioned = { position ->
                    size = position.size
                }) {
                    FloatingActionButton(icon = createImage(), text = "Extended")
                }
            }
        }
        with(size!!) {
            val expectedSize = 48.dp.toIntPx()
            assertThat(height.round()).isEqualTo(expectedSize)
            assertThat(width.round().value).isAtLeast(expectedSize.value)
        }
    }

    private fun createImage() = withDensity(composeTestRule.density) {
        val size = 24.dp.toIntPx().value
        Image(size, size, ImageConfig.Argb8888)
    }
}
