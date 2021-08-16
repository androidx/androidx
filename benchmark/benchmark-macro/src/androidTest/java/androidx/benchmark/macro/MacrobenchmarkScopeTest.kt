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
import androidx.benchmark.Shell
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

@RunWith(AndroidJUnit4::class)
@LargeTest
class MacrobenchmarkScopeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)

    @Before
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

    @SdkSuppress(minSdkVersion = 24) // TODO: define behavior for older platforms
    @Test
    fun compile_speedProfile() {
        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = true)
        val iterations = 1
        var executions = 0
        val compilation = CompilationMode.SpeedProfile(warmupIterations = iterations)
        compilation.compile(Packages.TARGET) {
            executions += 1
            scope.pressHome()
            scope.startActivityAndWait()
        }
        assertEquals(iterations, executions)
    }

    @SdkSuppress(minSdkVersion = 24) // TODO: define behavior for older platforms
    @Test
    fun compile_speed() {
        val compilation = CompilationMode.Speed
        compilation.compile(Packages.TARGET) {
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

        if (Shell.isSessionRooted() || Build.VERSION.SDK_INT <= 23) {
            // while device and adb session are both rooted, doesn't throw
            // TODO: verify whether pre-23 behavior requires userdebug device, only tested with
            //  emulator so far
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
}
