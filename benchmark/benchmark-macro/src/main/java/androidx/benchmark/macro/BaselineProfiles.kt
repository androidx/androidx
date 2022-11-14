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
import androidx.benchmark.DeviceInfo
import androidx.benchmark.InstrumentationResults
import androidx.benchmark.Outputs
import androidx.benchmark.Shell
import androidx.benchmark.userspaceTrace
import java.io.File

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
    val scope = buildMacrobenchmarkScope(packageName)
    val startTime = System.nanoTime()
    val killProcessBlock = scope.killProcessBlock()

    // always kill the process at beginning of a collection.
    killProcessBlock.invoke()
    try {
        userspaceTrace("generate profile for $packageName") {
            var iteration = 0
            // Disable because we're *creating* a baseline profile, not using it yet
            CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Disable,
                warmupIterations = iterations
            ).resetAndCompile(
                packageName = packageName,
                killProcessBlock = killProcessBlock
            ) {
                scope.iteration = iteration++
                profileBlock(scope)
            }
        }

        val unfilteredProfile = if (Build.VERSION.SDK_INT >= 33) {
            extractProfile(packageName)
        } else {
            extractProfileRooted(packageName)
        }

        check(unfilteredProfile.isNotBlank()) {
            """
                Generated Profile is empty, before filtering.
                Ensure your profileBlock invokes the target app, and
                runs a non-trivial amount of code.
            """.trimIndent()
        }
        // Filter
        val profile = filterProfileRulesToTargetP(unfilteredProfile)
        // Report
        reportResults(profile, packageFilters, uniqueName, startTime)
    } finally {
        killProcessBlock.invoke()
    }
}

/**
 * Collects baseline profiles using a given [profileBlock], while additionally
 * waiting until they are stable.
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@RequiresApi(28)
@JvmOverloads
fun collectStableBaselineProfile(
    uniqueName: String,
    packageName: String,
    stableIterations: Int,
    maxIterations: Int,
    strictStability: Boolean = false,
    packageFilters: List<String> = emptyList(),
    profileBlock: MacrobenchmarkScope.() -> Unit
) {
    val scope = buildMacrobenchmarkScope(packageName)
    val startTime = System.nanoTime()
    val killProcessBlock = scope.killProcessBlock()
    // always kill the process at beginning of a collection.
    killProcessBlock.invoke()

    try {
        var stableCount = 1
        var lastProfile: String? = null
        var iteration = 1

        while (iteration <= maxIterations) {
            userspaceTrace("generate profile for $packageName ($iteration)") {
                val mode = CompilationMode.Partial(
                    baselineProfileMode = BaselineProfileMode.Disable,
                    warmupIterations = 1
                )
                if (iteration == 1) {
                    Log.d(TAG, "Resetting compiled state for $packageName for stable profiles.")
                    mode.resetAndCompile(
                        packageName = packageName,
                        killProcessBlock = killProcessBlock
                    ) {
                        scope.iteration = iteration
                        profileBlock(scope)
                    }
                } else {
                    // Don't reset for subsequent iterations
                    Log.d(TAG, "Killing package $packageName")
                    killProcessBlock()
                    mode.compileImpl(packageName = packageName,
                        killProcessBlock = killProcessBlock
                    ) {
                        scope.iteration = iteration
                        Log.d(TAG, "Compile iteration (${scope.iteration}) for $packageName")
                        profileBlock(scope)
                    }
                }
            }
            val unfilteredProfile = if (Build.VERSION.SDK_INT >= 33) {
                extractProfile(packageName)
            } else {
                extractProfileRooted(packageName)
            }

            // Check stability
            val lastRuleSet = lastProfile?.lines()?.toSet() ?: emptySet()
            val existingRuleSet = unfilteredProfile.lines().toSet()
            if (lastRuleSet != existingRuleSet) {
                if (iteration != 1) {
                    Log.d(TAG, "Unstable profiles during iteration $iteration")
                }
                lastProfile = unfilteredProfile
                stableCount = 1
            } else {
                Log.d(TAG,
                    "Profiles stable in iteration $iteration (for $stableCount iterations)"
                )
                stableCount += 1
                if (stableCount == stableIterations) {
                    Log.d(TAG, "Baseline profile for $packageName is stable.")
                    break
                }
            }
            iteration += 1
        }

        if (strictStability) {
            check(stableCount == stableIterations) {
                "Baseline profiles for $packageName are not stable after $maxIterations."
            }
        }

        check(!lastProfile.isNullOrBlank()) {
            "Generated Profile is empty, before filtering. Ensure your profileBlock" +
                " invokes the target app, and runs a non-trivial amount of code"
        }

        val profile = filterProfileRulesToTargetP(lastProfile)
        reportResults(profile, packageFilters, uniqueName, startTime)
    } finally {
        killProcessBlock.invoke()
    }
}

/**
 * Builds a [MacrobenchmarkScope] instance after checking for the necessary pre-requisites.
 */
private fun buildMacrobenchmarkScope(packageName: String): MacrobenchmarkScope {
    require(
        Build.VERSION.SDK_INT >= 33 ||
            (Build.VERSION.SDK_INT >= 28 && Shell.isSessionRooted())
    ) {
        "Baseline Profile collection requires API 33+, or a rooted" +
            " device running API 28 or higher and rooted adb session (via `adb root`)."
    }
    getInstalledPackageInfo(packageName) // throws clearly if not installed
    return MacrobenchmarkScope(packageName, launchWithClearTask = true)
}

/**
 * Builds a function that can kill the target process using the provided [MacrobenchmarkScope].
 */
