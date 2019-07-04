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
import androidx.ui.core.OnPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.layout.Container
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.roundToInt

@MediumTest
@RunWith(JUnit4::class)
class DrawerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun modalDrawer_testOffset_whenOpened() {
        var position: PxPosition? = null
        composeTestRule.setMaterialContent {
            ModalDrawer(DrawerState.Opened, {}) {
                Container(expanded = true) {
                    OnPositioned { coords ->
                        position = coords.localToGlobal(PxPosition.Origin)
                    }
                }
            }
        }
        Truth.assertThat(position!!.x.value).isEqualTo(0f)
    }

    @Test
    fun modalDrawer_testOffset_whenClosed() {
        var position: PxPosition? = null
        composeTestRule.setMaterialContent {
            ModalDrawer(DrawerState.Closed, {}) {
                Container(expanded = true) {
                    OnPositioned { coords ->
                        position = coords.localToGlobal(PxPosition.Origin)
                    }
                }
            }
        }
        val width = composeTestRule.displayMetrics.widthPixels
        Truth.assertThat(position!!.x.round().value).isEqualTo(-width)
    }

    @Test
    fun modalDrawer_testEndPadding_whenOpened() {
        var size: PxSize? = null
        composeTestRule.setMaterialContent {
            ModalDrawer(DrawerState.Opened, {}) {
                Container(expanded = true) {
                    OnPositioned { coords ->
                        size = coords.size
                    }
                }
            }
        }

        val width = composeTestRule.displayMetrics.widthPixels
        withDensity(composeTestRule.density) {
            Truth.assertThat(size!!.width.round().value)
                .isEqualTo(width - 56.dp.toPx().round().value)
        }
    }

    @Test
    fun bottomDrawer_testOffset_whenOpened() {
        var position: PxPosition? = null
        composeTestRule.setMaterialContent {
            BottomDrawer(DrawerState.Opened, {}) {
                Container(expanded = true) {
                    OnPositioned { coords ->
                        position = coords.localToGlobal(PxPosition.Origin)
                    }
                }
            }
        }
        val height = composeTestRule.displayMetrics.heightPixels
        Truth.assertThat(position!!.y.round().value).isEqualTo((height / 2f).roundToInt())
    }

    @Test
    fun bottomDrawer_testOffset_whenClosed() {
        var position: PxPosition? = null
        composeTestRule.setMaterialContent {
            BottomDrawer(DrawerState.Closed, {}) {
                Container(expanded = true) {
                    OnPositioned { coords ->
                        position = coords.localToGlobal(PxPosition.Origin)
                    }
                }
            }
        }
        val height = composeTestRule.displayMetrics.heightPixels
        Truth.assertThat(position!!.y.round().value).isEqualTo(height)
    }

    @Test
    fun staticDrawer_testWidth_whenOpened() {
        composeTestRule
            .setMaterialContentAndTestSizes {
                StaticDrawer {
                    Container(expanded = true) {}
                }
            }
            .assertWidthEqualsTo(256.dp)
    }
}
