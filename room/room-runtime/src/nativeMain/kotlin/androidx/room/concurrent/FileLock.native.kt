/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.concurrent

import androidx.room.util.stringError
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import platform.posix.F_SETLK
import platform.posix.F_SETLKW
import platform.posix.F_UNLCK
import platform.posix.F_WRLCK
import platform.posix.O_CREAT
import platform.posix.O_RDWR
import platform.posix.SEEK_SET
import platform.posix.S_IRGRP
import platform.posix.S_IROTH
import platform.posix.S_IRUSR
import platform.posix.S_IWUSR
import platform.posix.close
import platform.posix.fcntl
import platform.posix.flock
import platform.posix.open

/**
 * A mutually exclusive advisory file lock implementation.
 *
 * The lock is cooperative and only protects the critical region from other [FileLock] users in
 * other process with the same `filename`. Do not use this class on its own, instead use
 * [ExclusiveLock] which guarantees in-process locking too.
 *
 * @param filename The path to the file to protect. Note that an actual lock is not grab on the file
 *   itself but on a temporary file create with the same path but ending with `.lck`.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal actual class FileLock actual constructor(filename: String) {
    private val lockFilename = "$filename.lck"
    private var lockFd: Int? = null

    actual fun lock() {
        if (lockFd != null) {
            return
        }
        // Open flags: open in read-write mode and create if doesn't exist
        // Open mode: user has read / write permissions, group and others only read (0644)
        val fd = open(lockFilename, O_RDWR or O_CREAT, S_IWUSR or S_IRUSR or S_IRGRP or S_IROTH)
        check(fd != -1) { "Unable to open lock file (${stringError()}): '$lockFilename'." }
        try {
            val cFlock =
                cValue<flock> {
                    l_type = F_WRLCK.toShort() // acquire write (exclusive) lock
                    l_whence = SEEK_SET.toShort() // lock from start of file
                    l_start = 0 // lock start offset
                    l_len = 0 // lock all bytes (special meaning)
                }
            // Command: 'Set lock waiting' will block until file lock is acquired by process
            if (memScoped { fcntl(fd, F_SETLKW, cFlock.ptr) } == -1) {
                error("Unable to lock file (${stringError()}): '$lockFilename'.")
            }
            lockFd = fd
        } catch (ex: Throwable) {
            close(fd)
            throw ex
        }
    }

    actual fun unlock() {
        val fd = lockFd ?: return
        try {
            val cFlock =
                cValue<flock> {
                    l_type = F_UNLCK.toShort() // release lock
                    l_whence = SEEK_SET.toShort() // lock from start of file
                    l_start = 0 // lock start offset
                    l_len = 0 // lock all bytes (special meaning)
                }
            // Command: 'Set lock' (without waiting because we are unlocking)
            if (memScoped { fcntl(fd, F_SETLK, cFlock.ptr) } == -1) {
                error("Unable to unlock file (${stringError()}): '$lockFilename'.")
            }
        } finally {
            close(fd)
        }
        lockFd = null
    }
}
