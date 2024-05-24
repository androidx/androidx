/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark

import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.benchmark.json.BenchmarkData
import androidx.test.platform.app.InstrumentationRegistry
import com.squareup.moshi.Moshi
import java.io.File
import java.io.IOException
import okio.FileSystem
import okio.Path.Companion.toPath

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object ResultWriter {

    @VisibleForTesting internal val reports = mutableListOf<BenchmarkData.TestResult>()

    internal val adapter =
        Moshi.Builder()
            .build()
            .adapter(BenchmarkData::class.java)
            .indent("    ") // chosen for test compat, will be changed later

    fun appendTestResult(testResult: BenchmarkData.TestResult) {
        reports.add(testResult)
        if (Arguments.outputEnable) {
            // Currently, we just overwrite the whole file
            // Ideally, append for efficiency
            val packageName =
                InstrumentationRegistry.getInstrumentation().targetContext!!.packageName

            Outputs.writeFile(
                fileName = "$packageName-benchmarkData.json",
                reportOnRunEndOnly = true
            ) {
                Log.d(BenchmarkState.TAG, "writing results to ${it.absolutePath}")
                writeReport(it, reports)
            }
        } else {
            Log.d(
                BenchmarkState.TAG,
                "androidx.benchmark.output.enable not set, not writing results json"
            )
        }
    }

    @VisibleForTesting
    internal fun writeReport(file: File, benchmarks: List<BenchmarkData.TestResult>) {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            try {
                file.createNewFile()
            } catch (exception: IOException) {
                throw IOException(
                    """
                            Failed to create file for benchmark report in:
                            $file.parent
                            Make sure the instrumentation argument additionalTestOutputDir is set
                            to a writable directory on device. If using a version of Android Gradle
                            Plugin that doesn't support additionalTestOutputDir, ensure your app's
                            manifest file enables legacy storage behavior by adding the
                            application attribute: android:requestLegacyExternalStorage="true"
                        """
                        .trimIndent(),
                    exception
                )
            }
        }

        val benchmarkData =
            BenchmarkData(context = BenchmarkData.Context(), benchmarks = benchmarks)

        FileSystem.SYSTEM.write(file.absolutePath.toPath()) { adapter.toJson(this, benchmarkData) }
    }

    fun getParams(testName: String): Map<String, String> {
        val parameterStrStart = testName.indexOf('[')
        val parameterStrEnd = testName.lastIndexOf(']')

        val params = HashMap<String, String>()
        if (parameterStrStart >= 0 && parameterStrEnd >= 0) {
            val paramListString = testName.substring(parameterStrStart + 1, parameterStrEnd)
            paramListString.split(",").forEach { paramString ->
                val separatorIndex = paramString.indexOfFirst { it == ':' || it == '=' }
                if (separatorIndex in 1 until paramString.length - 1) {
                    val key = paramString.substring(0, separatorIndex)
                    val value = paramString.substring(separatorIndex + 1)
                    params[key] = value
                }
            }
        }
        return params
    }
}
