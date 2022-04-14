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
import android.os.Build
import androidx.benchmark.DeviceInfo
import androidx.benchmark.Shell
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MacrobenchmarkScopeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    @Suppress("DEPRECATION")
    fun setup() {
        // validate target is installed with clear error message,
        // since error messages from e.g. startActivityAndWait may be less clear
        try {
            val pm = instrumentation.context.packageManager
            pm.getApplicationInfo(Packages.TARGET, 0)
        } catch (notFoundException: PackageManager.NameNotFoundException) {
            throw IllegalStateException(
                "Unable to find target ${Packages.TARGET}, is it installed?"
            )
        }
    }

    @Test
    fun killTest() {
        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = true)
        scope.pressHome()
        scope.startActivityAndWait()
        assertTrue(Shell.isPackageAlive(Packages.TARGET))
        scope.killProcess()
        assertFalse(Shell.isPackageAlive(Packages.TARGET))
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun compile_speedProfile() {
        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = true)
        val iterations = 1
        var executions = 0
        val compilation = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Disable,
            warmupIterations = iterations
        )
        compilation.resetAndCompile(Packages.TARGET) {
            executions += 1
            scope.pressHome()
            scope.startActivityAndWait()
        }
        assertEquals(iterations, executions)
    }

    @Test
    fun compile_full() {
        val compilation = CompilationMode.Full()
        compilation.resetAndCompile(Packages.TARGET) {
            fail("Should never be called for $compilation")
        }
    }

    @Test
    fun startActivityAndWait_activityNotExported() {
        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = true)
        scope.pressHome()

        val intent = Intent()
        intent.setPackage(Packages.TARGET)
        intent.action = "${Packages.TARGET}.NOT_EXPORTED_ACTIVITY"

        // Workaround b/227512788 - isSessionRooted isn't reliable below API 24 on rooted devices
        assumeTrue(Build.VERSION.SDK_INT > 23 || !DeviceInfo.isRooted)

        if (Shell.isSessionRooted()) {
            // while device and adb session are both rooted, doesn't throw
            scope.startActivityAndWait(intent)
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertTrue(device.hasObject(By.text("NOT EXPORTED ACTIVITY")))
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

    // Note: Test flakes locally on API 23, appears to be UI automation issue. In API 23 CI running
    // MRA58K crashes, but haven't repro'd locally, so just skip on API 23.
    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun startActivityAndWait_invalidActivity() {
        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = true)
        scope.pressHome()

        val intent = Intent()
        intent.setPackage("this.is.not.a.real.package")
        intent.action = "${Packages.TARGET}.NOT_EXPORTED_ACTIVITY"

        // should throw, unable to resolve Intent
        val exceptionMessage = assertFailsWith<IllegalStateException> {
            scope.startActivityAndWait(intent)
        }.message
        assertNotNull(exceptionMessage)
        assertTrue(exceptionMessage.contains("unable to resolve Intent"))
    }

    @Test
    fun startActivityAndWait_sameActivity() {
        val scope = MacrobenchmarkScope(
            Packages.TEST, // self-instrumenting macrobench, so don't kill the process!
            launchWithClearTask = true
        )
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

    private fun validateLaunchAndFrameStats(pressHome: Boolean) {
        val scope = MacrobenchmarkScope(
            Packages.TEST, // self-instrumenting macrobench, so don't kill the process!
            launchWithClearTask = false
        )
        // check that initial launch (home -> activity) is detected
        scope.pressHome()
        scope.startActivityAndWait(ConfigurableActivity.createIntent("InitialText"))
        val initialFrameStats = scope.getFrameStats()
            .sortedBy { it.lastFrameNs }
            .first()
        assertTrue(initialFrameStats.uniqueName.contains("ConfigurableActivity"))

        if (pressHome) {
            scope.pressHome()
        }

        // check that hot startup is detected
        scope.startActivityAndWait(ConfigurableActivity.createIntent("InitialText"))
        val secondFrameStats = scope.getFrameStats()
            .sortedBy { it.lastFrameNs }
            .first()
        assertTrue(secondFrameStats.uniqueName.contains("ConfigurableActivity"))

        if (pressHome) {
            assertTrue(secondFrameStats.lastFrameNs!! > initialFrameStats.lastFrameNs!!)
            if (Build.VERSION.SDK_INT >= 29) {
                // data not trustworthy before API 29
                assertTrue(secondFrameStats.lastLaunchNs!! > initialFrameStats.lastLaunchNs!!)
            }
        }
    }

    /** Tests getFrameStats after launch which resumes app */
    @Test
    fun getFrameStats_home() = validateLaunchAndFrameStats(pressHome = true)

    /** Tests getFrameStats after launch which does nothing, as Activity already visible */
    @Test
    fun getFrameStats_noop() = validateLaunchAndFrameStats(pressHome = false)
}