private fun MacrobenchmarkScope.killProcessBlock(): () -> Unit {
    val killProcessBlock = {
        // When generating baseline profiles we want to default to using
        // killProcess if the session is rooted. This is so we can collect
        // baseline profiles for System Apps.
        this.killProcess(useKillAll = Shell.isSessionRooted())
        Thread.sleep(Arguments.killProcessDelayMillis)
    }
    return killProcessBlock
}

/**
 * Reports the results after having collected baseline profiles.
 */
private fun reportResults(
    profile: String,
    packageFilters: List<String>,
    uniqueFilePrefix: String,
    startTime: Long
) {
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

    val fileName = "$uniqueFilePrefix-baseline-prof.txt"
    val absolutePath = Outputs.writeFile(fileName, "baseline-profile") {
        it.writeText(filteredProfile)
    }
    var startupProfilePath: String? = null
    if (startupProfile != null) {
        val startupProfileFileName = "$uniqueFilePrefix-startup-prof.txt"
        startupProfilePath = Outputs.writeFile(startupProfileFileName, "startup-profile") {
            it.writeText(startupProfile)
        }
    }
    val tsFileName = "$uniqueFilePrefix-baseline-prof-${Outputs.dateToFileName()}.txt"
    val tsAbsolutePath = Outputs.writeFile(tsFileName, "baseline-profile-ts") {
        Log.d(TAG, "Pull Baseline Profile with: `adb pull \"${it.absolutePath}\" .`")
        it.writeText(filteredProfile)
    }
    var tsStartupAbsolutePath: String? = null
    if (startupProfile != null) {
        val tsStartupFileName = "$uniqueFilePrefix-startup-prof-${Outputs.dateToFileName()}.txt"
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
}

/**
 * Use `pm dump-profiles` to get profile from the target app,
 * which puts results in `/data/misc/profman/`
 *
 * Does not require root.
 */
@RequiresApi(33)
private fun extractProfile(packageName: String): String {
    Shell.executeScriptSilent(
        "pm dump-profiles --dump-classes-and-methods $packageName"
    )
    val fileName = "$packageName-primary.prof.txt"
    Shell.executeScriptSilent(
        "mv /data/misc/profman/$fileName ${Outputs.dirUsableByAppAndShell}/"
    )

    val rawRuleOutput = File(Outputs.dirUsableByAppAndShell, fileName)
    try {
        return rawRuleOutput.readText()
    } finally {
        rawRuleOutput.delete()
    }
}

/**
 * Use profman to extract profiles from the current or reference profile
 *
 * Requires root.
 */
private fun extractProfileRooted(packageName: String): String {
    // The path of the reference profile
    val referenceProfile = "/data/misc/profiles/ref/$packageName/primary.prof"
    // The path to the primary profile
    val currentProfile = "/data/misc/profiles/cur/0/$packageName/primary.prof"
    Log.d(TAG, "Reference profile location: $referenceProfile")
    val pathResult = Shell.executeScriptCaptureStdout("pm path $packageName")
    // The result looks like: `package: <result>`
    val apkPath = pathResult.substringAfter("package:").trim()
    Log.d(TAG, "APK Path: $apkPath")
    // Convert to HRF
    Log.d(TAG, "Converting to human readable profile format")
    // Look at reference profile first, and then fallback to current profile
    return profmanGetProfileRules(apkPath, listOf(referenceProfile, currentProfile))
}

private fun profmanGetProfileRules(apkPath: String, pathOptions: List<String>): String {
    // When compiling with CompilationMode.SpeedProfile, ART stores the profile in one of
    // 2 locations. The `ref` profile path, or the `current` path.
    // The `current` path is eventually merged  into the `ref` path after background dexopt.
    val profiles = pathOptions.mapNotNull { currentPath ->
        Log.d(TAG, "Using profile location: $currentPath")
        val profile = Shell.executeScriptCaptureStdout(
            "profman --dump-classes-and-methods --profile-file=$currentPath --apk=$apkPath"
        )
        profile.ifBlank { null }
    }
    if (profiles.isEmpty()) {
        throw IllegalStateException("The profile is empty.")
    }
    // Merge rules
    val rules = mutableSetOf<String>()
    profiles.forEach { profile ->
        profile.lines().forEach { rule ->
            rules.add(rule)
        }
    }
    val builder = StringBuilder()
    rules.forEach {
        builder.append(it)
        builder.append("\n")
    }
    return builder.toString()
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
                adb pull $deviceSpecifier"${record.profilePath}" .
            """.trimIndent()
        )

    val startupProfilePath = record.startupProfilePath
    if (!startupProfilePath.isNullOrBlank()) {
        summary.append("\n")
            .append("\n")
            .append(
                """
                    To copy the startup profile use:
                    adb pull $deviceSpecifier"${record.startupProfilePath}" .
                """.trimIndent()
            )
    }
    return summary.toString()
}

/**
 * adb device specifier, blank if can't be defined. Includes right side space.
 */
internal val deviceSpecifier by lazy {
    if (DeviceInfo.isEmulator) {
        // emulators have serials that aren't usable via ADB -s,
        // so we just specify emulator and hope there's only one
        "-e "
    } else {
        val getpropOutput = Shell.executeScriptCaptureStdoutStderr("getprop ro.serialno")
        if (getpropOutput.stdout.isBlank() || getpropOutput.stderr.isNotBlank()) {
            "" // failed to get serial
        } else {
            "-s ${getpropOutput.stdout.trim()} "
        }
    }
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
