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

package androidx.core.os

import android.os.Process
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * A lightweight, point-in-time snapshot of memory metrics for processes in an application.
 *
 * [MemorySnapshot] provides low-overhead memory statistics designed for routine telemetry,
 * performance monitoring, and regression tracking. Unlike heavyweight diagnostic tools such as
 * [android.os.Debug.getMemoryInfo] or [android.app.ActivityManager.getProcessMemoryInfo], capturing
 * a [MemorySnapshot] is optimized for minimal overhead, making it practical for periodic sampling
 * and in-app health diagnostics without incurring expensive system traversals or platform
 * rate-limiting.
 *
 * An instance is created by calling [MemorySnapshot.capture]. All memory quantities are reported in
 * bytes. If an individual metric is not available on the current device, its property returns
 * `-1L`.
 *
 * ### Thread Safety
 * [MemorySnapshot] instances are immutable and thread-safe. [MemorySnapshot.capture] is thread-safe
 * and can be called concurrently from any thread.
 *
 * @sample androidx.core.os.captureMemorySnapshotSample
 * @sample androidx.core.os.captureProcessMemorySnapshotSample
 * @sample androidx.core.os.trackMemoryUsageDeltaSample
 * @see MemorySnapshot.capture
 */
public class MemorySnapshot private constructor(private val procStatus: ProcStatus?) {
    public companion object {
        private const val INVALID_C_GROUP_FD: Int = -1

        /**
         * Captures a point-in-time [MemorySnapshot] for the current process.
         *
         * This method parses `/proc/self/status` and reads process-specific and package-wide cgroup
         * memory counters when accessible. The operation is designed to be low-latency,
         * memory-efficient and threadsafe (can be called concurrently from any thread).
         *
         * @return an immutable [MemorySnapshot] instance reflecting current memory state, or `null`
         *   if the kernel memory stats cannot be read or both `VmRSS` and `VmSize` are unavailable.
         *   A `null` return value here indicates a persistent error and subsequent calling of this
         *   method is not recommended.
         */
        @JvmStatic
        public fun capture(): MemorySnapshot? {
            return capture(0)
        }

        /**
         * Captures a point-in-time [MemorySnapshot] for the specified process id. The target
         * process must belong to the calling application.
         *
         * Note: When the target process is not the calling process, [vssBytes] may not be
         * available, and its value in the returned [MemorySnapshot] may be `-1L`.
         *
         * @sample androidx.core.os.captureProcessMemorySnapshotSample
         * @param pid the process ID to capture, or `0` for the calling process.
         * @return an immutable [MemorySnapshot] instance reflecting current memory state, or `null`
         *   if capture fails (for example, if the target process has already terminated or kernel
         *   memory statistics cannot be read).
         * @throws IllegalArgumentException if [pid] is negative or does not belong to the calling
         *   application.
         */
        @JvmStatic
        public fun capture(pid: Int): MemorySnapshot? {
            val status = ProcStatus.read(pid)
            return if (status != null) {
                MemorySnapshot(status)
            } else {
                null
            }
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @VisibleForTesting
        public fun parseKbToBytes(line: String): Long {
            val colonIdx = line.indexOf(':')
            if (colonIdx != -1) {
                val kb = parseUnsignedLong(line, colonIdx + 1, Long.MAX_VALUE shr 10)
                if (kb >= 0) {
                    return kb * 1024L
                }
            }
            return -1L
        }

        /**
         * In-place parser for unsigned long integers from a [CharSequence].
         *
         * Features:
         * - Zero heap allocations (no intermediate strings or boxed objects).
         * - No exceptions thrown or caught (returns `-1` on error or overflow).
         * - Skips leading whitespace and handles trailing whitespace delimiters.
         * - Protects against overflow beyond specified max value.
         *
         * @param s the character sequence to parse
         * @param start the index to start parsing from
         * @param max the max value
         * @return the parsed positive long value, or `-1` on failure or overflow
         */
        private fun parseUnsignedLong(s: CharSequence, start: Int, max: Long): Long {
            if (start < 0 || max < 0) {
                return -1L
            }

            val len = s.length
            var i = start

            // Skip leading arbitrary whitespace
            while (i < len && Character.isWhitespace(s[i])) {
                i++
            }
            if (i >= len) {
                return -1L
            }

            // Must have at least one digit
            var c = s[i]
            if (c < '0' || c > '9') {
                return -1L
            }

            var value = 0L
            val maxDiv10 = max / 10
            val maxMod10 = (max % 10).toInt()

            while (i < len) {
                c = s[i]
                if (c in '0'..'9') {
                    val d = c - '0'
                    if (value > maxDiv10 || (value == maxDiv10 && d > maxMod10)) {
                        return -1L // Overflow protection
                    }
                    value = value * 10 + d
                } else {
                    break
                }
                i++
            }

            // If there are trailing characters, verify the immediate next character is whitespace
            if (i < len && !Character.isWhitespace(s[i])) {
                return -1L
            }
            return value
        }
    }

    /**
     * Total Resident Set Size (RSS) in bytes, or `-1L` if unavailable.
     *
     * Represents the total amount of physical RAM currently occupied by the target process,
     * including anonymous allocations, memory-mapped files, and shared memory mappings. This is
     * equal to the sum of [anonRssBytes], [fileRssBytes] and [shmemRssBytes].
     *
     * Corresponds to `VmRSS` in `/proc/self/status`.
     */
    public val rssBytes: Long
        get() = procStatus?.rssBytes ?: -1L

    /**
     * Anonymous Resident Set Size in bytes, or `-1L` if unavailable.
     *
     * Represents the portion of physical RAM allocated to memory not backed by a file, such as the
     * Java heap, native heap (`malloc`/`mmap`), and thread stacks. This is typically the most
     * critical metric for tracking heap allocation growth and diagnosing memory leaks.
     *
     * Corresponds to `RssAnon` in `/proc/self/status`.
     *
     * NOTE: on Android devices with swap/zRAM enabled, anonymous memory under pressure may be paged
     * to swap, evaluating total anonymous memory footprint requires combining [anonRssBytes] with
     * [swapBytes].
     */
    public val anonRssBytes: Long
        get() = procStatus?.anonRssBytes ?: -1L

    /**
     * File-backed Resident Set Size in bytes, or `-1L` if unavailable.
     *
     * Represents the memory mapped to files on disk, such as DEX bytecode, native shared libraries
     * (`.so`), assets, and resource caches. Clean file-backed pages can be reclaimed by the system
     * under memory pressure without requiring swap space. Corresponds to `RssFile` in
     * `/proc/self/status`.
     */
    public val fileRssBytes: Long
        get() = procStatus?.fileRssBytes ?: -1L

    /**
     * Shared memory Resident Set Size in bytes, or `-1L` if unavailable.
     *
     * Represents the memory occupied by shared memory mappings. This includes memfd/ashmem,
     * inter-process shared memory (rather than regular shared file mappings, which are accounted
     * under file RSS). Corresponds to `RssShmem` in `/proc/self/status`.
     */
    public val shmemRssBytes: Long
        get() = procStatus?.shmemRssBytes ?: -1L

    /**
     * Total swap memory used by the process in bytes, or `-1L` if unavailable.
     *
     * Represents the amount of anonymous memory paged out into swap or compressed RAM (such as
     * zRAM). Corresponds to `VmSwap` in `/proc/self/status`.
     */
    public val swapBytes: Long
        get() = procStatus?.swapBytes ?: -1L

    /**
     * Virtual Set Size (VSS) in bytes, or `-1L` if unavailable.
     *
     * Represents the total virtual address space mapped by the process. This includes memory
     * residing in physical RAM, memory swapped out, as well as memory that is reserved but with no
     * physical pages allocated. Corresponds to `VmSize` in `/proc/self/status`.
     *
     * NOTE: This does *NOT* reflect the physical memory consumed, and is *NOT* indicative of memory
     * leaks or OOM risks.
     */
    public val vssBytes: Long
        get() = procStatus?.vssBytes ?: -1L

    /**
     * Resident Set Size High-Water Mark (RSS HWM) in bytes, or `-1L` if unavailable.
     *
     * Represents the peak physical RAM usage (maximum RSS) reached by the process during its
     * lifetime up to the snapshot. Useful for tracking peak memory consumption across workflows.
     * Corresponds to `VmHWM` in `/proc/self/status`.
     *
     * NOTE: This is cumulative over the lifetime of the process and does *NOT* reset between
     * snapshots.
     */
    public val rssHwmBytes: Long
        get() = procStatus?.rssHwmBytes ?: -1L

    /**
     * Process-specific cgroup memory usage in bytes, or `-1L` if unavailable.
     *
     * Represents memory usage attributed directly to the process by the memory controller
     * (`memory.current` in the process cgroup).
     *
     * NOTE: [processMemoryUsageBytes] and [packageMemoryUsageBytes] requires Android 17 or higher,
     * and will return `-1L` otherwise (on devices where system memory accounting is unavailable).
     */
    public val processMemoryUsageBytes: Long
        get() = procStatus?.processMemoryUsageBytes ?: -1L

    /**
     * Package-wide (UID cgroup) memory usage in bytes, or `-1L` if unavailable.
     *
     * Represents the aggregate memory usage across all processes running under the application's
     * UID, as accounted by memory controller (`memory.current` in the UID cgroup).
     *
     * NOTE: This gives multi-process apps their combined memory usage without inter-process
     * queries.
     */
    public val packageMemoryUsageBytes: Long
        get() = procStatus?.packageMemoryUsageBytes ?: -1L

    private class ProcStatus {
        var rssBytes: Long = -1L
        var anonRssBytes: Long = -1L
        var fileRssBytes: Long = -1L
        var shmemRssBytes: Long = -1L
        var swapBytes: Long = -1L
        var vssBytes: Long = -1L
        var rssHwmBytes: Long = -1L
        var processMemoryUsageBytes: Long = -1L
        var packageMemoryUsageBytes: Long = -1L

        companion object {
            private val myPid: Int by lazy { Process.myPid() }
            private var procRoot: String = "/proc"

            fun isSelf(pid: Int): Boolean {
                return (pid == 0 || pid == myPid)
            }

            fun isValidPid(pid: Int): Boolean {
                require(pid >= 0) { "pid ($pid) must be non-negative" }
                if (!isSelf(pid)) {
                    // Os.kill(pid, 0) succeeds if pid exists and belongs to the same UID.
                    try {
                        Os.kill(pid, 0)
                    } catch (e: ErrnoException) {
                        if (e.errno == OsConstants.EPERM) {
                            throw IllegalArgumentException(
                                "Process $pid does not belong to the calling application",
                                e,
                            )
                        }
                        return false
                    }
                }
                return true
            }

            fun read(pid: Int): ProcStatus? {
                if (!isValidPid(pid)) {
                    return null
                }

                val targetPid = if (pid == 0) myPid else pid
                val isSelf = isSelf(pid)
                val procStatusPath =
                    if (isSelf) "$procRoot/self/status" else "$procRoot/$targetPid/status"
                val status = ProcStatus()
                val procRead = readProcStatus(procStatusPath, status)

                if (CgroupFdHolder.FD != INVALID_C_GROUP_FD) {
                    status.processMemoryUsageBytes =
                        readLongFromFile(CgroupFdHolder.getPath(targetPid, "memory.current"))
                    status.packageMemoryUsageBytes =
                        readLongFromFile(CgroupFdHolder.getPath("memory.current"))

                    if (!procRead && !isSelf) {
                        // Secondary process fallback: read memory metrics directly from UID-scoped
                        // cgroup v2 when /proc/<pid>/status is inaccessible
                        parseCgroupMemoryStat(
                            CgroupFdHolder.getPath(targetPid, "memory.stat"),
                            status,
                        )
                        status.swapBytes =
                            readLongFromFile(
                                CgroupFdHolder.getPath(targetPid, "memory.swap.current")
                            )
                        val peak =
                            readLongFromFile(CgroupFdHolder.getPath(targetPid, "memory.peak"))
                        val rssSum =
                            (if (status.anonRssBytes > 0) status.anonRssBytes else 0L) +
                                (if (status.fileRssBytes > 0) status.fileRssBytes else 0L) +
                                (if (status.shmemRssBytes > 0) status.shmemRssBytes else 0L)
                        status.rssBytes =
                            if (status.processMemoryUsageBytes > 0) {
                                maxOf(status.processMemoryUsageBytes, rssSum)
                            } else if (rssSum > 0) {
                                rssSum
                            } else {
                                -1L
                            }
                        status.rssHwmBytes =
                            if (peak >= 0) {
                                maxOf(peak, status.rssBytes)
                            } else {
                                status.rssBytes
                            }
                    }
                }

                if (status.rssBytes == -1L || (isSelf && status.vssBytes == -1L)) {
                    return null
                }
                return status
            }

            private fun readProcStatus(path: String, status: ProcStatus): Boolean {
                try {
                    BufferedReader(FileReader(path), 1024).use { reader ->
                        var line: String?
                        var nlines = 0
                        while (reader.readLine().also { line = it } != null && nlines < 7) {
                            val l = line!!
                            if (l.startsWith("VmRSS:")) {
                                status.rssBytes = parseKbToBytes(l)
                                nlines++
                            } else if (l.startsWith("RssAnon:")) {
                                status.anonRssBytes = parseKbToBytes(l)
                                nlines++
                            } else if (l.startsWith("RssFile:")) {
                                status.fileRssBytes = parseKbToBytes(l)
                                nlines++
                            } else if (l.startsWith("RssShmem:")) {
                                status.shmemRssBytes = parseKbToBytes(l)
                                nlines++
                            } else if (l.startsWith("VmSwap:")) {
                                status.swapBytes = parseKbToBytes(l)
                                nlines++
                            } else if (l.startsWith("VmSize:")) {
                                status.vssBytes = parseKbToBytes(l)
                                nlines++
                            } else if (l.startsWith("VmHWM:")) {
                                status.rssHwmBytes = parseKbToBytes(l)
                                nlines++
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore, procfs read failed
                }
                return status.rssBytes != -1L && status.vssBytes != -1L
            }

            private fun parseCgroupMemoryStat(path: String, status: ProcStatus) {
                try {
                    BufferedReader(FileReader(path), 512).use { reader ->
                        var line: String?
                        var nlines = 0
                        while (reader.readLine().also { line = it } != null && nlines < 3) {
                            val l = line!!
                            if (
                                l.startsWith("anon") &&
                                    (l.length > 4 && Character.isWhitespace(l[4]))
                            ) {
                                status.anonRssBytes = parseUnsignedLong(l, 5, Long.MAX_VALUE)
                                nlines++
                            } else if (
                                l.startsWith("file") &&
                                    (l.length > 4 && Character.isWhitespace(l[4]))
                            ) {
                                status.fileRssBytes = parseUnsignedLong(l, 5, Long.MAX_VALUE)
                                nlines++
                            } else if (
                                l.startsWith("shmem") &&
                                    (l.length > 5 && Character.isWhitespace(l[5]))
                            ) {
                                status.shmemRssBytes = parseUnsignedLong(l, 6, Long.MAX_VALUE)
                                nlines++
                            }
                        }
                    }
                    // In cgroup v2 memory.stat, `file` includes `shmem` pages. Subtract `shmem`
                    // so `fileRssBytes` reflects pure file-backed pages matching procfs.
                    if (status.fileRssBytes >= 0 && status.shmemRssBytes > 0) {
                        status.fileRssBytes = maxOf(0L, status.fileRssBytes - status.shmemRssBytes)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }

            private fun readLongFromFile(path: String): Long {
                try {
                    BufferedReader(FileReader(path), 128).use { reader ->
                        val line = reader.readLine() ?: return -1L
                        if (line.trim() == "max") {
                            return Long.MAX_VALUE
                        }
                        return parseUnsignedLong(line, 0, Long.MAX_VALUE)
                    }
                } catch (e: Exception) {
                    // Ignore and return -1
                }
                return -1L
            }
        }
    }

    private object CgroupFdHolder {
        @Volatile private var cachedFd: Int = INVALID_C_GROUP_FD

        val FD: Int
            get() =
                if (cachedFd == INVALID_C_GROUP_FD) {
                    findCgroupFd(File("/proc/self/fd")).also { cachedFd = it }
                } else {
                    cachedFd
                }

        fun getPath(attribute: String): String {
            return "/proc/self/fd/$FD/$attribute"
        }

        fun getPath(pid: Int, attribute: String): String {
            return "/proc/self/fd/$FD/pid_$pid/$attribute"
        }

        fun findCgroupFd(fdDir: File): Int {
            val fds = fdDir.listFiles() ?: return INVALID_C_GROUP_FD

            for (fdFile in fds) {
                try {
                    val target = Os.readlink(fdFile.absolutePath)
                    if (
                        target.startsWith("/sys/fs/cgroup/apps/uid_") ||
                            target.startsWith("/sys/fs/cgroup/system/uid_") ||
                            target.startsWith("/sys/fs/cgroup/uid_")
                    ) {
                        return fdFile.name.toInt()
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
            return INVALID_C_GROUP_FD
        }
    }
}
