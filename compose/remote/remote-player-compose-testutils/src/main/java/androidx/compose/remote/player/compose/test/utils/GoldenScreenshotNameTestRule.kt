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

package androidx.compose.remote.player.compose.test.utils

import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A [TestWatcher] rule that captures the current test's [Description] to be used to generate the
 * golden screenshot name automatically.
 */
class GoldenScreenshotNameTestRule : TestWatcher() {
    lateinit var testDescription: Description
        private set

    override fun starting(description: Description) {
        testDescription = description
    }

    /** Returns a [GoldenScreenshotName] based on the captured test [Description]. */
    fun getGoldenScreenshotName(suffix: String? = null): GoldenScreenshotName {
        return GoldenScreenshotName(testDescription, suffix)
    }
}
