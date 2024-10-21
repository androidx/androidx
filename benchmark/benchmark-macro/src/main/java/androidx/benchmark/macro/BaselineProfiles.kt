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
import androidx.benchmark.Markdown
import androidx.benchmark.Outputs
import androidx.benchmark.Shell
import androidx.benchmark.UserInfo
import androidx.benchmark.VirtualFile
import androidx.tracing.trace
import java.io.File

/**
 * Collects baseline profiles using a given [profileBlock], while additionally waiting until they
 * are stable.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(28)
fun collect(
    uniqueName: String,
    packageName: String,
    stableIterations: Int,
    maxIterations: Int,
    strictStability: Boolean = false,
    includeInStartupProfile: Boolean,
    filterPredicate: ((String) -> Boolean),
    profileBlock: MacrobenchmarkScope.() -> Unit
) {
    val scope = buildMacrobenchmarkScope(packageName)
    val uid = UserInfo.currentUserId
    val startTime = System.nanoTime()
    // Ensure the device is awake
    scope.device.wakeUp()
    // always kill the process at beginning of a collection.
    scope.killProcess()

    try {
        var stableCount = 1
        var lastProfile: String? = null
        var iteration = 1
        val finalMaxIterations = if (Arguments.dryRunMode) 1 else maxIterations

        while (iteration <= finalMaxIterations) {
            trace("generate profile for $packageName ($iteration)") {
                val mode =
                    CompilationMode.Partial(
                        baselineProfileMode = BaselineProfileMode.Disable,
                        warmupIterations = 1
                    )
                if (iteration == 1) {
                    Log.d(TAG, "Resetting compiled state for $packageName for stable profiles.")
                    mode.resetAndCompile(
                        scope,
                        allowCompilationSkipping = false,
                    ) {
                        scope.iteration = iteration
                        profileBlock(scope)
                    }
                } else {
                    // Don't reset for subsequent iterations
                    Log.d(TAG, "Killing package $packageName")
                    scope.killProcess()
                    mode.compileImpl(scope) {
                        scope.iteration = iteration
                        Log.d(TAG, "Compile iteration (${scope.iteration}) for $packageName")
                        profileBlock(scope)
                    }
                }
            }
            val unfilteredProfile =
                if (Build.VERSION.SDK_INT >= 33) {
                    extractProfile(packageName)
                } else {
                    extractProfileRooted(uid, packageName)
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
                Log.d(TAG, "Profiles stable in iteration $iteration (for $stableCount iterations)")
                stableCount += 1
                if (stableCount == stableIterations) {
                    Log.d(TAG, "Baseline profile for $packageName is stable.")
                    break
                }
            }
            iteration += 1
        }

        if (strictStability && !Arguments.dryRunMode) {
            check(stableCount == stableIterations) {
                "Baseline profiles for $packageName are not stable after $maxIterations."
            }
        }

        check(!lastProfile.isNullOrBlank()) {
            "Generated Profile is empty, before filtering. Ensure your profileBlock" +
                " invokes the target app, and runs a non-trivial amount of code"
        }

        val profile =
            filterProfileRulesToTargetP(
                profile = lastProfile,
                sortRules = true,
                filterPredicate = filterPredicate
            )
        reportResults(
            profile = profile,
            uniqueFilePrefix = uniqueName,
            startTime = startTime,
            includeInStartupProfile = includeInStartupProfile
        )
    } finally {
        scope.killProcess()
    }
}

/** Builds a [MacrobenchmarkScope] instance after checking for the necessary pre-requisites. */
private fun buildMacrobenchmarkScope(packageName: String): MacrobenchmarkScope {
    Arguments.throwIfError()
    require(DeviceInfo.supportsBaselineProfileCaptureError == null) {
        DeviceInfo.supportsBaselineProfileCaptureError!!
    }
    getInstalledPackageInfo(packageName) // throws clearly if not installed
    return MacrobenchmarkScope(packageName, launchWithClearTask = true)
}

/** Reports the results after having collected baseline profiles. */
private fun reportResults(
    profile: String,
    uniqueFilePrefix: String,
    startTime: Long,
    includeInStartupProfile: Boolean
) {
    // Write a file with a timestamp to be able to disambiguate between runs with the same
    // unique name.

    val (fileName, tsFileName) =
        if (includeInStartupProfile && Arguments.enableStartupProfiles) {
            arrayOf(
                "$uniqueFilePrefix-startup-prof.txt",
                "$uniqueFilePrefix-startup-prof-${Outputs.dateToFileName()}.txt"
            )
        } else {
            arrayOf(
                "$uniqueFilePrefix-baseline-prof.txt",
                "$uniqueFilePrefix-baseline-prof-${Outputs.dateToFileName()}.txt"
            )
        }

    val absolutePath = Outputs.writeFile(fileName) { it.writeText(profile) }
    val tsAbsolutePath =
        Outputs.writeFile(tsFileName) {
            Log.d(TAG, "Pull Baseline Profile with: `adb pull \"${it.absolutePath}\" .`")
            it.writeText(profile)
        }

    val totalRunTime = System.nanoTime() - startTime
    val results =
        Summary(
            totalRunTime = totalRunTime,
            profilePath = absolutePath,
            profileTsPath = tsAbsolutePath
        )
    InstrumentationResults.instrumentationReport {
        // Ideally would link trace as a profiler result for consistency with other codepaths,
        // but we don't to change BP's custom link appearance to the default simple one
        reportSummaryToIde(
            testName = uniqueFilePrefix,
            message = summaryRecord(results),
        )
        Log.d(TAG, "Total Run Time Ns: $totalRunTime")
    }
}

