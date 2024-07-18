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
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExpandableTest {
    @get:Rule
    val rule = createComposeRule()

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun initially_collapsed() =
        verifyExpandable(
            setupState = { rememberExpandableState(initiallyExpanded = false) },
            bitmapAssert = {
                assertDoesContainColor(COLLAPSED_COLOR)
            }
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun initially_expanded() =
        verifyExpandable(
            setupState = { rememberExpandableState(initiallyExpanded = true) },
            bitmapAssert = {
                assertDoesContainColor(EXPANDED_COLOR)
            }
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun expand() =
        verifyExpandable(
            setupState = { rememberExpandableState(initiallyExpanded = false) },
            bitmapAssert = {
                assertDoesContainColor(EXPANDED_COLOR)
            }
        ) { state ->
            state.expanded = true
            waitForIdle()
        }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun collapse() =
        verifyExpandable(
            setupState = { rememberExpandableState(initiallyExpanded = true) },
            bitmapAssert = {
                assertDoesContainColor(COLLAPSED_COLOR)
            }
        ) { state ->
            state.expanded = false
            waitForIdle()
        }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun collapsed_click() = verifyClick(false)

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun expanded_click() = verifyClick(true)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifyClick(initiallyExpanded: Boolean) {
        val clicked = mutableListOf<Boolean>()
        verifyExpandable(
            setupState = { rememberExpandableState(initiallyExpanded = initiallyExpanded) },
            bitmapAssert = {
                assertEquals(listOf(initiallyExpanded), clicked)
            },
            expandableContent = { expanded ->
                Box(modifier = Modifier.fillMaxSize().clickable {
                    clicked.add(expanded)
                })
            }
        ) { _ ->
            onNodeWithTag(TEST_TAG).performClick()
            waitForIdle()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifyExpandable(
        setupState: @Composable () -> ExpandableState,
        bitmapAssert: ImageBitmap.() -> Unit,
        expandableContent: @Composable (Boolean) -> Unit = { },
        act: ComposeTestRule.(ExpandableState) -> Unit = { }
    ) {
        // Arrange - set up the content for the test including expandable content
        var slcState: ScalingLazyListState? = null
        var state: ExpandableState? = null
        rule.setContent {
            state = setupState()
            Box(
                Modifier
                    .testTag(TEST_TAG)
                    .size(100.dp)) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState().also { slcState = it },
                    // We can only test expandableItem inside a ScalingLazyColumn, but we can make
                    // it behave mostly as it wasn't there.
                    scalingParams = ScalingLazyColumnDefaults
                        .scalingParams(edgeScale = 1f, edgeAlpha = 1f),
                    autoCentering = null,
                    verticalArrangement = Arrangement.spacedBy(space = 0.dp),
                ) {
                    expandableItem(state!!) { expanded ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(
                                    if (expanded) EXPANDED_COLOR else COLLAPSED_COLOR
                                )
                        ) {
                            expandableContent(expanded)
                        }
                    }
                }
            }
        }
        rule.waitUntil { slcState?.initialized?.value ?: false }

        // Act - exercise the expandable if required for the test.
        with(rule) {
            act(state!!)
        }

        // Assert - verify the object under test worked correctly
        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .apply { bitmapAssert() }
    }

    private val EXPANDED_COLOR = Color.Red
    private val COLLAPSED_COLOR = Color.Green
}
