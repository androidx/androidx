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
        val line: Int?,
        val className: String,
        val methodName: String?,
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

    companion object {
        const val SCHEMA_VERSION = 1
        const val SUITE_TYPE = "androidx.test.backup"

        const val STATUS_PASSED = "PASSED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_SKIPPED = "SKIPPED"

        const val CALLOUT_ASSERTION_FAILURE = "ASSERTION_FAILURE"
        const val CALLOUT_STEP_FAILURE = "STEP_FAILURE"

        private val PRETTY_JSON = Json { prettyPrint = true }
    }
}
