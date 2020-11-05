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

package androidx.benchmark.perfetto

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

/**
 * Add this rule to record a Perfetto trace for each test on Q+ devices.
 *
 * Relies on either AGP's additionalTestOutputDir copying, or (in Jetpack CI),
 * `additionalTestOutputFile_***` copying.
 *
 * When invoked locally with Gradle, file will be copied to host path like the following:
 *
 * ```
 * out/androidx/benchmark/benchmark-perfetto/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/<deviceName>/androidx.mypackage.TestClass_testMethod.trace
 * ```
 *
 * Note: if run from Studio, the file must be `adb pull`-ed manually, e.g.:
 * ```
 * > adb pull /storage/emulated/0/Android/data/androidx.mypackage.test/files/test_data/androidx.mypackage.TestClass_testMethod.trace
 * ```
 *
 * You can check logcat for messages tagged "PerfettoRule:" for the path of each perfetto trace.
 * ```
 * > adb pull /storage/emulated/0/Android/data/mypackage.test/files/PerfettoCaptureTest.trace
 * ```
 */
class PerfettoRule : TestRule {
    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ignorePerfettoTestIfUnsupportedCuttlefish()

                val traceName = description.className + "_" + description.methodName + ".trace"
                PerfettoCapture().recordAndReportFile(traceName) {
                    base.evaluate()
                }
            } else {
                Log.d(TAG, "Perfetto trace skipped due to API level (${Build.VERSION.SDK_INT})")
                base.evaluate()
            }
        }
    }

    companion object {
        internal const val TAG = "PerfettoRule"
    }
}

internal fun ignorePerfettoTestIfUnsupportedCuttlefish() {
    if (Build.MODEL.contains("Cuttlefish")) {
        // Workaround for Cuttlefish perfetto issue on Q (b/171085599)
        Assume.assumeTrue(
            "This test requires a R+ platform build on Cuttlefish",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
internal fun PerfettoCapture.recordAndReportFile(traceName: String, block: () -> Unit) {
    try {
        Log.d(PerfettoRule.TAG, "Recording perfetto trace $traceName")
        start()
        block()
        val dst = destinationPath(traceName)
        stop(dst.absolutePath)
        Log.d(PerfettoRule.TAG, "Finished recording to ${dst.absolutePath}")
        reportAdditionalFileToCopy("perfetto_trace", dst.absolutePath)
    } finally {
        cancel()
    }
}

/*
   NOTE: this method of getting additionalTestOutputDir duplicates behavior in
   androidx.benchmark.Arguments`, and should be unified at some point.
*/
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun destinationPath(traceName: String): File {
    val additionalTestOutputDir = InstrumentationRegistry.getArguments()
        .getString("additionalTestOutputDir")

    @Suppress("DEPRECATION") // Legacy code path for versions of agp older than 3.6
    val testOutputDir = additionalTestOutputDir?.let { File(it) }
        ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    return File(testOutputDir, traceName)
}

/**
 * Report additional file to be copied.
 *
 * Note that this is a temporary reimplementation of
 * `InstrumentationResults.reportAdditionalFileToCopy`, and should be unified at some point.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun reportAdditionalFileToCopy(
    @Suppress("SameParameterValue") key: String,
    absoluteFilePath: String
) {
    val bundle = Bundle().also {
        it.putString("additionalTestOutputFile_$key", absoluteFilePath)
    }
    InstrumentationRegistry
        .getInstrumentation()
        .sendStatus(2, bundle)
}
