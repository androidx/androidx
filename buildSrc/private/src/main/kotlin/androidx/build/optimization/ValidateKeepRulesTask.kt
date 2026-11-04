/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.build.optimization

import androidx.build.logging.TERMINAL_RED
import androidx.build.logging.TERMINAL_RESET
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToLong
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Validates the JSON [KeepRulesAnalysisReport] produced by [AnalyzeKeepRulesTask] against the
 * library's [BaselineConfig] in [KEEP_RULES_BASELINE_RELATIVE_PATH].
 *
 * Note that baselines act as upper bounds: libraries whose metrics improve below their baselined
 * [BaselineConfig.overrideMaxKeepRatio] or [BaselineConfig.overrideMaxD8SizeMegabytes], or that
 * remove baselined package-wide keep rules, do not currently fail validation (running
 * [UpdateKeepRulesBaselineTask] will prune fixed baseline entries).
 */
@CacheableTask
internal abstract class ValidateKeepRulesTask : DefaultTask() {

    init {
        group = "Verification"
        description =
            "Validates consumer keep rules in the published artifact using android analyze and " +
                "compares against $KEEP_RULES_BASELINE_RELATIVE_PATH."
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val reportFile: RegularFileProperty

    @get:Nested @get:Optional abstract val baseline: Property<BaselineConfig>

    @TaskAction
    fun validate() {
        val file = reportFile.get().asFile
        val report =
            KeepRulesJson.gson.fromJson(file.readText(), KeepRulesAnalysisReport::class.java)
                ?: throw GradleException("Could not parse keep rules report at ${file.path}")
        val analysisResult = report.results?.firstOrNull()?.optimization?.libraryAnalysisResult
        val warningsSuffix = formatWarningsSuffix(analysisResult?.warnings)
        val dexResult =
            analysisResult?.dex
                ?: throw GradleException(
                    "Missing libraryAnalysisResult.dex in ${file.path} for ${report.projectPath}$warningsSuffix"
                )
        KeepRulesErrorReporter.validateLibraryResult(
            projectPath = report.projectPath,
            result = dexResult,
            baseline = baseline.orNull,
            warnings = analysisResult.warnings.orEmpty(),
        )
    }

    companion object {
        const val TASK_NAME = "validateKeepRules"
    }
}

/** Validates library optimization analysis results against default thresholds and baselines. */
internal object KeepRulesErrorReporter {
    private const val BYTES_PER_MEGABYTE: Long = 1_000_000L
    const val DEFAULT_D8_MAX_SIZE_BYTES: Long = 5L * BYTES_PER_MEGABYTE // 5 MB
    private const val DEFAULT_R8_MAX_SIZE_BYTES: Long = 1L * BYTES_PER_MEGABYTE // 1 MB
    const val DEFAULT_MAX_KEEP_RATIO: Float = 0.20f
    private const val IGNORED_BYTES_FOR_KEEP_RATIO: Long =
        5_000L // 5 KB budget before ratio goes above 0
    private val UNSUPPRESSABLE_BLOCKED_KEEP_RULES: Set<String> =
        setOf("-dontoptimize", "-dontshrink", "-dontobfuscate")

    fun computeKeepRatio(d8SizeBytes: Long, r8SizeBytes: Long): Float {
        if (d8SizeBytes <= 0L) return 0.0f
        return (max(r8SizeBytes - IGNORED_BYTES_FOR_KEEP_RATIO, 0L).toDouble() / d8SizeBytes)
            .toFloat()
    }

    /**
     * Computes the baselined `keepRatio` threshold as `current + 0.02`, floored to the nearest
     * percentile (`0.01f`).
     */
    fun computeBaselineKeepRatio(keepRatio: Float): Float {
        val flooredCents = floor((keepRatio.toDouble() + 0.02) * 100.0).toInt().coerceAtMost(100)
        return flooredCents / 100.0f
    }

    /**
     * Computes the baselined `maxD8SizeMegabytes` threshold in megabytes, rounded up (ceiling) to
     * the nearest 100 KB (`0.1f` MB).
     */
    fun computeBaselineMaxD8SizeMegabytes(d8SizeBytes: Long): Float {
        val ceiledTenths = ceil(d8SizeBytes.toDouble() / 100_000.0).toInt()
        return ceiledTenths / 10.0f
    }

