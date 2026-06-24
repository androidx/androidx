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

package androidx.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.TestCase.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BenchmarkStateTest {
    @Test
    fun thermalThrottleDoesNotCrash() {
        val state =
            BenchmarkState(
                TestDefinition("com.example.TestClass", "TestClass", "testMethod"),
                MicrobenchmarkConfig(warmupCount = 1, measurementCount = 1),
            )
        try {
            // Force thermal throttle event to return true on checks
            ThrottleDetector.insertSingleFakeThrottleDetection = true

            var loopCount = 0
            while (state.keepRunning()) {
                loopCount++
            }
            assertFalse(
                "Expected thermal throttle",
                ThrottleDetector.insertSingleFakeThrottleDetection,
            )
        } finally {
            // Restore clean state
            ThrottleDetector.insertSingleFakeThrottleDetection = false
        }
    }
}
