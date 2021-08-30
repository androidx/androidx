/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark

import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.benchmark.BenchmarkState.Companion.TAG
import java.io.File
import java.io.IOException

internal object ThreadPriority {
    /**
     * Max priority for a linux process, see docs of android.os.Process
     *
     * For some reason, constant not provided as platform API.
     */
    const val HIGH_PRIORITY = -20

    private const val BENCH_THREAD_PRIORITY = HIGH_PRIORITY
    /**
     * Set JIT lower than bench thread, to reduce chance of it preempting during measurement
     */
    private const val JIT_THREAD_PRIORITY =
        HIGH_PRIORITY + Process.THREAD_PRIORITY_LESS_FAVORABLE * 5

    private const val TASK_PATH = "/proc/self/task"
    private const val JIT_THREAD_NAME = "Jit thread pool"
    private val JIT_TID: Int?
    val JIT_INITIAL_PRIORITY: Int

    init {
        if (Build.VERSION.SDK_INT >= 24) {
            // JIT thread expected to exist on N+ devices
            val tidsToNames = File(TASK_PATH).listFiles()?.associateBy(
                {
                    // tid
                    it.name.toInt()
                },
                {
                    // thread name
                    try {
                        File(it, "comm").readLines().firstOrNull() ?: ""
                    } catch (e: IOException) {
                        // if we fail to read thread name, file may not exist because thread
                        // died. Expect no error reading Jit thread name, so just name thread
                        // incorrectly.
                        "ERROR READING THREAD NAME"
                    }
                }
            )
            if (tidsToNames.isNullOrEmpty()) {
                Log.d(TAG, "NOTE: Couldn't find threads in this process for priority pinning.")
                JIT_TID = null
            } else {
                JIT_TID =
                    tidsToNames.filter { it.value.startsWith(JIT_THREAD_NAME) }.keys.firstOrNull()
                if (JIT_TID == null) {
                    Log.d(TAG, "NOTE: Couldn't JIT thread, threads found:")
                    tidsToNames.forEach {
                        Log.d(TAG, "    tid: ${it.key}, name:'${it.value}'")
                    }
                }
            }
        } else {
            JIT_TID = null
        }

        JIT_INITIAL_PRIORITY = if (JIT_TID != null) Process.getThreadPriority(JIT_TID) else 0
    }

    private val lock = Any()

    @GuardedBy("lock")
    private var initialTid: Int = -1
    @GuardedBy("lock")
    private var initialPriority: Int = Int.MAX_VALUE

    /*
     * [android.os.Process.getThreadPriority] is not very clear in which conditions it will fail,
     * so setting JIT / benchmark thread priorities are best-effort for now
     */
    private fun setThreadPriority(label: String, tid: Int, priority: Int): Boolean {
        val previousPriority = Process.getThreadPriority(tid)
        try {
            Process.setThreadPriority(tid, priority)
        } catch (e: SecurityException) {
            return false
        }

        val newPriority = Process.getThreadPriority(tid)
        if (newPriority != previousPriority) {
            Log.d(
                TAG,
                "Set $tid ($label) to priority $priority. Was $previousPriority, now $newPriority"
            )
            return true
        }
        return false
    }

    /**
     * Bump thread priority of the current thread and JIT to be high, resetting any other bumped
     * thread.
     *
     * Only one benchmark thread can be be bumped at a time.
     */
    fun bumpCurrentThreadPriority() = synchronized(lock) {
        val myTid = Process.myTid()
        if (initialTid == myTid) {
            // already bumped
            return
        }

        // ensure we don't have multiple threads bumped at once
        resetBumpedThread()

        initialTid = myTid
        initialPriority = Process.getThreadPriority(initialTid)

        setThreadPriority("Bench thread", initialTid, BENCH_THREAD_PRIORITY)
        if (JIT_TID != null) {
            setThreadPriority("Jit", JIT_TID, JIT_THREAD_PRIORITY)
        }
    }

    fun resetBumpedThread() = synchronized(lock) {
        if (initialTid > 0) {
            setThreadPriority("Bench thread", initialTid, initialPriority)
            if (JIT_TID != null) {
                setThreadPriority("Jit", JIT_TID, JIT_INITIAL_PRIORITY)
            }
            initialTid = -1
        }
    }

    fun getJit(): Int {
        checkNotNull(JIT_TID) { "Jit thread not found!" }
        return Process.getThreadPriority(JIT_TID)
    }

    fun get(): Int {
        return Process.getThreadPriority(Process.myTid())
    }
}