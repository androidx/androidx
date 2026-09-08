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
import androidx.compose.remote.creation.compose.layout.RemoteFitBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
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
@Config(sdk = [35])
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
}
