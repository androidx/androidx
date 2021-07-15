/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SdkSuppress(minSdkVersion = 27) // Lowest version validated
@RunWith(AndroidJUnit4::class)
@LargeTest
public class MacrobenchmarkScopeTest {
    @Before
    fun setup() {
        // validate target is installed with clear error message,
        // since error messages from e.g. startActivityAndWait may be less clear
        try {
            val pm = InstrumentationRegistry.getInstrumentation().context.packageManager
            pm.getApplicationInfo(TARGET_PACKAGE_NAME, 0)
        } catch (notFoundException: PackageManager.NameNotFoundException) {
            throw IllegalStateException(
                "Unable to find target $TARGET_PACKAGE_NAME, is it installed?"
            )
        }
    }

    @Test
    public fun killTest() {
        val scope = MacrobenchmarkScope(TARGET_PACKAGE_NAME, launchWithClearTask = true)
        scope.pressHome()
        scope.startActivityAndWait()
        assertTrue(isProcessAlive(TARGET_PACKAGE_NAME))
        scope.killProcess()
        assertFalse(isProcessAlive(TARGET_PACKAGE_NAME))
    }

    @Test
    public fun compile_speedProfile() {
        val scope = MacrobenchmarkScope(TARGET_PACKAGE_NAME, launchWithClearTask = true)
        val iterations = 1
        var executions = 0
        val compilation = CompilationMode.SpeedProfile(warmupIterations = iterations)
        compilation.compile(TARGET_PACKAGE_NAME) {
            executions += 1
            scope.pressHome()
            scope.startActivityAndWait()
        }
        assertEquals(iterations, executions)
    }

    @Test
    public fun compile_speed() {
        val compilation = CompilationMode.Speed
        compilation.compile(TARGET_PACKAGE_NAME) {
            fail("Should never be called for $compilation")
        }
    }

    @Test
    public fun startActivityAndWait_activityNotExported() {
        val scope = MacrobenchmarkScope(TARGET_PACKAGE_NAME, launchWithClearTask = true)
        scope.pressHome()

        val intent = Intent()
        intent.setPackage(TARGET_PACKAGE_NAME)
        intent.action = "$TARGET_PACKAGE_NAME.NOT_EXPORTED_ACTIVITY"

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val prop = device.executeShellCommand("getprop service.adb.root").trim()
        if (prop == "1") {
            // while device and adb session are both rooted, doesn't throw
            scope.startActivityAndWait(intent)
        } else {
            // should throw, warning to set exported = true
            // Note: rooted device will hit this path, unless `adb root` is invoked
            val exceptionMessage = assertFailsWith<SecurityException> {
                scope.startActivityAndWait(intent)
            }.message
            assertNotNull(exceptionMessage)
            assertTrue(exceptionMessage.contains("Permission Denial"))
            assertTrue(exceptionMessage.contains("NotExportedActivity"))
            assertTrue(exceptionMessage.contains("not exported"))
        }
    }

    @Test
    public fun startActivityAndWait_invalidActivity() {
        val scope = MacrobenchmarkScope(TARGET_PACKAGE_NAME, launchWithClearTask = true)
        scope.pressHome()

        val intent = Intent()
        intent.setPackage("this.is.not.a.real.package")
        intent.action = "$TARGET_PACKAGE_NAME.NOT_EXPORTED_ACTIVITY"

        // should throw, unable to resolve Intent
        val exceptionMessage = assertFailsWith<IllegalStateException> {
            scope.startActivityAndWait(intent)
        }.message
        assertNotNull(exceptionMessage)
        assertTrue(exceptionMessage.contains("unable to resolve Intent"))
    }

    @Test
    public fun startActivityAndWait_sameActivity() {
        val scope = MacrobenchmarkScope(LOCAL_PACKAGE_NAME, launchWithClearTask = true)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Launch first activity, and validate it is displayed
        scope.startActivityAndWait(ConfigurableActivity.createIntent("InitialText"))
        assertTrue(device.hasObject(By.text("InitialText")))

        // Launch second activity, and validate it is displayed
        // By having the activity sleep during launch, we validate that the wait actually occurs,
        // and that we're not seeing the previous Activity (which could happen if the wait
        // doesn't occur, or if launch is extremely fast).
        scope.startActivityAndWait(
            ConfigurableActivity.createIntent(
                text = "UpdatedText",
                sleepDurMs = 1000L
            )
        )
        assertTrue(device.hasObject(By.text("UpdatedText")))
    }

    private fun processes(): List<String> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Note: ps -A doesn't work on API 25 (does on API 27)
        val output = instrumentation.device().executeShellCommand("ps -A")
        return output.split("\r?\n".toRegex())
    }

    private fun isProcessAlive(packageName: String): Boolean {
        return processes().any { it.contains(packageName) }
    }

    public companion object {
        // Separate target app. Use this app/package if killing/compiling target process.
        private const val TARGET_PACKAGE_NAME =
            "androidx.benchmark.integration.macrobenchmark.target"

        // This test app. Use this app/package if not killing/compiling target.
        private const val LOCAL_PACKAGE_NAME = "androidx.benchmark.macro.test"
    }
}
