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

import androidx.compose.Composable
import androidx.test.filters.SmallTest
import androidx.ui.text.LastBaseline
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.globalPosition
import androidx.ui.core.onChildPositioned
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Box
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.foundation.currentTextStyle
import androidx.ui.graphics.Color
import androidx.ui.graphics.painter.ColorPainter
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByText
import androidx.ui.text.TextStyle
import androidx.ui.unit.Density
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import androidx.ui.unit.sp
import androidx.ui.unit.toPx
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.roundToInt

@SmallTest
@RunWith(JUnit4::class)
class AppBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val appBarHeight = 56.dp

    @Test
    fun topAppBar_expandsToScreen() {
        val dm = composeTestRule.displayMetrics
        composeTestRule
            .setMaterialContentAndCollectSizes {
                TopAppBar(title = { Text("Title") })
            }
            .assertHeightEqualsTo(appBarHeight)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun topAppBar_withTitle() {
        val title = "Title"
        composeTestRule.setMaterialContent {
            TopAppBar(title = { Text(title) })
        }
        findByText(title).assertIsDisplayed()
    }

    @Test
    fun topAppBar_default_positioning() {
        var appBarCoords: LayoutCoordinates? = null
        var navigationIconCoords: LayoutCoordinates? = null
        var titleCoords: LayoutCoordinates? = null
        // Position of the baseline relative to the top of the text
        var titleLastBaselineRelativePosition: Float? = null
        var actionCoords: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Box(Modifier.onChildPositioned { appBarCoords = it }) {
                TopAppBar(
                    navigationIcon = {
                        FakeIcon(Modifier.onPositioned { navigationIconCoords = it })
                    },
                    title = {
                        Text("title", Modifier.onPositioned { coords: LayoutCoordinates ->
                            titleCoords = coords
                            titleLastBaselineRelativePosition =
                                coords[LastBaseline]!!.toPx().value
                        })
                    },
                    actions = {
                        FakeIcon(Modifier.onPositioned { actionCoords = it })
                    }
                )
            }
        }

        composeTestRule.runOnIdleComposeWithDensity {
            val appBarBottomEdgeY = appBarCoords!!.globalPosition.y +
                    appBarCoords!!.size.height.value

            // Navigation icon should be 4.dp from the start
            val navigationIconPositionX = navigationIconCoords!!.globalPosition.x
            val navigationIconExpectedPositionX = AppBarStartAndEndPadding.toIntPx().toPx().value
            assertThat(navigationIconPositionX).isEqualTo(navigationIconExpectedPositionX)

            // Navigation icon should be 4.dp from the bottom
            val navigationIconPositionY = navigationIconCoords!!.globalPosition.y
            val navigationIconExpectedPositionY = (appBarBottomEdgeY -
                    AppBarStartAndEndPadding.toPx() - FakeIconSize.toPx()
            ).roundToInt().toFloat()
            assertThat(navigationIconPositionY).isEqualTo(navigationIconExpectedPositionY)

            // Title should be 72.dp from the start
            val titlePositionX = titleCoords!!.globalPosition.x
            // 4.dp padding for the whole app bar + 68.dp inset
            val titleExpectedPositionX = (4.dp.toIntPx() + 68.dp.toIntPx()).value.toFloat()
            assertThat(titlePositionX).isEqualTo(titleExpectedPositionX)

            // Absolute position of the baseline
            val titleLastBaselinePositionY = titleLastBaselineRelativePosition!! +
                    titleCoords!!.globalPosition.y
            // Baseline should be 20.sp from the bottom of the app bar
            val titleExpectedLastBaselinePositionY = (appBarBottomEdgeY.px - 20.sp.toIntPx()
                .toPx()).value
            assertThat(titleLastBaselinePositionY).isEqualTo(titleExpectedLastBaselinePositionY)

            // Action should be placed at the end
            val actionPositionX = actionCoords!!.globalPosition.x
            val actionExpectedPositionX =
                expectedActionPosition(appBarCoords!!.size.width.toPx().value)
            assertThat(actionPositionX).isEqualTo(actionExpectedPositionX)

            // Action should be 4.dp from the bottom
            val actionPositionY = actionCoords!!.globalPosition.y
            val actionExpectedPositionY = (appBarBottomEdgeY - AppBarStartAndEndPadding.toPx() -
                FakeIconSize.toPx()
            ).roundToInt().toFloat()
            assertThat(actionPositionY).isEqualTo(actionExpectedPositionY)
        }
    }

    @Test
    fun topAppBar_noNavigationIcon_positioning() {
        var appBarCoords: LayoutCoordinates? = null
        var titleCoords: LayoutCoordinates? = null
        var actionCoords: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Box(Modifier.onChildPositioned { appBarCoords = it }) {
                TopAppBar(
                    title = {
                        Text("title",
                            Modifier.onPositioned { titleCoords = it })
                    },
                    actions = {
                        FakeIcon(Modifier.onPositioned { actionCoords = it })
                    }
                )
            }
        }

        composeTestRule.runOnIdleComposeWithDensity {
            // Title should now be placed 16.dp from the start, as there is no navigation icon
            val titlePositionX = titleCoords!!.globalPosition.x
            // 4.dp padding for the whole app bar + 12.dp inset
            val titleExpectedPositionX = (4.dp.toIntPx() + 12.dp.toIntPx()).toPx().value
            assertThat(titlePositionX).isEqualTo(titleExpectedPositionX)

            // Action should still be placed at the end
            val actionPositionX = actionCoords!!.globalPosition.x
            val actionExpectedPositionX =
                expectedActionPosition(appBarCoords!!.size.width.toPx().value)
            assertThat(actionPositionX).isEqualTo(actionExpectedPositionX)
        }
    }

    @Test
    fun topAppBar_titleDefaultStyle() {
        var textStyle: TextStyle? = null
        var h6Style: TextStyle? = null
        composeTestRule.setMaterialContent {
            Box {
                TopAppBar(
                    title = {
                        Text("App Bar Title")
                        textStyle = currentTextStyle()
                        h6Style = MaterialTheme.typography.h6
                    }
                )
            }
        }
        assertThat(textStyle!!.fontSize).isEqualTo(h6Style!!.fontSize)
        assertThat(textStyle!!.fontFamily).isEqualTo(h6Style!!.fontFamily)
    }

    @Test
    fun bottomAppBar_expandsToScreen() {
        val dm = composeTestRule.displayMetrics
        composeTestRule
            .setMaterialContentAndCollectSizes {
                BottomAppBar {}
            }
            .assertHeightEqualsTo(appBarHeight)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun bottomAppBar_default_positioning() {
        var appBarCoords: LayoutCoordinates? = null
        var childCoords: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Box(Modifier.onChildPositioned { appBarCoords = it }) {
                BottomAppBar {
                    FakeIcon(Modifier.onPositioned { childCoords = it })
                }
            }
        }

        composeTestRule.runOnIdleComposeWithDensity {
            // Child icon should be 4.dp from the start
            val childIconPositionX = childCoords!!.globalPosition.x
            val childIconExpectedPositionX = AppBarStartAndEndPadding.toIntPx().toPx().value
            assertThat(childIconPositionX).isEqualTo(childIconExpectedPositionX)

            val appBarBottomEdgeY = appBarCoords!!.globalPosition.y +
                    appBarCoords!!.size.height.value

            // Child icon should be 4.dp from the bottom
            val childIconPositionY = childCoords!!.globalPosition.y
            val childIconExpectedPositionY = (appBarBottomEdgeY - AppBarStartAndEndPadding.toPx() -
                FakeIconSize.toPx()
            ).roundToInt().toFloat()
            assertThat(childIconPositionY).isEqualTo(childIconExpectedPositionY)
        }
    }

    /**
     * [IconButton] that just draws a red box, to simulate a real icon for testing positions.
     */
    private val FakeIcon = @Composable { modifier: Modifier ->
        IconButton(onClick = {}, modifier = modifier) {
            Icon(ColorPainter(Color.Red))
        }
    }

    private fun Density.expectedActionPosition(appBarWidth: Float): Float {
        return appBarWidth - AppBarStartAndEndPadding.toIntPx().value -
                FakeIconSize.toIntPx().value
    }

    private val AppBarStartAndEndPadding = 4.dp

    private val FakeIconSize = 48.dp
}
