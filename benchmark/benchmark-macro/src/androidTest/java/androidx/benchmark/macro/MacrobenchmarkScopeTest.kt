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

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.benchmark.DeviceInfo
import androidx.benchmark.DeviceInfo.isEmulator
import androidx.benchmark.Shell
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import kotlin.test.assertContains
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

    @Test
    fun killTest_processSuffix() {
        // regression test for b/408673462, where killall fails
        // due to each process has a suffix name (com.mypackage:foo)
        val scope =
            MacrobenchmarkScope(
                "com.google.android.googlequicksearchbox",
                launchWithClearTask = true,
            )

        // test only useful if package is alive
        assumeTrue(Shell.getPidsForProcess(scope.packageName).isNotEmpty())

        // killProcess shouldn't fail
        scope.killProcess()
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun compile_speedProfile() {

        // Emulator api 30 does not have dex2oat (b/264938965)
        assumeTrue(Build.VERSION.SDK_INT != Build.VERSION_CODES.R)

        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = true)
        val iterations = 1
        var executions = 0
        val compilation =
            CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Disable,
                warmupIterations = iterations,
            )
        compilation.resetAndCompile(scope) {
            executions += 1
            scope.pressHome()
            scope.startActivityAndWait()
        }
        assertEquals(iterations, executions)
    }

    /**
     * Verify that profile flushes happen when the target app is killed, with the lambda defining
     * kill behavior
     */
    @RequiresApi(24)
    fun verify_compile_speedProfile_withProfileFlushes(killProcess: (MacrobenchmarkScope) -> Unit) {
        if (DeviceInfo.isEmulator) {
            // Emulator API 30 does not have dex2oat (b/264938965)
            assumeTrue(Build.VERSION.SDK_INT != 30)

            // Emulator API 26 times out when compiling (b/393186249)
            assumeTrue(Build.VERSION.SDK_INT != 26)
        }

        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = true)
        val warmupIterations = 2
        var executions = 0
        val compilation =
            CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Disable,
                warmupIterations = warmupIterations,
            )
        assertEquals(MacrobenchmarkScope.KillMode.None, scope.killMode)
        compilation.resetAndCompile(scope) {
            assertTrue(scope.killMode.flushArtProfiles)
            executions += 1

            // on first iter, kill doesn't kill anything, so profiles are not yet flushed
            killProcess(scope)
            assertEquals(
                executions != 1,
                scope.hasFlushedArtProfiles,
                "execution nr $executions, flushed = ${scope.hasFlushedArtProfiles}",
            )

            scope.pressHome()
            scope.startActivityAndWait()
        }
        assertEquals(MacrobenchmarkScope.KillMode.None, scope.killMode)
        assertEquals(warmupIterations, executions)
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun compile_speedProfile_withProfileFlushes() {
        verify_compile_speedProfile_withProfileFlushes { it.killProcess() }
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun compile_speedProfile_withProfileFlushes_noBroadcast() {
        assumeTrue(DeviceInfo.isRooted) // codepath only works with root
        verify_compile_speedProfile_withProfileFlushes {
            it.killProcessAndFlushArtProfiles(allowFlushWithBroadcast = false)
        }
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun compile_speedProfile_noLaunch() {
        // Emulator api 30 does not have dex2oat (b/264938965)
        assumeTrue(Build.VERSION.SDK_INT != Build.VERSION_CODES.R)
        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = true)

        var executions = 0
        val compilation =
            CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Disable,
                warmupIterations = 2,
            )
        assertEquals(MacrobenchmarkScope.KillMode.None, scope.killMode)
        assertContains(
            assertFailsWith<IllegalStateException> {
                    compilation.resetAndCompile(scope) {
                        assertTrue(scope.killMode.flushArtProfiles)
                        assertFalse(scope.hasFlushedArtProfiles)
                        // not launching process so profiles can't flush, should fail after this
                        executions++
                    }
                }
                .message!!,
            "never flushed profiles in any process",
        )
        assertEquals(MacrobenchmarkScope.KillMode.None, scope.killMode)
        assertEquals(2, executions)
    }

    @Test
    fun compile_full() {

        // Emulator api 30 does not have dex2oat (b/264938965)
        assumeTrue(Build.VERSION.SDK_INT != Build.VERSION_CODES.R)

        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = true)
        val compilation = CompilationMode.Full()
        compilation.resetAndCompile(scope) { fail("Should never be called for $compilation") }
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
            val exceptionMessage =
                assertFailsWith<SecurityException> { scope.startActivityAndWait(intent) }.message
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
        val exceptionMessage =
            assertFailsWith<IllegalStateException> { scope.startActivityAndWait(intent) }.message
        assertNotNull(exceptionMessage)
        assertTrue(exceptionMessage.contains("unable to resolve Intent"))
    }

    @Test
    fun startActivityAndWait_sameActivity() {
        val scope =
            MacrobenchmarkScope(
                Packages.TEST, // self-instrumenting macrobench, so don't kill the process!
                launchWithClearTask = true,
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
            ConfigurableActivity.createIntent(text = "UpdatedText", sleepDurMs = 1000L)
        )
        assertTrue(device.hasObject(By.text("UpdatedText")))
    }

    @Test
    fun measureBlock_methodTracing() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        val scope =
            MacrobenchmarkScope(
                Packages.TEST, // self-instrumenting macrobench, so don't kill the process!
                launchWithClearTask = true,
            )
        scope.fileLabel = "TEST-UNIQUE-NAME"
        scope.startMethodTracing()
        // Launch first activity, and validate it is displayed
        scope.startActivityAndWait(ConfigurableActivity.createIntent("InitialText"))
        assertTrue(scope.device.hasObject(By.text("InitialText")))
        val testOutputs = scope.stopMethodTracing()
        val trace =
            testOutputs.singleOrNull { file ->
                file.outputRelativePath.endsWith(".trace") &&
                    file.outputRelativePath.contains("-methodTracing-")
            }
        // One method trace should have been created
        assertNotNull(trace)
        assertTrue(trace.outputRelativePath.startsWith("TEST-UNIQUE-NAME-methodTracing-"))
    }

    @Test
    fun multipleMethodTraces_onProcessStartStop() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = true)
        scope.fileLabel = "TEST-UNIQUE-NAME"
        scope.startMethodTracing()
        scope.startActivityAndWait()
        scope.killProcess()
        scope.startActivityAndWait()
        scope.killProcess()
        val testOutputs = scope.stopMethodTracing()
        // We should have 2 method traces
        val traces =
            testOutputs.filter { file ->
                file.outputRelativePath.endsWith(".trace") &&
                    file.outputRelativePath.contains("-methodTracing-")
            }
        assertEquals(traces.size, 2)
    }

    private fun validateLaunchAndFrameStats(pressHome: Boolean) {
        val scope =
            MacrobenchmarkScope(
                Packages.TEST, // self-instrumenting macrobench, so don't kill the process!
                launchWithClearTask = false,
            )
        // check that initial launch (home -> activity) is detected
        scope.pressHome()
        scope.startActivityAndWait(ConfigurableActivity.createIntent("InitialText"))
        val initialFrameStats = scope.getFrameStats().sortedBy { it.lastFrameNs }.first()
        assertTrue(initialFrameStats.uniqueName.contains("ConfigurableActivity"))

        if (pressHome) {
            scope.pressHome()
        }

        // check that hot startup is detected
        scope.startActivityAndWait(ConfigurableActivity.createIntent("InitialText"))
        val secondFrameStats = scope.getFrameStats().sortedBy { it.lastFrameNs }.first()
        assertTrue(secondFrameStats.uniqueName.contains("ConfigurableActivity"))

        if (pressHome) {
            assertTrue(secondFrameStats.lastFrameNs!! > initialFrameStats.lastFrameNs!!)
        }
    }

    /** Tests getFrameStats after launch which resumes app */
    @Test fun getFrameStats_home() = validateLaunchAndFrameStats(pressHome = true)

    /** Tests getFrameStats after launch which does nothing, as Activity already visible */
    @Test fun getFrameStats_noop() = validateLaunchAndFrameStats(pressHome = false)

    private fun validateShaderCache(empty: Boolean, packageName: String) {
        val path = MacrobenchmarkScope.getShaderCachePath(packageName)

        println("validating shader path $path")
        val fileCount =
            Shell.executeScriptCaptureStdout("find $path -type f | wc -l").trim().toInt()
        if (empty) {
            val files = Shell.executeScriptCaptureStdout("find $path -type f")
            assertEquals(0, fileCount, "Expected 0 files in $path, saw $fileCount (files = $files)")
        } else {
            assertNotEquals(0, fileCount, "Expected >0 files in $path, saw $fileCount")
        }
    }

    @SuppressLint("BanThreadSleep") // Need to await flush to disk
    private fun validateDropShaderCacheWithRoot(
        dropShaderCacheBlock: MacrobenchmarkScope.() -> Unit
    ) {
        // need root to inspect target app's code cache dir, and emulators
        // don't seem to store shaders
        assumeTrue(Shell.isSessionRooted() && !DeviceInfo.isEmulator)
        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = false)
        scope.fileLabel = "TEST-UNIQUE-NAME"
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

    @Test fun dropShaderCachePublicApi() = validateDropShaderCacheWithRoot { dropShaderCache() }

    @Test
    fun dropShaderCacheRoot() = validateDropShaderCacheWithRoot {
        assertTrue(dropShaderCacheRoot())
    }

    @Test
    fun dropKernelPageCache() {
        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = false)
        scope.dropKernelPageCache() // shouldn't crash
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun cancelBackgroundDexopt() {
        val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = false)
        scope.cancelBackgroundDexopt() // shouldn't crash
    }
}
