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
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
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
}
