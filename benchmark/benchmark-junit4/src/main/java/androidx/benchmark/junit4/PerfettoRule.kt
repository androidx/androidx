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

package androidx.benchmark.junit4

import androidx.annotation.RestrictTo
import androidx.benchmark.perfetto.PerfettoCaptureWrapper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Add this rule to record a Perfetto trace for each test on Q+ devices.
 *
 * Relies on either AGP's additionalTestOutputDir copying, or (in Jetpack CI),
 * `additionalTestOutputFile_***` copying.
 *
 * When invoked locally with Gradle, file will be copied to host path like the following:
 *
 * ```
 * out/androidx/benchmark/benchmark-macro/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/<deviceName>/androidx.mypackage.TestClass_testMethod.perfetto-trace
 * ```
 *
 * Note: if run from Studio, the file must be `adb pull`-ed manually, e.g.:
 * ```
 * > adb pull /storage/emulated/0/Android/data/androidx.mypackage.test/files/test_data/androidx.mypackage.TestClass_testMethod.trace
 * ```
 *
 * You can check logcat for messages tagged "PerfettoCapture:" for the path of each perfetto trace.
 * ```
 * > adb pull /storage/emulated/0/Android/data/mypackage.test/files/PerfettoCaptureTest.trace
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class PerfettoRule : TestRule {
    override fun apply(
        base: Statement,
        description: Description
    ): Statement = object : Statement() {
        override fun evaluate() {
            PerfettoCaptureWrapper().record(
                benchmarkName = "${description.className}_${description.methodName}",
                packages = listOf(InstrumentationRegistry.getInstrumentation().context.packageName)
            ) {
                base.evaluate()
            }
        }
    }
}
