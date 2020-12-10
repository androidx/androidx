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

package androidx.benchmark.macro.test

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.compile
import androidx.benchmark.macro.device
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ActionsTest {
    @Test
    @Ignore("Figure out why we can't launch the default activity.")
    fun killTest() {
        val scope = MacrobenchmarkScope(PACKAGE_NAME, launchWithClearTask = true)
        scope.pressHome()
        scope.launchPackageAndWait()
        assertTrue(isProcessAlive(PACKAGE_NAME))
        scope.killProcess()
        assertFalse(isProcessAlive(PACKAGE_NAME))
    }

    @Test
    @Ignore("Compilation modes are a bit flaky")
    fun compile_speedProfile() {
        val scope = MacrobenchmarkScope(PACKAGE_NAME, launchWithClearTask = true)
        val iterations = 1
        var executions = 0
        val compilation = CompilationMode.SpeedProfile(warmupIterations = iterations)
        compilation.compile(PACKAGE_NAME) {
            executions += 1
            scope.pressHome()
            scope.launchPackageAndWait()
        }
        assertEquals(iterations, executions)
    }

    @Test
    @Ignore("Compilation modes are a bit flaky")
    fun compile_speed() {
        val compilation = CompilationMode.Speed
        compilation.compile(PACKAGE_NAME) {
            fail("Should never be called for $compilation")
        }
    }

    private fun processes(): List<String> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val output = instrumentation.device().executeShellCommand("ps -A")
        return output.split("\r?\n".toRegex())
    }

    private fun isProcessAlive(packageName: String): Boolean {
        return processes().any { it.contains(packageName) }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.benchmark.integration.macrobenchmark.target"
    }
}
