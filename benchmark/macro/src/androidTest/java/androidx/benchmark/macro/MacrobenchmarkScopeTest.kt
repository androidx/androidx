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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.IllegalStateException
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@LargeTest
public class MacrobenchmarkScopeTest {
    @Test
    @Ignore("Apk dependencies not working in presubmit, b/181810492")
    public fun killTest() {
        val scope = MacrobenchmarkScope(TARGET_PACKAGE_NAME, launchWithClearTask = true)
        scope.pressHome()
        scope.startActivityAndWait()
        assertTrue(isProcessAlive(TARGET_PACKAGE_NAME))
        scope.killProcess()
        assertFalse(isProcessAlive(TARGET_PACKAGE_NAME))
    }

    @Test
    @Ignore("Apk dependencies not working in presubmit, b/181810492")
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
    @Ignore("Apk dependencies not working in presubmit, b/181810492")
    public fun compile_speed() {
        val compilation = CompilationMode.Speed
        compilation.compile(TARGET_PACKAGE_NAME) {
            fail("Should never be called for $compilation")
        }
    }

    @Test
    @Ignore("Apk dependencies not working in presubmit, b/181810492")
    public fun startActivityAndWait_activityNotExported() {
        val scope = MacrobenchmarkScope(TARGET_PACKAGE_NAME, launchWithClearTask = true)
        scope.pressHome()

        val intent = Intent()
        intent.setPackage(TARGET_PACKAGE_NAME)
        intent.action = "$TARGET_PACKAGE_NAME.NOT_EXPORTED_ACTIVITY"

        // should throw, warning to set exported = true
        val exceptionMessage = assertFailsWith<SecurityException> {
            scope.startActivityAndWait(intent)
        }.message
        assertNotNull(exceptionMessage)
        assertTrue(exceptionMessage.contains("android:exported=true"))
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

    @Test
    public fun waitOnPackage_throw() {
        val scope = MacrobenchmarkScope(LOCAL_PACKAGE_NAME, launchWithClearTask = true)

        val intent = ConfigurableActivity.createIntent(
            text = "ignored",
            sleepDurMs = 10000
        ).apply {
            // launch unique intent
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        InstrumentationRegistry.getInstrumentation().context.startActivity(intent)

        // validate that 10 second launch triggers 1 second timeout
        val exception = assertFailsWith<IllegalStateException> {
            scope.waitOnPackageLaunch(1)
        }
        assertTrue(exception.message!!.contains("Unable to detect Activity"))
    }

    private fun processes(): List<String> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
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
