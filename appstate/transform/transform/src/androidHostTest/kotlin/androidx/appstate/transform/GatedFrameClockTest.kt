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

package androidx.appstate.transform

import androidx.kruth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GatedFrameClockTest {

    @Test
    fun testGatedFrameClockInitialization() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val clock = GatedFrameClock(testScope, testDispatcher)

        assertThat(clock.isRunning).isTrue()
        clock.isRunning = false
        assertThat(clock.isRunning).isFalse()
    }

    @Test
    fun testGlobalSnapshotManagerEnsureStarted() {
        val testDispatcher = StandardTestDispatcher()
        // Ensure GlobalSnapshotManager can be initialized without error
        GlobalSnapshotManager.ensureStarted(testDispatcher)
    }
}
