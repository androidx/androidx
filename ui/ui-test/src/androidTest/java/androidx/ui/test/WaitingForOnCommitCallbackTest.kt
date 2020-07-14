/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test

import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.onCommit
import androidx.compose.setValue
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

@MediumTest
@RunWith(JUnit4::class)
class WaitingForOnCommitCallbackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun setContentAndWaitForIdleReleasesAfterOnCommitCallback() {
        val atomicBoolean = AtomicBoolean(false)
        var switch by mutableStateOf(true)
        composeTestRule.setContent {
            onCommit(switch) {
                atomicBoolean.set(switch)
            }
        }

        assertThat(atomicBoolean.get()).isTrue()

        runOnIdleCompose {
            switch = false
        }
        waitForIdle()

        assertThat(atomicBoolean.get()).isFalse()
    }

    @Test
    @FlakyTest(bugId = 160399857, detail = "Fails about 1% of the time")
    fun cascadingOnCommits() {
        // Collect unique values (markers) at each step during the process and
        // at the end verify that they were collected in the right order
        val values = mutableListOf<Int>()

        // Use a latch to make sure all collection events have occurred, to avoid
        // concurrent modification exceptions when checking the collected values,
        // in case some values still need to be collected due to a bug.
        var latch = CountDownLatch(0)

        var switch1 by mutableStateOf(true)
        var switch2 by mutableStateOf(true)
        var switch3 by mutableStateOf(true)
        var switch4 by mutableStateOf(true)
        composeTestRule.setContent {
            onCommit(switch1) {
                values.add(2)
                switch2 = switch1
            }
            onCommit(switch2) {
                values.add(3)
                switch3 = switch2
            }
            onCommit(switch3) {
                values.add(4)
                switch4 = switch3
            }
            onCommit(switch4) {
                values.add(5)
                latch.countDown()
            }
        }

        runOnIdleCompose {
            latch = CountDownLatch(1)
            values.clear()

            // Kick off the cascade
            values.add(1)
            switch1 = false
        }

        waitForIdle()
        // Mark the end
        values.add(6)

        // Make sure all writes into the list are complete
        latch.await()

        // And check if all was in the right order
        assertThat(values).containsExactly(1, 2, 3, 4, 5, 6).inOrder()
    }
}
