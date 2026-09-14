/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.compose.integration.tests

import androidx.activity.ComponentActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterDefaults
import androidx.xr.compose.spatial.OrbiterPosition
import androidx.xr.compose.spatial.OrbiterPosition.EdgeAlignment
import androidx.xr.compose.spatial.SpatialDialog
import androidx.xr.compose.spatial.SpatialElevation
import androidx.xr.compose.spatial.SpatialElevationLevel
import androidx.xr.compose.spatial.SpatialPopup
import androidx.xr.compose.unit.DpVolumeOffset
import androidx.xr.testutils.XrDeviceTest
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrument tests for the Orbiter/Elevation CUJ test case in Compose XR.
 *
 * Covers:
 * - Elevated Orbiters rendering (NavigationRail, IconMenuOrnament)
 * - Elevated UI Controls (Floating Action Buttons)
 * - SpatialDialog presentation and dismissal
 * - SpatialPopup presentation and dismissal
 * - Expanding and collapsing menu ornaments
 * - Item content editing within elevated dialog/surface
 * - List scrolling via NavigationRail orbiter controls ("Up", "Down", "Top"/"Menu")
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SpatialElevationTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Validates that clicking NavigationRail orbiter items scrolls the LazyColumn list. */
    @Test
    fun orbiterElevation_navigationRail_scrollsListContent() {
        composeTestRule.setContent { SpatialElevationContent() }

        // Initially, top item exists in the composition
        composeTestRule.onNodeWithText("Test item 0").assertExists()

        // Click "Down" to scroll list forward; top item should be scrolled off and unmounted
        composeTestRule.onNodeWithText("Down").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithText("Test item 0").assertDoesNotExist()

        // Click "Top" to scroll back to the beginning; top item should be mounted again
        composeTestRule.onNodeWithText("Top").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithText("Test item 0").assertExists()

        // Click "Down" to scroll away from top again
        composeTestRule.onNodeWithText("Down").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithText("Test item 0").assertDoesNotExist()

        // Click "Up" to scroll backward to top
        composeTestRule.onNodeWithText("Up").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithText("Test item 0").assertExists()
    }

    /** Validates that the primary layout, orbiters, and elevated action buttons are rendered. */
    @Test
    fun orbiterElevation_initialLayout_rendersOrbitersAndElevationControls() {
        composeTestRule.setContent { SpatialElevationContent() }

        // Validate navigation rail orbiter items exist
        composeTestRule.onNodeWithContentDescription("Up", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Menu", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Down", useUnmergedTree = true).assertExists()

        // Validate icon menu ornament items exist
        composeTestRule.onNodeWithContentDescription("Home").assertExists()
        composeTestRule.onNodeWithContentDescription("Search").assertExists()
        composeTestRule.onNodeWithContentDescription("ShoppingCart").assertExists()

        // Validate elevated action buttons
        composeTestRule.onNodeWithText("Show Popup Test").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show Dialog Popup Test").assertIsDisplayed()

        // Validate initial content list items
        composeTestRule.onNodeWithText("Test item 0").assertIsDisplayed()
    }

    /**
     * Validates that clicking the dialog button presents an elevated SpatialDialog and dismisses
     * it.
     */
    @Test
    fun orbiterElevation_dialogPopup_showsAndDismisses() {
        composeTestRule.setContent { SpatialElevationContent() }

        composeTestRule.onNodeWithText("Show Dialog Popup Test").performClick()

        composeTestRule.onNodeWithText("This is a popup dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dismiss").assertIsDisplayed()

        composeTestRule.onNodeWithText("Dismiss").performClick()

        composeTestRule.onNodeWithText("This is a popup dialog").assertDoesNotExist()
    }

    /** Validates that clicking the popup button presents a SpatialPopup and closes it. */
    @Test
    fun orbiterElevation_spatialPopup_showsAndDismisses() {
        composeTestRule.setContent { SpatialElevationContent() }

        composeTestRule.onNodeWithText("Show Popup Test").performClick()

        composeTestRule
            .onNodeWithText("This is a popup: click anywhere to exit")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Close").assertIsDisplayed()

        composeTestRule.onNodeWithText("Close").performClick()

        composeTestRule
            .onNodeWithText("This is a popup: click anywhere to exit")
            .assertDoesNotExist()
    }

    /** Validates expanding the icon menu ornament on the right. */
    @Test
    fun orbiterElevation_iconMenuOrnament_expandsOnItemClick() {
        composeTestRule.setContent { SpatialElevationContent() }

        composeTestRule.onNodeWithContentDescription("Search").performClick()

        composeTestRule.onNodeWithText("Opening additional context for Search.").assertExists()
    }

    /** Validates that clicking the edit button allows editing the item text and updates it. */
    @Test
    fun orbiterElevation_editItem_updatesContentText() {
        composeTestRule.setContent { SpatialElevationContent() }

        composeTestRule.onNodeWithText("Test item 0").assertIsDisplayed()

        // Click the first edit icon button
        composeTestRule.onAllNodesWithContentDescription("Edit").onFirst().performClick()

        // Replace text in the text field
        composeTestRule.onNodeWithText("Test item 0").performTextReplacement("Updated item 0")

        // Click Done
        composeTestRule.onNodeWithText("Done").performClick()

        // Verify updated text is displayed
        composeTestRule.onNodeWithText("Updated item 0").assertIsDisplayed()
    }

    @Composable
    private fun SpatialElevationContent() {
        var showDialog by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var shouldExpand by remember { mutableStateOf(false) }
        var contentText by remember { mutableStateOf("Opening additional context.") }
        var showPopup by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                floatingActionButton = {
                    SpatialElevation(SpatialElevationLevel.Level2) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Button(onClick = { showPopup = true }) { Text("Show Popup Test") }
                            Button(onClick = { showDialog = true }) {
                                Text("Show Dialog Popup Test")
                            }
                        }
                    }
                }
            ) { padding ->
                LazyColumn(modifier = Modifier.padding(padding), state = listState) {
                    items(10) { RowItem { EditItem(it, modifier = Modifier.padding(20.dp)) } }
                }
            }

            // Left navigation rail orbiter
            Orbiter(
                position =
                    OrbiterPosition.CenterStart(
                        EdgeAlignment.Outside,
                        offset = DpVolumeOffset(x = 0.dp, y = 0.dp, z = OrbiterDefaults.Elevation),
                    )
            ) {
                NavigationRail(
                    modifier =
                        Modifier.width(80.dp)
                            .height(IntrinsicSize.Min)
                            .clip(RoundedCornerShape(20.dp))
                ) {
                    NavigationRailItem(
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(
                                    (listState.firstVisibleItemIndex -
                                            listState.layoutInfo.visibleItemsInfo.size)
                                        .coerceAtLeast(0)
                                )
                            }
                        },
                        icon = { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Up") },
                        label = { Text("Up") },
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                        icon = { Icon(Icons.Rounded.Menu, contentDescription = "Menu") },
                        label = { Text("Top") },
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(
                                    (listState.firstVisibleItemIndex +
                                            listState.layoutInfo.visibleItemsInfo.size)
                                        .coerceAtMost(listState.layoutInfo.totalItemsCount - 1)
                                )
                            }
                        },
                        icon = {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Down")
                        },
                        label = { Text("Down") },
                    )
                }
            }

            // Right icon menu ornament orbiter
            Orbiter(
                position =
                    OrbiterPosition.CenterEnd(
                        EdgeAlignment.Inside,
                        offset = DpVolumeOffset(x = 0.dp, y = 0.dp, z = OrbiterDefaults.Elevation),
                    )
            ) {
                Row(
                    modifier = Modifier.animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (shouldExpand) {
                        Card(Modifier.widthIn(max = 320.dp)) {
                            Text(
                                text = contentText,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(Modifier.size(16.dp))
                    }
                    IconMenuOrnament(
                        onClick = {
                            shouldExpand = true
                            contentText = "Opening additional context for $it."
                        }
                    )
                }
            }
        }

        if (showPopup) {
            SpatialPopup(alignment = Alignment.Center, onDismissRequest = { showPopup = false }) {
                Box(
                    Modifier.size(250.dp, 100.dp)
                        .background(Color.DarkGray, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "This is a popup: click anywhere to exit",
                            modifier = Modifier.padding(10.dp),
                        )
                        Button(onClick = { showPopup = false }) { Text("Close") }
                    }
                }
            }
        }

        if (showDialog) {
            SpatialDialog(onDismissRequest = { showDialog = false }) {
                Surface(color = Color.White, shape = RoundedCornerShape(5.dp)) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("This is a popup dialog", modifier = Modifier.padding(10.dp))
                        Button(onClick = { showDialog = false }) { Text("Dismiss") }
                    }
                }
            }
        }
    }

    @Composable
    private fun IconMenuOrnament(onClick: (String) -> Unit) {
        Card(shape = RoundedCornerShape(10.dp)) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = { onClick("Home") },
                    modifier = Modifier.semantics { contentDescription = "Home" },
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                }
                IconButton(
                    onClick = { onClick("Search") },
                    modifier = Modifier.semantics { contentDescription = "Search" },
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
                IconButton(
                    onClick = { onClick("ShoppingCart") },
                    modifier = Modifier.semantics { contentDescription = "ShoppingCart" },
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                }
            }
        }
    }

    @Composable
    private fun RowItem(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Surface(
            color = Color.Gray,
            modifier = modifier.padding(8.dp).fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            content = content,
        )
    }

    @Composable
    private fun EditItem(index: Int, modifier: Modifier = Modifier) {
        var isEditing by remember { mutableStateOf(false) }
        var itemText by rememberSaveable { mutableStateOf("Test item $index") }

        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEditing) {
                TextField(
                    value = itemText,
                    onValueChange = { itemText = it },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { isEditing = false }) { Text("Done") }
            } else {
                Text(itemText)
                IconButton(onClick = { isEditing = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        }
    }
}
