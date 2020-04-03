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
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.TestTag
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Box
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredHeight
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite
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

    private val scaffoldTag = "Scaffold"

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
                Text("One",
                    Modifier.onPositioned { child1 = it.positionInParent }
                )
                Text("Two",
                    Modifier.onPositioned { child2 = it.positionInParent }
                )
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
                    Box(Modifier
                        .onPositioned { positioned: LayoutCoordinates ->
                            appbarPosition = positioned.localToGlobal(PxPosition.Origin)
                            appbarSize = positioned.size
                        }
                        .fillMaxWidth()
                        .preferredHeight(50.dp)
                        .drawBackground(Color.Red)
                    )
                }
            ) {
                Box(Modifier
                    .onPositioned { contentPosition = it.localToGlobal(PxPosition.Origin) }
                    .fillMaxWidth()
                    .preferredHeight(50.dp)
                    .drawBackground(Color.Blue)
                )
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
                    Box(Modifier
                        .onPositioned { positioned: LayoutCoordinates ->
                            appbarPosition = positioned.positionInParent
                            appbarSize = positioned.size
                        }
                        .fillMaxWidth()
                        .preferredHeight(50.dp)
                        .drawBackground(Color.Red)
                    )
                }
            ) {
                Box(Modifier
                    .onPositioned { positioned: LayoutCoordinates ->
                        contentPosition = positioned.positionInParent
                        contentSize = positioned.size
                    }
                    .fillMaxWidth()
                    .preferredHeight(50.dp)
                    .drawBackground(Color.Blue)
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
                            Box(Modifier
                                .onPositioned { positioned: LayoutCoordinates ->
                                    drawerChildPosition = positioned.positionInParent
                                }
                                .fillMaxWidth()
                                .preferredHeight(50.dp)
                                .drawBackground(Color.Blue)
                            )
                        }
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .preferredHeight(50.dp)
                                .drawBackground(Color.Blue)
                        )
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
                            Box(Modifier
                                .onPositioned { positioned: LayoutCoordinates ->
                                    drawerChildPosition = positioned.positionInParent
                                }
                                .fillMaxWidth()
                                .preferredHeight(50.dp)
                                .drawBackground(Color.Blue)
                            )
                        }
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .preferredHeight(50.dp)
                                .drawBackground(Color.Blue)
                        )
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
                    FloatingActionButton(onClick = {}) {
                        Icon(Icons.Filled.Favorite)
                    }
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
                    FloatingActionButton(onClick = {}) {
                        Icon(Icons.Filled.Favorite)
                    }
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
                        modifier = Modifier.onPositioned { positioned: LayoutCoordinates ->
                            fabSize = positioned.size
                            fabPosition = positioned.localToGlobal(positioned.positionInParent)
                        },
                        onClick = {}) {
                        Icon(Icons.Filled.Favorite)
                    }
                },
                floatingActionButtonPosition = Scaffold.FabPosition.CenterDocked,
                bottomAppBar = {
                    Box(Modifier
                        .onPositioned { positioned: LayoutCoordinates ->
                            bottomBarPosition =
                                positioned.localToGlobal(positioned.positionInParent)
                        }
                        .fillMaxWidth()
                        .preferredHeight(100.dp)
                        .drawBackground(Color.Red)
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
                        modifier = Modifier.onPositioned { positioned: LayoutCoordinates ->
                            fabSize = positioned.size
                            fabPosition = positioned.localToGlobal(positioned.positionInParent)
                        },
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Favorite)
                    }
                },
                floatingActionButtonPosition = Scaffold.FabPosition.EndDocked,
                bottomAppBar = {
                    Box(Modifier
                        .onPositioned { positioned: LayoutCoordinates ->
                            bottomBarPosition =
                                positioned.localToGlobal(positioned.positionInParent)
                        }
                        .fillMaxWidth()
                        .preferredHeight(100.dp)
                        .drawBackground(Color.Red)
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
