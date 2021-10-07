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

import android.os.Build
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.benchmark.ConfigurationError
import androidx.benchmark.DeviceInfo
import androidx.benchmark.InstrumentationResults
import androidx.benchmark.Outputs
import androidx.benchmark.Shell
import androidx.benchmark.checkAndGetSuppressionState
import androidx.benchmark.conditionalError
import androidx.benchmark.userspaceTrace

/**
 * A list of configuration errors applicable for baseline profile collection.
 */
private val errors: List<ConfigurationError> = listOfNotNull(
    conditionalError(
        hasError = !DeviceInfo.isRooted,
        id = "NEEDS-ROOT",
        summary = "Run on a rooted device",
        message = "Baseline Profile Collection needs to run on a rooted device."
    )
)

/**
 * Collects baseline profiles using a given [profileBlock].
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun collectBaselineProfile(
    uniqueName: String,
    packageName: String,
    setupBlock: MacrobenchmarkScope.() -> Unit,
    profileBlock: MacrobenchmarkScope.() -> Unit
) {
    require(Build.VERSION.SDK_INT >= 28) {
        "Baseline Profile Collection requires API 28 or higher."
    }

    require(Shell.isSessionRooted()) {
        "Baseline Profile Collection requires a rooted session. Use `adb root`."
    }

    errors.checkAndGetSuppressionState(emptySet())

    val startTime = System.nanoTime()
    val scope = MacrobenchmarkScope(packageName, /* launchWithClearTask */ true)
    val speedProfile = CompilationMode.SpeedProfile(warmupIterations = 3)

    // always kill the process at beginning of a collection.
    scope.killProcess()
    try {
        userspaceTrace("compile $packageName") {
            speedProfile.compile(packageName) {
                setupBlock(scope)
                profileBlock(scope)
            }
        }
        // The path of the reference profile
        val referenceProfile = "/data/misc/profiles/ref/$packageName/primary.prof"
        // The path to the primary profile
        val currentProfile = "/data/misc/profiles/cur/0/$packageName/primary.prof"
        Log.d(TAG, "Reference profile location: $referenceProfile")
        val pathResult = Shell.executeScript("pm path $packageName")
        // The result looks like: `package: <result>`
        val apkPath = pathResult.substringAfter("package:").trim()
        Log.d(TAG, "APK Path: $apkPath")
        // Convert to HRF
        Log.d(TAG, "Converting to human readable profile format")
        // Look at reference profile first, and then fallback to current profile
        val profile = profile(apkPath, listOf(referenceProfile, currentProfile))
        InstrumentationResults.instrumentationReport {
            val fileName = "$uniqueName-baseline-prof.txt"
            val absolutePath = Outputs.writeFile(fileName, "baseline-profile") {
                it.writeText(profile)
            }
            val totalRunTime = System.nanoTime() - startTime
            val summary = summaryRecord(totalRunTime, absolutePath)
            ideSummaryRecord(summaryV1 = summary, summaryV2 = summary)
            Log.d(TAG, "Total Run Time Ns: $totalRunTime")
        }
    } finally {
        scope.killProcess()
    }
}

private fun profile(apkPath: String, pathOptions: List<String>): String {
    // When compiling with CompilationMode.SpeedProfile, ART stores the profile in one of
    // 2 locations. The `ref` profile path, or the `current` path.
    // The `current` path is eventually merged  into the `ref` path after background dexopt.
    for (currentPath in pathOptions) {
        Log.d(TAG, "Using profile location: $currentPath")
        val profile = Shell.executeScript(
            "profman --dump-classes-and-methods --profile-file=$currentPath --apk=$apkPath"
        )
        if (profile.isNotBlank()) {
            return profile
        }
    }
    throw IllegalStateException("The profile is empty.")
}

private fun summaryRecord(totalRunTime: Long, absolutePath: String): String {
    val relativePath = Outputs.relativePathFor(absolutePath)
        .replace("(", "\\(")
        .replace(")", "\\)")

    return """
        Total run time Ns: $totalRunTime.

        Baseline profile [results](file://$relativePath)
    """.trimIndent()
}
