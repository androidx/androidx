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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

        // Emulator api 30 does not have dex2oat (b/264938965)
        assumeTrue(Build.VERSION.SDK_INT != Build.VERSION_CODES.R)

        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = true)
        val iterations = 1
        var executions = 0
        val compilation = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Disable,
            warmupIterations = iterations
        )
        compilation.resetAndCompile(
            Packages.TARGET,
            killProcessBlock = scope::killProcess
        ) {
            executions += 1
            scope.pressHome()
            scope.startActivityAndWait()
        }
        assertEquals(iterations, executions)
    }

    @Test
    fun compile_full() {

        // Emulator api 30 does not have dex2oat (b/264938965)
        assumeTrue(Build.VERSION.SDK_INT != Build.VERSION_CODES.R)

        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = true)
        val compilation = CompilationMode.Full()
        compilation.resetAndCompile(
            Packages.TARGET,
            killProcessBlock = scope::killProcess
        ) {
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
        }
    }

    /** Tests getFrameStats after launch which resumes app */
    @Test
    fun getFrameStats_home() = validateLaunchAndFrameStats(pressHome = true)

    /** Tests getFrameStats after launch which does nothing, as Activity already visible */
    @Test
    fun getFrameStats_noop() = validateLaunchAndFrameStats(pressHome = false)

    private fun validateShaderCache(empty: Boolean, packageName: String) {
        val path = MacrobenchmarkScope.getShaderCachePath(packageName)

        println("validating shader path $path")
        val fileCount = Shell.executeScriptCaptureStdout("find $path -type f | wc -l")
            .trim()
            .toInt()
        if (empty) {
            val files = Shell.executeScriptCaptureStdout("find $path -type f")
            assertEquals(0, fileCount, "Expected 0 files in $path, saw $fileCount (files = $files)")
        } else {
            assertNotEquals(0, fileCount, "Expected >0 files in $path, saw $fileCount")
        }
    }

    private fun validateDropShaderCacheWithRoot(
        dropShaderCacheBlock: MacrobenchmarkScope.() -> Unit
    ) {
        // need root to inspect target app's code cache dir, and emulators
        // don't seem to store shaders
        assumeTrue(Shell.isSessionRooted() && !DeviceInfo.isEmulator)

        val scope = MacrobenchmarkScope(
            Packages.TARGET,
            launchWithClearTask = false
        )
        // reset to empty to begin with
        scope.killProcess()
        scope.dropShaderCacheBlock()
        validateShaderCache(empty = true, scope.packageName)

        // start an activity, expecting shader compilation
        scope.pressHome()
        // NOTE: if platform fixes default activity to not compile shaders,
        //   may need to update this test UI to trigger shader creation
        scope.startActivityAndWait()
        Thread.sleep(5000) // sleep to await flushing cache to disk
        scope.killProcess()
        validateShaderCache(empty = false, scope.packageName)

        // verify deletion
        scope.killProcess()
        scope.dropShaderCacheBlock()
        validateShaderCache(empty = true, scope.packageName)
    }

    @Test
    fun dropShaderCacheBroadcast() = validateDropShaderCacheWithRoot {
        // since this test runs on root and the public api falls back to
        // a root impl, test the broadcast directly
        assertNull(ProfileInstallBroadcast.dropShaderCache(packageName))
    }

    @Test
    fun dropShaderCachePublicApi() = validateDropShaderCacheWithRoot {
        dropShaderCache()
    }

    @Test
    fun dropShaderCacheRoot() = validateDropShaderCacheWithRoot {
        assertTrue(dropShaderCacheRoot())
    }

    @Test
    fun dropKernelPageCache() {
        val scope = MacrobenchmarkScope(
            Packages.TARGET,
            launchWithClearTask = false
        )
        scope.dropKernelPageCache() // shouldn't crash
    }
}
