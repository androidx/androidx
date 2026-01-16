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

package androidx.xr.glimmer

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.test.runner.AndroidJUnitRunner
import org.junit.runner.Description
import org.junit.runner.manipulation.Filter

/**
 * Test runner for Glimmer tests that does not run tests on devices with SDK below 33.
 *
 * The expected min SDK for Glimmer is 35, however, we test on 33+ for wider device coverage (some
 * APIs are not available below 33).
 */
class GlimmerTestRunner : AndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.w(
                "GlimmerTestRunner",
                "Skipping tests: Device SDK ${Build.VERSION.SDK_INT} < ${Build.VERSION_CODES.TIRAMISU}",
            )
            // Inject a custom filter that rejects all tests.
            arguments.putString("filter", SkipAllFilter::class.java.name)
        }
        super.onCreate(arguments)
    }

    /**
     * A JUnit Filter that rejects all tests.
     *
     * This must be public so the runner can instantiate it via reflection.
     */
    class SkipAllFilter : Filter() {
        override fun shouldRun(description: Description?): Boolean = false

        override fun describe(): String = "Skips all tests due to low SDK"
    }
}
