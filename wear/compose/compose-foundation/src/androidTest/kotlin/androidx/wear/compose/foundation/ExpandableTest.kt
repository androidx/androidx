/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.foundation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ExpandableTest {
    @get:Rule val rule = createComposeRule()

    private val restorationTester = StateRestorationTester(rule)

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun initially_collapsed() =
        verifyExpandable(
            setupState = { rememberExpandableState(initiallyExpanded = false) },
            bitmapAssert = { assertDoesContainColor(COLLAPSED_COLOR) },
        )

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun initially_expanded() =
        verifyExpandable(
            setupState = { rememberExpandableState(initiallyExpanded = true) },
            bitmapAssert = { assertDoesContainColor(EXPANDED_COLOR) },
        )

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun expand() =
        verifyExpandable(
            setupState = { rememberExpandableState(initiallyExpanded = false) },
            bitmapAssert = { assertDoesContainColor(EXPANDED_COLOR) },
        ) { state ->
            state.expanded = true
            waitForIdle()
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun collapse() =
        verifyExpandable(
            setupState = { rememberExpandableState(initiallyExpanded = true) },
            bitmapAssert = { assertDoesContainColor(COLLAPSED_COLOR) },
        ) { state ->
            state.expanded = false
            waitForIdle()
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun collapsed_click() = verifyClick(false)

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun expanded_click() = verifyClick(true)

    @Test
    fun restoreState_after_recomposition() {
        var expandableState: ExpandableState? = null
        restorationTester.setContent {
            expandableState = rememberExpandableState() // initially set expanded to false
            expandableState?.expanded = true
        }

        rule.runOnUiThread {
            // set to null which signifies recomposition
            expandableState = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(expandableState?.expanded).isTrue()
    }

    @OptIn(ExperimentalWearFoundationApi::class)
    @Test
    fun rememberExpandableStateMapping_defaultParameters_createsCollapsedStateAndCachesInstance() {
        lateinit var mapping: ExpandableStateMapping<String>
        rule.setContent { mapping = rememberExpandableStateMapping() }

        val firstLookup = rule.runOnIdle { mapping.getOrPutNew("section_a") }
        val secondLookup = rule.runOnIdle { mapping.getOrPutNew("section_a") }

        rule.runOnIdle {
            assertThat(firstLookup.expanded).isFalse()
            assertThat(firstLookup.expandProgress).isWithin(0.0001f).of(0f)
            assertThat(secondLookup).isSameInstanceAs(firstLookup)
        }
    }

    @OptIn(ExperimentalWearFoundationApi::class)
    @Test
    fun rememberExpandableStateMapping_customParameters_appliesInitialStateAndTransitions() {
        lateinit var mapping: ExpandableStateMapping<Int>
        rule.setContent {
            mapping =
                rememberExpandableStateMapping(
                    initiallyExpanded = { key -> key % 2 == 0 },
                    expandAnimationSpec = TweenSpec(durationMillis = 100),
                    collapseAnimationSpec = TweenSpec(durationMillis = 100),
                )
        }

        val evenState = rule.runOnIdle { mapping.getOrPutNew(2) }
        val oddState = rule.runOnIdle { mapping.getOrPutNew(3) }
        val oddExpandedBeforeToggle = rule.runOnIdle { oddState.expanded }
        rule.runOnIdle { oddState.expanded = true }

        rule.runOnIdle {
            assertThat(oddExpandedBeforeToggle).isFalse()
            assertThat(evenState.expanded).isTrue()
            assertThat(evenState.expandProgress).isWithin(0.0001f).of(1f)
            assertThat(oddState.expanded).isTrue()
            assertThat(oddState.expandProgress).isWithin(0.0001f).of(1f)
        }
    }

    @Test
    fun rememberExpandableState_customAnimationSpecs_transitionsExpandedAndCollapsedStates() {
        lateinit var state: ExpandableState
        rule.setContent {
            state =
                rememberExpandableState(
                    initiallyExpanded = false,
                    expandAnimationSpec = TweenSpec(durationMillis = 100),
                    collapseAnimationSpec = TweenSpec(durationMillis = 100),
                )
        }

        rule.runOnIdle { state.expanded = true }
        val progressAfterExpand = rule.runOnIdle { state.expandProgress }
        rule.runOnIdle { state.expanded = false }

        rule.runOnIdle {
            assertThat(progressAfterExpand).isWithin(0.0001f).of(1f)
            assertThat(state.expanded).isFalse()
            assertThat(state.expandProgress).isWithin(0.0001f).of(0f)
        }
    }

    @Test
    fun expandableItems_whenCollapsed_emitsZeroItems() {
        lateinit var slcState: ScalingLazyListState
        rule.setContent {
            val state = rememberExpandableState(initiallyExpanded = false)
            ScalingLazyColumn(
                modifier = Modifier.size(100.dp),
                state = rememberScalingLazyListState().also { slcState = it },
                autoCentering = null,
            ) {
                expandableItems(state = state, count = 3) {
                    Box(modifier = Modifier.fillMaxWidth().height(20.dp))
                }
            }
        }

        val itemCount = rule.runOnIdle { slcState.layoutInfo.totalItemsCount }

        assertThat(itemCount).isEqualTo(0)
    }

    @Test
    fun expandableItems_whenExpanded_emitsAllItems() {
        lateinit var slcState: ScalingLazyListState
        rule.setContent {
            val state = rememberExpandableState(initiallyExpanded = true)
            ScalingLazyColumn(
                modifier = Modifier.size(100.dp),
                state = rememberScalingLazyListState().also { slcState = it },
                autoCentering = null,
            ) {
                expandableItems(state = state, count = 3) {
                    Box(modifier = Modifier.fillMaxWidth().height(20.dp))
                }
            }
        }

        val itemCount = rule.runOnIdle { slcState.layoutInfo.totalItemsCount }

        assertThat(itemCount).isEqualTo(3)
    }

    @Test
    fun expandableItems_withCustomKey_expandsAndPreservesItemKeys() {
        lateinit var slcState: ScalingLazyListState
        lateinit var state: ExpandableState
        rule.setContent {
            state = rememberExpandableState(initiallyExpanded = false)
            ScalingLazyColumn(
                modifier = Modifier.size(100.dp),
                state = rememberScalingLazyListState().also { slcState = it },
                autoCentering = null,
            ) {
                expandableItems(state = state, count = 2, key = { index -> "item_key_$index" }) {
                    Box(modifier = Modifier.fillMaxWidth().height(20.dp))
                }
            }
        }

        rule.runOnIdle { state.expanded = true }

        rule.runOnIdle {
            assertThat(slcState.layoutInfo.visibleItemsInfo.map { it.key })
                .containsExactly("item_key_0", "item_key_1")
                .inOrder()
        }
    }

    @Test
    fun expandableButton_whenCollapsedWithDefaultKey_isDisplayedAtFullHeight() {
        rule.setContent {
            val state = rememberExpandableState(initiallyExpanded = false)
            ScalingLazyColumn(modifier = Modifier.size(100.dp), autoCentering = null) {
                expandableButton(state = state) {
                    Box(modifier = Modifier.testTag("expand_btn").size(40.dp))
                }
            }
        }

        rule.onNodeWithTag("expand_btn").assertIsDisplayed().assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun expandableButton_whenExpandedWithCustomKey_collapsesHeightToZero() {
        lateinit var slcState: ScalingLazyListState
        lateinit var state: ExpandableState
        rule.setContent {
            state = rememberExpandableState(initiallyExpanded = false)
            ScalingLazyColumn(
                modifier = Modifier.size(100.dp),
                state = rememberScalingLazyListState().also { slcState = it },
                autoCentering = null,
            ) {
                expandableButton(state = state, key = "show_more_button") {
                    Box(modifier = Modifier.fillMaxWidth().height(30.dp))
                }
            }
        }

        rule.runOnIdle { state.expanded = true }

        rule.runOnIdle {
            val buttonItem =
                slcState.layoutInfo.visibleItemsInfo.first { it.key == "show_more_button" }
            assertThat(buttonItem.size).isEqualTo(0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifyClick(initiallyExpanded: Boolean) {
        val clicked = mutableListOf<Boolean>()
        verifyExpandable(
            setupState = { rememberExpandableState(initiallyExpanded = initiallyExpanded) },
            bitmapAssert = { assertThat(clicked).containsExactly(initiallyExpanded) },
            expandableContent = { expanded ->
                Box(modifier = Modifier.fillMaxSize().clickable { clicked.add(expanded) })
            },
        ) { _ ->
            onNodeWithTag(TEST_TAG).performClick()
            waitForIdle()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifyExpandable(
        setupState: @Composable () -> ExpandableState,
        bitmapAssert: ImageBitmap.() -> Unit,
        expandableContent: @Composable (Boolean) -> Unit = {},
        act: ComposeTestRule.(ExpandableState) -> Unit = {},
    ) {
        // Arrange - set up the content for the test including expandable content
        var slcState: ScalingLazyListState? = null
        var state: ExpandableState? = null
        rule.setContent {
            state = setupState()
            Box(Modifier.testTag(TEST_TAG).size(100.dp)) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { slcState = it },
                    // We can only test expandableItem inside a ScalingLazyColumn, but we can make
                    // it behave mostly as it wasn't there.
                    scalingParams =
                        ScalingLazyColumnDefaults.scalingParams(edgeScale = 1f, edgeAlpha = 1f),
                    autoCentering = null,
                    verticalArrangement = Arrangement.spacedBy(space = 0.dp),
                ) {
                    expandableItem(state!!) { expanded ->
                        Box(
                            Modifier.fillMaxWidth()
                                .height(100.dp)
                                .background(if (expanded) EXPANDED_COLOR else COLLAPSED_COLOR)
                        ) {
                            expandableContent(expanded)
                        }
                    }
                }
            }
        }
        rule.waitUntil { slcState?.initialized?.value ?: false }

        // Act - exercise the expandable if required for the test.
        with(rule) { act(state!!) }

        // Assert - verify the object under test worked correctly
        rule.onNodeWithTag(TEST_TAG).captureToImage().apply { bitmapAssert() }
    }

    private val EXPANDED_COLOR = Color.Red
    private val COLLAPSED_COLOR = Color.Green
}
