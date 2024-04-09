/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.hardware

import android.util.Log
import androidx.graphics.utils.HandlerThreadExecutor
import java.io.File
import java.lang.NumberFormatException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Class to monitor open file descriptors and clean up those with fences that have already signalled
 */
internal class FileDescriptorMonitor(
    executor: HandlerThreadExecutor? = null,
    private val scheduleMillis: Long = MONITOR_DELAY,
    manageExecutor: Boolean = executor != null
) {

    private val mIsMonitoring = AtomicBoolean(false)
    private val mProcFd = File("/proc/self/fd/")
    private val mCleanupCompleteCallbacks = ArrayList<() -> Unit>()

    private val mIsManagingHandlerThread = AtomicBoolean(false)
    private var mExecutor: HandlerThreadExecutor

    private data class FdSignalPair(val fd: Int, val signalTime: Long)
    private val pendingFileDescriptors = ArrayList<FdSignalPair>()

    init {
        mExecutor = executor ?: HandlerThreadExecutor("fdcleanup")
        mIsManagingHandlerThread.set(manageExecutor)
    }

    private fun closePendingFileDescriptors() {
        pendingFileDescriptors.sortByDescending { fdSignalTimePair -> fdSignalTimePair.signalTime }
        while (pendingFileDescriptors.size > MAX_FD) {
            val fdSignalPair = pendingFileDescriptors.removeLast()
            try {
                val fd = fdSignalPair.fd
                // Re-query the signal time in case the fd was re-used
                val signalTime = SyncFenceBindings.nGetSignalTime(fd)
                val diff = signalTime.signalTimeDiffMillis()
                if (diff > SIGNAL_TIME_DELTA_MILLIS) {
                    SyncFenceBindings.nForceClose(fd)
                }
            } catch (_: Throwable) {
                // Just in case the owner actually does close the fd
            }
        }
    }

    private fun Long.signalTimeDiffMillis(): Long {
        val now = System.nanoTime()
        val signalled = this != SyncFenceCompat.SIGNAL_TIME_INVALID &&
            this != SyncFenceCompat.SIGNAL_TIME_PENDING
        return if (signalled && now > this) {
            TimeUnit.NANOSECONDS.toMillis(now - this)
        } else {
            -1
        }
    }

    private val mCleanupRunnable = Runnable {
        mProcFd.listFiles()?.let { files ->
            for (file in files) {
                try {
                    val fd = Integer.parseInt(file.name)
                    val signalTime = SyncFenceBindings.nGetSignalTime(fd)
                    val diff = signalTime.signalTimeDiffMillis()
                    if (diff > SIGNAL_TIME_DELTA_MILLIS) {
                        // Store the signal time as it can potentially change in the middle of
                        // executing the sorting algorithm and can throw exceptions
                        pendingFileDescriptors.add(FdSignalPair(fd, signalTime))
                        if (pendingFileDescriptors.size > MAX_FD) {
                            closePendingFileDescriptors()
                        }
                    }
                } catch (formatException: NumberFormatException) {
                    Log.w(TAG, "Unable to parse fd value from name ${file.name}")
                }
            }
        }

        if (mIsMonitoring.get()) {
            scheduleCleanupTask()
        } else {
            teardownExecutorIfNecessary()
        }
        invokeCleanupCallbacks()
    }

    private fun invokeCleanupCallbacks() {
        synchronized(mCleanupCompleteCallbacks) {
            for (callback in mCleanupCompleteCallbacks) {
                callback.invoke()
            }
            mCleanupCompleteCallbacks.clear()
        }
    }

    private fun scheduleCleanupTask() {
        if (mExecutor.isRunning) {
            mExecutor.postDelayed(mCleanupRunnable, scheduleMillis)
        }
    }

    fun addCleanupCallback(callback: () -> Unit) {
        synchronized(mCleanupCompleteCallbacks) {
            mCleanupCompleteCallbacks.add(callback)
        }
    }

    /**
     * Starts periodic cleaning of file descriptors if it has not already been started previously
     */
    fun startMonitoring() {
        if (!mIsMonitoring.get()) {
            initExecutorIfNecessary()
            scheduleCleanupTask()
            mIsMonitoring.set(true)
        }
    }

    private fun initExecutorIfNecessary() {
        if (mIsManagingHandlerThread.get() && !mExecutor.isRunning) {
            mExecutor = HandlerThreadExecutor("fdcleanup")
        }
    }

    private fun teardownExecutorIfNecessary() {
        if (mIsManagingHandlerThread.get() && mExecutor.isRunning) {
            mExecutor.quit()
        }
    }

    /**
     * Stop scheduling of the periodic clean up of file descriptors
     * @param cancelPending Cancels any pending request to clean up contents. If false, the last
     * pending request to clean up content will still be scheduled but no more will be afterwards.
     */
    fun stopMonitoring(cancelPending: Boolean = false) {
        if (mIsMonitoring.get()) {
            if (cancelPending) {
                mExecutor.removeCallbacks(mCleanupRunnable)
                teardownExecutorIfNecessary()
            }
            mIsMonitoring.set(false)
        }
    }

    /**
     * Returns true if [startMonitoring] has been invoked without a corresponding call
     * to [stopMonitoring]
     */
    val isMonitoring: Boolean
        get() = mIsMonitoring.get()

    companion object {
        const val TAG = "FileDescriptorMonitor"

        /**
         * Delta in which if a fence has signalled it should be removed
         */
        const val SIGNAL_TIME_DELTA_MILLIS = 3000

        const val MONITOR_DELAY = 1000L

        const val MAX_FD = 100
    }
}
