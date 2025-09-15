/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.NavigationDrawerTokens
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DismissibleNavigationDrawerTest {

    @get:Rule val rule = createComposeRule()

    private val NavigationDrawerWidth = NavigationDrawerTokens.ContainerWidth

    @Test
    fun dismissibleNavigationDrawer_testOffset_whenOpen() {
        rule.setMaterialContent(lightColorScheme()) {
            val drawerState = rememberDrawerState(DrawerValue.Open)
            DismissibleNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    DismissibleDrawerSheet { Box(Modifier.fillMaxSize().testTag("content")) }
                },
                content = {},
            )
        }

        rule.onNodeWithTag("content").assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun dismissibleNavigationDrawer_sheet_respectsContentPadding() {
        rule.setMaterialContent(lightColorScheme()) {
            val drawerState = rememberDrawerState(DrawerValue.Open)
            DismissibleNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    DismissibleDrawerSheet(windowInsets = WindowInsets(7.dp, 7.dp, 7.dp, 7.dp)) {
                        Box(Modifier.fillMaxSize().testTag("content"))
                    }
                },
                content = {},
            )
        }

        rule
            .onNodeWithTag("content")
            .assertLeftPositionInRootIsEqualTo(7.dp)
            .assertTopPositionInRootIsEqualTo(7.dp)
    }

    @Test
    fun dismissibleNavigationDrawer_testOffset_whenClosed() {
        rule.setMaterialContent(lightColorScheme()) {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            DismissibleNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    DismissibleDrawerSheet { Box(Modifier.fillMaxSize().testTag("content")) }
                },
                content = { Box(Modifier.fillMaxSize().testTag("ScreenContent")) },
            )
        }

        rule.onNodeWithTag("ScreenContent").assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun dismissibleNavigationDrawer_testOffset_customWidthSmaller_whenOpen() {
        val customWidth = 200.dp
        rule.setMaterialContent(lightColorScheme()) {
            val drawerState = rememberDrawerState(DrawerValue.Open)
            DismissibleNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    DismissibleDrawerSheet(Modifier.width(customWidth)) {
                        Box(Modifier.fillMaxSize().testTag("content"))
                    }
                },
                content = {},
            )
        }

        rule.onNodeWithTag("content").assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun dismissibleNavigationDrawer_testOffset_customWidthSmaller_whenClosed() {
        val customWidth = 200.dp
        lateinit var drawerState: DrawerState
        var customWidthInPx = 0f
        rule.setMaterialContent(lightColorScheme()) {
            customWidthInPx = LocalDensity.current.run { customWidth.toPx() }
            drawerState = rememberDrawerState(DrawerValue.Closed)
            Box(Modifier.fillMaxSize()) {
                DismissibleNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DismissibleDrawerSheet(Modifier.width(customWidth)) {
                            Box(Modifier.fillMaxSize().testTag("content"))
                        }
                    },
                    content = { Box(Modifier.fillMaxSize().testTag("ScreenContent")) },
                )
            }
        }

        assertThat(drawerState.currentOffset).isWithin(1f).of(-customWidthInPx)
    }

    @Test
    fun dismissibleNavigationDrawer_testOffset_customWidthLarger_whenOpen() {
        val customWidth = NavigationDrawerWidth + 20.dp
        val density = Density(0.5f)
        lateinit var coords: LayoutCoordinates
        rule.setMaterialContent(lightColorScheme()) {
            // Reduce density to ensure wide drawer fits on screen
            CompositionLocalProvider(LocalDensity provides density) {
                val drawerState = rememberDrawerState(DrawerValue.Open)
                DismissibleNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DismissibleDrawerSheet(Modifier.width(customWidth)) {
                            Box(Modifier.fillMaxSize().onGloballyPositioned { coords = it })
                        }
                    },
                    content = {},
                )
            }
        }

        rule.runOnIdle { assertThat(coords.positionOnScreen().x).isEqualTo(0f) }
    }

    @Test
    fun dismissibleNavigationDrawer_testOffset_customWidthLarger_whenClosed() {
        val customWidth = NavigationDrawerWidth + 20.dp
        var customWidthInPx = 0f
        lateinit var drawerState: DrawerState
        val density = Density(0.5f)
        rule.setMaterialContent(lightColorScheme()) {
            // Reduce density to ensure wide drawer fits on screen
            CompositionLocalProvider(LocalDensity provides density) {
                customWidthInPx = LocalDensity.current.run { customWidth.toPx() }
                drawerState = rememberDrawerState(DrawerValue.Closed)
                Box(Modifier.fillMaxSize()) {
                    DismissibleNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            DismissibleDrawerSheet(Modifier.width(customWidth)) {
                                Box(Modifier.fillMaxSize().testTag("content"))
                            }
                        },
                        content = { Box(Modifier.fillMaxSize().testTag("ScreenContent")) },
                    )
                }
            }
        }

        rule.runOnIdle {
            with(density) {
                assertThat(drawerState.currentOffset).isWithin(1f).of(-customWidthInPx)
            }
        }
    }

    @Test
    fun dismissibleNavigationDrawer_testWidth_whenOpen() {
        rule.setMaterialContent(lightColorScheme()) {
            val drawerState = rememberDrawerState(DrawerValue.Open)
            DismissibleNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    DismissibleDrawerSheet { Box(Modifier.fillMaxSize().testTag("content")) }
                },
                content = {},
            )
        }

        rule.onNodeWithTag("content").assertWidthIsEqualTo(NavigationDrawerWidth)
    }

    @Test
    @SmallTest
    fun dismissibleNavigationDrawer_hasPaneTitle() {
        lateinit var navigationMenu: String
        rule.setMaterialContent(lightColorScheme()) {
            DismissibleNavigationDrawer(
                drawerState = rememberDrawerState(DrawerValue.Open),
                drawerContent = {
                    DismissibleDrawerSheet(Modifier.testTag("navigationDrawerTag")) {
                        Box(Modifier.fillMaxSize())
                    }
                },
                content = {},
            )
            navigationMenu = getString(Strings.NavigationMenu)
        }

        rule
            .onNodeWithTag("navigationDrawerTag", useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, navigationMenu))
    }

    @Test
    @LargeTest
    fun dismissibleNavigationDrawer_openAndClose(): Unit =
        runBlocking(AutoTestFrameClock()) {
            lateinit var drawerState: DrawerState
            rule.setMaterialContent(lightColorScheme()) {
                drawerState = rememberDrawerState(DrawerValue.Closed)
                DismissibleNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DismissibleDrawerSheet {
                            Box(Modifier.fillMaxSize().testTag(DrawerTestTag))
                        }
                    },
                    content = { Box(Modifier.fillMaxSize().testTag("ScreenContent")) },
                )
            }

            // Drawer should start in closed state
            rule.onNodeWithTag("ScreenContent").assertLeftPositionInRootIsEqualTo(0.dp)

            // When the drawer state is set to Opened
            drawerState.open()
            // Then the drawer should be opened
            rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(0.dp)

            // When the drawer state is set to Closed
            drawerState.close()
            // Then the drawer should be closed
            rule.onNodeWithTag("ScreenContent").assertLeftPositionInRootIsEqualTo(0.dp)
        }

    @Test
    @LargeTest
    fun dismissibleNavigationDrawer_animateTo(): Unit =
        runBlocking(AutoTestFrameClock()) {
            lateinit var drawerState: DrawerState
            rule.setMaterialContent(lightColorScheme()) {
                drawerState = rememberDrawerState(DrawerValue.Closed)
                DismissibleNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DismissibleDrawerSheet {
                            Box(Modifier.fillMaxSize().testTag(DrawerTestTag))
                        }
                    },
                    content = { Box(Modifier.fillMaxSize().testTag("ScreenContent")) },
                )
            }

            // Drawer should start in closed state
            rule.onNodeWithTag("ScreenContent").assertLeftPositionInRootIsEqualTo(0.dp)

            // When the drawer state is set to Opened
            @Suppress("DEPRECATION") // animateTo is deprecated, but we are testing it
            drawerState.animateTo(DrawerValue.Open, TweenSpec())
            // Then the drawer should be opened
            rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(0.dp)

            // When the drawer state is set to Closed
            @Suppress("DEPRECATION") // animateTo is deprecated, but we are testing it
            drawerState.animateTo(DrawerValue.Closed, TweenSpec())
            // Then the drawer should be closed
            rule.onNodeWithTag("ScreenContent").assertLeftPositionInRootIsEqualTo(0.dp)
        }

    @Test
    @LargeTest
    fun dismissibleNavigationDrawer_snapTo(): Unit =
        runBlocking(AutoTestFrameClock()) {
            lateinit var drawerState: DrawerState
            rule.setMaterialContent(lightColorScheme()) {
                drawerState = rememberDrawerState(DrawerValue.Closed)
                DismissibleNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DismissibleDrawerSheet {
                            Box(Modifier.fillMaxSize().testTag(DrawerTestTag))
                        }
                    },
                    content = { Box(Modifier.fillMaxSize().testTag("ScreenContent")) },
                )
            }

            // Drawer should start in closed state
            rule.onNodeWithTag("ScreenContent").assertLeftPositionInRootIsEqualTo(0.dp)

            // When the drawer state is set to Opened
            drawerState.snapTo(DrawerValue.Open)
            // Then the drawer should be opened
            rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(0.dp)

            // When the drawer state is set to Closed
            drawerState.snapTo(DrawerValue.Closed)
            // Then the drawer should be closed
            rule.onNodeWithTag("ScreenContent").assertLeftPositionInRootIsEqualTo(0.dp)
        }

    @Test
    @LargeTest
    fun dismissibleNavigationDrawer_currentValue(): Unit =
        runBlocking(AutoTestFrameClock()) {
            lateinit var drawerState: DrawerState
            rule.setMaterialContent(lightColorScheme()) {
                drawerState = rememberDrawerState(DrawerValue.Closed)
                DismissibleNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DismissibleDrawerSheet {
                            Box(Modifier.fillMaxSize().testTag(DrawerTestTag))
                        }
                    },
                    content = {},
                )
            }

            // Drawer should start in closed state
            assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Closed)

            // When the drawer state is set to Opened
            drawerState.snapTo(DrawerValue.Open)
            // Then the drawer should be opened
            rule.runOnIdle { assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Open) }

            // When the drawer state is set to Closed
            drawerState.snapTo(DrawerValue.Closed)
            // Then the drawer should be closed
            rule.runOnIdle { assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Closed) }
        }

    @Test
    @LargeTest
    fun dismissibleNavigationDrawer_drawerContent_doesntPropagateClicksWhenOpen(): Unit =
        runBlocking(AutoTestFrameClock()) {
            var bodyClicks = 0
            lateinit var drawerState: DrawerState
            rule.setMaterialContent(lightColorScheme()) {
                drawerState = rememberDrawerState(DrawerValue.Closed)
                DismissibleNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DismissibleDrawerSheet {
                            Box(Modifier.fillMaxSize().testTag(DrawerTestTag))
                        }
                    },
                    content = { Box(Modifier.fillMaxSize().clickable { bodyClicks += 1 }) },
                )
            }

            // Click in the middle of the drawer
            rule.onNodeWithTag(DrawerTestTag).performClick()

            rule.runOnIdle { assertThat(bodyClicks).isEqualTo(1) }
            drawerState.open()

            // Click on the left-center pixel of the drawer
            rule.onNodeWithTag(DrawerTestTag).performTouchInput { click(centerLeft) }

            rule.runOnIdle { assertThat(bodyClicks).isEqualTo(1) }
        }

    @Test
    @LargeTest
    fun dismissibleNavigationDrawer_openBySwipe() {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            Box(Modifier.testTag(DrawerTestTag)) {
                DismissibleNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DismissibleDrawerSheet {
                            Box(Modifier.fillMaxSize().background(color = Color.Magenta))
                        }
                    },
                    content = { Box(Modifier.fillMaxSize().background(color = Color.Red)) },
                )
            }
        }

        rule.onNodeWithTag(DrawerTestTag).performTouchInput { swipeRight() }

        rule.runOnIdle { assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Open) }

        rule.onNodeWithTag(DrawerTestTag).performTouchInput { swipeLeft() }

        rule.runOnIdle { assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Closed) }
    }

    @Test
    @LargeTest
    fun dismissibleNavigationDrawer_confirmStateChangeRespect() {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            drawerState =
                rememberDrawerState(
                    DrawerValue.Open,
                    confirmStateChange = { it != DrawerValue.Closed },
                )
            Box(Modifier.testTag(DrawerTestTag)) {
                DismissibleNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DismissibleDrawerSheet(Modifier.testTag("content")) {
                            Box(Modifier.fillMaxSize().background(color = Color.Magenta))
                        }
                    },
                    content = { Box(Modifier.fillMaxSize().background(color = Color.Red)) },
                )
            }
        }

        rule.onNodeWithTag(DrawerTestTag).performTouchInput { swipeLeft() }

        // still open
        rule.runOnIdle { assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Open) }

        rule
            .onNodeWithTag("content", useUnmergedTree = true)
            .onParent()
            .performSemanticsAction(SemanticsActions.Dismiss)

        rule.runOnIdle { assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Open) }
    }

    @Test
    @LargeTest
    fun dismissibleNavigationDrawer_openBySwipe_rtl() {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            // emulate click on the screen
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.testTag(DrawerTestTag)) {
                    DismissibleNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            DismissibleDrawerSheet {
                                Box(Modifier.fillMaxSize().background(color = Color.Magenta))
                            }
                        },
                        content = { Box(Modifier.fillMaxSize().background(color = Color.Red)) },
                    )
                }
            }
        }

        rule.onNodeWithTag(DrawerTestTag).performTouchInput { swipeLeft() }

        rule.runOnIdle { assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Open) }

        rule.onNodeWithTag(DrawerTestTag).performTouchInput { swipeRight() }

        rule.runOnIdle { assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Closed) }
    }

    @Test
    @LargeTest
    fun dismissibleNavigationDrawer_noDismissActionWhenClosed_hasDismissActionWhenOpen(): Unit =
        runBlocking(AutoTestFrameClock()) {
            lateinit var drawerState: DrawerState
            rule.setMaterialContent(lightColorScheme()) {
                drawerState = rememberDrawerState(DrawerValue.Closed)
                DismissibleNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DismissibleDrawerSheet(Modifier.testTag(DrawerTestTag)) {
                            Box(Modifier.fillMaxSize())
                        }
                    },
                    content = {},
                )
            }

            // Drawer should start in closed state and have no dismiss action
            rule
                .onNodeWithTag(DrawerTestTag, useUnmergedTree = true)
                .onParent()
                .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Dismiss))

            // When the drawer state is set to Opened
            drawerState.open()
            // Then the drawer should be opened and have dismiss action
            rule
                .onNodeWithTag(DrawerTestTag, useUnmergedTree = true)
                .onParent()
                .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))

            // When the drawer state is set to Closed using dismiss action
            rule
                .onNodeWithTag(DrawerTestTag, useUnmergedTree = true)
                .onParent()
                .performSemanticsAction(SemanticsActions.Dismiss)
            // Then the drawer should be closed and have no dismiss action
            rule
                .onNodeWithTag(DrawerTestTag, useUnmergedTree = true)
                .onParent()
                .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Dismiss))
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    @LargeTest
    fun dismissibleNavigationDrawer_drawerIsFocused_whenOpened() {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            val scope = rememberCoroutineScope()
            drawerState = rememberDrawerState(DrawerValue.Closed)
            DismissibleNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    DismissibleDrawerSheet {
                        Button(
                            onClick = { scope.launch { drawerState.close() } },
                            modifier = Modifier.testTag(DrawerTestTag).focusable(),
                        ) {}
                    }
                },
                content = {
                    Button(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier.testTag("Button").focusable(),
                    ) {}
                },
            )
        }

        rule.onNodeWithTag("Button").requestFocus()
        rule.onNodeWithTag("Button").assertIsFocused()

        // Open drawer
        rule.onNodeWithTag("Button").performClick()
        rule.runOnIdle { assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Open) }

        // Assert drawer content is focused.
        rule.onNodeWithTag(DrawerTestTag).assertIsFocused()
    }
}

private const val DrawerTestTag = "drawer"
