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

/** Formats [bytes] into a concise human-readable unit string (`412 B`, `3.1 KB`, `1.8 MB`). */
internal fun formatByteSize(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0L)
    return when {
        safeBytes < 1024L -> "$safeBytes B"
        safeBytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", safeBytes / 1024.0)
        safeBytes < 1024L * 1024L * 1024L ->
            String.format(Locale.US, "%.1f MB", safeBytes / (1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.1f GB", safeBytes / (1024.0 * 1024.0 * 1024.0))
    }
}

/** Holds UTF-8 tail bytes and whether [capUtf8TailBounded] truncated the input. */
internal class BoundedUtf8Result(val bytes: ByteArray, val truncated: Boolean)

/**
 * Encodes [text] as UTF-8 bounded to [maxBytes] tail bytes, reporting whether truncation occurred.
 */
internal fun capUtf8TailBounded(text: String, maxBytes: Int): BoundedUtf8Result {
    if (maxBytes <= 0) return BoundedUtf8Result(ByteArray(0), truncated = text.isNotEmpty())
    val bytes = text.toByteArray(Charsets.UTF_8)
    if (bytes.size <= maxBytes) return BoundedUtf8Result(bytes, truncated = false)

    val noticeBytes = TRUNCATION_NOTICE_BYTES
    if (maxBytes <= noticeBytes.size + 4) {
        var start = bytes.size - maxBytes
        while (start < bytes.size && isUtf8Continuation(bytes[start])) {
            start++
        }
        return BoundedUtf8Result(bytes.copyOfRange(start, bytes.size), truncated = true)
    }

    val tailBudget = maxBytes - noticeBytes.size
    var start = bytes.size - tailBudget
    while (start < bytes.size && isUtf8Continuation(bytes[start])) {
        start++
    }
    // Prefer starting on a line boundary; stop before the last byte so tailLength > 0.
    val scanLimit = minOf(bytes.size - 1, start + 256)
    for (i in start until scanLimit) {
        if (bytes[i] == '\n'.code.toByte()) {
            start = i + 1
            break
        }
    }
    val tailLength = bytes.size - start
    val out = ByteArray(noticeBytes.size + tailLength)
    System.arraycopy(noticeBytes, 0, out, 0, noticeBytes.size)
    System.arraycopy(bytes, start, out, noticeBytes.size, tailLength)
    return BoundedUtf8Result(out, truncated = true)
}

private val TRUNCATION_NOTICE_BYTES = "... [truncated]\n".toByteArray(Charsets.UTF_8)

private fun isUtf8Continuation(byte: Byte): Boolean = (byte.toInt() and 0xC0) == 0x80
