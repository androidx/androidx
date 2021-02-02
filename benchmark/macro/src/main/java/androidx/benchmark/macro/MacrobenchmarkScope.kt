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
import android.util.Log
import androidx.benchmark.macro.perfetto.executeShellScript
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/**
 * Provides access to common operations in app automation, such as killing the app,
 * or navigating home.
 */
public class MacrobenchmarkScope(
    private val packageName: String,
    /**
     * Controls whether launches will automatically set [Intent.FLAG_ACTIVITY_CLEAR_TASK].
     *
     * Default to true, so Activity launches go through full creation lifecycle stages, instead of
     * just resume.
     */
    private val launchWithClearTask: Boolean
) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.context
    private val device = UiDevice.getInstance(instrumentation)

    /**
     * Launch the package, with a customizable intent.
     */
    fun launchPackageAndWait(
        block: (Intent) -> Unit = {}
    ) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: throw IllegalStateException("Unable to acquire intent for package $packageName")

        block(intent)
        launchIntentAndWait(intent)
    }

    fun launchIntentAndWait(intent: Intent) {
        // Must launch with new task, as we're not launching from an existing task
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (launchWithClearTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        device.wait(
            Until.hasObject(By.pkg(packageName).depth(0)),
            5000 /* ms */
        )
    }

    fun pressHome(delayDurationMs: Long = 300) {
        device.pressHome()
        Thread.sleep(delayDurationMs)
    }

    fun killProcess() {
        Log.d(TAG, "Killing process $packageName")
        device.executeShellCommand("am force-stop $packageName")
    }

    /**
     * Drop Kernel's in-memory cache of disk pages.
     *
     * Enables measuring disk-based startup cost, without simply accessing cache of disk data
     * held in memory, such as during [cold startup](androidx.benchmark.macro.StartupMode.COLD).
     */
    fun dropKernelPageCache() {
        val result = device.executeShellScript(
            "echo 3 > /proc/sys/vm/drop_caches && echo Success || echo Failure"
        ).trim()
        // User builds don't allow drop caches yet.
        if (result != "Success") {
            Log.w(TAG, "Failed to drop kernel page cache, result: '$result'")
        }
    }
}
