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

import androidx.test.filters.MediumTest
import androidx.ui.core.TestTag
import androidx.ui.core.Text
import androidx.ui.core.onPositioned
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color
import androidx.ui.layout.DpConstraints
import androidx.ui.semantics.Semantics
import androidx.ui.test.createComposeRule
import androidx.ui.test.doGesture
import androidx.ui.test.findByTag
import androidx.ui.test.positionInParent
import androidx.ui.test.sendSwipeLeft
import androidx.ui.test.sendSwipeRight
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.px
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class ScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    val scaffoldTag = "Scaffold"

    @Test
    fun scaffold_onlyContent_takesWholeScreen() {
        composeTestRule.setMaterialContentAndCollectSizes(
            parentConstraints = DpConstraints(maxWidth = 100.dp, maxHeight = 100.dp)
        ) {
            Scaffold {
                Text("Scaffold body")
            }
        }
            .assertWidthEqualsTo(100.dp)
            .assertHeightEqualsTo(100.dp)
    }

    @Test
    fun scaffold_onlyContent_stackSlot() {
        lateinit var child1: PxPosition
        lateinit var child2: PxPosition
        composeTestRule.setMaterialContent {
            Scaffold {
                Text("One", onPositioned { child1 = it.positionInParent })
                Text("Two", onPositioned { child2 = it.positionInParent })
            }
        }
        assertThat(child1.y).isEqualTo(child2.y)
        assertThat(child1.x).isEqualTo(child2.x)
    }

    @Test
    fun scaffold_AppbarAndContent_inColumn() {
        lateinit var appbarPosition: PxPosition
        lateinit var appbarSize: IntPxSize
        lateinit var contentPosition: PxPosition
        composeTestRule.setMaterialContent {
            Scaffold(
                topAppBar = {
                        ColoredRect(
                            Color.Red,
                            onPositioned { positioned ->
                                appbarPosition = positioned.localToGlobal(PxPosition.Origin)
                                appbarSize = positioned.size
                            },
                            height = 50.dp
                        )
                }
            ) {
                ColoredRect(
                    Color.Blue,
                    onPositioned { contentPosition = it.localToGlobal(PxPosition.Origin) },
                    height = 50.dp)
            }
        }
        assertThat(appbarPosition.y + appbarSize.height).isEqualTo(contentPosition.y)
    }

    @Test
    fun scaffold_bottomBarAndContent_inStack() {
        lateinit var appbarPosition: PxPosition
        lateinit var appbarSize: IntPxSize
        lateinit var contentPosition: PxPosition
        lateinit var contentSize: IntPxSize
        composeTestRule.setMaterialContent {
            Scaffold(
                bottomAppBar = {
                    ColoredRect(
                        Color.Red,
                        onPositioned { positioned ->
                            appbarPosition = positioned.positionInParent
                            appbarSize = positioned.size
                        },
                        height = 50.dp
                    )
                }
            ) {
                ColoredRect(
                    Color.Blue,
                    onPositioned { positioned ->
                        contentPosition = positioned.positionInParent
                        contentSize = positioned.size
                    },
                    height = 50.dp
                )
            }
        }
        val appBarBottom = appbarPosition.y + appbarSize.height
        val contentBottom = contentPosition.y + contentSize.height
        assertThat(appBarBottom).isEqualTo(contentBottom)
    }

    @Test
    @Ignore("unignore once animation sync is ready (b/147291885)")
    fun scaffold_drawer_gestures() {
        lateinit var drawerChildPosition: PxPosition
        val scaffoldState = ScaffoldState(isDrawerGesturesEnabled = false)
        composeTestRule.setMaterialContent {
            TestTag(scaffoldTag) {
                Semantics(container = true) {
                    Scaffold(
                        scaffoldState = scaffoldState,
                        drawerContent = {
                            ColoredRect(
                                Color.Blue,
                                onPositioned { positioned ->
                                    drawerChildPosition = positioned.positionInParent
                                },
                                height = 50.dp
                            )
                        }
                    ) {
                        ColoredRect(Color.Blue, height = 50.dp)
                    }
                }
            }
        }
        assertThat(drawerChildPosition.x).isLessThan(0.px)
        findByTag(scaffoldTag).doGesture {
            sendSwipeRight()
        }
        assertThat(drawerChildPosition.x).isLessThan(0.px)
        findByTag(scaffoldTag).doGesture {
            sendSwipeLeft()
        }
        assertThat(drawerChildPosition.x).isLessThan(0.px)

        composeTestRule.runOnUiThread {
            scaffoldState.isDrawerGesturesEnabled = true
        }

        findByTag(scaffoldTag).doGesture {
            sendSwipeRight()
        }
        assertThat(drawerChildPosition.x).isEqualTo(0.px)
        findByTag(scaffoldTag).doGesture {
            sendSwipeLeft()
        }
        assertThat(drawerChildPosition.x).isLessThan(0.px)
    }

    @Test
    @Ignore("unignore once animation sync is ready (b/147291885)")
    fun scaffold_drawer_manualControl() {
        lateinit var drawerChildPosition: PxPosition
        val scaffoldState = ScaffoldState()
        composeTestRule.setMaterialContent {
            TestTag(scaffoldTag) {
                Semantics(container = true) {
                    Scaffold(
                        scaffoldState = scaffoldState,
                        drawerContent = {
                            ColoredRect(
                                Color.Blue,
                                onPositioned { positioned ->
                                    drawerChildPosition = positioned.positionInParent
                                },
                                height = 50.dp
                            )
                        }
                    ) {
                        ColoredRect(Color.Blue, height = 50.dp)
                    }
                }
            }
        }
        assertThat(drawerChildPosition.x).isLessThan(0.px)
        composeTestRule.runOnUiThread {
            scaffoldState.drawerState = DrawerState.Opened
        }
        assertThat(drawerChildPosition.x).isLessThan(0.px)
        composeTestRule.runOnUiThread {
            scaffoldState.drawerState = DrawerState.Closed
        }
        assertThat(drawerChildPosition.x).isLessThan(0.px)
    }

    @Test(expected = IllegalArgumentException::class)
    fun scaffold_centerDockedFab_withoutBottomAppBar_shouldCrash() {
        composeTestRule.setContent {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton("text", onClick = {})
                },
                floatingActionButtonPosition = Scaffold.FabPosition.CenterDocked
            ) {
                Text("body")
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun scaffold_endDockedFab_withoutBottomAppBar_shouldCrash() {
        composeTestRule.setContent {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton("text", onClick = {})
                },
                floatingActionButtonPosition = Scaffold.FabPosition.EndDocked
            ) {
                Text("body")
            }
        }
    }

    @Test
    fun scaffold_centerDockedFab_position() {
        lateinit var fabPosition: PxPosition
        lateinit var fabSize: IntPxSize
        lateinit var bottomBarPosition: PxPosition
        composeTestRule.setContent {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        "text",
                        modifier = onPositioned { positioned ->
                            fabSize = positioned.size
                            fabPosition = positioned.localToGlobal(positioned.positionInParent)
                        },
                        onClick = {})
                },
                floatingActionButtonPosition = Scaffold.FabPosition.CenterDocked,
                bottomAppBar = {
                    ColoredRect(
                        Color.Red,
                        onPositioned { positioned ->
                            bottomBarPosition =
                                positioned.localToGlobal(positioned.positionInParent)
                        },
                        height = 100.dp
                    )
                }
            ) {
                Text("body")
            }
        }
        val expectedFabY = bottomBarPosition.y - fabSize.height / 2
        assertThat(fabPosition.y).isEqualTo(expectedFabY)
    }

    @Test
    fun scaffold_endDockedFab_position() {
        lateinit var fabPosition: PxPosition
        lateinit var fabSize: IntPxSize
        lateinit var bottomBarPosition: PxPosition
        composeTestRule.setContent {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        "text",
                        modifier = onPositioned { positioned ->
                            fabSize = positioned.size
                            fabPosition = positioned.localToGlobal(positioned.positionInParent)
                        },
                        onClick = {}
                    )
                },
                floatingActionButtonPosition = Scaffold.FabPosition.EndDocked,
                bottomAppBar = {
                    ColoredRect(
                        Color.Red,
                        onPositioned { positioned ->
                            bottomBarPosition =
                                positioned.localToGlobal(positioned.positionInParent)
                        },
                        height = 100.dp
                    )
                }
            ) {
                Text("body")
            }
        }
        val expectedFabY = bottomBarPosition.y - fabSize.height / 2
        assertThat(fabPosition.y).isEqualTo(expectedFabY)
    }
}
