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

import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.room.util.stringError
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.posix.exit
import platform.posix.fork
import platform.posix.gettimeofday
import platform.posix.remove
import platform.posix.timeval
import platform.posix.usleep
import platform.posix.waitpid

@OptIn(ExperimentalForeignApi::class)
class FileLockTest {

    private val testFile = "/tmp/test-${Random.nextInt()}.db"
    private val parentLogFile = "/tmp/test-${Random.nextInt()}-parent"
    private val childLogFile = "/tmp/test-${Random.nextInt()}-child"

    @BeforeTest
    fun before() {
        remove(testFile)
        remove(parentLogFile)
        remove(childLogFile)
    }

    @AfterTest
    fun after() {
        remove(testFile)
        remove(parentLogFile)
        remove(childLogFile)
    }

    @Test
    fun processLock() {
        val pid = fork()
        if (pid == -1) {
            error("fork() failed: ${stringError()}")
        }

        // Forked code is next, both process concurrently attempt to acquire the file lock,
        // recording the time they did so to later validate exclusive access to the critical
        // region.
        val timeStamps = TimeStamps()
        val lock = FileLock(testFile)
        timeStamps.before = getUnixMicroseconds()
        lock.lock()
        // sleep for 200ms total to give a chance for contention
        usleep(100u * 1000u)
        timeStamps.during = getUnixMicroseconds()
        usleep(100u * 1000u)
        lock.unlock()
        timeStamps.after = getUnixMicroseconds()
        writeTimestamps(
            logFile = if (pid == 0) childLogFile else parentLogFile,
            timeStamps = timeStamps,
        )

        when (pid) {
            // Child process, terminate
            0 -> {
                exit(0)
            }
            // Parent process (this test), wait for child
            else -> {
                val result = waitpid(pid, null, 0)
                if (result == -1) {
                    error("wait() failed: ${stringError()}")
                }
            }
        }

        val parentTimeStamps = readTimestamps(parentLogFile)
        val childTimeStamps = readTimestamps(childLogFile)

        // Initial check, attempt was before acquire, and release was after acquired
        assertThat(parentTimeStamps.before).isLessThan(parentTimeStamps.during)
        assertThat(parentTimeStamps.during).isLessThan(parentTimeStamps.after)
        assertThat(childTimeStamps.before).isLessThan(childTimeStamps.during)
        assertThat(childTimeStamps.during).isLessThan(childTimeStamps.after)

        // Find out who got the lock first
        val (first, second) =
            if (parentTimeStamps.during < childTimeStamps.during) {
                parentTimeStamps to childTimeStamps
            } else {
                childTimeStamps to parentTimeStamps
            }
        // Now really validate second acquired the lock *after* first released it
        assertWithMessage("Comparing first unlock time with second acquire time")
            .that(first.after)
            .isLessThan(second.during)
    }

    private fun writeTimestamps(logFile: String, timeStamps: TimeStamps) {
        FileSystem.SYSTEM.write(logFile.toPath(), mustCreate = true) {
            writeUtf8("${timeStamps.before},${timeStamps.during},${timeStamps.after}")
        }
    }

    private fun readTimestamps(logFile: String): TimeStamps {
        return FileSystem.SYSTEM.read(logFile.toPath()) {
            val stamps = readUtf8().split(",")
            TimeStamps(
                before = stamps[0].toLong(),
                during = stamps[1].toLong(),
                after = stamps[2].toLong()
            )
        }
    }

    // All times are in microseconds
    data class TimeStamps(var before: Long = -1, var during: Long = -1, var after: Long = -1)

    private fun getUnixMicroseconds(): Long = memScoped {
        val tv = alloc<timeval>()
        gettimeofday(tv.ptr, null)
        return (tv.tv_sec * 1000000) + tv.tv_usec
    }
}
