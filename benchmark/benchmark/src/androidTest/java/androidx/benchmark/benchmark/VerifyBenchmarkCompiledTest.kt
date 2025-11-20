/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark.benchmark

import androidx.benchmark.Arguments
import androidx.benchmark.Shell
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test to help validate compilation occurs. In the future, consider moving this to be a module-wide
 * configurable assert
 *
 * Note that while most non-benchmark tests shouldn't live in benchmark modules, this is an
 * exception, as it's validating runtime conditions (esp in CI)
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class VerifyBenchmarkCompiledTest {
    @Test
    fun verifyCompilation() {
        assumeFalse("ignoring compilation state in dry run mode", Arguments.dryRunMode)
        val stdout =
            Shell.executeScriptCaptureStdout(
                "dumpsys package dexopt | grep -A 1 \"androidx.benchmark.benchmark.test\""
            )
        assertTrue(
            "expected exactly one instance of compilation status, output = $stdout",
            stdout.indexOf("[status=") == stdout.lastIndexOf("[status="),
        )
        assertTrue(
            "expected dexopt to show speed compilation, output = $stdout",
            stdout.contains("[status=speed]"),
        )
    }
}
