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

/** Joins [prefix] and [suffix] with a single newline when both are non-empty. */
internal fun joinLines(prefix: String, suffix: String): String =
    when {
        prefix.isEmpty() -> suffix
        suffix.isEmpty() -> prefix
        prefix.endsWith('\n') -> prefix + suffix
        else -> "$prefix\n$suffix"
    }

/** Holds UTF-8 tail bytes and whether [capUtf8TailBounded] truncated the input. */
internal class BoundedUtf8Result(val bytes: ByteArray, val truncated: Boolean)

/**
 * Encodes [text] as UTF-8 bounded to [maxBytes] tail bytes, reporting whether truncation occurred.
 *
 * When [alreadyTruncated] is `true` (for example, an in-memory buffer dropped earlier lines before
 * encoding), prepends the truncation notice once at the start of the output while staying within
 * [maxBytes].
 */
internal fun capUtf8TailBounded(
    text: String,
    maxBytes: Int,
    alreadyTruncated: Boolean = false,
): BoundedUtf8Result {
    if (maxBytes <= 0) {
        return BoundedUtf8Result(ByteArray(0), truncated = alreadyTruncated || text.isNotEmpty())
    }
    val bytes = text.toByteArray(Charsets.UTF_8)
    if (!alreadyTruncated && bytes.size <= maxBytes) {
        return BoundedUtf8Result(bytes, truncated = false)
    }

    val noticeBytes = TRUNCATION_NOTICE_BYTES
    if (alreadyTruncated && noticeBytes.size + bytes.size <= maxBytes) {
        val out = ByteArray(noticeBytes.size + bytes.size)
        System.arraycopy(noticeBytes, 0, out, 0, noticeBytes.size)
        System.arraycopy(bytes, 0, out, noticeBytes.size, bytes.size)
        return BoundedUtf8Result(out, truncated = true)
    }

    if (maxBytes <= noticeBytes.size + 4) {
        if (bytes.size <= maxBytes) {
            return BoundedUtf8Result(bytes, truncated = true)
        }
        var start = bytes.size - maxBytes
        while (start < bytes.size && isUtf8Continuation(bytes[start])) {
            start++
        }
        return BoundedUtf8Result(bytes.copyOfRange(start, bytes.size), truncated = true)
    }

    val tailBudget = maxBytes - noticeBytes.size
    var start = (bytes.size - tailBudget).coerceAtLeast(0)
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

internal const val TRUNCATION_NOTICE = "... [truncated]\n"

private val TRUNCATION_NOTICE_BYTES = TRUNCATION_NOTICE.toByteArray(Charsets.UTF_8)

private fun isUtf8Continuation(byte: Byte): Boolean = (byte.toInt() and 0xC0) == 0x80
