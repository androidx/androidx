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
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class DividerUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val bigConstraints = DpConstraints(maxWidth = 5000.dp, maxHeight = 5000.dp)

    private val defaultHeight = 1.dp

    @Test
    fun divider_DefaultSizes() {
        var size: PxSize? = null
        composeTestRule.setMaterialContent {
            Container(constraints = bigConstraints) {
                OnChildPositioned(onPositioned = { position ->
                    size = position.size
                }) {
                    Divider()
                }
            }
        }
        val dm = composeTestRule.displayMetrics
        withDensity(composeTestRule.density) {
            assertThat(size?.height?.round()).isEqualTo(defaultHeight.toIntPx())
            assertThat(size?.width?.value?.toInt()).isEqualTo(dm.widthPixels)
        }
    }

    @Test
    fun divider_CustomSizes() {
        var size: PxSize? = null
        val height = 20.dp

        composeTestRule.setMaterialContent {
            Container(constraints = bigConstraints) {
                OnChildPositioned(onPositioned = { position ->
                    size = position.size
                }) {
                    Divider(height = height)
                }
            }
        }
        val dm = composeTestRule.displayMetrics
        withDensity(composeTestRule.density) {
            assertThat(size?.height?.round()).isEqualTo(height.toIntPx())
            assertThat(size?.width?.value?.toInt()).isEqualTo(dm.widthPixels)
        }
    }

    @Test
    fun divider_SizesWithIndent_DoesNotChanged() {
        var size: PxSize? = null
        val indent = 75.dp
        val height = 21.dp

        composeTestRule.setMaterialContent {
            Container(constraints = bigConstraints) {
                OnChildPositioned(onPositioned = { position ->
                    size = position.size
                }) {
                    Divider(indent = indent, height = height)
                }
            }
        }
        val dm = composeTestRule.displayMetrics
        withDensity(composeTestRule.density) {
            assertThat(size?.height?.round()).isEqualTo(height.toIntPx())
            assertThat(size?.width?.value?.toInt()).isEqualTo(dm.widthPixels)
        }
    }
}