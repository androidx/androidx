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

import androidx.compose.Model
import androidx.test.filters.MediumTest
import androidx.ui.core.OnPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.TestTag
import androidx.ui.core.dp
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.foundation.Clickable
import androidx.ui.layout.Container
import androidx.ui.semantics.Semantics
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import com.google.common.truth.Truth
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.roundToInt

@Model
data class DrawerStateHolder(var state: DrawerState)

@MediumTest
@RunWith(JUnit4::class)
class DrawerTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun modalDrawer_testOffset_whenOpened() {
        var position: PxPosition? = null
        composeTestRule.setMaterialContent {
            ModalDrawerLayout(DrawerState.Opened, {}, drawerContent = {
                Container(expanded = true) {
                    OnPositioned { coords ->
                        position = coords.localToGlobal(PxPosition.Origin)
                    }
                }
            }) {}
        }
        Truth.assertThat(position!!.x.value).isEqualTo(0f)
    }

    @Test
    fun modalDrawer_testOffset_whenClosed() {
        var position: PxPosition? = null
        composeTestRule.setMaterialContent {
            ModalDrawerLayout(DrawerState.Closed, {}, drawerContent = {
                Container(expanded = true) {
                    OnPositioned { coords ->
                        position = coords.localToGlobal(PxPosition.Origin)
                    }
                }
            }) {}
        }
        val width = composeTestRule.displayMetrics.widthPixels
        Truth.assertThat(position!!.x.round().value).isEqualTo(-width)
    }

    @Test
    fun modalDrawer_testEndPadding_whenOpened() {
        var size: PxSize? = null
        composeTestRule.setMaterialContent {
            ModalDrawerLayout(DrawerState.Opened, {}, drawerContent = {
                Container(expanded = true) {
                    OnPositioned { coords ->
                        size = coords.size
                    }
                }
            }) {}
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
            BottomDrawerLayout(DrawerState.Opened, {}, drawerContent = {
                Container(expanded = true) {
                    OnPositioned { coords ->
                        position = coords.localToGlobal(PxPosition.Origin)
                    }
                }
            }) {}
        }
        val width = composeTestRule.displayMetrics.widthPixels
        val height = composeTestRule.displayMetrics.heightPixels
        // temporary calculation of landscape screen
        val expectedHeight = if (width > height) height else (height / 2f).roundToInt()
        Truth.assertThat(position!!.y.round().value).isEqualTo(expectedHeight)
    }

    @Test
    fun bottomDrawer_testOffset_whenClosed() {
        var position: PxPosition? = null
        composeTestRule.setMaterialContent {
            BottomDrawerLayout(DrawerState.Closed, {}, drawerContent = {
                Container(expanded = true) {
                    OnPositioned { coords ->
                        position = coords.localToGlobal(PxPosition.Origin)
                    }
                }
            }) {}
        }
        val height = composeTestRule.displayMetrics.heightPixels
        Truth.assertThat(position!!.y.round().value).isEqualTo(height)
    }

    @Test
    fun staticDrawer_testWidth_whenOpened() {
        composeTestRule
            .setMaterialContentAndCollectSizes {
                StaticDrawer {
                    Container(expanded = true) {}
                }
            }
            .assertWidthEqualsTo(256.dp)
    }

    @Test
    @Ignore("(malkov/mount): unignore this when b/136678145 is fixed")
    fun modalDrawer_bodyContent_clickable() {
        var drawerClicks = 0
        var bodyClicks = 0
        val drawerState = DrawerStateHolder(DrawerState.Closed)
        composeTestRule.setMaterialContent {
            // emulate click on the screen
            TestTag("Drawer") {
                Semantics(container = true) {
                    ModalDrawerLayout(drawerState.state, { drawerState.state = it },
                        drawerContent = {
                            Clickable(onClick = { drawerClicks += 1 }) {
                                Container(expanded = true) {}
                            }
                        },
                        bodyContent = {
                            Clickable(onClick = { bodyClicks += 1 }) {
                                Container(expanded = true) {}
                            }
                        })
                }
            }
        }

        findByTag("Drawer")
            .doClick()

        composeTestRule.runOnIdleCompose {
            Truth.assertThat(drawerClicks).isEqualTo(0)
            Truth.assertThat(bodyClicks).isEqualTo(1)
        }

        composeTestRule.runOnUiThread {
            drawerState.state = DrawerState.Opened
        }

        findByTag("Drawer")
            .doClick()

        composeTestRule.runOnIdleCompose {
            Truth.assertThat(drawerClicks).isEqualTo(1)
            Truth.assertThat(bodyClicks).isEqualTo(1)
        }
    }

    @Test
    @Ignore("(malkov/mount): unignore this when b/136678145 is fixed")
    fun bottomDrawer_bodyContent_clickable() {
        var drawerClicks = 0
        var bodyClicks = 0
        val drawerState = DrawerStateHolder(DrawerState.Closed)
        composeTestRule.setMaterialContent {
            // emulate click on the screen
            TestTag("Drawer") {
                Semantics(container = true) {
                    BottomDrawerLayout(drawerState.state, { drawerState.state = it },
                        drawerContent = {
                            Clickable(onClick = { drawerClicks += 1 }) {
                                Container(expanded = true) {}
                            }
                        },
                        bodyContent = {
                            Clickable(onClick = { bodyClicks += 1 }) {
                                Container(expanded = true) {}
                            }
                        })
                }
            }
        }

        findByTag("Drawer")
            .doClick()

        Truth.assertThat(drawerClicks).isEqualTo(0)
        Truth.assertThat(bodyClicks).isEqualTo(1)

        // TODO (malkov/pavlis) : uncomment this when custom onClick location will be implemented
//        composeTestRule.runOnUiThread {
//            drawerState.state = DrawerState.Opened
//        }
//        Thread.sleep(100L)
//
//        findByTag("Drawer")
//            .doClick()
//
//
//        Truth.assertThat(drawerClicks).isEqualTo(1)
//        Truth.assertThat(bodyClicks).isEqualTo(1)
    }
}