/**
 * Use `pm dump-profiles` to get profile from the target app, which puts results in
 * `/data/misc/profman/`
 *
 * Does not require root.
 */
@RequiresApi(33)
private fun extractProfile(packageName: String): String {
    val dumpCommand = "pm dump-profiles --dump-classes-and-methods $packageName"
    val stdout = Shell.executeScriptCaptureStdout(dumpCommand).trim()
    val expected = "Profile saved to '/data/misc/profman/$packageName-primary.prof.txt'"

    // Output of profman was empty in previous version and can be `expected` on newer versions.
    check(stdout.isBlank() || stdout == expected) {
        "Expected `pm dump-profiles` stdout to be either black or `$expected` but was $stdout"
    }

    if (UserInfo.isAdditionalUser) {
        return VirtualFile.fromPath("/data/misc/profman/$packageName-primary.prof.txt").readText()
    }

    val fileName = "$packageName-primary.prof.txt"
    Shell.executeScriptSilent("mv /data/misc/profman/$fileName ${Outputs.dirUsableByAppAndShell}/")

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
private fun extractProfileRooted(uid: Int, packageName: String): String {
    // The path of the reference profile
    val referenceProfile = "/data/misc/profiles/ref/$packageName/primary.prof"
    // The path to the primary profile
    val currentProfile = "/data/misc/profiles/cur/$uid/$packageName/primary.prof"
    Log.d(TAG, "Reference profile location: $referenceProfile")

    @Suppress("SimplifiableCallChain") // join+block makes ordering unclear
    val mergedProfile =
        Shell.pmPath(packageName)
            .map { apkPath ->
                Log.d(TAG, "APK Path: $apkPath")
                // Convert to HRF
                Log.d(TAG, "Converting to human readable profile format")
                // Look at reference profile first, and then fallback to current profile
                profmanGetProfileRules(apkPath, listOf(referenceProfile, currentProfile))
            }
            .joinToString(separator = "\n")
    if (mergedProfile.isBlank()) {
        throw IllegalStateException("No profiles found for all apks in app")
    }

    return mergedProfile
}

private fun profmanGetProfileRules(apkPath: String, pathOptions: List<String>): String {
    // When compiling with CompilationMode.SpeedProfile, ART stores the profile in one of
    // 2 locations. The `ref` profile path, or the `current` path.
    // The `current` path is eventually merged  into the `ref` path after background dexopt.
    val profiles =
        pathOptions.mapNotNull { currentPath ->
            Log.d(TAG, "Using profile location: $currentPath")
            val profile =
                Shell.executeScriptCaptureStdout(
                    "profman --dump-classes-and-methods --profile-file=$currentPath --apk=$apkPath"
                )
            profile.ifBlank { null }
        }
    if (profiles.isEmpty()) {
        Log.d(TAG, "No profiles found for $apkPath")
        return ""
    }

    // Merge rules
    val rules = mutableSetOf<String>()
    profiles.forEach { profile -> profile.lines().forEach { rule -> rules.add(rule) } }
    val builder = StringBuilder()
    rules.forEach {
        builder.append(it)
        builder.append("\n")
    }
    return builder.toString()
}

@VisibleForTesting
internal fun filterProfileRulesToTargetP(
    profile: String,
    sortRules: Boolean = true,
    filterPredicate: ((String) -> Boolean)
): String {
    val rules = profile.lines()
    var filteredRules =
        rules
            .filterNot { rule ->
                // We want to filter out rules that are not supported on P. (b/216508418)
                // These include rules that have array qualifiers and inline cache specifiers.
                if (rule.startsWith("[")) { // Array qualifier
                    true
                } else rule.contains("+") // Inline cache specifier
            }
            .filter(filterPredicate)

    if (sortRules) {
        filteredRules =
            filteredRules
                .mapNotNull { ProfileRule.parse(it) }
                .sortedWith(ProfileRule.comparator)
                .map { it.underlying }
    }
    return filteredRules.joinToString(separator = "\n")
}

private fun summaryRecord(record: Summary): String {
    val summary = StringBuilder()

    // Links

    // Link to a path with timestamp to prevent studio from caching the file
    val relativePath = Outputs.relativePathFor(record.profileTsPath)

    summary.append(
        """
            Total run time Ns: ${record.totalRunTime}.
            Baseline profile ${Markdown.createFileLink("results", relativePath)}
        """
            .trimIndent()
    )

    // Add commands that can be used to pull these files.
    summary
        .append("\n")
        .append("\n")
        .append(
            """
                To copy the profile use:
                adb ${deviceSpecifier}pull "${record.profilePath}" .
            """
                .trimIndent()
        )
    return summary.toString()
}

/** adb device specifier, blank if can't be defined. Includes right side space. */
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
)
