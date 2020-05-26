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

package androidx.ui.test.partialgesturescope

import androidx.test.filters.MediumTest
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.createComposeRule
import androidx.ui.test.partialgesturescope.Common.partialGesture
import androidx.ui.test.sendCancel
import androidx.ui.test.sendDown
import androidx.ui.test.sendMove
import androidx.ui.test.sendUp
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.expectError
import androidx.ui.unit.PxPosition
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

/**
 * Tests the error states of [sendMove] that are not tested in [SendMoveToTest] and [SendMoveByTest]
 */
@MediumTest
class SendMoveTest() {
    companion object {
        private val downPosition1 = PxPosition(10f, 10f)
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val inputDispatcherRule: TestRule =
        AndroidInputDispatcher.TestRule(disableDispatchInRealTime = true)

    @Before
    fun setUp() {
        // Given some content
        composeTestRule.setContent {
            ClickableTestBox()
        }
    }

    @Test
    fun moveWithoutDown() {
        expectError<IllegalStateException> {
            partialGesture { sendMove() }
        }
    }

    @Test
    fun moveAfterUp() {
        partialGesture { sendDown(downPosition1) }
        partialGesture { sendUp() }
        expectError<IllegalStateException> {
            partialGesture { sendMove() }
        }
    }

    @Test
    fun moveAfterCancel() {
        partialGesture { sendDown(downPosition1) }
        partialGesture { sendCancel() }
        expectError<IllegalStateException> {
            partialGesture { sendMove() }
        }
    }
}
