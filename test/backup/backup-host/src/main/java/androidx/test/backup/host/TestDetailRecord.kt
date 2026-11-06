/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Holds structured per-test execution details written to `suite-details/<id>.json`.
 *
 * Linked from JUnit Platform report entries via [BackupReportKeys.DETAIL_PATH].
 */
internal data class TestDetailRecord(
    val schemaVersion: Int = SCHEMA_VERSION,
    val suiteType: String? = SUITE_TYPE,
    val sourceLocation: SourceLocation? = null,
    val summaryAttributes: List<SummaryAttribute> = emptyList(),
    val callout: Callout? = null,
    val actors: List<ActorRecord> = emptyList(),
    val steps: List<StepRecord> = emptyList(),
    val logs: List<LogRecord> = emptyList(),
    val artifacts: List<ArtifactEntry> = emptyList(),
) {
    /** Identifies the source file and declaration line of a test. */
    internal data class SourceLocation(
        val file: String?,
        val line: Int? = null,
        val className: String = "",
        val methodName: String? = null,
    ) {
        fun toJsonObject(): JsonObject = buildJsonObject {
            put("file", file)
            put("line", line)
            put("className", className)
            put("methodName", methodName)
        }
    }

    /** Describes a key-value attribute surfaced on summary rows and detail headers. */
    internal data class SummaryAttribute(
        val key: String,
        val label: String,
        val value: String,
        val displayValue: String,
        val filterable: Boolean = false,
    ) {
        fun toJsonObject(): JsonObject = buildJsonObject {
            put("key", key)
            put("label", label)
            put("value", value)
            put("displayValue", displayValue)
            put("filterable", filterable)
        }
    }

    /** Summarizes a test failure, naming the failed step and diagnosed backup-rule cause. */
    internal data class Callout(
        val kind: String,
        val failedStep: Int?,
        val title: String,
        val message: String,
        val likelyCause: String? = null,
    ) {
        fun toJsonObject(): JsonObject = buildJsonObject {
            put("kind", kind)
            put("failedStep", failedStep)
            put("title", title)
            put("message", message)
            put("likelyCause", likelyCause)
        }
    }

    /** Describes a device or emulator participant in the test execution. */
    internal data class ActorRecord(
        val id: String,
        val role: String,
        val name: String,
        val api: Int,
        val serial: String,
        val image: String,
        val playServices: String? = null,
    ) {
        fun toJsonObject(): JsonObject = buildJsonObject {
            put("id", id)
            put("role", role)
            put("name", name)
            put("api", api)
            put("serial", serial)
            put("image", image)
            put("playServices", playServices)
        }
    }

    /** Records a single execution step in the test timeline. */
    internal data class StepRecord(
        val n: Int?,
        val name: String,
        val on: String?,
        val shared: Boolean = false,
        val call: String,
        val durationMs: Long,
        val status: String,
    ) {
        fun toJsonObject(): JsonObject = buildJsonObject {
            put("n", n)
            put("name", name)
            put("on", on)
            put("shared", shared)
            put("call", call)
            put("durationMs", durationMs)
            put("status", status)
        }
    }

    /** References a captured log file relative to the suite results directory. */
    internal data class LogRecord(
        val title: String,
        val path: String,
        val truncated: Boolean = false,
    ) {
        fun toJsonObject(): JsonObject = buildJsonObject {
            put("title", title)
            put("path", path)
            put("truncated", truncated)
        }
    }

    /** Describes an artifact summary or file attachment relative to the results directory. */
    internal data class ArtifactEntry(
        val name: String,
        val values: Map<String, String> = emptyMap(),
        val path: String? = null,
    ) {
        fun toJsonObject(): JsonObject = buildJsonObject {
            put("name", name)
            put(
                "values",
                buildJsonObject {
                    for ((k, v) in values) {
                        put(k, v)
                    }
                },
            )
            put("path", path)
        }
    }

    fun toJsonObject(): JsonObject = buildJsonObject {
        put("schemaVersion", schemaVersion)
        put("suiteType", suiteType)
        put("sourceLocation", sourceLocation?.toJsonObject() ?: JsonNull)
        put("summaryAttributes", JsonArray(summaryAttributes.map { it.toJsonObject() }))
        put("callout", callout?.toJsonObject() ?: JsonNull)
        put("actors", JsonArray(actors.map { it.toJsonObject() }))
        put("steps", JsonArray(steps.map { it.toJsonObject() }))
        put("logs", JsonArray(logs.map { it.toJsonObject() }))
        put("artifacts", JsonArray(artifacts.map { it.toJsonObject() }))
    }

    fun toJsonString(): String = PRETTY_JSON.encodeToString(JsonObject.serializer(), toJsonObject())

    /** Flattens this record into ordered non-blank `androidx.report.*` JUnit report entries. */
    fun toReportEntries(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        putNonBlank(out, BackupReportKeys.SCHEMA_VERSION, schemaVersion.toString())
        putNonBlank(out, BackupReportKeys.SUITE_TYPE, suiteType)

        sourceLocation?.let { loc ->
            val file = loc.file?.takeIf { it.isNotBlank() }
            if (file != null) {
                val formatted = if (loc.line != null && loc.line > 0) "$file:${loc.line}" else file
                putNonBlank(out, BackupReportKeys.SOURCE, formatted)
            }
        }

        for (attr in summaryAttributes) {
            if (attr.key.isBlank()) continue
            val prefix = "${BackupReportKeys.ATTR_PREFIX}${attr.key}"
            putNonBlank(out, prefix, attr.value)
            if (attr.label != defaultSummaryAttributeLabel(attr.key)) {
                putNonBlank(out, "$prefix.label", attr.label)
            }
            if (attr.displayValue != attr.value) {
                putNonBlank(out, "$prefix.displayValue", attr.displayValue)
            }
            if (attr.filterable) {
                putNonBlank(out, "$prefix.filterable", "true")
            }
        }

        for (actor in actors) {
            if (actor.id.isBlank()) continue
            val prefix = "${BackupReportKeys.ACTOR_PREFIX}${actor.id}"
            putNonBlank(out, "$prefix.role", actor.role)
            putNonBlank(out, "$prefix.name", actor.name)
            putNonBlank(out, "$prefix.api", actor.api.toString())
            putNonBlank(out, "$prefix.serial", actor.serial)
            putNonBlank(out, "$prefix.image", actor.image)
            putNonBlank(out, "$prefix.playServices", actor.playServices)
        }

        var unnumberedIndex = 0
        for (step in steps) {
            val stepKey = step.n?.toString() ?: "prepare_${++unnumberedIndex}"
            val prefix = "${BackupReportKeys.STEP_PREFIX}$stepKey"
            putNonBlank(out, "$prefix.name", step.name)
            putNonBlank(out, "$prefix.on", step.on)
            if (step.shared) {
                putNonBlank(out, "$prefix.shared", "true")
            }
            putNonBlank(out, "$prefix.call", step.call)
            putNonBlank(out, "$prefix.durationMs", step.durationMs.toString())
            putNonBlank(out, "$prefix.status", step.status)
        }

        callout?.let { co ->
            val prefix = BackupReportKeys.CALLOUT_PREFIX
            putNonBlank(out, "${prefix}kind", co.kind)
            co.failedStep?.let { putNonBlank(out, "${prefix}failedStep", it.toString()) }
            putNonBlank(out, "${prefix}title", co.title)
            putNonBlank(out, "${prefix}message", co.message)
            putNonBlank(out, "${prefix}likelyCause", co.likelyCause)
        }

        var summaryIndex = 0
        for (artifact in artifacts) {
            if (artifact.path == null) {
                summaryIndex++
                val prefix = "${BackupReportKeys.ARTIFACT_PREFIX}$summaryIndex"
                putNonBlank(out, "$prefix.name", artifact.name)
                if (artifact.values.isNotEmpty()) {
                    putNonBlank(out, "$prefix.values", formatKeyValues(artifact.values))
                }
            } else {
                val fileName = artifact.path.substringAfterLast('/')
                if (fileName.isNotBlank()) {
                    if (artifact.name != fileName) {
                        putNonBlank(
                            out,
                            "${BackupReportKeys.FILE_PREFIX}$fileName.name",
                            artifact.name,
                        )
                    }
                    if (artifact.values.isNotEmpty()) {
                        putNonBlank(
                            out,
                            "${BackupReportKeys.FILE_PREFIX}$fileName.values",
                            formatKeyValues(artifact.values),
                        )
                    }
                }
            }
        }

        for (log in logs) {
            val fileName = log.path.substringAfterLast('/')
            if (fileName.isNotBlank()) {
                val isDefaultHostLogTitle = fileName == "host-log.txt" && log.title == "Host log"
                if (log.title.isNotBlank() && log.title != fileName && !isDefaultHostLogTitle) {
                    putNonBlank(out, "${BackupReportKeys.FILE_PREFIX}$fileName.name", log.title)
                }
                if (log.truncated) {
                    putNonBlank(out, "${BackupReportKeys.FILE_PREFIX}$fileName.truncated", "true")
                }
            }
        }

        return out
    }

    companion object {
        const val SCHEMA_VERSION = 1
        const val SUITE_TYPE = "androidx.test.backup"

        const val STATUS_PASSED = "PASSED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_SKIPPED = "SKIPPED"

        const val CALLOUT_ASSERTION_FAILURE = "ASSERTION_FAILURE"
        const val CALLOUT_STEP_FAILURE = "STEP_FAILURE"

        private val PRETTY_JSON = Json { prettyPrint = true }

        internal fun defaultSummaryAttributeLabel(key: String): String =
            key.replace('_', ' ').replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }

        private fun formatKeyValues(values: Map<String, String>): String =
            values.entries.joinToString(", ") { (k, v) -> "$k=$v" }

        private fun putNonBlank(map: MutableMap<String, String>, key: String, value: String?) {
            if (key.isNotBlank() && !value.isNullOrBlank()) {
                map[key] = value
            }
        }
    }
}
