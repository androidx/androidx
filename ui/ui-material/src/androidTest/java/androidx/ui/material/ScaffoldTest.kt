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

import android.os.Build
import androidx.compose.Composable
import androidx.compose.mutableStateOf
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.drawShadow
import androidx.ui.core.onPositioned
import androidx.ui.core.positionInParent
import androidx.ui.core.semantics.semantics
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.InnerPadding
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.size
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.testTag
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.doGesture
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.runOnUiThread
import androidx.ui.test.sendSwipeLeft
import androidx.ui.test.sendSwipeRight
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.px
import androidx.ui.unit.toPxSize
import androidx.ui.unit.toSize
import androidx.ui.unit.width
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
                topBar = {
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
        assertThat(appbarPosition.y + appbarSize.height.value.toFloat())
            .isEqualTo(contentPosition.y)
    }

    @Test
    fun scaffold_bottomBarAndContent_inStack() {
        lateinit var appbarPosition: PxPosition
        lateinit var appbarSize: IntPxSize
        lateinit var contentPosition: PxPosition
        lateinit var contentSize: IntPxSize
        composeTestRule.setMaterialContent {
            Scaffold(
                bottomBar = {
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
        val appBarBottom = appbarPosition.y + appbarSize.height.value
        val contentBottom = contentPosition.y + contentSize.height.value
        assertThat(appBarBottom).isEqualTo(contentBottom)
    }

    @Test
    @Ignore("unignore once animation sync is ready (b/147291885)")
    fun scaffold_drawer_gestures() {
        lateinit var drawerChildPosition: PxPosition
        val scaffoldState = ScaffoldState(isDrawerGesturesEnabled = false)
        composeTestRule.setMaterialContent {
            Semantics(properties = { testTag = scaffoldTag }) {
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
        assertThat(drawerChildPosition.x).isLessThan(0f)
        findByTag(scaffoldTag).doGesture {
            sendSwipeRight()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
        findByTag(scaffoldTag).doGesture {
            sendSwipeLeft()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)

        runOnUiThread {
            scaffoldState.isDrawerGesturesEnabled = true
        }

        findByTag(scaffoldTag).doGesture {
            sendSwipeRight()
        }
        assertThat(drawerChildPosition.x).isEqualTo(0f)
        findByTag(scaffoldTag).doGesture {
            sendSwipeLeft()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
    }

    @Test
    @Ignore("unignore once animation sync is ready (b/147291885)")
    fun scaffold_drawer_manualControl() {
        lateinit var drawerChildPosition: PxPosition
        val scaffoldState = ScaffoldState()
        composeTestRule.setMaterialContent {
            Semantics(properties = { testTag = scaffoldTag }) {
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
        assertThat(drawerChildPosition.x).isLessThan(0f)
        runOnUiThread {
            scaffoldState.drawerState = DrawerState.Opened
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
        runOnUiThread {
            scaffoldState.drawerState = DrawerState.Closed
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
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
                floatingActionButtonPosition = Scaffold.FabPosition.Center,
                isFloatingActionButtonDocked = true,
                bottomBar = {
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
        val expectedFabY = bottomBarPosition.y - (fabSize.height / 2).value
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
                floatingActionButtonPosition = Scaffold.FabPosition.End,
                isFloatingActionButtonDocked = true,
                bottomBar = {
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
        val expectedFabY = bottomBarPosition.y - (fabSize.height / 2).value
        assertThat(fabPosition.y).isEqualTo(expectedFabY)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_topAppBarIsDrawnOnTopOfContent() {
        composeTestRule.setContent {
            Stack(
                Modifier
                    .size(10.dp, 20.dp)
                    .semantics(mergeAllDescendants = true)
                    .testTag("Scaffold")
            ) {
                Scaffold(
                    topBar = {
                        Box(
                            Modifier.size(10.dp)
                                .drawShadow(4.dp)
                                .drawBackground(Color.White)
                        )
                    }
                ) {
                    Box(
                        Modifier.size(10.dp)
                            .drawBackground(Color.White)
                    )
                }
            }
        }

        findByTag("Scaffold")
            .captureToBitmap().apply {
                // asserts the appbar(top half part) has the shadow
                val yPos = height / 2 + 2
                assertThat(Color(getPixel(0, yPos))).isNotEqualTo(Color.White)
                assertThat(Color(getPixel(width / 2, yPos))).isNotEqualTo(Color.White)
                assertThat(Color(getPixel(width - 1, yPos))).isNotEqualTo(Color.White)
            }
    }

    @Test
    fun scaffold_geometry_fabSize() {
        lateinit var fabSize: IntPxSize
        val showFab = mutableStateOf(true)
        val scaffoldState = ScaffoldState()
        composeTestRule.setContent {
            val fab: @Composable (() -> Unit)? = if (showFab.value) {
                @Composable {
                    FloatingActionButton(
                        modifier = Modifier.onPositioned { positioned: LayoutCoordinates ->
                            fabSize = positioned.size
                        },
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Favorite)
                    }
                }
            } else {
                null
            }
            Scaffold(
                scaffoldState = scaffoldState,
                floatingActionButton = fab,
                floatingActionButtonPosition = Scaffold.FabPosition.End
            ) {
                Text("body")
            }
        }
        runOnIdleCompose {
            assertThat(scaffoldState.floatingActionButtonSize).isEqualTo(fabSize.toPxSize())
            showFab.value = false
        }

        runOnIdleCompose {
            assertThat(scaffoldState.floatingActionButtonSize).isEqualTo(null)
        }
    }

    @Test
    fun scaffold_geometry_bottomBarSize() {
        lateinit var bottomBarSize: IntPxSize
        val showBottom = mutableStateOf(true)
        val scaffoldState = ScaffoldState()
        composeTestRule.setContent {
            val bottom: @Composable (() -> Unit)? = if (showBottom.value) {
                @Composable {
                    Box(Modifier
                        .onPositioned { positioned: LayoutCoordinates ->
                            bottomBarSize = positioned.size
                        }
                        .fillMaxWidth()
                        .preferredHeight(100.dp)
                        .drawBackground(Color.Red)
                    )
                }
            } else {
                null
            }
            Scaffold(
                scaffoldState = scaffoldState,
                bottomBar = bottom
            ) {
                Text("body")
            }
        }
        runOnIdleCompose {
            assertThat(scaffoldState.bottomBarSize).isEqualTo(bottomBarSize.toPxSize())
            showBottom.value = false
        }

        runOnIdleCompose {
            assertThat(scaffoldState.bottomBarSize).isEqualTo(null)
        }
    }

    @Test
    fun scaffold_geometry_topBarSize() {
        lateinit var topBarSize: IntPxSize
        val showTop = mutableStateOf(true)
        val scaffoldState = ScaffoldState()
        composeTestRule.setContent {
            val top: @Composable (() -> Unit)? = if (showTop.value) {
                @Composable {
                    Box(Modifier
                        .onPositioned { positioned: LayoutCoordinates ->
                            topBarSize = positioned.size
                        }
                        .fillMaxWidth()
                        .preferredHeight(100.dp)
                        .drawBackground(Color.Red)
                    )
                }
            } else {
                null
            }
            Scaffold(
                scaffoldState = scaffoldState,
                topBar = top
            ) {
                Text("body")
            }
        }
        runOnIdleCompose {
            assertThat(scaffoldState.topBarSize).isEqualTo(topBarSize.toPxSize())
            showTop.value = false
        }

        runOnIdleCompose {
            assertThat(scaffoldState.topBarSize).isEqualTo(null)
        }
    }

    @Test
    fun scaffold_innerPadding_lambdaParam() {
        lateinit var bottomBarSize: IntPxSize
        lateinit var innerPadding: InnerPadding

        val scaffoldState = ScaffoldState()
        composeTestRule.setContent {
            Scaffold(
                scaffoldState = scaffoldState,
                bottomBar = {
                    Box(Modifier
                        .onPositioned { positioned: LayoutCoordinates ->
                            bottomBarSize = positioned.size
                        }
                        .fillMaxWidth()
                        .preferredHeight(100.dp)
                        .drawBackground(Color.Red)
                    )
                }
            ) {
                innerPadding = it
                Text("body")
            }
        }
        runOnIdleCompose {
            with(composeTestRule.density) {
                assertThat(innerPadding.bottom).isEqualTo(bottomBarSize.toPxSize().height.toDp())
            }
        }
    }

    @Test
    fun scaffold_bottomBar_geometryPropagation() {
        lateinit var bottomBarSize: IntPxSize
        lateinit var geometry: ScaffoldGeometry

        val scaffoldState = ScaffoldState()
        composeTestRule.setContent {
            Scaffold(
                scaffoldState = scaffoldState,
                bottomBar = {
                    geometry = ScaffoldGeometryAmbient.current
                    Box(Modifier
                        .onPositioned { positioned: LayoutCoordinates ->
                            bottomBarSize = positioned.size
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
        runOnIdleCompose {
            assertThat(geometry.bottomBarBounds?.toSize()).isEqualTo(bottomBarSize.toPxSize())
            assertThat(geometry.bottomBarBounds?.width).isNotEqualTo(0.px)
        }
    }
}
