/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.material3.lazy

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ResponsiveTransformingLazyColumnTest {
    private val firstItemTag = "firstItemTag"
    private val lastItemTag = "lastItemTag"
    private val lazyListTag = "LazyListTag"

    @get:Rule val rule = createComposeRule(effectContext = StandardTestDispatcher())

    @Test
    fun firstItemIsDisplayed() {
        rule.setContent {
            ResponsiveTransformingLazyColumn {
                items(100) {
                    Box(
                        Modifier.requiredSize(50.dp)
                            .testTag(
                                when (it) {
                                    0 -> firstItemTag
                                    99 -> lastItemTag
                                    else -> "empty"
                                }
                            )
                    )
                }
            }
        }
        rule.onNodeWithTag(firstItemTag).assertExists()
        rule.onNodeWithTag(lastItemTag).assertIsNotPlaced()
    }

    @Test
    fun appliesCorrectPadding_basedOnItemType() {
        lateinit var state: TransformingLazyColumnState
        var expectedTopPaddingPx = 0
        var expectedBottomPaddingPx = 0
        val screenHeightDp = 200

        rule.setContentWithConfigurationOverride(screenHeightDp) {
            val density = LocalDensity.current
            state = rememberTransformingLazyColumnState()

            // Calculate expected values based on the screen height
            // ListHeader = 13%, Card = 23%
            expectedTopPaddingPx = with(density) { (screenHeightDp.dp * 0.13f).roundToPx() }
            expectedBottomPaddingPx = with(density) { (screenHeightDp.dp * 0.23f).roundToPx() }

            ResponsiveTransformingLazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
                item(itemType = ResponsiveItemType.ListHeader) { Box(Modifier.requiredSize(50.dp)) }
                items(2) { BasicText("Item $it") }
                item(itemType = ResponsiveItemType.Card) { BasicText("Footer Card") }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(expectedTopPaddingPx)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(expectedBottomPaddingPx)
        }
    }

    @Test
    fun updatesPadding_whenFirstItemChanges() {
        val screenHeightDp = 200
        lateinit var state: TransformingLazyColumnState
        var headerPaddingPx = 0
        var cardPaddingPx = 0
        // Start with a Header (13%), followed by a Card (23%)
        val items =
            mutableStateListOf(
                TestItem(ResponsiveItemType.ListHeader),
                TestItem(ResponsiveItemType.Card),
            )
        rule.setContentWithConfigurationOverride(screenHeightDp) {
            state = rememberTransformingLazyColumnState()
            val density = LocalDensity.current
            headerPaddingPx = with(density) { (screenHeightDp.dp * 0.13f).roundToPx() }
            cardPaddingPx = with(density) { (screenHeightDp.dp * 0.23f).roundToPx() }

            ResponsiveTransformingLazyColumn(state = state) {
                itemsIndexed(items = items, itemType = { _, item -> item.type }) { _, _ ->
                    BasicText("Item")
                }
            }
        }
        // Initial State: Header is first.
        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(headerPaddingPx)
        }

        // Remove the Header. Now Card is first.
        rule.runOnIdle { items.removeAt(0) }

        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(cardPaddingPx)
        }
    }

    @Test
    fun updatesPadding_whenLastItemChanges() {
        val screenHeightDp = 200
        lateinit var state: TransformingLazyColumnState
        var buttonPaddingPx = 0 // 23%
        var compactButtonPaddingPx = 0 // 13%
        val items =
            mutableStateListOf(
                TestItem(ResponsiveItemType.Button),
                TestItem(ResponsiveItemType.CompactButton),
            )
        rule.setContentWithConfigurationOverride(screenHeightDp) {
            state = rememberTransformingLazyColumnState()
            val density = LocalDensity.current
            buttonPaddingPx = with(density) { (screenHeightDp.dp * 0.23f).roundToPx() }
            compactButtonPaddingPx = with(density) { (screenHeightDp.dp * 0.13f).roundToPx() }

            ResponsiveTransformingLazyColumn(state = state) {
                items(items = items, itemType = { _, item -> item.type }) { _ -> BasicText("Item") }
            }
        }

        // Initial State: CompactButton is last (13%)
        rule.runOnIdle {
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(compactButtonPaddingPx)
        }

        // Remove the CompactButton. Now Button is last (23%)
        rule.runOnIdle { items.removeAt(1) }

        rule.runOnIdle {
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(buttonPaddingPx)
        }
    }

    @Test
    fun respectsMinimumContentPadding() {
        lateinit var state: TransformingLazyColumnState
        val userTopPadding = 100.dp // Large value, definitively bigger than 13% of 200dp
        var expectedTopPaddingPx = 0
        val screenHeightDp = 200

        rule.setContentWithConfigurationOverride(screenHeightDp) {
            val density = LocalDensity.current
            state = rememberTransformingLazyColumnState()
            expectedTopPaddingPx = with(density) { userTopPadding.roundToPx() }

            ResponsiveTransformingLazyColumn(
                state = state,
                // This should override the smaller 13% header padding
                contentPadding = PaddingValues(top = userTopPadding),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(itemType = ResponsiveItemType.ListHeader) { Box(Modifier.requiredSize(50.dp)) }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(expectedTopPaddingPx)
        }
    }

    @Test
    fun respectsResponsivePadding_whenLargerThanMinimum() {
        val screenHeightDp = 1000 // Large screen
        lateinit var state: TransformingLazyColumnState
        var largeResponsivePaddingPx = 0
        // User asks for 10dp.
        // Responsive Card asks for 23% of 1000 = 230dp.
        // Result should be 230dp.
        rule.setContentWithConfigurationOverride(screenHeightDp) {
            state = rememberTransformingLazyColumnState()
            val density = LocalDensity.current
            largeResponsivePaddingPx = with(density) { (screenHeightDp.dp * 0.23f).roundToPx() }

            ResponsiveTransformingLazyColumn(
                state = state,
                contentPadding = PaddingValues(top = 10.dp),
            ) {
                item(itemType = ResponsiveItemType.Card) { BasicText("Card") }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(largeResponsivePaddingPx)
        }
    }

    @Test
    fun supportsEmptyItems() {
        rule.setContent {
            ResponsiveTransformingLazyColumn(modifier = Modifier.testTag(lazyListTag)) {
                items(10) {}
            }
        }
        rule.onNodeWithTag(lazyListTag).assertIsDisplayed()
    }

    @Test
    fun ignoresEmptyItems_whenCalculatingTopPadding() {
        val screenHeightDp = 200
        lateinit var state: TransformingLazyColumnState
        var expectedTopPaddingPx = 0

        rule.setContentWithConfigurationOverride(screenHeightDp) {
            state = rememberTransformingLazyColumnState()
            val density = LocalDensity.current
            // Card expects 23% top padding
            expectedTopPaddingPx = with(density) { (screenHeightDp.dp * 0.23f).roundToPx() }

            ResponsiveTransformingLazyColumn(state = state) {
                // 1. Add an empty block.
                items(0) { BasicText("Invisible") }
                // 2. Add the actual first item (Card). This should drive the padding.
                item(itemType = ResponsiveItemType.Card) { BasicText("Actual First Item") }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(expectedTopPaddingPx)
        }
    }

    @Test
    fun ignoresEmptyItems_whenCalculatingBottomPadding() {
        val screenHeightDp = 200
        lateinit var state: TransformingLazyColumnState
        var expectedBottomPaddingPx = 0

        rule.setContentWithConfigurationOverride(screenHeightDp) {
            state = rememberTransformingLazyColumnState()
            val density = LocalDensity.current
            // Card expects 23% bottom padding
            expectedBottomPaddingPx = with(density) { (screenHeightDp.dp * 0.23f).roundToPx() }

            ResponsiveTransformingLazyColumn(state = state) {
                // 1. Add the actual last item (Card). This should drive the padding.
                item(itemType = ResponsiveItemType.Card) { BasicText("Actual Last Item") }
                // 2. Add a trailing empty block.
                items(0) { BasicText("Invisible") }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(expectedBottomPaddingPx)
        }
    }

    @Test
    fun defaultsToZeroPadding_whenAllItemsAreEmpty() {
        lateinit var state: TransformingLazyColumnState

        rule.setContentWithConfigurationOverride(200) {
            state = rememberTransformingLazyColumnState()
            ResponsiveTransformingLazyColumn(state = state) {
                items(0) {}
                itemsIndexed(emptyList<String>()) { _, _ -> }
            }
        }

        rule.runOnIdle {
            // Should fallback to Default (0px) rather than crashing or using weird values
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(0)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(0)
        }
    }

    // Helper to force a specific screen height for deterministic percentage calculations
    private fun ComposeContentTestRule.setContentWithConfigurationOverride(
        screenHeightDp: Int,
        content: @Composable () -> Unit,
    ) {
        this.setContent {
            val newConfiguration = Configuration(LocalConfiguration.current)
            newConfiguration.screenHeightDp = screenHeightDp

            CompositionLocalProvider(LocalConfiguration provides newConfiguration) { content() }
        }
    }

    private data class TestItem(val type: ResponsiveItemType)
}

/**
 * Asserts that the current semantics node is not placed.
 *
 * Throws [AssertionError] if the node is placed.
 */
internal fun SemanticsNodeInteraction.assertIsNotPlaced() {
    try {
        // If the node does not exist, it implies that it is also not placed.
        assertDoesNotExist()
    } catch (e: AssertionError) {
        // If the node exists, we need to assert that it is not placed.
        val errorMessageOnFail = "Assert failed: The component is placed!"
        if (fetchSemanticsNode().layoutInfo.isPlaced) {
            throw AssertionError(errorMessageOnFail)
        }
    }
}
