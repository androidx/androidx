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
import androidx.compose.composer
import androidx.test.filters.SmallTest
import androidx.ui.core.dp
import androidx.ui.core.withDensity
import androidx.ui.layout.Container
import com.google.common.truth.Truth
import androidx.compose.unaryPlus
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.Text
import androidx.ui.core.currentTextStyle
import androidx.ui.core.ipx
import androidx.ui.core.round
import androidx.ui.core.toPx
import androidx.ui.foundation.ColoredRect
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.material.BottomAppBar.FabConfiguration
import androidx.ui.material.BottomAppBar.FabPosition
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.testTag
import androidx.ui.text.TextStyle
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertIsVisible
import androidx.ui.test.createComposeRule
import androidx.ui.test.findAllByTag
import androidx.ui.test.findByText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

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
                TopAppBar<Nothing>()
            }
            .assertHeightEqualsTo(appBarHeight)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun topAppBar_withTitle() {
        val title = "Title"
        composeTestRule.setMaterialContent {
            TopAppBar<Nothing>(title = { Text(title) })
        }
        findByText(title).assertIsVisible()
    }

    @Test
    fun topAppBar_default_positioning() {
        var appBarCoords: LayoutCoordinates? = null
        var navigationIconCoords: LayoutCoordinates? = null
        var titleCoords: LayoutCoordinates? = null
        var actionCoords: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Container {
                OnChildPositioned(onPositioned = { coords ->
                    appBarCoords = coords
                }) {
                    TopAppBar(
                        navigationIcon = {
                            OnChildPositioned(onPositioned = { coords ->
                                navigationIconCoords = coords
                            }) {
                                FakeIcon()
                            }
                        },
                        title = {
                            OnChildPositioned(onPositioned = { coords ->
                                titleCoords = coords
                            }) {
                                Text("title")
                            }
                        },
                        contextualActions = createImageList(1),
                        action = {
                            OnChildPositioned(onPositioned = { coords ->
                                actionCoords = coords
                            }) { it() }
                        }
                    )
                }
            }
        }

        withDensity(composeTestRule.density) {
            // Navigation icon should be 16.dp from the start
            val navigationIconPositionX = navigationIconCoords!!.localToGlobal(PxPosition.Origin).x
            val navigationIconExpectedPositionX = 16.dp.toIntPx().toPx()
            Truth.assertThat(navigationIconPositionX).isEqualTo(navigationIconExpectedPositionX)

            // Title should be 72.dp from the start
            val titlePositionX = titleCoords!!.localToGlobal(PxPosition.Origin).x
            val titleExpectedPositionX = 72.dp.toIntPx().toPx()
            Truth.assertThat(titlePositionX).isEqualTo(titleExpectedPositionX)

            // Action should be placed at the end
            val actionPositionX = actionCoords!!.localToGlobal(PxPosition.Origin).x
            val actionExpectedPositionX =
                appBarCoords!!.size.width - 16.dp.toIntPx() - 24.dp.toIntPx()
            Truth.assertThat(actionPositionX).isEqualTo(actionExpectedPositionX)
        }
    }

    @Test
    fun topAppBar_noNavigationIcon_positioning() {
        var appBarCoords: LayoutCoordinates? = null
        var titleCoords: LayoutCoordinates? = null
        var actionCoords: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Container {
                OnChildPositioned(onPositioned = { coords ->
                    appBarCoords = coords
                }) {
                    TopAppBar(
                        title = {
                            OnChildPositioned(onPositioned = { coords ->
                                titleCoords = coords
                            }) {
                                Text("title")
                            }
                        },
                        contextualActions = createImageList(1),
                        action = {
                            OnChildPositioned(onPositioned = { coords ->
                                actionCoords = coords
                            }) { it() }
                        }
                    )
                }
            }
        }

        withDensity(composeTestRule.density) {
            // Title should now be placed 16.dp from the start, as there is no navigation icon
            val titlePositionX = titleCoords!!.localToGlobal(PxPosition.Origin).x
            val titleExpectedPositionX = 16.dp.toIntPx().toPx()
            Truth.assertThat(titlePositionX).isEqualTo(titleExpectedPositionX)

            // Action should still be placed at the end
            val actionPositionX = actionCoords!!.localToGlobal(PxPosition.Origin).x
            val actionExpectedPositionX =
                appBarCoords!!.size.width - 16.dp.toIntPx() - 24.dp.toIntPx()
            Truth.assertThat(actionPositionX).isEqualTo(actionExpectedPositionX)
        }
    }

    @Test
    fun topAppBar_oneAction() {
        val tag = "action"
        val numberOfActions = 1
        composeTestRule.setMaterialContent {
            Container {
                TopAppBar(
                    contextualActions = createImageList(numberOfActions),
                    action = { action ->
                        Semantics(properties = { testTag = tag }) {
                            action()
                        }
                    }
                )
            }
        }

        findAllByTag(tag).assertCountEquals(numberOfActions)
    }

    @Test
    fun topAppBar_fiveActions_onlyTwoShouldBeVisible() {
        val tag = "action"
        val numberOfActions = 5
        val maxNumberOfActions = 2
        composeTestRule.setMaterialContent {
            Container {
                TopAppBar(
                    contextualActions = createImageList(numberOfActions),
                    action = { action ->
                        Semantics(properties = { testTag = tag }) {
                            action()
                        }
                    }
                )
            }
        }

        findAllByTag(tag).assertCountEquals(maxNumberOfActions)
    }

    @Test
    fun topAppBar_titleDefaultStyle() {
        var textStyle: TextStyle? = null
        var h6Style: TextStyle? = null
        composeTestRule.setMaterialContent {
            Container {
                TopAppBar<Nothing>(
                    title = {
                        textStyle = +currentTextStyle()
                        h6Style = +themeTextStyle { h6 }
                    }
                )
            }
        }
        Truth.assertThat(textStyle!!.fontSize).isEqualTo(h6Style!!.fontSize)
        Truth.assertThat(textStyle!!.fontFamily).isEqualTo(h6Style!!.fontFamily)
    }

    @Test
    fun bottomAppBar_expandsToScreen() {
        val dm = composeTestRule.displayMetrics
        composeTestRule
            .setMaterialContentAndCollectSizes {
                BottomAppBar<Nothing>()
            }
            .assertHeightEqualsTo(appBarHeight)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun bottomAppBar_noNavigationIcon_positioning() {
        var appBarCoords: LayoutCoordinates? = null
        var actionCoords: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Container {
                OnChildPositioned(onPositioned = { coords ->
                    appBarCoords = coords
                }) {
                    BottomAppBar(
                        contextualActions = createImageList(1),
                        action = {
                            OnChildPositioned(onPositioned = { coords ->
                                actionCoords = coords
                            }) { it() }
                        }
                    )
                }
            }
        }

        withDensity(composeTestRule.density) {
            // Action should still be placed at the end, even though there is no navigation icon
            val actionPositionX = actionCoords!!.localToGlobal(PxPosition.Origin).x
            val actionExpectedPositionX = appBarCoords!!.size.width.round().toPx() -
                    16.dp.toIntPx().toPx() - 24.dp.toIntPx().toPx()
            Truth.assertThat(actionPositionX).isEqualTo(actionExpectedPositionX)
        }
    }

    @Test
    fun bottomAppBar_noFab_positioning() {
        var appBarCoords: LayoutCoordinates? = null
        var navigationIconCoords: LayoutCoordinates? = null
        var actionCoords: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Container {
                OnChildPositioned(onPositioned = { coords ->
                    appBarCoords = coords
                }) {
                    BottomAppBar(
                        navigationIcon = {
                            OnChildPositioned(onPositioned = { coords ->
                                navigationIconCoords = coords
                            }) {
                                FakeIcon()
                            }
                        },
                        contextualActions = createImageList(1),
                        action = {
                            OnChildPositioned(onPositioned = { coords ->
                                actionCoords = coords
                            }) { it() }
                        }
                    )
                }
            }
        }

        withDensity(composeTestRule.density) {
            // Navigation icon should be at the beginning
            val navigationIconPositionX = navigationIconCoords!!.localToGlobal(PxPosition.Origin).x
            val navigationIconExpectedPositionX = 16.dp.toIntPx().toPx()
            Truth.assertThat(navigationIconPositionX).isEqualTo(navigationIconExpectedPositionX)

            // Action should be placed at the end
            val actionPositionX = actionCoords!!.localToGlobal(PxPosition.Origin).x
            val actionExpectedPositionX = appBarCoords!!.size.width.round().toPx() -
                    16.dp.toIntPx().toPx() - 24.dp.toIntPx().toPx()
            Truth.assertThat(actionPositionX).isEqualTo(actionExpectedPositionX)
        }
    }

    @Test
    fun bottomAppBar_centerFab_positioning() {
        var appBarCoords: LayoutCoordinates? = null
        var navigationIconCoords: LayoutCoordinates? = null
        var fabCoords: LayoutCoordinates? = null
        var actionCoords: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Container {
                OnChildPositioned(onPositioned = { coords ->
                    appBarCoords = coords
                }) {
                    BottomAppBar(
                        navigationIcon = {
                            OnChildPositioned(onPositioned = { coords ->
                                navigationIconCoords = coords
                            }) {
                                FakeIcon()
                            }
                        },
                        fabConfiguration = FabConfiguration(FabPosition.Center) {
                            OnChildPositioned(onPositioned = { coords ->
                                fabCoords = coords
                            }) {
                                FakeIcon()
                            }
                        },
                        contextualActions = createImageList(1),
                        action = {
                            OnChildPositioned(onPositioned = { coords ->
                                actionCoords = coords
                            }) { it() }
                        }
                    )
                }
            }
        }

        withDensity(composeTestRule.density) {
            // Navigation icon should be at the beginning
            val navigationIconPositionX = navigationIconCoords!!.localToGlobal(PxPosition.Origin).x
            val navigationIconExpectedPositionX = 16.dp.toIntPx().toPx()
            Truth.assertThat(navigationIconPositionX).isEqualTo(navigationIconExpectedPositionX)

            // FAB should be placed in the center
            val fabPositionX = fabCoords!!.localToGlobal(PxPosition.Origin).x
            val fabExpectedPositionX =
                ((appBarCoords!!.size.width - 24.dp.toPx()) / 2).round().toPx()
            Truth.assertThat(fabPositionX).isEqualTo(fabExpectedPositionX)

            // Action should be placed at the end
            val actionPositionX = actionCoords!!.localToGlobal(PxPosition.Origin).x
            val actionExpectedPositionX = appBarCoords!!.size.width.round().toPx() -
                    16.dp.toIntPx().toPx() - 24.dp.toIntPx().toPx()
            Truth.assertThat(actionPositionX).isEqualTo(actionExpectedPositionX)
        }
    }

    @Test
    fun bottomAppBar_centerCutoutFab_positioning() {
        var appBarCoords: LayoutCoordinates? = null
        var navigationIconCoords: LayoutCoordinates? = null
        var fabCoords: LayoutCoordinates? = null
        var actionCoords: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Container {
                OnChildPositioned(onPositioned = { coords ->
                    appBarCoords = coords
                }) {
                    BottomAppBar(
                        navigationIcon = {
                            OnChildPositioned(onPositioned = { coords ->
                                navigationIconCoords = coords
                            }) {
                                FakeIcon()
                            }
                        },
                        fabConfiguration = FabConfiguration(FabPosition.Center, CircleShape) {
                            OnChildPositioned(onPositioned = { coords ->
                                fabCoords = coords
                            }) {
                                FakeIcon()
                            }
                        },
                        contextualActions = createImageList(1),
                        action = {
                            OnChildPositioned(onPositioned = { coords ->
                                actionCoords = coords
                            }) { it() }
                        }
                    )
                }
            }
        }

        withDensity(composeTestRule.density) {
            // Navigation icon should be at the beginning
            val navigationIconPositionX = navigationIconCoords!!.localToGlobal(PxPosition.Origin).x
            val navigationIconExpectedPositionX = 16.dp.toIntPx().toPx()
            Truth.assertThat(navigationIconPositionX).isEqualTo(navigationIconExpectedPositionX)

            // FAB should be placed in the center
            val fabPositionX = fabCoords!!.localToGlobal(PxPosition.Origin).x
            val fabExpectedPositionX =
                ((appBarCoords!!.size.width - 24.dp.toPx()) / 2).round().toPx()
            Truth.assertThat(fabPositionX).isEqualTo(fabExpectedPositionX)

            // Action should be placed at the end
            val actionPositionX = actionCoords!!.localToGlobal(PxPosition.Origin).x
            val actionExpectedPositionX = appBarCoords!!.size.width.round().toPx() -
                    16.dp.toIntPx().toPx() - 24.dp.toIntPx().toPx()
            Truth.assertThat(actionPositionX).isEqualTo(actionExpectedPositionX)
        }
    }

    @Test
    fun bottomAppBar_endFab_positioning() {
        var appBarCoords: LayoutCoordinates? = null
        var fabCoords: LayoutCoordinates? = null
        var actionCoords: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Container {
                OnChildPositioned(onPositioned = { coords ->
                    appBarCoords = coords
                }) {
                    BottomAppBar(
                        fabConfiguration = FabConfiguration(FabPosition.End) {
                            OnChildPositioned(onPositioned = { coords ->
                                fabCoords = coords
                            }) {
                                FakeIcon()
                            }
                        },
                        contextualActions = createImageList(1),
                        action = {
                            OnChildPositioned(onPositioned = { coords ->
                                actionCoords = coords
                            }) { it() }
                        }
                    )
                }
            }
        }

        withDensity(composeTestRule.density) {
            // Action should be placed at the start
            val actionPositionX = actionCoords!!.localToGlobal(PxPosition.Origin).x
            val actionExpectedPositionX = 16.dp.toIntPx().toPx()
            Truth.assertThat(actionPositionX).isEqualTo(actionExpectedPositionX)

            // FAB should be placed at the end
            val fabPositionX = fabCoords!!.localToGlobal(PxPosition.Origin).x
            val fabExpectedPositionX = appBarCoords!!.size.width.round().toPx() -
                    16.dp.toIntPx().toPx() - 24.dp.toIntPx().toPx()
            Truth.assertThat(fabPositionX).isEqualTo(fabExpectedPositionX)
        }
    }

    @Test
    fun bottomAppBar_endCutoutFab_positioning() {
        var appBarCoords: LayoutCoordinates? = null
        var fabCoords: LayoutCoordinates? = null
        var actionCoords: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Container {
                OnChildPositioned(onPositioned = { coords ->
                    appBarCoords = coords
                }) {
                    BottomAppBar(
                        fabConfiguration = FabConfiguration(FabPosition.End, CircleShape) {
                            OnChildPositioned(onPositioned = { coords ->
                                fabCoords = coords
                            }) {
                                FakeIcon()
                            }
                        },
                        contextualActions = createImageList(1),
                        action = {
                            OnChildPositioned(onPositioned = { coords ->
                                actionCoords = coords
                            }) { it() }
                        }
                    )
                }
            }
        }

        withDensity(composeTestRule.density) {
            // Action should be placed at the start
            val actionPositionX = actionCoords!!.localToGlobal(PxPosition.Origin).x
            val actionExpectedPositionX = 16.dp.toIntPx().toPx()
            Truth.assertThat(actionPositionX).isEqualTo(actionExpectedPositionX)

            // FAB should be placed at the end
            val fabPositionX = fabCoords!!.localToGlobal(PxPosition.Origin).x
            val fabExpectedPositionX = appBarCoords!!.size.width.round().toPx() -
                    16.dp.toIntPx().toPx() - 24.dp.toIntPx().toPx()
            Truth.assertThat(fabPositionX).isEqualTo(fabExpectedPositionX)
        }
    }

    @Test
    fun bottomAppBar_oneAction() {
        val tag = "action"
        val numberOfActions = 1
        composeTestRule.setMaterialContent {
            Container {
                BottomAppBar(
                    contextualActions = createImageList(numberOfActions),
                    action = { action ->
                        Semantics(properties = { testTag = tag }) {
                            action()
                        }
                    }
                )
            }
        }

        findAllByTag(tag).assertCountEquals(numberOfActions)
    }

    @Test
    fun bottomAppBar_fiveActions_onlyFourShouldBeVisible() {
        val tag = "action"
        val numberOfActions = 5
        val maxNumberOfActions = 4
        composeTestRule.setMaterialContent {
            Container {
                BottomAppBar(
                    contextualActions = createImageList(numberOfActions),
                    action = { action ->
                        Semantics(properties = { testTag = tag }) {
                            action()
                        }
                    }
                )
            }
        }

        findAllByTag(tag).assertCountEquals(maxNumberOfActions)
    }

    private fun createImageList(count: Int) =
        List<@Composable() () -> Unit>(count) { { FakeIcon() } }

    // Render a red rectangle to simulate an icon
    @Composable
    private fun FakeIcon() = ColoredRect(Color.Red, 24.dp, 24.dp)
}
