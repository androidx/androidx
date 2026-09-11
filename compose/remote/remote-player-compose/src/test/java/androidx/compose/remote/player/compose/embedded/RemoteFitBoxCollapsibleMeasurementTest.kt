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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleRow
import androidx.compose.remote.creation.compose.layout.RemoteFitBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w450dp-h900dp")
class RemoteFitBoxCollapsibleMeasurementTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun fitBox_withCollapsibleColumnChild_adaptsWhenResizedSmallerThenBigger() {
        val containerSize = mutableStateOf(DpSize(400.dp, 500.dp))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        rule.setRemoteContent(
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = context,
                    size = Size(400f * density, 500f * density),
                ),
            playComposableWrapper = { content ->
                Box(
                    modifier = Modifier.size(containerSize.value.width, containerSize.value.height)
                ) {
                    content()
                }
            },
        ) {
            RemoteFitBox(modifier = RemoteModifier.fillMaxSize()) {
                // Candidate 0 (Primary Tier):
                // Header (250dp) + Section A (200dp) + Section B (200dp) = 650dp.
                // At 500dp available height: Section B collapses (needs 650dp > 500dp).
                // Header + Section A = 450dp (<= 500dp), so Candidate 0 fits at 500dp.
                // At 200dp available height: Header alone is 250dp (> 200dp), so Candidate 0
                // cannot fit even with all collapsible sections collapsed. FitBox falls back
                // to Candidate 1 (100dp).
                RemoteCollapsibleColumn(modifier = RemoteModifier.fillMaxWidth()) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxWidth().height(250.rdp).collapsiblePriority(10f)
                    ) {
                        RemoteText("Primary Tier Header".rs)
                    }
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxWidth().height(200.rdp).collapsiblePriority(5f)
                    ) {
                        RemoteText("Primary Tier Section A".rs)
                    }
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxWidth().height(200.rdp).collapsiblePriority(1f)
                    ) {
                        RemoteText("Primary Tier Section B".rs)
                    }
                }
                // Candidate 1 (Compact Fallback Tier):
                RemoteBox(modifier = RemoteModifier.fillMaxWidth().height(100.rdp)) {
                    RemoteText("Fallback Compact Tier".rs)
                }
            }
        }

        // At 500dp height, Primary Tier fits because Section B collapses to 450dp.
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("Primary Tier Header").assertIsDisplayed()
        rule.onNodeWithText("Primary Tier Section A").assertIsDisplayed()
        rule.onNodeWithText("Primary Tier Section B").assertIsNotDisplayed()
        rule.onNodeWithText("Fallback Compact Tier").assertDoesNotExist()

        // Resize smaller to 200dp: Primary Tier minimum height is 250dp (Header alone),
        // which no longer fits in 200dp. FitBox adapts and switches to Fallback Compact Tier
        // (100dp).
        containerSize.value = DpSize(400.dp, 200.dp)
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("Fallback Compact Tier").assertIsDisplayed()
        rule.onNodeWithText("Primary Tier Header").assertDoesNotExist()

        // Resize bigger back to 500dp: Primary Tier fits again and is selected.
        containerSize.value = DpSize(400.dp, 500.dp)
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("Primary Tier Header").assertIsDisplayed()
        rule.onNodeWithText("Primary Tier Section A").assertIsDisplayed()
        rule.onNodeWithText("Primary Tier Section B").assertIsNotDisplayed()
        rule.onNodeWithText("Fallback Compact Tier").assertDoesNotExist()
    }

    @Test
    fun fitBox_withCollapsibleColumnExactWidth_selectsCandidateWhenWidthFits() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        rule.setRemoteContent(
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = context,
                    size = Size(411f * density, 891f * density),
                ),
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(411.dp, 891.dp)) {
                    content()
                }
            },
        ) {
            RemoteFitBox(modifier = RemoteModifier.fillMaxSize()) {
                // Candidate 0: CollapsibleColumn with exact width (380dp) + padding (14dp),
                // total width fits in 411dp. Mimics WeatherDashboard in b/559158081.
                RemoteCollapsibleColumn(modifier = RemoteModifier.width(380.rdp).padding(14.rdp)) {
                    RemoteBox(modifier = RemoteModifier.fillMaxWidth().height(200.rdp)) {
                        RemoteText("WeatherDashboard".rs)
                    }
                }
                // Candidate 1: Compact fallback (mimics WeatherMiniWidgetCard)
                RemoteBox(modifier = RemoteModifier.size(140.rdp, 120.rdp)) {
                    RemoteText("WeatherMiniWidget".rs)
                }
            }
        }

        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("WeatherDashboard").assertIsDisplayed()
        rule.onNodeWithText("WeatherMiniWidget").assertDoesNotExist()
    }

    @Test
    fun fitBox_withCollapsibleColumnExactWidth_fallsBackWhenWidthTooNarrow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        rule.setRemoteContent(
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = context,
                    size = Size(350f * density, 800f * density),
                ),
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(350.dp, 800.dp)) {
                    content()
                }
            },
        ) {
            RemoteFitBox(modifier = RemoteModifier.fillMaxSize()) {
                // Candidate 0: Width 380dp exceeds 350dp container width -> should NOT fit
                RemoteCollapsibleColumn(modifier = RemoteModifier.width(380.rdp).padding(14.rdp)) {
                    RemoteBox(modifier = RemoteModifier.fillMaxWidth().height(200.rdp)) {
                        RemoteText("WideDashboard".rs)
                    }
                }
                // Candidate 1: Compact fallback (140dp fits in 350dp)
                RemoteBox(modifier = RemoteModifier.size(140.rdp, 120.rdp)) {
                    RemoteText("CompactFallback".rs)
                }
            }
        }

        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("CompactFallback").assertIsDisplayed()
        rule.onNodeWithText("WideDashboard").assertDoesNotExist()
    }

    @Test
    fun fitBox_withCollapsibleColumnExactWidth_adaptsWhenResizedBetweenNarrowAndWide() {
        val containerSize = mutableStateOf(DpSize(411.dp, 600.dp))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        rule.setRemoteContent(
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = context,
                    size = Size(411f * density, 600f * density),
                ),
            playComposableWrapper = { content ->
                Box(
                    modifier = Modifier.size(containerSize.value.width, containerSize.value.height)
                ) {
                    content()
                }
            },
        ) {
            RemoteFitBox(modifier = RemoteModifier.fillMaxSize()) {
                // Candidate 0: Exact width 380dp
                RemoteCollapsibleColumn(modifier = RemoteModifier.width(380.rdp).padding(14.rdp)) {
                    RemoteBox(modifier = RemoteModifier.fillMaxWidth().height(200.rdp)) {
                        RemoteText("DashboardTier".rs)
                    }
                }
                // Candidate 1: Fallback 140dp
                RemoteBox(modifier = RemoteModifier.size(140.rdp, 120.rdp)) {
                    RemoteText("MiniTier".rs)
                }
            }
        }

        // Initially 411dp wide: DashboardTier (380dp) fits
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("DashboardTier").assertIsDisplayed()
        rule.onNodeWithText("MiniTier").assertDoesNotExist()

        // Resize narrower to 300dp: DashboardTier (380dp) does not fit, falls back to MiniTier
        containerSize.value = DpSize(300.dp, 600.dp)
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("MiniTier").assertIsDisplayed()
        rule.onNodeWithText("DashboardTier").assertDoesNotExist()

        // Resize wider back to 411dp: DashboardTier fits again
        containerSize.value = DpSize(411.dp, 600.dp)
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("DashboardTier").assertIsDisplayed()
        rule.onNodeWithText("MiniTier").assertDoesNotExist()
    }

    @Test
    fun fitBox_withCollapsibleRowExactHeight_selectsCandidateWhenHeightFits() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        rule.setRemoteContent(
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = context,
                    size = Size(400f * density, 300f * density),
                ),
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(400.dp, 300.dp)) {
                    content()
                }
            },
        ) {
            RemoteFitBox(modifier = RemoteModifier.fillMaxSize()) {
                // Candidate 0: Exact height 150dp fits in 300dp height
                RemoteCollapsibleRow(
                    modifier = RemoteModifier.fillMaxWidth().height(150.rdp).padding(10.rdp)
                ) {
                    RemoteBox(modifier = RemoteModifier.width(100.rdp).fillMaxHeight()) {
                        RemoteText("RowPrimary".rs)
                    }
                }
                // Candidate 1: Fallback
                RemoteBox(modifier = RemoteModifier.size(80.rdp, 60.rdp)) {
                    RemoteText("RowFallback".rs)
                }
            }
        }

        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("RowPrimary").assertIsDisplayed()
        rule.onNodeWithText("RowFallback").assertDoesNotExist()
    }

    @Test
    fun fitBox_withCollapsibleRowExactHeight_fallsBackWhenHeightTooShort() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        rule.setRemoteContent(
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = context,
                    size = Size(400f * density, 120f * density),
                ),
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(400.dp, 120.dp)) {
                    content()
                }
            },
        ) {
            RemoteFitBox(modifier = RemoteModifier.fillMaxSize()) {
                // Candidate 0: Exact height 150dp does NOT fit in 120dp height
                RemoteCollapsibleRow(
                    modifier = RemoteModifier.fillMaxWidth().height(150.rdp).padding(10.rdp)
                ) {
                    RemoteBox(modifier = RemoteModifier.width(100.rdp).fillMaxHeight()) {
                        RemoteText("RowPrimary".rs)
                    }
                }
                // Candidate 1: Fallback 60dp height fits in 120dp
                RemoteBox(modifier = RemoteModifier.size(80.rdp, 60.rdp)) {
                    RemoteText("RowFallback".rs)
                }
            }
        }

        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("RowFallback").assertIsDisplayed()
        rule.onNodeWithText("RowPrimary").assertDoesNotExist()
    }

    @Test
    fun fitBox_withCollapsibleColumnExactWidthAndCollapsibleChildren_adaptsByHeight() {
        val containerSize = mutableStateOf(DpSize(411.dp, 400.dp))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        rule.setRemoteContent(
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = context,
                    size = Size(411f * density, 400f * density),
                ),
            playComposableWrapper = { content ->
                Box(
                    modifier = Modifier.size(containerSize.value.width, containerSize.value.height)
                ) {
                    content()
                }
            },
        ) {
            RemoteFitBox(modifier = RemoteModifier.fillMaxSize()) {
                // Candidate 0: Exact width 380dp, with two sections:
                // Header: 150dp (priority 10)
                // Section: 150dp (priority 1)
                // Total = 300dp. Min collapsible height = 150dp.
                RemoteCollapsibleColumn(modifier = RemoteModifier.width(380.rdp).padding(10.rdp)) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxWidth().height(150.rdp).collapsiblePriority(10f)
                    ) {
                        RemoteText("ColHeader".rs)
                    }
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxWidth().height(150.rdp).collapsiblePriority(1f)
                    ) {
                        RemoteText("ColSection".rs)
                    }
                }
                // Candidate 1: Fallback 80dp
                RemoteBox(modifier = RemoteModifier.size(100.rdp, 80.rdp)) {
                    RemoteText("CompactTier".rs)
                }
            }
        }

        // At 400dp height: 300dp total fits -> both Header and Section displayed
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("ColHeader").assertIsDisplayed()
        rule.onNodeWithText("ColSection").assertIsDisplayed()
        rule.onNodeWithText("CompactTier").assertDoesNotExist()

        // At 200dp height: Section collapses (needs 300dp > 200dp).
        // Header (150dp) fits <= 200dp, so Candidate 0 is still chosen.
        containerSize.value = DpSize(411.dp, 200.dp)
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("ColHeader").assertIsDisplayed()
        rule.onNodeWithText("ColSection").assertIsNotDisplayed()
        rule.onNodeWithText("CompactTier").assertDoesNotExist()

        // At 100dp height: Header (150dp) no longer fits in 100dp.
        // Fallback to Candidate 1 (80dp).
        containerSize.value = DpSize(411.dp, 100.dp)
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("CompactTier").assertIsDisplayed()
        rule.onNodeWithText("ColHeader").assertDoesNotExist()
    }
}
