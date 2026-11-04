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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.Serializable
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/** Shared Gson instances for serializing and deserializing keep rule analysis reports. */
internal object KeepRulesJson {
    val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
    val prettyGson: Gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()

    private val baselineMapType = object : TypeToken<Map<String, BaselineConfig>>() {}.type

    fun parseBaselineJson(json: String?): Map<String, BaselineConfig> {
        if (json.isNullOrBlank()) return emptyMap()
        return gson.fromJson<Map<String, BaselineConfig>>(json, baselineMapType).orEmpty()
    }

    fun renderBaselineJson(baselines: Map<String, BaselineConfig>): String {
        val normalized =
            baselines.entries
                .sortedBy { it.key }
                .associate { (projectPath, config) ->
                    projectPath to
                        BaselineConfig(
                            overrideMaxKeepRatio = config.overrideMaxKeepRatio,
                            overrideMaxD8SizeMegabytes = config.overrideMaxD8SizeMegabytes,
                            baselineUsedPackageWideKeepRules =
                                config.baselineUsedPackageWideKeepRules
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.toSortedSet(),
                            baselineUnusedPackageWideKeepRules =
                                config.baselineUnusedPackageWideKeepRules
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.toSortedSet(),
                        )
                }
        return prettyGson.toJson(normalized) + "\n"
    }
}

/**
 * Per-library baseline configuration for consumer keep rule analysis (`android analyze`), stored in
 * [KEEP_RULES_BASELINE_RELATIVE_PATH].
 */
internal data class BaselineConfig(
    @get:Input @get:Optional val overrideMaxKeepRatio: Float? = null,
    @get:Input @get:Optional val overrideMaxD8SizeMegabytes: Float? = null,
    @get:Input @get:Optional val baselineUsedPackageWideKeepRules: Set<String>? = null,
    @get:Input @get:Optional val baselineUnusedPackageWideKeepRules: Set<String>? = null,
) : Serializable

/**
 * Intermediate report written by [AnalyzeKeepRulesTask] and consumed by validation/baseline tasks.
 */
internal data class KeepRulesAnalysisReport(
    val projectPath: String,
    val results: List<AnalyzeResultItemJson>? = null,
)

/** Top-level JSON output emitted by `android analyze --analysis=optimization -f json`. */
internal data class AnalyzeCommandOutput(val results: List<AnalyzeResultItemJson>? = null)

internal data class AnalyzeResultItemJson(val optimization: OptimizationAnalysisJson? = null)

internal data class OptimizationAnalysisJson(
    val libraryAnalysisResult: LibraryAnalysisResultJson? = null
)

internal data class LibraryAnalysisResultJson(
    val warnings: List<String>? = null,
    val dex: LibraryAnalyzerResultJson? = null,
    val r8Version: String? = null,
    val version: String? = null,
)

internal data class LibraryAnalyzerResultJson(
    val d8CompileResult: D8CompileResultJson? = null,
    val r8CompileResult: R8CompileResultJson? = null,
    val validateConsumerKeepRulesResult: ValidateConsumerKeepRulesResultJson? = null,
)

// Note: Gson's built-in Long adapter accepts both JSON numbers (e.g. `100000`) and JSON strings
// (e.g. `"100000"`, as emitted by Protobuf's canonical int64 JSON mapping).
internal data class D8CompileResultJson(val dexSizeBytes: Long? = null)

internal data class R8CompileResultJson(
    val dexSizeBytes: Long? = null,
    val configuration: ConfigurationSummaryJson? = null,
)

internal data class ConfigurationSummaryJson(
    val keepRules: List<KeepRuleSummaryJson>? = null,
    val usedPackageWideKeepRules: List<KeepRuleSummaryJson>? = null,
    val unusedPackageWideKeepRules: List<KeepRuleSummaryJson>? = null,
)

internal data class KeepRuleSummaryJson(
    val source: String = "",
    val keptItemCount: Int? = null,
    val noObfuscation: Boolean? = null,
    val noOptimization: Boolean? = null,
    val noShrinking: Boolean? = null,
)

internal data class ValidateConsumerKeepRulesResultJson(
    val blockedKeepRules: List<BlockedConsumerKeepRuleJson>? = null
)

internal data class BlockedConsumerKeepRuleJson(val source: String = "")