    fun validateLibraryResult(
        projectPath: String,
        result: LibraryAnalyzerResultJson,
        baseline: BaselineConfig?,
        warnings: List<String> = emptyList(),
    ) {
        val warningsSuffix = formatWarningsSuffix(warnings)
        val d8CompileResult =
            result.d8CompileResult
                ?: throw GradleException(
                    "Unable to compile library $projectPath with D8 during keep rule analysis$warningsSuffix"
                )
        val r8CompileResult =
            result.r8CompileResult
                ?: throw GradleException(
                    "Unable to compile library $projectPath with R8 during keep rule analysis$warningsSuffix"
                )

        val d8SizeBytes = d8CompileResult.dexSizeBytes ?: 0L
        val r8SizeBytes = r8CompileResult.dexSizeBytes ?: 0L
        val keepRatio = computeKeepRatio(d8SizeBytes, r8SizeBytes)

        val errors = mutableListOf<String>()
        var reportHighImpactKeepRules = false

        // Checks that support baseline exemptions:
        checkKeepRatio(keepRatio, d8SizeBytes, r8SizeBytes, baseline)?.let { error ->
            reportHighImpactKeepRules = true
            errors.add(error)
        }
        checkD8Size(d8SizeBytes, baseline)?.let { errors.add(it) }
        checkUsedPackageWideKeepRules(r8CompileResult, baseline)?.let { error ->
            reportHighImpactKeepRules = true
            errors.add(error)
        }
        checkUnusedPackageWideKeepRules(r8CompileResult, baseline)?.let { errors.add(it) }

        // Checks without baseline exemptions:
        checkR8Size(r8SizeBytes)?.let { error ->
            reportHighImpactKeepRules = true
            errors.add(error)
        }
        checkBlockedKeepRules(result)?.let { errors.add(it) }

        if (errors.isNotEmpty()) {
            val problemCount = errors.size
            if (reportHighImpactKeepRules) {
                errors.add(formatHighImpactKeepRulesSummary(r8CompileResult))
            }
            val header =
                "Keep rule analysis failed for $projectPath: found $problemCount problem ${pluralize("pattern", problemCount)}:"
            throw GradleException(
                "$header\n\n" +
                    errors.joinToString("\n\n") +
                    "\n\nIf this violation is tracked and requires a baseline exemption in " +
                    "$KEEP_RULES_BASELINE_RELATIVE_PATH, run:\n" +
                    "  ${TERMINAL_RED}PROJECT_PREFIX=$projectPath ./gradlew ${UpdateKeepRulesBaselineTask.TASK_NAME}$TERMINAL_RESET"
            )
        }
    }

    private fun checkKeepRatio(
        keepRatio: Float,
        d8SizeBytes: Long,
        r8SizeBytes: Long,
        baseline: BaselineConfig?,
    ): String? {
        val keepRatioLimit = baseline?.overrideMaxKeepRatio ?: DEFAULT_MAX_KEEP_RATIO
        if (keepRatio <= keepRatioLimit) return null
        return """
            Error: Keep ratio is too high ($keepRatio, limit: $keepRatioLimit (d8: $d8SizeBytes bytes, r8: $r8SizeBytes bytes)).
            The d8 and r8 values represent the minimum size of this artifact by itself when compiled
            with d8 vs r8.
            """
            .trimIndent()
    }

    private fun checkUsedPackageWideKeepRules(
        r8CompileResult: R8CompileResultJson,
        baseline: BaselineConfig?,
    ): String? {
        val baselineUsedRules =
            baseline?.baselineUsedPackageWideKeepRules?.mapTo(mutableSetOf()) { it.trim() }
                ?: emptySet()
        val usedPackageWideKeepRules =
            r8CompileResult.configuration
                ?.usedPackageWideKeepRules
                .orEmpty()
                .map { it.source.trim() }
                .filter { it.isNotEmpty() && it !in baselineUsedRules }
                .toSet()
        if (usedPackageWideKeepRules.isEmpty()) return null

        val rulesString = formatKeepRules(usedPackageWideKeepRules)
        val header =
            "${usedPackageWideKeepRules.size} used package-wide keep ${pluralize("rule", usedPackageWideKeepRules.size)}:"
        return """
            Error: The library uses package-wide keep rules to keep library code from being optimized.
            These keep rules aren't appropriate for shipping in a production library.
            $header
            """
            .trimIndent() + "\n" + rulesString
    }

