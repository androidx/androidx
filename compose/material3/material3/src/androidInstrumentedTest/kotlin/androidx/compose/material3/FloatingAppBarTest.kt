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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FloatingAppBarExitDirection.Companion.Bottom
import androidx.compose.material3.FloatingAppBarExitDirection.Companion.End
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
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
class FloatingAppBarTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun horizontalFloatingAppBar_scrolledPositioning() {
        lateinit var scrollBehavior: FloatingAppBarScrollBehavior
        var backgroundColor = Color.Unspecified
        var containerColor = Color.Unspecified
        val scrollHeightOffsetDp = 20.dp
        var scrollHeightOffsetPx = 0f
        var containerSizePx = 0f
        val screenOffsetDp = FloatingAppBarDefaults.ScreenOffset
        var screenOffsetPx = 0f

        rule.setMaterialContent(lightColorScheme()) {
            backgroundColor = MaterialTheme.colorScheme.background
            containerColor = FloatingAppBarDefaults.ContainerColor
            scrollBehavior = FloatingAppBarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)
            scrollHeightOffsetPx = with(LocalDensity.current) { scrollHeightOffsetDp.toPx() }
            containerSizePx =
                with(LocalDensity.current) { FloatingAppBarDefaults.ContainerSize.toPx() }
            screenOffsetPx = with(LocalDensity.current) { screenOffsetDp.toPx() }
            HorizontalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag).offset(y = -screenOffsetDp),
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
        rule.onNodeWithTag(FloatingAppBarTestTag).captureToImage().assertPixels(null) { pos ->
            val scrolled = (scrollHeightOffsetPx - screenOffsetPx).roundToInt()
            when (pos.y) {
                0 -> backgroundColor
                scrolled - 2 -> backgroundColor
                scrolled -> containerColor
                else -> null
            }
        }
    }

    @Test
    fun verticalFloatingAppBar_scrolledPositioning() {
        lateinit var scrollBehavior: FloatingAppBarScrollBehavior
        var backgroundColor = Color.Unspecified
        var containerColor = Color.Unspecified
        val scrollHeightOffsetDp = 20.dp
        var scrollHeightOffsetPx = 0f
        var containerSizePx = 0f
        val screenOffsetDp = FloatingAppBarDefaults.ScreenOffset
        var screenOffsetPx = 0f

        rule.setMaterialContent(lightColorScheme()) {
            backgroundColor = MaterialTheme.colorScheme.background
            containerColor = FloatingAppBarDefaults.ContainerColor
            scrollBehavior = FloatingAppBarDefaults.exitAlwaysScrollBehavior(exitDirection = End)
            scrollHeightOffsetPx = with(LocalDensity.current) { scrollHeightOffsetDp.toPx() }
            containerSizePx =
                with(LocalDensity.current) { FloatingAppBarDefaults.ContainerSize.toPx() }
            screenOffsetPx = with(LocalDensity.current) { screenOffsetDp.toPx() }
            VerticalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag).offset(x = -screenOffsetDp),
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
        rule.onNodeWithTag(FloatingAppBarTestTag).captureToImage().assertPixels(null) { pos ->
            val scrolled = (scrollHeightOffsetPx - screenOffsetPx).roundToInt()
            when (pos.x) {
                0 -> backgroundColor
                scrolled - 2 -> backgroundColor
                scrolled -> containerColor
                else -> null
            }
        }
    }

    @Test
    fun horizontalFloatingAppBar_transparentContainerColor() {
        val expectedColorBehindAppBar: Color = Color.Red
        rule.setMaterialContent(lightColorScheme()) {
            Box(modifier = Modifier.background(color = expectedColorBehindAppBar)) {
                HorizontalFloatingAppBar(
                    modifier = Modifier.testTag(FloatingAppBarTestTag),
                    expanded = false,
                    containerColor = Color.Transparent,
                    content = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    }
                )
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertContainsColor(expectedColorBehindAppBar)
    }

    @Test
    fun verticalFloatingAppBar_transparentContainerColor() {
        val expectedColorBehindAppBar: Color = Color.Red
        rule.setMaterialContent(lightColorScheme()) {
            Box(modifier = Modifier.background(color = expectedColorBehindAppBar)) {
                VerticalFloatingAppBar(
                    modifier = Modifier.testTag(FloatingAppBarTestTag),
                    expanded = false,
                    containerColor = Color.Transparent,
                    content = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                    }
                )
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertContainsColor(expectedColorBehindAppBar)
    }

    @Test
    fun horizontalFloatingAppBar_customContentPadding() {
        val expectedPadding: Dp = 20.dp
        rule.setMaterialContent(lightColorScheme()) {
            HorizontalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
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
            .onNodeWithTag(FloatingAppBarTestTag)
            .onChild()
            .assertTopPositionInRootIsEqualTo(expectedPadding)
    }

    @Test
    fun verticalFloatingAppBar_customContentPadding() {
        val expectedPadding: Dp = 20.dp
        rule.setMaterialContent(lightColorScheme()) {
            VerticalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
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
            .onNodeWithTag(FloatingAppBarTestTag)
            .onChild()
            .assertLeftPositionInRootIsEqualTo(expectedPadding)
    }

    @Test
    fun horizontalFloatingAppBar_trailingContent_expanded() {
        rule.setMaterialContent(lightColorScheme()) {
            HorizontalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
                expanded = true,
                trailingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                },
                content = {}
            )
        }

        rule.onNodeWithTag(FloatingAppBarTestTag).onChildren().assertCountEquals(1)
        rule.onNodeWithTag(FloatingAppBarTestTag).onChild().assertExists()
    }

    @Test
    fun horizontalFloatingAppBar_trailingContent_notExpanded() {
        rule.setMaterialContent(lightColorScheme()) {
            HorizontalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
                expanded = false,
                trailingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                },
                content = {}
            )
        }

        rule.onNodeWithTag(FloatingAppBarTestTag).onChildren().assertCountEquals(0)
        rule.onNodeWithTag(FloatingAppBarTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun horizontalFloatingAppBar_leadingContent_expanded() {
        rule.setMaterialContent(lightColorScheme()) {
            HorizontalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
                expanded = true,
                leadingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                },
                content = {}
            )
        }

        rule.onNodeWithTag(FloatingAppBarTestTag).onChildren().assertCountEquals(1)
        rule.onNodeWithTag(FloatingAppBarTestTag).onChild().assertExists()
    }

    @Test
    fun horizontalFloatingAppBar_leadingContent_notExpanded() {
        rule.setMaterialContent(lightColorScheme()) {
            HorizontalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
                expanded = false,
                leadingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                },
                content = {}
            )
        }

        rule.onNodeWithTag(FloatingAppBarTestTag).onChildren().assertCountEquals(0)
        rule.onNodeWithTag(FloatingAppBarTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun horizontalFloatingAppBar_leadingAndTrailingContent_expanded() {
        rule.setMaterialContent(lightColorScheme()) {
            HorizontalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
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

        rule.onNodeWithTag(FloatingAppBarTestTag).onChildren().assertCountEquals(2)
        rule.onNodeWithTag(FloatingAppBarTestTag).onChildAt(0).assertExists()
        rule.onNodeWithTag(FloatingAppBarTestTag).onChildAt(1).assertExists()
    }

    @Test
    fun horizontalFloatingAppBar_leadingAndTrailingContent_notExpanded() {
        rule.setMaterialContent(lightColorScheme()) {
            HorizontalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
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

        rule.onNodeWithTag(FloatingAppBarTestTag).onChildren().assertCountEquals(0)
        rule.onNodeWithTag(FloatingAppBarTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun verticalFloatingAppBar_trailingContent_expanded() {
        rule.setMaterialContent(lightColorScheme()) {
            VerticalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
                expanded = true,
                trailingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                },
                content = {}
            )
        }

        rule.onNodeWithTag(FloatingAppBarTestTag).onChildren().assertCountEquals(1)
        rule.onNodeWithTag(FloatingAppBarTestTag).onChild().assertExists()
    }

    @Test
    fun verticalFloatingAppBar_trailingContent_notExpanded() {
        rule.setMaterialContent(lightColorScheme()) {
            VerticalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
                expanded = false,
                trailingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                },
                content = {}
            )
        }

        rule.onNodeWithTag(FloatingAppBarTestTag).onChildren().assertCountEquals(0)
        rule.onNodeWithTag(FloatingAppBarTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun verticalFloatingAppBar_leadingContent_expanded() {
        rule.setMaterialContent(lightColorScheme()) {
            VerticalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
                expanded = true,
                leadingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                },
                content = {}
            )
        }

        rule.onNodeWithTag(FloatingAppBarTestTag).onChildren().assertCountEquals(1)
        rule.onNodeWithTag(FloatingAppBarTestTag).onChild().assertExists()
    }

    @Test
    fun verticalFloatingAppBar_leadingContent_notExpanded() {
        rule.setMaterialContent(lightColorScheme()) {
            VerticalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
                expanded = false,
                leadingContent = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                },
                content = {}
            )
        }

        rule.onNodeWithTag(FloatingAppBarTestTag).onChildren().assertCountEquals(0)
        rule.onNodeWithTag(FloatingAppBarTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun verticalFloatingAppBar_leadingAndTrailingContent_expanded() {
        rule.setMaterialContent(lightColorScheme()) {
            VerticalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
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

        rule.onNodeWithTag(FloatingAppBarTestTag).onChildren().assertCountEquals(2)
        rule.onNodeWithTag(FloatingAppBarTestTag).onChildAt(0).assertExists()
        rule.onNodeWithTag(FloatingAppBarTestTag).onChildAt(1).assertExists()
    }

    @Test
    fun verticalFloatingAppBar_leadingAndTrailingContent_notExpanded() {
        rule.setMaterialContent(lightColorScheme()) {
            VerticalFloatingAppBar(
                modifier = Modifier.testTag(FloatingAppBarTestTag),
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

        rule.onNodeWithTag(FloatingAppBarTestTag).onChildren().assertCountEquals(0)
        rule.onNodeWithTag(FloatingAppBarTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun state_restoresFloatingAppBarState() {
        val restorationTester = StateRestorationTester(rule)
        var floatingAppBarState: FloatingAppBarState? = null
        restorationTester.setContent { floatingAppBarState = rememberFloatingAppBarState() }

        rule.runOnIdle {
            floatingAppBarState!!.offsetLimit = -350f
            floatingAppBarState!!.offset = -300f
            floatingAppBarState!!.contentOffset = -550f
        }

        floatingAppBarState = null

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(floatingAppBarState!!.offsetLimit).isEqualTo(-350f)
            assertThat(floatingAppBarState!!.offset).isEqualTo(-300f)
            assertThat(floatingAppBarState!!.contentOffset).isEqualTo(-550f)
        }
    }

    private val FloatingAppBarTestTag = "floatingAppBar"
}
