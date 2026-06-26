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

package androidx.compose.ui.test.injectionscope.indirecttouch

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.inputDeviceBottomCenter
import androidx.compose.ui.test.inputDeviceTopCenter
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performIndirectPointerInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test to see if we can achieve precise scroll motion when injecting indirect pointer events in the
 * presence of touch slop.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class SwipeWithTouchSlopTest {
    companion object {
        private val inputDeviceSize = IntSize(1000, 1500)
    }

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun swipeScrollable_accountForTouchSlop() {
        val touchSlop = 18f
        val scrollState = ScrollState(initial = 5000)
        var composeView: android.view.View? = null
        rule.setContent {
            composeView = androidx.compose.ui.platform.LocalView.current
            WithTouchSlop(touchSlop) {
                with(LocalDensity.current) {
                    // Scrollable with a viewport the size of 10 boxes
                    Column(
                        Modifier.testTag("scrollable")
                            .requiredSize(100.toDp(), 1000.toDp())
                            .verticalScroll(scrollState)
                            .focusable()
                    ) {
                        repeat(100) { ClickableTestBox() }
                    }
                }
            }
        }

        assertThat(scrollState.value).isEqualTo(5000)
        // numBoxes * boxHeight - viewportHeight = 100 * 100 - 1000
        assertThat(scrollState.maxValue).isEqualTo(9000)

        val swipeDistance = 800f - touchSlop
        rule.onNodeWithTag("scrollable").requestFocus()
        rule.onNodeWithTag("scrollable").assertIsFocused()
        rule.onNodeWithTag("scrollable").performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.Y,
            inputDeviceSize,
        ) {
            val from = inputDeviceBottomCenter - Offset(0f, 499f)
            val touchSlopThreshold = from - Offset(0f, touchSlop)
            val to = inputDeviceTopCenter + Offset(0f, 200f)

            down(from)
            moveTo(touchSlopThreshold)
            moveTo(to)
            up()
        }

        assertThat(scrollState.value).isEqualTo(5000 - swipeDistance.roundToInt())
        assertThat(scrollState.maxValue).isEqualTo(9000)
    }
}
