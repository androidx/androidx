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

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.AssumptionViolatedException

sealed class CompilationMode(
    // for modes other than [None], is argument passed `cmd package compile`
    private val compileArgument: String?
) {
    internal fun compileArgument(): String {
        if (compileArgument == null) {
            throw UnsupportedOperationException("No compileArgument for mode $this")
        }
        return compileArgument
    }

    object None : CompilationMode(null) {
        override fun toString() = "None"
    }

    class SpeedProfile(val warmupIterations: Int = 3) : CompilationMode("speed-profile") {
        override fun toString() = "SpeedProfile(iterations=$warmupIterations)"
    }

    object Speed : CompilationMode("speed") {
        override fun toString() = "Speed"
    }

    object Interpreted : CompilationMode(null) {
        override fun toString() = "Interpreted"
    }
}

internal fun CompilationMode.compile(packageName: String, block: () -> Unit) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    // Clear profile between runs.
    clearProfile(instrumentation, packageName)
    if (this == CompilationMode.None || this == CompilationMode.Interpreted) {
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

fun CompilationMode.isSupportedWithVmSettings(): Boolean {
    val device = InstrumentationRegistry.getInstrumentation().device()
    val getProp = device.executeShellCommand("getprop dalvik.vm.extra-opts")
    val vmRunningInterpretedOnly = getProp.contains("-Xusejit:false")

    // true if requires interpreted, false otherwise
    val interpreted = this == CompilationMode.Interpreted
    return vmRunningInterpretedOnly == interpreted
}

internal fun CompilationMode.assumeSupportedWithVmSettings() {
    if (!isSupportedWithVmSettings()) {
        throw AssumptionViolatedException(
            when {
                DeviceInfo.isRooted && this == CompilationMode.Interpreted ->
                    """
                        To run benchmarks with CompilationMode $this,
                        you must disable jit on your device with the following command:
                        `adb shell setprop dalvik.vm.extra-opts -Xusejit:false; adb shell stop; adb shell start`                         
                    """.trimIndent()
                DeviceInfo.isRooted && this != CompilationMode.Interpreted ->
                    """
                        To run benchmarks with CompilationMode $this,
                        you must enable jit on your device with the following command:
                        `adb shell setprop dalvik.vm.extra-opts \"\"; adb shell stop; adb shell start` 
                    """.trimIndent()
                else ->
                    "You must toggle usejit on the VM to use CompilationMode $this, this requires" +
                        "rooting your device."
            }
        )
    }
}