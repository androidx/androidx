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
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.benchmark.Arguments
import androidx.benchmark.InstrumentationResults
import androidx.benchmark.Outputs
import androidx.benchmark.Shell
import androidx.benchmark.userspaceTrace

/**
 * Collects baseline profiles using a given [profileBlock].
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@RequiresApi(28)
@JvmOverloads
fun collectBaselineProfile(
    uniqueName: String,
    packageName: String,
    iterations: Int = 3,
    packageFilters: List<String> = emptyList(),
    profileBlock: MacrobenchmarkScope.() -> Unit,
) {
    require(Build.VERSION.SDK_INT >= 28) {
        "Baseline Profile Collection requires API 28 or higher."
    }

    require(Shell.isSessionRooted()) {
        "Baseline Profile Collection requires a rooted device, and a rooted adb session." +
            " Use `adb root`."
    }

    val startTime = System.nanoTime()
    val scope = MacrobenchmarkScope(packageName, launchWithClearTask = true)

    // Disable because we're *creating* a baseline profile, not using it yet
    val compilationMode = CompilationMode.Partial(
        baselineProfileMode = BaselineProfileMode.Disable,
        warmupIterations = iterations
    )

    val killProcessBlock = {
        // When generating baseline profiles we want to default to using
        // killProcess if the session is rooted. This is so we can collect
        // baseline profiles for System Apps.
        scope.killProcess(useKillAll = Shell.isSessionRooted())
        Thread.sleep(Arguments.killProcessDelayMillis)
    }

    // always kill the process at beginning of a collection.
    killProcessBlock.invoke()
    try {
        userspaceTrace("compile $packageName") {
            compilationMode.resetAndCompile(
                packageName = packageName,
                killProcessBlock = killProcessBlock
            ) {
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
        // Build a startup profile
        var startupProfile: String? = null
        if (Arguments.enableStartupProfiles) {
            startupProfile =
                startupProfile(profile, includeStartupOnly = Arguments.strictStartupProfiles)
        }

        // Filter profile if necessary based on filters
        val filteredProfile = applyPackageFilters(profile, packageFilters)

        // Write a file with a timestamp to be able to disambiguate between runs with the same
        // unique name.

        val fileName = "$uniqueName-baseline-prof.txt"
        val absolutePath = Outputs.writeFile(fileName, "baseline-profile") {
            it.writeText(filteredProfile)
        }
        var startupProfilePath: String? = null
        if (startupProfile != null) {
            val startupProfileFileName = "$uniqueName-startup-prof.txt"
            startupProfilePath = Outputs.writeFile(startupProfileFileName, "startup-profile") {
                it.writeText(startupProfile)
            }
        }
        val tsFileName = "$uniqueName-baseline-prof-${Outputs.dateToFileName()}.txt"
        val tsAbsolutePath = Outputs.writeFile(tsFileName, "baseline-profile-ts") {
            Log.d(TAG, "Pull Baseline Profile with: `adb pull \"${it.absolutePath}\" .`")
            it.writeText(filteredProfile)
        }
        var tsStartupAbsolutePath: String? = null
        if (startupProfile != null) {
            val tsStartupFileName = "$uniqueName-startup-prof-${Outputs.dateToFileName()}.txt"
            tsStartupAbsolutePath = Outputs.writeFile(tsStartupFileName, "startup-profile-ts") {
                Log.d(TAG, "Pull Startup Profile with: `adb pull \"${it.absolutePath}\" .`")
                it.writeText(startupProfile)
            }
        }

        val totalRunTime = System.nanoTime() - startTime
        val results = Summary(
            totalRunTime = totalRunTime,
            profilePath = absolutePath,
            profileTsPath = tsAbsolutePath,
            startupProfilePath = startupProfilePath,
            startupTsProfilePath = tsStartupAbsolutePath
        )
        InstrumentationResults.instrumentationReport {
            val summary = summaryRecord(results)
            ideSummaryRecord(summaryV1 = summary, summaryV2 = summary)
            Log.d(TAG, "Total Run Time Ns: $totalRunTime")
        }
    } finally {
        killProcessBlock.invoke()
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
            return filterProfileRulesToTargetP(profile)
        }
    }
    throw IllegalStateException("The profile is empty.")
}

@VisibleForTesting
internal fun filterProfileRulesToTargetP(profile: String): String {
    val rules = profile.lines()
    val filteredRules = rules.filterNot { rule ->
        // We want to filter out rules that are not supported on P. (b/216508418)
        // These include rules that have array qualifiers and inline cache specifiers.
        if (rule.startsWith("[")) { // Array qualifier
            true
        } else rule.contains("+") // Inline cache specifier
    }
    return filteredRules.joinToString(separator = "\n")
}

private fun applyPackageFilters(profile: String, packageFilters: List<String>): String {
    // Filters the profile output with the given set or rules.
    // Note that the filter rules are package name but the profile file lines contain
    // jvm method signature, ex: `HSPLandroidx/room/RoomDatabase;-><init>()V`.
    // In order to simplify this for developers we transform the filters from regular package names.
    return if (packageFilters.isEmpty()) profile else {
        // Ensure that the package name ends with `/`
        val fixedPackageFilters = packageFilters
            .map { "${it.replace(".", "/")}${if (it.endsWith(".")) "" else "/"}" }
        profile
            .lines()
            .filter { line -> fixedPackageFilters.any { line.contains(it) } }
            .joinToString(System.lineSeparator())
    }
}

private fun summaryRecord(record: Summary): String {
    val summary = StringBuilder()

    // Links

    // Link to a path with timestamp to prevent studio from caching the file
    val relativePath = Outputs.relativePathFor(record.profileTsPath)
        .replace("(", "\\(")
        .replace(")", "\\)")

    summary.append(
        """
            Total run time Ns: ${record.totalRunTime}.
            Baseline profile [results](file://$relativePath)
        """.trimIndent()
    )

    // Link to a path with timestamp to prevent studio from caching the file
    val startupTsProfilePath = record.startupTsProfilePath
    if (!startupTsProfilePath.isNullOrBlank()) {
        val startupRelativePath = Outputs.relativePathFor(startupTsProfilePath)
            .replace("(", "\\(")
            .replace(")", "\\)")
        summary.append("\n").append(
            """
                Startup profile [results](file://$startupRelativePath)
            """.trimIndent()
        )
    }

    // Add commands that can be used to pull these files.

    summary.append("\n")
        .append("\n")
        .append(
            """
                To copy the profile use:
                adb pull "${record.profilePath}" .
            """.trimIndent()
        )

    val startupProfilePath = record.startupProfilePath
    if (!startupProfilePath.isNullOrBlank()) {
        summary.append("\n")
            .append("\n")
            .append(
                """
                    To copy the startup profile use:
                    adb pull "${record.startupProfilePath}" .
                """.trimIndent()
            )
    }
    return summary.toString()
}

private data class Summary(
    val totalRunTime: Long,
    val profilePath: String,
    val profileTsPath: String,
    val startupProfilePath: String? = null,
    val startupTsProfilePath: String? = null
) {
    init {
        if (startupProfilePath.isNullOrBlank()) {
            require(startupTsProfilePath.isNullOrBlank())
        } else {
            require(!startupTsProfilePath.isNullOrBlank())
        }
    }
}