    private fun checkUnusedPackageWideKeepRules(
        r8CompileResult: R8CompileResultJson,
        baseline: BaselineConfig?,
    ): String? {
        val baselineUnusedRules =
            baseline?.baselineUnusedPackageWideKeepRules?.mapTo(mutableSetOf()) { it.trim() }
                ?: emptySet()
        val unusedPackageWideKeepRules =
            r8CompileResult.configuration
                ?.unusedPackageWideKeepRules
                .orEmpty()
                .map { it.source.trim() }
                .filter { it.isNotEmpty() && it !in baselineUnusedRules }
                .toSet()
        if (unusedPackageWideKeepRules.isEmpty()) return null

        val rulesString = formatKeepRules(unusedPackageWideKeepRules)
        val header =
            "${unusedPackageWideKeepRules.size} unused package-wide keep ${pluralize("rule", unusedPackageWideKeepRules.size)}:"
        return """
            Error: The library contains package-wide keep rules which do not target library code.
            Either these rules are not useful, or they should be expressed targeting specific classes
            in a different library as necessary. If a bug is tracking the target library fixing their
            own keep rules, they may be added to the baseline for this library.
            $header
            """
            .trimIndent() + "\n" + rulesString
    }

    private fun checkD8Size(d8SizeBytes: Long, baseline: BaselineConfig?): String? {
        val d8SizeLimitBytes =
            baseline?.overrideMaxD8SizeMegabytes?.let {
                (it.toDouble() * BYTES_PER_MEGABYTE).roundToLong()
            } ?: DEFAULT_D8_MAX_SIZE_BYTES
        if (d8SizeBytes <= d8SizeLimitBytes) return null
        return """
            Error: D8 size is too high ($d8SizeBytes bytes, limit: $d8SizeLimitBytes bytes). This indicates your library without any POM
            dependencies contains an extremely large amount of dex code which will increase app size
            drastically.

            Are you including unnecessary code?
            """
            .trimIndent()
    }

    private fun checkR8Size(r8SizeBytes: Long): String? {
        if (r8SizeBytes <= DEFAULT_R8_MAX_SIZE_BYTES) return null
        return """
            Error: R8 size is too high ($r8SizeBytes bytes, limit: $DEFAULT_R8_MAX_SIZE_BYTES bytes). This indicates your library is keeping too much
            code with keep rules.
            """
            .trimIndent()
    }

    private fun checkBlockedKeepRules(result: LibraryAnalyzerResultJson): String? {
        val blockedKeepRules =
            result.validateConsumerKeepRulesResult
                ?.blockedKeepRules
                .orEmpty()
                .map { it.source.trim() }
                .filter { it in UNSUPPRESSABLE_BLOCKED_KEEP_RULES }
                .toSet()
        if (blockedKeepRules.isEmpty()) return null

        val rulesString = formatKeepRules(blockedKeepRules)
        val header =
            "${blockedKeepRules.size} blocked global keep ${pluralize("rule", blockedKeepRules.size)}:"
        return """
            Error: The library contains blocked keep rules which should never be in consumer rules.
            These keep rules disable optimization, shrinking, or obfuscation for the entire
            application, and cannot be suppressed.
            $header
            """
            .trimIndent() + "\n" + rulesString
    }

    private fun formatHighImpactKeepRulesSummary(r8CompileResult: R8CompileResultJson): String {
        val rulesString =
            formatKeepRules(
                r8CompileResult.configuration?.keepRules.orEmpty().map { keepRule ->
                    "(impact = ${keepRule.keptItemCount ?: 0}) ${keepRule.source.trim()}"
                }
            )
        return """
        This library contains keep rules which significantly limit the effectiveness of optimization
        when embedded in an app. This is measured by compiling your library with R8, without any
        references (ideally, this size would be nearly zero).

        For more information on why limiting keep rules is important, see:
        https://developer.android.com/topic/performance/app-optimization/library-optimization

        Your library contains the following highest impact (# of classes+fields+methods targeted):
        """
            .trimIndent() + "\n" + rulesString
    }

    private fun formatKeepRules(rules: Collection<String>): String =
        rules.joinToString("\n") { rule -> rule.trimEnd().prependIndent("    ") }

    private fun pluralize(word: String, count: Int): String = if (count == 1) word else "${word}s"
}

private fun formatWarningsSuffix(warnings: List<String>?): String =
    warnings?.takeIf { it.isNotEmpty() }?.joinToString("\n", prefix = "\nWarnings:\n") ?: ""
