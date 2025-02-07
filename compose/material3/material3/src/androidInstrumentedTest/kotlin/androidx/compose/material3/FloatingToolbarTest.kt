/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.End
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class FloatingToolbarTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun horizontalFloatingToolbar_scrolledPositioning() {
        lateinit var scrollBehavior: FloatingToolbarScrollBehavior
        lateinit var colors: FloatingToolbarColors
        var backgroundColor = Color.Unspecified
        val scrollHeightOffsetDp = 20.dp
        var scrollHeightOffsetPx = 0f
        var containerSizePx = 0f
        val screenOffsetDp = ScreenOffset
        var screenOffsetPx = 0f

        rule.setMaterialContent(lightColorScheme()) {
            backgroundColor = MaterialTheme.colorScheme.background
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors()
            scrollBehavior =
                FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)
            scrollHeightOffsetPx = with(LocalDensity.current) { scrollHeightOffsetDp.toPx() }
            containerSizePx =
                with(LocalDensity.current) { FloatingToolbarDefaults.ContainerSize.toPx() }
            screenOffsetPx = with(LocalDensity.current) { screenOffsetDp.toPx() }
            HorizontalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag).offset(y = -screenOffsetDp),
                expanded = false,
                scrollBehavior = scrollBehavior,
                shape = RectangleShape,
                content = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                }
            )
        }

        assertThat(scrollBehavior.state.offsetLimit).isEqualTo(-(containerSizePx + screenOffsetPx))
        // Simulate scrolled content.
        rule.runOnIdle {
            scrollBehavior.state.offset = -scrollHeightOffsetPx
            scrollBehavior.state.contentOffset = -scrollHeightOffsetPx
        }
        rule.waitForIdle()
        rule.onNodeWithTag(FloatingToolbarTestTag).captureToImage().assertPixels(null) { pos ->
            val scrolled = (scrollHeightOffsetPx - screenOffsetPx).roundToInt()
            when (pos.y) {
                0 -> backgroundColor
                scrolled - 2 -> backgroundColor
                scrolled -> colors.toolbarContainerColor
                else -> null
            }
        }
    }

    @Test
    fun verticalFloatingToolbar_scrolledPositioning() {
        lateinit var scrollBehavior: FloatingToolbarScrollBehavior
        lateinit var colors: FloatingToolbarColors
        var backgroundColor = Color.Unspecified
        val scrollHeightOffsetDp = 20.dp
        var scrollHeightOffsetPx = 0f
        var containerSizePx = 0f
        val screenOffsetDp = ScreenOffset
        var screenOffsetPx = 0f

        rule.setMaterialContent(lightColorScheme()) {
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors()
            backgroundColor = MaterialTheme.colorScheme.background
            scrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = End)
            scrollHeightOffsetPx = with(LocalDensity.current) { scrollHeightOffsetDp.toPx() }
            containerSizePx =
                with(LocalDensity.current) { FloatingToolbarDefaults.ContainerSize.toPx() }
            screenOffsetPx = with(LocalDensity.current) { screenOffsetDp.toPx() }
            VerticalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag).offset(x = -screenOffsetDp),
                expanded = false,
                scrollBehavior = scrollBehavior,
                shape = RectangleShape,
                content = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                }
            )
        }

        assertThat(scrollBehavior.state.offsetLimit).isEqualTo(-(containerSizePx + screenOffsetPx))
        // Simulate scrolled content.
        rule.runOnIdle {
            scrollBehavior.state.offset = -scrollHeightOffsetPx
            scrollBehavior.state.contentOffset = -scrollHeightOffsetPx
        }
        rule.waitForIdle()
        rule.onNodeWithTag(FloatingToolbarTestTag).captureToImage().assertPixels(null) { pos ->
            val scrolled = (scrollHeightOffsetPx - screenOffsetPx).roundToInt()
            when (pos.x) {
                0 -> backgroundColor
                scrolled - 2 -> backgroundColor
                scrolled -> colors.toolbarContainerColor
                else -> null
            }
        }
    }

    @Test
    fun horizontalFloatingToolbar_transparentContainerColor() {
        val expectedColorBehindToolbar: Color = Color.Red
        rule.setMaterialContent(lightColorScheme()) {
            Box(modifier = Modifier.background(color = expectedColorBehindToolbar)) {
                HorizontalFloatingToolbar(
                    modifier = Modifier.testTag(FloatingToolbarTestTag),
                    expanded = false,
                    colors =
                        FloatingToolbarDefaults.standardFloatingToolbarColors(
                            toolbarContainerColor = Color.Transparent
                        ),
                    content = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    }
                )
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertContainsColor(expectedColorBehindToolbar)
    }

    @Test
    fun verticalFloatingToolbar_transparentContainerColor() {
        val expectedColorBehindToolbar: Color = Color.Red
        rule.setMaterialContent(lightColorScheme()) {
            Box(modifier = Modifier.background(color = expectedColorBehindToolbar)) {
                VerticalFloatingToolbar(
                    modifier = Modifier.testTag(FloatingToolbarTestTag),
                    expanded = false,
                    colors =
                        FloatingToolbarDefaults.standardFloatingToolbarColors(
                            toolbarContainerColor = Color.Transparent
                        ),
                    content = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    }
                )
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertContainsColor(expectedColorBehindToolbar)
    }

    @Test
    fun horizontalFloatingToolbar_customContentPadding() {
        val expectedPadding: Dp = 20.dp
        rule.setMaterialContent(lightColorScheme()) {
            HorizontalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = false,
                contentPadding = PaddingValues(expectedPadding),
                content = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                }
            )
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .onChild()
            .assertTopPositionInRootIsEqualTo(expectedPadding)
    }

    @Test
    fun verticalFloatingToolbar_customContentPadding() {
        val expectedPadding: Dp = 20.dp
        rule.setMaterialContent(lightColorScheme()) {
            VerticalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = false,
                contentPadding = PaddingValues(expectedPadding),
                content = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                }
            )
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .onChild()
            .assertLeftPositionInRootIsEqualTo(expectedPadding)
    }

    @Test
    fun horizontalFloatingToolbar_trailingContent_expanded() {
        var iconButtonSize: Dp = IconButtonDefaults.smallContainerSize().width
        rule
            .setMaterialContentForSizeAssertions {
                if (LocalMinimumInteractiveComponentSize.current > iconButtonSize) {
                    iconButtonSize = LocalMinimumInteractiveComponentSize.current
                }
                HorizontalFloatingToolbar(
                    expanded = true,
                    trailingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    content = {}
                )
            }
            .assertHeightIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateTopPadding() +
                    iconButtonSize +
                    FloatingToolbarDefaults.ContentPadding.calculateBottomPadding()
            )
            .assertWidthIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateStartPadding(LayoutDirection.Ltr) +
                    iconButtonSize +
                    FloatingToolbarDefaults.ContentPadding.calculateEndPadding(LayoutDirection.Ltr)
            )
    }

    @Test
    fun horizontalFloatingToolbar_trailingContent_notExpanded() {
        rule
            .setMaterialContentForSizeAssertions {
                HorizontalFloatingToolbar(
                    expanded = false,
                    trailingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    content = {}
                )
            }
            // Expecting a width of the default content padding
            .assertWidthIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateStartPadding(LayoutDirection.Ltr) +
                    FloatingToolbarDefaults.ContentPadding.calculateEndPadding(LayoutDirection.Ltr)
            )
    }

    @Test
    fun horizontalFloatingToolbar_leadingContent_expanded() {
        var iconButtonSize: Dp = IconButtonDefaults.smallContainerSize().width
        rule
            .setMaterialContentForSizeAssertions {
                if (LocalMinimumInteractiveComponentSize.current > iconButtonSize) {
                    iconButtonSize = LocalMinimumInteractiveComponentSize.current
                }
                HorizontalFloatingToolbar(
                    expanded = true,
                    leadingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    content = {}
                )
            }
            .assertHeightIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateTopPadding() +
                    iconButtonSize +
                    FloatingToolbarDefaults.ContentPadding.calculateBottomPadding()
            )
            .assertWidthIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateStartPadding(LayoutDirection.Ltr) +
                    iconButtonSize +
                    FloatingToolbarDefaults.ContentPadding.calculateEndPadding(LayoutDirection.Ltr)
            )
    }

    @Test
    fun horizontalFloatingToolbar_leadingContent_notExpanded() {
        rule
            .setMaterialContentForSizeAssertions {
                HorizontalFloatingToolbar(
                    expanded = false,
                    leadingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    content = {}
                )
            }
            // Expecting a width of the default content padding
            .assertWidthIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateStartPadding(LayoutDirection.Ltr) +
                    FloatingToolbarDefaults.ContentPadding.calculateEndPadding(LayoutDirection.Ltr)
            )
    }

    @Test
    fun horizontalFloatingToolbar_leadingAndTrailingContent_expanded() {
        var iconButtonSize: Dp = IconButtonDefaults.smallContainerSize().width
        rule
            .setMaterialContentForSizeAssertions {
                if (LocalMinimumInteractiveComponentSize.current > iconButtonSize) {
                    iconButtonSize = LocalMinimumInteractiveComponentSize.current
                }
                HorizontalFloatingToolbar(
                    expanded = true,
                    leadingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    content = {}
                )
            }
            .assertHeightIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateTopPadding() +
                    iconButtonSize +
                    FloatingToolbarDefaults.ContentPadding.calculateBottomPadding()
            )
            .assertWidthIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateStartPadding(LayoutDirection.Ltr) +
                    iconButtonSize +
                    iconButtonSize +
                    FloatingToolbarDefaults.ContentPadding.calculateEndPadding(LayoutDirection.Ltr)
            )
    }

    @Test
    fun horizontalFloatingToolbar_leadingAndTrailingContent_notExpanded() {
        rule
            .setMaterialContentForSizeAssertions {
                HorizontalFloatingToolbar(
                    expanded = false,
                    leadingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    content = {}
                )
            }
            // Expecting a width of the default content padding
            .assertWidthIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateStartPadding(LayoutDirection.Ltr) +
                    FloatingToolbarDefaults.ContentPadding.calculateEndPadding(LayoutDirection.Ltr)
            )
    }

    @Test
    fun verticalFloatingToolbar_trailingContent_expanded() {
        var iconButtonSize: Dp = IconButtonDefaults.smallContainerSize().height
        rule
            .setMaterialContentForSizeAssertions {
                if (LocalMinimumInteractiveComponentSize.current > iconButtonSize) {
                    iconButtonSize = LocalMinimumInteractiveComponentSize.current
                }
                VerticalFloatingToolbar(
                    expanded = true,
                    trailingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    content = {}
                )
            }
            .assertHeightIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateTopPadding() +
                    iconButtonSize +
                    FloatingToolbarDefaults.ContentPadding.calculateBottomPadding()
            )
            .assertWidthIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateStartPadding(LayoutDirection.Ltr) +
                    iconButtonSize +
                    FloatingToolbarDefaults.ContentPadding.calculateEndPadding(LayoutDirection.Ltr)
            )
    }

    @Test
    fun verticalFloatingToolbar_trailingContent_notExpanded() {
        rule
            .setMaterialContentForSizeAssertions {
                VerticalFloatingToolbar(
                    expanded = false,
                    trailingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    content = {}
                )
            }
            // Expecting a height of the default content padding
            .assertHeightIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateTopPadding() +
                    FloatingToolbarDefaults.ContentPadding.calculateBottomPadding()
            )
    }

    @Test
    fun verticalFloatingToolbar_leadingContent_expanded() {
        var iconButtonSize: Dp = IconButtonDefaults.smallContainerSize().height
        rule
            .setMaterialContentForSizeAssertions {
                if (LocalMinimumInteractiveComponentSize.current > iconButtonSize) {
                    iconButtonSize = LocalMinimumInteractiveComponentSize.current
                }
                VerticalFloatingToolbar(
                    expanded = true,
                    leadingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    content = {}
                )
            }
            .assertHeightIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateTopPadding() +
                    iconButtonSize +
                    FloatingToolbarDefaults.ContentPadding.calculateBottomPadding()
            )
            .assertWidthIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateStartPadding(LayoutDirection.Ltr) +
                    iconButtonSize +
                    FloatingToolbarDefaults.ContentPadding.calculateEndPadding(LayoutDirection.Ltr)
            )
    }

    @Test
    fun verticalFloatingToolbar_leadingContent_notExpanded() {
        rule
            .setMaterialContentForSizeAssertions {
                VerticalFloatingToolbar(
                    expanded = false,
                    leadingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    content = {}
                )
            }
            // Expecting a height of the default content padding
            .assertHeightIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateTopPadding() +
                    FloatingToolbarDefaults.ContentPadding.calculateBottomPadding()
            )
    }

    @Test
    fun verticalFloatingToolbar_leadingAndTrailingContent_expanded() {
        var iconButtonSize: Dp = IconButtonDefaults.smallContainerSize().height
        rule
            .setMaterialContentForSizeAssertions {
                if (LocalMinimumInteractiveComponentSize.current > iconButtonSize) {
                    iconButtonSize = LocalMinimumInteractiveComponentSize.current
                }
                VerticalFloatingToolbar(
                    expanded = true,
                    leadingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    content = {}
                )
            }
            .assertHeightIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateTopPadding() +
                    iconButtonSize +
                    iconButtonSize +
                    FloatingToolbarDefaults.ContentPadding.calculateBottomPadding()
            )
            .assertWidthIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateStartPadding(LayoutDirection.Ltr) +
                    iconButtonSize +
                    FloatingToolbarDefaults.ContentPadding.calculateEndPadding(LayoutDirection.Ltr)
            )
    }

    @Test
    fun verticalFloatingToolbar_leadingAndTrailingContent_notExpanded() {
        rule
            .setMaterialContentForSizeAssertions {
                VerticalFloatingToolbar(
                    expanded = false,
                    leadingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    },
                    content = {}
                )
            }
            // Expecting a height of the default content padding
            .assertHeightIsEqualTo(
                FloatingToolbarDefaults.ContentPadding.calculateTopPadding() +
                    FloatingToolbarDefaults.ContentPadding.calculateBottomPadding()
            )
    }

    @Test
    fun state_restoresFloatingToolbarState() {
        val restorationTester = StateRestorationTester(rule)
        var floatingToolbarState: FloatingToolbarState? = null
        restorationTester.setContent { floatingToolbarState = rememberFloatingToolbarState() }

        rule.runOnIdle {
            floatingToolbarState!!.offsetLimit = -350f
            floatingToolbarState!!.offset = -300f
            floatingToolbarState!!.contentOffset = -550f
        }

        floatingToolbarState = null

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(floatingToolbarState!!.offsetLimit).isEqualTo(-350f)
            assertThat(floatingToolbarState!!.offset).isEqualTo(-300f)
            assertThat(floatingToolbarState!!.contentOffset).isEqualTo(-550f)
        }
    }

    @Test
    fun horizontalFloatingToolbar_expansionStateChange() {
        var expanded by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            HorizontalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = expanded,
                floatingActionButton = { ToolbarFab() },
            ) {
                ToolbarContent()
            }
        }

        // When collapsed, check that the FAB is in its largest size.
        rule
            .onNodeWithTag(FloatingActionButtonTestTag)
            .assertIsSquareWithSize(FloatingToolbarDefaults.FabSizeRange.endInclusive)

        // Check a sampled item from the content to ensure it's not visible.
        rule.onNodeWithTag(FloatingToolbarContentLastItemTestTag).assertIsNotDisplayed()

        val componentWidth =
            FloatingToolbarDefaults.FabSizeRange.endInclusive +
                /* 4 IconButtons at the ToolbarContent */ MinTouchTarget * 4
        // The total size of the component still the total size of all the elements.
        rule.onNodeWithTag(FloatingToolbarTestTag).assertWidthIsEqualTo(componentWidth)

        // Expand the component.
        expanded = true
        rule.waitForIdle()

        // When expanded, check that the FAB is in its smallest size.
        rule
            .onNodeWithTag(FloatingActionButtonTestTag)
            .assertIsSquareWithSize(FloatingToolbarDefaults.FabSizeRange.start)
        // Check a sampled item from the content to ensure it's visible.
        rule.onNodeWithTag(FloatingToolbarContentLastItemTestTag).assertIsDisplayed()
        // The total size of the component still the total size of all the elements.
        rule.onNodeWithTag(FloatingToolbarTestTag).assertWidthIsEqualTo(componentWidth)
    }

    @Test
    fun horizontalFloatingToolbar_customContentColor() {
        rule.setMaterialContent(lightColorScheme()) {
            HorizontalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                colors =
                    FloatingToolbarDefaults.standardFloatingToolbarColors(
                        toolbarContainerColor = Color.Blue
                    ),
                expanded = true,
                floatingActionButton = { ToolbarFab() },
            ) {
                ToolbarContent()
            }
        }

        rule.onNodeWithTag(FloatingToolbarTestTag).captureToImage().assertContainsColor(Color.Blue)
    }

    @Test
    fun horizontalFloatingToolbar_defaultContentPadding() {
        rule.setMaterialContent(lightColorScheme()) {
            HorizontalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = true,
                // Set a RectangleShape to get an accurate padding measure without the default
                // rounded shape influence over the size.
                shape = RectangleShape,
                floatingActionButton = { ToolbarFab() },
            ) {
                ToolbarContent()
            }
        }

        val componentWidth =
            FloatingToolbarDefaults.ContentPadding.calculateLeftPadding(LayoutDirection.Ltr) +
                FloatingToolbarDefaults.ContentPadding.calculateRightPadding(LayoutDirection.Ltr) +
                FloatingToolbarDefaults.FabSizeRange.start +
                /* 4 IconButtons at the ToolbarContent */ MinTouchTarget * 4 +
                FloatingToolbarDefaults.ToolbarToFabGap
        rule.onNodeWithTag(FloatingToolbarTestTag).assertWidthIsEqualTo(componentWidth)
    }

    @Test
    fun horizontalFloatingToolbar_withFab_customContentPadding() {
        val padding = 48.dp
        rule.setMaterialContent(lightColorScheme()) {
            HorizontalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                contentPadding = PaddingValues(horizontal = padding),
                expanded = true,
                floatingActionButton = { ToolbarFab() },
            ) {
                ToolbarContent()
            }
        }

        val componentWidth =
            padding * 2 +
                FloatingToolbarDefaults.FabSizeRange.start +
                /* 4 IconButtons at the ToolbarContent */ MinTouchTarget * 4 +
                FloatingToolbarDefaults.ToolbarToFabGap
        rule.onNodeWithTag(FloatingToolbarTestTag).assertWidthIsEqualTo(componentWidth)
    }

    @Test
    fun verticalFloatingToolbar_expansionStateChange() {
        var expanded by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            VerticalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = expanded,
                floatingActionButton = { ToolbarFab() },
            ) {
                ToolbarContent()
            }
        }

        // When collapsed, check that the FAB is in its largest size.
        rule
            .onNodeWithTag(FloatingActionButtonTestTag)
            .assertIsSquareWithSize(FloatingToolbarDefaults.FabSizeRange.endInclusive)

        // Check a sampled item from the content to ensure it's not visible.
        rule.onNodeWithTag(FloatingToolbarContentLastItemTestTag).assertIsNotDisplayed()

        val componentHeight =
            FloatingToolbarDefaults.FabSizeRange.endInclusive +
                /* 4 IconButtons at the ToolbarContent */ MinTouchTarget * 4
        // The total size of the component still the total size of all the elements.
        rule.onNodeWithTag(FloatingToolbarTestTag).assertHeightIsEqualTo(componentHeight)

        // Expand the component.
        expanded = true
        rule.waitForIdle()

        // When expanded, check that the FAB is in its smallest size.
        rule
            .onNodeWithTag(FloatingActionButtonTestTag)
            .assertIsSquareWithSize(FloatingToolbarDefaults.FabSizeRange.start)
        // Check a sampled item from the content to ensure it's visible.
        rule.onNodeWithTag(FloatingToolbarContentLastItemTestTag).assertIsDisplayed()
        // The total size of the component still the total size of all the elements.
        rule.onNodeWithTag(FloatingToolbarTestTag).assertHeightIsEqualTo(componentHeight)
    }

    @Test
    fun verticalFloatingToolbar_customContentColor() {
        rule.setMaterialContent(lightColorScheme()) {
            VerticalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                colors =
                    FloatingToolbarDefaults.standardFloatingToolbarColors(
                        toolbarContainerColor = Color.Blue
                    ),
                expanded = true,
                floatingActionButton = { ToolbarFab() },
            ) {
                ToolbarContent()
            }
        }

        rule.onNodeWithTag(FloatingToolbarTestTag).captureToImage().assertContainsColor(Color.Blue)
    }

    @Test
    fun verticalFloatingToolbar_defaultContentPadding() {
        rule.setMaterialContent(lightColorScheme()) {
            VerticalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = true,
                // Set a RectangleShape to get an accurate padding measure without the default
                // rounded shape influence over the size.
                shape = RectangleShape,
                floatingActionButton = { ToolbarFab() },
            ) {
                ToolbarContent()
            }
        }

        val componentHeight =
            FloatingToolbarDefaults.ContentPadding.calculateTopPadding() +
                FloatingToolbarDefaults.ContentPadding.calculateBottomPadding() +
                FloatingToolbarDefaults.FabSizeRange.start +
                /* 4 IconButtons at the ToolbarContent */ MinTouchTarget * 4 +
                FloatingToolbarDefaults.ToolbarToFabGap
        rule.onNodeWithTag(FloatingToolbarTestTag).assertHeightIsEqualTo(componentHeight)
    }

    @Test
    fun verticalFloatingToolbar_withFab_customContentPadding() {
        val padding = 64.dp
        rule.setMaterialContent(lightColorScheme()) {
            VerticalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                contentPadding = PaddingValues(vertical = padding),
                expanded = true,
                floatingActionButton = { ToolbarFab() },
            ) {
                ToolbarContent()
            }
        }

        val componentHeight =
            padding * 2 +
                FloatingToolbarDefaults.FabSizeRange.start +
                /* 4 IconButtons at the ToolbarContent */ MinTouchTarget * 4 +
                FloatingToolbarDefaults.ToolbarToFabGap
        rule.onNodeWithTag(FloatingToolbarTestTag).assertHeightIsEqualTo(componentHeight)
    }

    @Test
    fun floatingToolbarVerticalNestedScroll_verticalSwipesUpdateValue() {
        var expanded = true
        rule.setContent {
            VerticalNestedScrollTestContent(
                onExpanded = { expanded = true },
                onCollapsed = { expanded = false },
                initialValue = expanded,
            )
        }

        assertThat(expanded).isEqualTo(true)

        // Toggle the value by scrolling up and down.
        rule.onNodeWithTag(MainLayoutTag).performTouchInput { swipeUp(bottom, centerY) }
        rule.runOnIdle { assertThat(expanded).isEqualTo(false) }
        rule.onNodeWithTag(MainLayoutTag).performTouchInput { swipeDown(top, centerY) }
        rule.runOnIdle { assertThat(expanded).isEqualTo(true) }
    }

    @Test
    fun floatingToolbarVerticalNestedScroll_verticalSwipesUpdateValue_reverseLayout() {
        var expanded = true
        rule.setContent {
            VerticalNestedScrollTestContent(
                onExpanded = { expanded = true },
                onCollapsed = { expanded = false },
                initialValue = expanded,
                reverseLayout = true,
            )
        }

        assertThat(expanded).isEqualTo(true)

        // Toggle the value by scrolling down and up in this reverse layout..
        rule.onNodeWithTag(MainLayoutTag).performTouchInput { swipeDown(top, centerY) }
        rule.runOnIdle { assertThat(expanded).isEqualTo(false) }
        rule.onNodeWithTag(MainLayoutTag).performTouchInput { swipeUp(bottom, centerY) }
        rule.runOnIdle { assertThat(expanded).isEqualTo(true) }
    }

    @Test
    fun floatingToolbarVerticalNestedScroll_disableScrollInterception() {
        var expanded = true
        rule.setContent {
            VerticalNestedScrollTestContent(
                onExpanded = { expanded = true },
                onCollapsed = { expanded = false },
                toolbarNestedScrollEnabled = false,
                initialValue = expanded,
                reverseLayout = true
            )
        }

        assertThat(expanded).isEqualTo(true)

        // Scrolling up or down should not change the value.
        rule.onNodeWithTag(MainLayoutTag).performTouchInput { swipeUp(bottom, centerY) }
        rule.runOnIdle { assertThat(expanded).isEqualTo(true) }
        rule.onNodeWithTag(MainLayoutTag).performTouchInput { swipeDown(top, centerY) }
        rule.runOnIdle { assertThat(expanded).isEqualTo(true) }
    }

    @Test
    fun floatingToolbarVerticalNestedScroll_falseInitialValue() {
        var expanded = false
        rule.setContent {
            VerticalNestedScrollTestContent(
                onExpanded = { expanded = true },
                onCollapsed = { expanded = false },
                initialValue = expanded
            )
        }

        assertThat(expanded).isEqualTo(false)

        // Simulate a scroll up and ensure that the value is still false.
        rule.onNodeWithTag(MainLayoutTag).performTouchInput { swipeUp(bottom, centerY) }
        rule.runOnIdle { assertThat(expanded).isEqualTo(false) }
        // Simulate a scroll down to toggle the value to true.
        rule.onNodeWithTag(MainLayoutTag).performTouchInput { swipeDown(top, centerY) }
        rule.runOnIdle { assertThat(expanded).isEqualTo(true) }
    }

    @Test
    fun floatingToolbarVerticalNestedScroll_threshold() {
        var expanded = true
        var thresholdPx = 0f

        rule.setContent {
            VerticalNestedScrollTestContent(
                onExpanded = { expanded = true },
                onCollapsed = { expanded = false },
                initialValue = expanded
            )
            thresholdPx =
                with(LocalDensity.current) {
                    FloatingToolbarDefaults.ScrollDistanceThreshold.toPx()
                }
        }

        assertThat(expanded).isEqualTo(true)

        // Simulate a short scroll below the threshold and ensure that the value is still true.
        rule.onNodeWithTag(MainLayoutTag).performTouchInput {
            swipeUp(bottom, bottom - thresholdPx / 4f)
        }
        rule.runOnIdle { assertThat(expanded).isEqualTo(true) }

        // Simulate an additional scroll to cross the threshold and ensure that the value is now
        // false.
        rule.onNodeWithTag(MainLayoutTag).performTouchInput {
            swipeUp(bottom, bottom - thresholdPx * 2)
        }
        rule.runOnIdle { assertThat(expanded).isEqualTo(false) }
    }

    @Test
    fun floatingToolbarVerticalNestedScroll_customThreshold() {
        val customThreshold = 100.dp
        var expanded = true
        var thresholdPx = 0f

        rule.setContent {
            VerticalNestedScrollTestContent(
                onExpanded = { expanded = true },
                onCollapsed = { expanded = false },
                initialValue = expanded
            )
            thresholdPx = with(LocalDensity.current) { customThreshold.toPx() }
        }

        assertThat(expanded).isEqualTo(true)

        // Simulate a short scroll below the threshold and ensure that the value is still true.
        rule.onNodeWithTag(MainLayoutTag).performTouchInput {
            swipeUp(bottom, bottom - thresholdPx / 4f)
        }
        rule.runOnIdle { assertThat(expanded).isEqualTo(true) }

        // Simulate an additional scroll to cross the threshold and ensure that the value is now
        // false.
        rule.onNodeWithTag(MainLayoutTag).performTouchInput {
            swipeUp(bottom, bottom - thresholdPx)
        }
        rule.runOnIdle { assertThat(expanded).isEqualTo(false) }
    }

    @Test
    fun verticalFloatingToolbar_scrollBehavior() {
        rule.setMaterialContent(lightColorScheme()) {
            val scrollBehavior =
                FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = End)
            Scaffold(modifier = Modifier.nestedScroll(scrollBehavior).testTag(MainLayoutTag)) {
                innerPadding ->
                Box(Modifier.padding(innerPadding)) {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        Text(text = remember { LoremIpsum().values.first() })
                    }
                    VerticalFloatingToolbar(
                        expanded = true,
                        floatingActionButton = { ToolbarFab() },
                        modifier = Modifier.align(Alignment.CenterEnd).offset(x = -ScreenOffset),
                        scrollBehavior = scrollBehavior,
                    ) {
                        ToolbarContent()
                    }
                }
            }
        }

        // Check that the FAB and a sample from the toolbar content are displayed.
        rule.onNodeWithTag(FloatingActionButtonTestTag).assertIsDisplayed()
        rule.onNodeWithTag(FloatingToolbarContentLastItemTestTag).assertIsDisplayed()

        // Swipe the content up to collapse the FloatingToolbar.
        rule.onNodeWithTag(MainLayoutTag).performTouchInput { swipeUp(bottom, bottom - 1000) }
        rule.waitForIdle()
        // Check that the FAB and a sample from the toolbar content are not displayed.
        rule.onNodeWithTag(FloatingActionButtonTestTag).assertIsNotDisplayed()
        rule.onNodeWithTag(FloatingToolbarContentLastItemTestTag).assertIsNotDisplayed()
    }

    @Test
    fun horizontalFloatingToolbar_scrollBehavior() {
        rule.setMaterialContent(lightColorScheme()) {
            val scrollBehavior =
                FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = End)
            Scaffold(modifier = Modifier.nestedScroll(scrollBehavior).testTag(MainLayoutTag)) {
                innerPadding ->
                Box(Modifier.padding(innerPadding)) {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        Text(text = remember { LoremIpsum().values.first() })
                    }
                    HorizontalFloatingToolbar(
                        expanded = true,
                        floatingActionButton = { ToolbarFab() },
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
                        scrollBehavior = scrollBehavior,
                    ) {
                        ToolbarContent()
                    }
                }
            }
        }

        // Check that the FAB and a sample from the toolbar content are displayed.
        rule.onNodeWithTag(FloatingActionButtonTestTag).assertIsDisplayed()
        rule.onNodeWithTag(FloatingToolbarContentLastItemTestTag).assertIsDisplayed()

        // Swipe the content up to collapse the FloatingToolbar.
        rule.onNodeWithTag(MainLayoutTag).performTouchInput { swipeUp(bottom, bottom - 1000) }
        rule.waitForIdle()
        // Check that the FAB and a sample from the toolbar content are not displayed.
        rule.onNodeWithTag(FloatingActionButtonTestTag).assertIsNotDisplayed()
        rule.onNodeWithTag(FloatingToolbarContentLastItemTestTag).assertIsNotDisplayed()
    }

    @Test
    fun horizontalFloatingToolbar_expanded_semantics() {
        lateinit var actionLabel: String
        rule.setMaterialContent(lightColorScheme()) {
            actionLabel = getString(Strings.FloatingToolbarCollapse)
            HorizontalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = true,
                leadingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                },
                trailingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            ) {
                IconButton(
                    onClick = { /* doSomething() */ },
                    modifier = Modifier.testTag(FloatingToolbarMainContentTestTag)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }

        val action =
            rule
                .onNodeWithTag(FloatingToolbarMainContentTestTag)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]

        assertThat(action).hasSize(1)
        assertThat(action[0].label).isEqualTo(actionLabel)
    }

    @Test
    fun horizontalFloatingToolbar_collapsed_semantics() {
        lateinit var actionLabel: String
        rule.setMaterialContent(lightColorScheme()) {
            actionLabel = getString(Strings.FloatingToolbarExpand)
            HorizontalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = false,
                leadingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                },
                trailingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            ) {
                IconButton(
                    onClick = { /* doSomething() */ },
                    modifier = Modifier.testTag(FloatingToolbarMainContentTestTag)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }

        val action =
            rule
                .onNodeWithTag(FloatingToolbarMainContentTestTag)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]

        assertThat(action).hasSize(1)
        assertThat(action[0].label).isEqualTo(actionLabel)
    }

    @Test
    fun verticalFloatingToolbar_expanded_semantics() {
        lateinit var actionLabel: String
        rule.setMaterialContent(lightColorScheme()) {
            actionLabel = getString(Strings.FloatingToolbarCollapse)
            VerticalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = true,
                leadingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                },
                trailingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            ) {
                IconButton(
                    onClick = { /* doSomething() */ },
                    modifier = Modifier.testTag(FloatingToolbarMainContentTestTag)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }

        val action =
            rule
                .onNodeWithTag(FloatingToolbarMainContentTestTag)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]

        assertThat(action).hasSize(1)
        assertThat(action[0].label).isEqualTo(actionLabel)
    }

    @Test
    fun verticalFloatingToolbar_collapsed_semantics() {
        lateinit var actionLabel: String
        rule.setMaterialContent(lightColorScheme()) {
            actionLabel = getString(Strings.FloatingToolbarExpand)
            VerticalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = false,
                leadingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                },
                trailingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            ) {
                IconButton(
                    onClick = { /* doSomething() */ },
                    modifier = Modifier.testTag(FloatingToolbarMainContentTestTag)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
        }

        val action =
            rule
                .onNodeWithTag(FloatingToolbarMainContentTestTag)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]

        assertThat(action).hasSize(1)
        assertThat(action[0].label).isEqualTo(actionLabel)
    }

    @Test
    fun horizontalFloatingToolbar_withFab_expanded_semantics() {
        lateinit var actionLabel: String
        rule.setMaterialContent(lightColorScheme()) {
            actionLabel = getString(Strings.FloatingToolbarCollapse)
            HorizontalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = true,
                floatingActionButton = { ToolbarFab() },
            ) {
                ToolbarContent()
            }
        }

        val action =
            rule
                .onNodeWithTag(FloatingActionButtonTestTag)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]

        assertThat(action).hasSize(1)
        assertThat(action[0].label).isEqualTo(actionLabel)
    }

    @Test
    fun horizontalFloatingToolbar_withFab_collapsed_semantics() {
        lateinit var actionLabel: String
        rule.setMaterialContent(lightColorScheme()) {
            actionLabel = getString(Strings.FloatingToolbarExpand)
            HorizontalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = false,
                floatingActionButton = { ToolbarFab() },
            ) {
                ToolbarContent()
            }
        }

        val action =
            rule
                .onNodeWithTag(FloatingActionButtonTestTag)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]

        assertThat(action).hasSize(1)
        assertThat(action[0].label).isEqualTo(actionLabel)
    }

    @Test
    fun verticalFloatingToolbar_withFab_expanded_semantics() {
        lateinit var actionLabel: String
        rule.setMaterialContent(lightColorScheme()) {
            actionLabel = getString(Strings.FloatingToolbarCollapse)
            VerticalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = true,
                floatingActionButton = { ToolbarFab() },
            ) {
                ToolbarContent()
            }
        }

        val action =
            rule
                .onNodeWithTag(FloatingActionButtonTestTag)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]

        assertThat(action).hasSize(1)
        assertThat(action[0].label).isEqualTo(actionLabel)
    }

    @Test
    fun verticalFloatingToolbar_withFab_collapsed_semantics() {
        lateinit var actionLabel: String
        rule.setMaterialContent(lightColorScheme()) {
            actionLabel = getString(Strings.FloatingToolbarExpand)
            VerticalFloatingToolbar(
                modifier = Modifier.testTag(FloatingToolbarTestTag),
                expanded = false,
                floatingActionButton = { ToolbarFab() },
            ) {
                ToolbarContent()
            }
        }

        val action =
            rule
                .onNodeWithTag(FloatingActionButtonTestTag)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]

        assertThat(action).hasSize(1)
        assertThat(action[0].label).isEqualTo(actionLabel)
    }

    @Composable
    private fun VerticalNestedScrollTestContent(
        onExpanded: () -> Unit,
        onCollapsed: () -> Unit,
        toolbarNestedScrollEnabled: Boolean = true,
        initialValue: Boolean = true,
        reverseLayout: Boolean = false
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize() then
                    (if (toolbarNestedScrollEnabled) {
                        Modifier.floatingToolbarVerticalNestedScroll(
                            expanded = initialValue,
                            reverseLayout = reverseLayout,
                            onExpand = onExpanded,
                            onCollapse = onCollapsed
                        )
                    } else {
                        Modifier
                    })
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().testTag(MainLayoutTag).weight(1f),
                reverseLayout = reverseLayout
            ) {
                items(100) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp).background(Color.Gray)) {
                        Text(text = it.toString())
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    @Composable
    private fun ToolbarFab() {

        FloatingToolbarDefaults.StandardFloatingActionButton(
            modifier = Modifier.testTag(FloatingActionButtonTestTag),
            onClick = { /* doSomething() */ },
        ) {
            Icon(Icons.Filled.Check, "Localized description")
        }
    }

    @Composable
    private fun ToolbarContent() {
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Filled.Person, contentDescription = "Localized description")
        }
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Filled.Edit, contentDescription = "Localized description")
        }
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
        }
        IconButton(
            onClick = { /* doSomething() */ },
            modifier = Modifier.testTag(FloatingToolbarContentLastItemTestTag)
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Localized description")
        }
    }

    private val MinTouchTarget = 48.dp
    private val MainLayoutTag = "mainLayout"
    private val FloatingToolbarTestTag = "floatingToolbar"
    private val FloatingActionButtonTestTag = "floatingActionButton"
    private val FloatingToolbarContentLastItemTestTag = "floatingToolbarContentLastItem"
    private val FloatingToolbarMainContentTestTag = "floatingToolbarMainContentTestTag"
}
