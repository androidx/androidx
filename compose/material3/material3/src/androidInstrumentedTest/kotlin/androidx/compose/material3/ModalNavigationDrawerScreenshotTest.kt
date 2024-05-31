/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ModalNavigationDrawerScreenshotTest {

    @Suppress("DEPRECATION") @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private fun ComposeContentTestRule.setnavigationDrawer(drawerValue: DrawerValue) {
        setMaterialContent(lightColorScheme()) {
            Box(Modifier.requiredSize(400.dp, 32.dp).testTag(ContainerTestTag)) {
                ModalNavigationDrawer(
                    drawerState = rememberDrawerState(drawerValue),
                    drawerContent = {
                        ModalDrawerSheet { Spacer(modifier = Modifier.fillMaxSize()) }
                    },
                    content = {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    }
                )
            }
        }
    }

    private fun ComposeContentTestRule.setDarknavigationDrawer(drawerValue: DrawerValue) {
        setMaterialContent(darkColorScheme()) {
            Surface {
                Box(Modifier.requiredSize(400.dp, 32.dp).testTag(ContainerTestTag)) {
                    ModalNavigationDrawer(
                        drawerState = rememberDrawerState(drawerValue),
                        drawerContent = {
                            ModalDrawerSheet { Spacer(modifier = Modifier.fillMaxSize()) }
                        },
                        content = {
                            Box(
                                Modifier.fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            )
                        }
                    )
                }
            }
        }
    }

    @Test
    fun lightTheme_navigationDrawer_closed() {
        rule.setnavigationDrawer(DrawerValue.Closed)
        assertScreenshotAgainstGolden("navigationDrawer_closed")
    }

    @Test
    fun lightTheme_navigationDrawer_open() {
        rule.setnavigationDrawer(DrawerValue.Open)
        assertScreenshotAgainstGolden("navigationDrawer_light_opened")
    }

    @Test
    fun darkTheme_navigationDrawer_open() {
        rule.setDarknavigationDrawer(DrawerValue.Open)
        assertScreenshotAgainstGolden("navigationDrawer_dark_opened")
    }

    private fun assertScreenshotAgainstGolden(goldenName: String) {
        rule
            .onNodeWithTag("container")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    @Test
    fun predictiveBack_navigationDrawer_progress0AndSwipeEdgeLeft() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalNavigationDrawerPredictiveBack(progress = 0f, swipeEdgeLeft = true)
        }
        assertScreenshotAgainstGolden("navigationDrawer_predictiveBack_progress0AndSwipeEdgeLeft")
    }

    @Test
    fun predictiveBack_navigationDrawer_progress25AndSwipeEdgeLeft() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalNavigationDrawerPredictiveBack(progress = 0.25f, swipeEdgeLeft = true)
        }
        assertScreenshotAgainstGolden("navigationDrawer_predictiveBack_progress25AndSwipeEdgeLeft")
    }

    @Test
    fun predictiveBack_navigationDrawer_progress50AndSwipeEdgeLeft() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalNavigationDrawerPredictiveBack(progress = 0.5f, swipeEdgeLeft = true)
        }
        assertScreenshotAgainstGolden("navigationDrawer_predictiveBack_progress50AndSwipeEdgeLeft")
    }

    @Test
    fun predictiveBack_navigationDrawer_progress75AndSwipeEdgeLeft() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalNavigationDrawerPredictiveBack(progress = 0.75f, swipeEdgeLeft = true)
        }
        assertScreenshotAgainstGolden("navigationDrawer_predictiveBack_progress75AndSwipeEdgeLeft")
    }

    @Test
    fun predictiveBack_navigationDrawer_progress100AndSwipeEdgeLeft() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalNavigationDrawerPredictiveBack(progress = 1f, swipeEdgeLeft = true)
        }
        assertScreenshotAgainstGolden("navigationDrawer_predictiveBack_progress100AndSwipeEdgeLeft")
    }

    @Test
    fun predictiveBack_navigationDrawer_progress0AndSwipeEdgeRight() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalNavigationDrawerPredictiveBack(progress = 0f, swipeEdgeLeft = false)
        }
        assertScreenshotAgainstGolden("navigationDrawer_predictiveBack_progress0AndSwipeEdgeRight")
    }

    @Test
    fun predictiveBack_navigationDrawer_progress25AndSwipeEdgeRight() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalNavigationDrawerPredictiveBack(progress = 0.25f, swipeEdgeLeft = false)
        }
        assertScreenshotAgainstGolden("navigationDrawer_predictiveBack_progress25AndSwipeEdgeRight")
    }

    @Test
    fun predictiveBack_navigationDrawer_progress50AndSwipeEdgeRight() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalNavigationDrawerPredictiveBack(progress = 0.5f, swipeEdgeLeft = false)
        }
        assertScreenshotAgainstGolden("navigationDrawer_predictiveBack_progress50AndSwipeEdgeRight")
    }

    @Test
    fun predictiveBack_navigationDrawer_progress75AndSwipeEdgeRight() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalNavigationDrawerPredictiveBack(progress = 0.75f, swipeEdgeLeft = false)
        }
        assertScreenshotAgainstGolden("navigationDrawer_predictiveBack_progress75AndSwipeEdgeRight")
    }

    @Test
    fun predictiveBack_navigationDrawer_progress100AndSwipeEdgeRight() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalNavigationDrawerPredictiveBack(progress = 1f, swipeEdgeLeft = false)
        }
        assertScreenshotAgainstGolden(
            "navigationDrawer_predictiveBack_progress100AndSwipeEdgeRight"
        )
    }
}

