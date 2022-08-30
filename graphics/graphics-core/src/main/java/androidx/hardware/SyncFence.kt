/*
 * Copyright 2022 The Android Open Source Project
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

/*
 * Copyright 2022 The Android Open Source Project
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

import android.os.Build
import androidx.annotation.RequiresApi
import java.util.concurrent.TimeUnit

/**
 * A SyncFence represents a synchronization primitive which signals when hardware buffers have
 * completed work on a particular resource.
 *
 * For example, GPU rendering to a frame buffer may generate a synchronization fence which signals
 * when rendering has completed.
 *
 * When the fence signals, then the backing storage for the framebuffer may be safely read from,
 * such as for display or media encoding.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
class SyncFence(private var fd: Int) : AutoCloseable {

    /**
     * Checks if the SyncFence object is valid.
     * @return `true` if it is valid, `false` otherwise
     */
    fun isValid(): Boolean = fd != -1

    /**
     * Returns the time that the fence signaled in the [CLOCK_MONOTONIC] time domain.
     * This returns [SyncFence.SIGNAL_TIME_INVALID] if the SyncFence is invalid.
     */
    // Relies on NDK APIs sync_file_info/sync_file_info_free which were introduced in API level 26
    @RequiresApi(Build.VERSION_CODES.O)
    fun getSignalTime(): Long =
        if (isValid()) {
            nGetSignalTime(fd)
        } else {
            SIGNAL_TIME_INVALID
        }

    /**
     * Waits for a SyncFence to signal for up to the [timeoutNanos] duration. An invalid SyncFence,
     * that is if [isValid] is `false`, is treated equivalently to a SyncFence that has already
     * signaled. That is, wait() will immediately return `true`.
     *
     * @param timeoutNanos Timeout duration in nanoseconds. Providing a negative value will wait
     * indefinitely until the fence is signaled
     * @return `true` if the fence signaled or is not valid, `false` otherwise
     */
    fun await(timeoutNanos: Long): Boolean {
        if (isValid()) {
            val timeout: Int
            if (timeoutNanos < 0) {
                timeout = -1
            } else {
                timeout = TimeUnit.NANOSECONDS.toMillis(timeoutNanos).toInt()
            }
            return nWait(fd, timeout)
        } else {
            // invalid file descriptors will always return true
            return true
        }
    }

    /**
     * Waits forever for a SyncFence to signal. An invalid SyncFence is treated equivalently to a
     * SyncFence that has already signaled. That is, wait() will immediately return `true`.
     *
     * @return `true` if the fence signaled or isn't valid, `false` otherwise
     */
    fun awaitForever(): Boolean = await(-1)

    /**
     * Close the SyncFence instance. After this method is invoked the fence is invalid. That
     * is subsequent calls to [isValid] will return `false`
     */
    override fun close() {
        nClose(fd)
        fd = -1
    }

    protected fun finalize() {
        close()
    }

    // SyncFence in the framework implements timeoutNanos as a long but
    // it is casted down to an int within native code and eventually calls into
    // the poll API which consumes a timeout in nanoseconds as an int.
    private external fun nWait(fd: Int, timeoutMillis: Int): Boolean
    private external fun nGetSignalTime(fd: Int): Long
    private external fun nClose(fd: Int)

    companion object {

        /**
         * An invalid signal time. Represents either the signal time for a SyncFence that isn't
         * valid (that is, [isValid] is `false`), or if an error occurred while attempting to
         * retrieve the signal time.
         */
        const val SIGNAL_TIME_INVALID: Long = -1L

        /**
         * A pending signal time. This is equivalent to the max value of a long, representing an
         * infinitely far point in the future.
         */
        const val SIGNAL_TIME_PENDING: Long = Long.MAX_VALUE

        init {
            System.loadLibrary("sync-fence")
        }
    }
}