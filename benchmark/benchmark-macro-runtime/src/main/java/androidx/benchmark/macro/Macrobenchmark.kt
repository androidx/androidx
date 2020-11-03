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

package androidx.benchmark.macro

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/**
 * Provides access to common operations in app automation, such as killing the app,
 * or navigating home.
 */
public class MacrobenchmarkScope(
    private val packageName: String
) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.context
    private val device = UiDevice.getInstance(instrumentation)

    fun launchPackageAndWait(block: (Intent) -> Unit) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)!!
        block(intent)
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
        device.executeShellCommand("am force-stop $packageName")
    }
}

data class MacrobenchmarkConfig(
    val packageName: String,
    val metrics: List<Metric>,
    val compilationMode: CompilationMode = CompilationMode.SpeedProfile(),
    val killProcessEachIteration: Boolean = false,
    val iterations: Int
)

/**
 * Primary macrobenchmark test entrypoint.
 *
 * TODO: wrap in a JUnit4 rule, which defines [benchmarkName] based on test name
 */
fun macrobenchmark(
    benchmarkName: String,
    config: MacrobenchmarkConfig,
    block: MacrobenchmarkScope.() -> Unit
) = withPermissiveSeLinuxPolicy {
    val scope = MacrobenchmarkScope(config.packageName)

    // always kill the process at beginning of test
    scope.killProcess()

    config.compilationMode.compile(config.packageName) {
        block(scope)
    }

    // Perfetto collector is separate from metrics, so we can control file
    // output, and give it different (test-wide) lifecycle
    val perfettoCollector = PerfettoCaptureWrapper()
    try {
        perfettoCollector.start()
        config.metrics.forEach {
            it.start()
        }
        repeat(config.iterations) {
            if (config.killProcessEachIteration) {
                scope.killProcess()
            }
            block(scope)
        }
        config.metrics.forEach {
            it.stop()
        }
        config.metrics.map {
            it.collector
        }.report()
    } finally {
        perfettoCollector.stop("$benchmarkName.trace")
        scope.killProcess()
    }
}

internal fun CompilationMode.compile(packageName: String, block: () -> Unit) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    // Clear profile between runs.
    clearProfile(instrumentation, packageName)
    if (this == CompilationMode.None) {
        return // nothing to do
    }
    if (this is CompilationMode.SpeedProfile) {
        repeat(this.warmupIterations) {
            block()
        }
    }
    // TODO: merge in below method
    compilationFilter(
        InstrumentationRegistry.getInstrumentation(),
        packageName,
        compileArgument()
    )
}
