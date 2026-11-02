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

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.logging.Formatter
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

/**
 * Buffers formatted JUL records for one test or class lifecycle.
 *
 * Serialized output is bounded to [maxBytes] UTF-8 bytes from the tail.
 */
internal class HostLogCollector(val maxBytes: Int = MAX_HOST_LOG_BYTES) {
    private val lock = Any()
    private val buffer = StringBuilder()
    private var overflowTruncated: Boolean = false

    fun appendRecord(record: LogRecord) {
        appendRaw(formatRecord(record))
    }

    fun appendRaw(text: String) {
        if (text.isEmpty()) return
        synchronized(lock) {
            buffer.append(text)
            val highWaterChars = maxBytes * 2
            if (buffer.length > highWaterChars) {
                overflowTruncated = true
                val dropTarget = buffer.length - maxBytes
                val scanLimit = minOf(buffer.length - 1, dropTarget + 256)
                val newline = buffer.indexOf("\n", dropTarget)
                var cut =
                    if (newline in dropTarget until scanLimit) {
                        newline + 1
                    } else {
                        dropTarget
                    }
                if (cut < buffer.length && buffer[cut].isLowSurrogate()) {
                    cut++
                }
                buffer.delete(0, cut)
                buffer.insert(0, "... [truncated]\n")
            }
        }
    }

    fun snapshotText(): String = synchronized(lock) { buffer.toString() }

    fun isOverflowTruncated(): Boolean = synchronized(lock) { overflowTruncated }

    /**
     * Encodes [prefixText] and buffered text as UTF-8 capped at [maxBytes] bytes.
     *
     * Reports whether truncation occurred in either prefix or buffered text.
     */
    fun toBoundedUtf8(
        prefixText: String = "",
        prefixTruncated: Boolean = false,
    ): BoundedUtf8Result {
        val (ownText, ownTruncated) = synchronized(lock) { buffer.toString() to overflowTruncated }
        val combined =
            when {
                prefixText.isEmpty() -> ownText
                ownText.isEmpty() -> prefixText
                prefixText.endsWith('\n') -> prefixText + ownText
                else -> "$prefixText\n$ownText"
            }
        val bounded = capUtf8TailBounded(combined, maxBytes)
        return BoundedUtf8Result(
            bytes = bounded.bytes,
            truncated = prefixTruncated || ownTruncated || bounded.truncated,
        )
    }

    companion object {
        const val MAX_HOST_LOG_BYTES = 256 * 1024

        private val TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneOffset.UTC)

        private val MESSAGE_FORMATTER =
            object : Formatter() {
                override fun format(record: LogRecord): String = formatMessage(record)
            }

        internal fun formatRecord(record: LogRecord): String {
            val timestamp = TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(record.millis))
            val loggerSimple =
                record.loggerName?.substringAfterLast('.')?.ifEmpty { record.loggerName } ?: "host"
            val message = MESSAGE_FORMATTER.formatMessage(record).orEmpty()
            return buildString {
                append('[')
                append(timestamp)
                append("] ")
                append(record.level.name)
                append(' ')
                append(loggerSimple)
                append(": ")
                append(message)
                append('\n')
                val thrown = record.thrown
                if (thrown != null) {
                    append(thrown.stackTraceToString())
                    if (!endsWith('\n')) append('\n')
                }
            }
        }
    }
}

/**
 * Routes `androidx.test.backup` JUL records to the active [HostLogCollector].
 *
 * Attaches the shared handler while at least one collector is registered and restores the logger's
 * previous level once the last collector closes.
 */
internal object HostLogManager {
    private val lock = Any()

    // Strong reference prevents LogManager from garbage-collecting the configured logger mid-run.
    private val trackedLogger: Logger = Logger.getLogger("androidx.test.backup")

    private var savedLevel: Level? = null
    private var levelSaved: Boolean = false
    private val registeredCollectors = HashSet<HostLogCollector>()

    private val sharedHandler =
        object : Handler() {
            init {
                level = Level.ALL
            }

            override fun publish(record: LogRecord?) {
                if (record == null) return
                synchronized(lock) { registeredCollectors.toList() }
                    .forEach { it.appendRecord(record) }
            }

            override fun flush() {}

            override fun close() {}
        }

    /** Registers [collector] and attaches the JUL handler if none was active. */
    fun register(collector: HostLogCollector) {
        synchronized(lock) {
            val wasEmpty = registeredCollectors.isEmpty()
            registeredCollectors.add(collector)
            if (wasEmpty) {
                savedLevel = trackedLogger.level
                levelSaved = true
                if (!trackedLogger.isLoggable(Level.FINE)) {
                    trackedLogger.level = Level.FINE
                }
                if (sharedHandler !in trackedLogger.handlers) {
                    trackedLogger.addHandler(sharedHandler)
                }
            }
        }
    }

    /** Unregisters [collector] and detaches the JUL handler once the last collector closes. */
    fun unregister(collector: HostLogCollector) {
        synchronized(lock) {
            registeredCollectors.remove(collector)
            if (registeredCollectors.isEmpty()) {
                trackedLogger.removeHandler(sharedHandler)
                if (levelSaved) {
                    trackedLogger.level = savedLevel
                    savedLevel = null
                    levelSaved = false
                }
            }
        }
    }

    internal fun activeCollectorCountForTesting(): Int =
        synchronized(lock) { registeredCollectors.size }

    internal fun hasAttachedHandlersForTesting(): Boolean =
        synchronized(lock) { sharedHandler in trackedLogger.handlers }
}
