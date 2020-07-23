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
import androidx.compose.foundation.Box
import androidx.compose.foundation.Icon
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.DpConstraints
import androidx.compose.foundation.layout.InnerPadding
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.size
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.performGesture
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.runOnIdle
import androidx.ui.test.runOnUiThread
import androidx.ui.test.swipeLeft
import androidx.ui.test.swipeRight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.width
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
        composeTestRule.setMaterialContentForSizeAssertions(
            parentConstraints = DpConstraints(maxWidth = 100.dp, maxHeight = 100.dp)
        ) {
            Scaffold {
                Text("Scaffold body")
            }
        }
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun scaffold_onlyContent_stackSlot() {
        var child1: Offset = Offset.Zero
        var child2: Offset = Offset.Zero
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
        var appbarPosition: Offset = Offset.Zero
        lateinit var appbarSize: IntSize
        var contentPosition: Offset = Offset.Zero
        composeTestRule.setMaterialContent {
            Scaffold(
                topBar = {
                    Box(Modifier
                        .onPositioned { positioned: LayoutCoordinates ->
                            appbarPosition = positioned.localToGlobal(Offset.Zero)
                            appbarSize = positioned.size
                        }
                        .fillMaxWidth()
                        .preferredHeight(50.dp)
                        .background(color = Color.Red)
                    )
                }
            ) {
                Box(Modifier
                    .onPositioned { contentPosition = it.localToGlobal(Offset.Zero) }
                    .fillMaxWidth()
                    .preferredHeight(50.dp)
                    .background(Color.Blue)
                )
            }
        }
        assertThat(appbarPosition.y + appbarSize.height.toFloat())
            .isEqualTo(contentPosition.y)
    }

    @Test
    fun scaffold_bottomBarAndContent_inStack() {
        var appbarPosition: Offset = Offset.Zero
        lateinit var appbarSize: IntSize
        var contentPosition: Offset = Offset.Zero
        lateinit var contentSize: IntSize
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
                        .background(color = Color.Red)
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
                    .background(color = Color.Blue)
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
        var drawerChildPosition: Offset = Offset.Zero
        val scaffoldState = ScaffoldState(isDrawerGesturesEnabled = false)
        composeTestRule.setMaterialContent {
            Box(Modifier.testTag(scaffoldTag)) {
                Scaffold(
                    scaffoldState = scaffoldState,
                    drawerContent = {
                        Box(Modifier
                            .onPositioned { positioned: LayoutCoordinates ->
                                drawerChildPosition = positioned.positionInParent
                            }
                            .fillMaxWidth()
                            .preferredHeight(50.dp)
                            .background(color = Color.Blue)
                        )
                    }
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .preferredHeight(50.dp)
                            .background(color = Color.Blue)
                    )
                }
            }
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
        onNodeWithTag(scaffoldTag).performGesture {
            swipeRight()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
        onNodeWithTag(scaffoldTag).performGesture {
            swipeLeft()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)

        runOnUiThread {
            scaffoldState.isDrawerGesturesEnabled = true
        }

        onNodeWithTag(scaffoldTag).performGesture {
            swipeRight()
        }
        assertThat(drawerChildPosition.x).isEqualTo(0f)
        onNodeWithTag(scaffoldTag).performGesture {
            swipeLeft()
        }
        assertThat(drawerChildPosition.x).isLessThan(0f)
    }

    @Test
    @Ignore("unignore once animation sync is ready (b/147291885)")
    fun scaffold_drawer_manualControl() {
        var drawerChildPosition: Offset = Offset.Zero
        val scaffoldState = ScaffoldState()
        composeTestRule.setMaterialContent {
            Box(Modifier.testTag(scaffoldTag)) {
                Scaffold(
                    scaffoldState = scaffoldState,
                    drawerContent = {
                        Box(Modifier
                            .onPositioned { positioned: LayoutCoordinates ->
                                drawerChildPosition = positioned.positionInParent
                            }
                            .fillMaxWidth()
                            .preferredHeight(50.dp)
                            .background(color = Color.Blue)
                        )
                    }
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .preferredHeight(50.dp)
                            .background(color = Color.Blue)
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
        var fabPosition: Offset = Offset.Zero
        lateinit var fabSize: IntSize
        var bottomBarPosition: Offset = Offset.Zero
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
                        .background(color = Color.Red)
                    )
                }
            ) {
                Text("body")
            }
        }
        val expectedFabY = bottomBarPosition.y - (fabSize.height / 2)
        assertThat(fabPosition.y).isEqualTo(expectedFabY)
    }

    @Test
    fun scaffold_endDockedFab_position() {
        var fabPosition: Offset = Offset.Zero
        lateinit var fabSize: IntSize
        var bottomBarPosition: Offset = Offset.Zero
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
                        .background(color = Color.Red)
                    )
                }
            ) {
                Text("body")
            }
        }
        val expectedFabY = bottomBarPosition.y - (fabSize.height / 2)
        assertThat(fabPosition.y).isEqualTo(expectedFabY)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scaffold_topAppBarIsDrawnOnTopOfContent() {
        composeTestRule.setContent {
            Stack(
                Modifier
                    .size(10.dp, 20.dp)
                    .semantics(mergeAllDescendants = true) {}
                    .testTag("Scaffold")
            ) {
                Scaffold(
                    topBar = {
                        Box(
                            Modifier.size(10.dp)
                                .drawShadow(4.dp)
                                .background(color = Color.White)
                        )
                    }
                ) {
                    Box(
                        Modifier.size(10.dp)
                            .background(color = Color.White)
                    )
                }
            }
        }

        onNodeWithTag("Scaffold")
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
        lateinit var fabSize: IntSize
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
        runOnIdle {
            assertThat(scaffoldState.floatingActionButtonSize).isEqualTo(fabSize.toSize())
            showFab.value = false
        }

        runOnIdle {
            assertThat(scaffoldState.floatingActionButtonSize).isEqualTo(null)
        }
    }

    @Test
    fun scaffold_geometry_bottomBarSize() {
        lateinit var bottomBarSize: IntSize
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
                        .background(color = Color.Red)
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
        runOnIdle {
            assertThat(scaffoldState.bottomBarSize).isEqualTo(bottomBarSize.toSize())
            showBottom.value = false
        }

        runOnIdle {
            assertThat(scaffoldState.bottomBarSize).isEqualTo(null)
        }
    }

    @Test
    fun scaffold_geometry_topBarSize() {
        lateinit var topBarSize: IntSize
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
                        .background(color = Color.Red)
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
        runOnIdle {
            assertThat(scaffoldState.topBarSize).isEqualTo(topBarSize.toSize())
            showTop.value = false
        }

        runOnIdle {
            assertThat(scaffoldState.topBarSize).isEqualTo(null)
        }
    }

    @Test
    fun scaffold_innerPadding_lambdaParam() {
        lateinit var bottomBarSize: IntSize
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
                        .background(color = Color.Red)
                    )
                }
            ) {
                innerPadding = it
                Text("body")
            }
        }
        runOnIdle {
            with(composeTestRule.density) {
                assertThat(innerPadding.bottom).isEqualTo(bottomBarSize.toSize().height.toDp())
            }
        }
    }

    @Test
    fun scaffold_bottomBar_geometryPropagation() {
        lateinit var bottomBarSize: IntSize
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
                        .background(color = Color.Red)
                    )
                }
            ) {
                Text("body")
            }
        }
        runOnIdle {
            assertThat(geometry.bottomBarBounds?.toSize()).isEqualTo(bottomBarSize.toSize())
            assertThat(geometry.bottomBarBounds?.width).isNotEqualTo(0f)
        }
    }
}