private val ContainerTestTag = "container"

private val items =
    listOf(
        Icons.Default.AccountCircle,
        Icons.Default.Build,
        Icons.Default.Check,
        Icons.Default.DateRange,
        Icons.Default.Email,
        Icons.Default.Favorite,
        Icons.Default.Home,
        Icons.Default.Info,
        Icons.Default.Lock,
        Icons.Default.Notifications,
        Icons.Default.Place,
        Icons.Default.Refresh,
        Icons.Default.ShoppingCart,
        Icons.Default.ThumbUp,
        Icons.Default.Warning,
    )

@Composable
private fun ModalNavigationDrawerPredictiveBack(progress: Float, swipeEdgeLeft: Boolean) {
    val maxScaleXDistanceGrow: Float
    val maxScaleXDistanceShrink: Float
    val maxScaleYDistance: Float
    with(LocalDensity.current) {
        maxScaleXDistanceGrow = PredictiveBackDrawerMaxScaleXDistanceGrow.toPx()
        maxScaleXDistanceShrink = PredictiveBackDrawerMaxScaleXDistanceShrink.toPx()
        maxScaleYDistance = PredictiveBackDrawerMaxScaleYDistance.toPx()
    }

    val drawerPredictiveBackState =
        DrawerPredictiveBackState().apply {
            update(
                progress = progress,
                swipeEdgeLeft = swipeEdgeLeft,
                isRtl = false,
                maxScaleXDistanceGrow = maxScaleXDistanceGrow,
                maxScaleXDistanceShrink = maxScaleXDistanceShrink,
                maxScaleYDistance = maxScaleYDistance
            )
        }

    ModalNavigationDrawer(
        modifier = Modifier.testTag(ContainerTestTag),
        drawerState = rememberDrawerState(DrawerValue.Open),
        drawerContent = {
            // Use the internal DrawerSheet instead of ModalDrawerSheet so we can simulate different
            // back progress values for the test, and avoid the real PredictiveBackHandler.
            DrawerSheet(
                drawerPredictiveBackState,
                DrawerDefaults.windowInsets,
                Modifier,
                DrawerDefaults.shape,
                DrawerDefaults.modalContainerColor,
                contentColorFor(DrawerDefaults.modalContainerColor),
                DrawerDefaults.ModalDrawerElevation
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(12.dp))
                    items.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item, contentDescription = null) },
                            label = { Text(item.name) },
                            selected = item == Icons.Default.AccountCircle,
                            onClick = {},
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        },
        content = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) }
    )
}
