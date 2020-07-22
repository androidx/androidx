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

package androidx.ui.test.inputdispatcher

import androidx.compose.ui.geometry.Offset
import androidx.ui.test.InputDispatcher.InputDispatcherTestRule
import androidx.ui.test.android.AndroidInputDispatcher
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class IsGestureInProgressTest {
    companion object {
        private val anyPosition = Offset.Zero
    }

    @get:Rule
    val inputDispatcherRule: TestRule = InputDispatcherTestRule(disableDispatchInRealTime = true)

    private val subject = AndroidInputDispatcher {}

    @Test
    fun downUp() {
        assertThat(subject.isGestureInProgress).isFalse()
        subject.sendDown(1, anyPosition)
        assertThat(subject.isGestureInProgress).isTrue()
        subject.sendUp(1)
        assertThat(subject.isGestureInProgress).isFalse()
    }

    @Test
    fun downCancel() {
        assertThat(subject.isGestureInProgress).isFalse()
        subject.sendDown(1, anyPosition)
        assertThat(subject.isGestureInProgress).isTrue()
        subject.sendCancel()
        assertThat(subject.isGestureInProgress).isFalse()
    }
}