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

package androidx.camera.camera2.internal

import androidx.camera.camera2.internal.annotation.CameraExecutor
import androidx.camera.core.Logger
import androidx.camera.core.impl.annotation.ExecutedBy
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

private const val LOG_TAG = "VideoUsageControl"

/** Tracks the video usage count of whether a camera is being used for a video output or not. */
internal class VideoUsageControl(@CameraExecutor private val executor: Executor) {
    /**
     * An AtomicInteger for tracking the video usage count.
     *
     * To avoid race condition between main thread and camera thread, this should always be updated
     * within the camera thread [executor] only. See b/345676557 for details.
     */
    private val mVideoUsage = AtomicInteger(0)

    /**
     * Increments usage count by 1.
     *
     * This method posts the task to the camera thread [executor].
     */
    fun incrementUsage() {
        executor.execute {
            val currentVal = mVideoUsage.incrementAndGet()
            Logger.d(LOG_TAG, "incrementUsage: mVideoUsage = $currentVal")
        }
    }

    /**
     * Decrements usage count by 1.
     *
     * This method posts the task to the camera thread [executor].
     */
    fun decrementUsage() {
        executor.execute {
            val currentVal = mVideoUsage.decrementAndGet()
            if (currentVal < 0) {
                Logger.w(
                    LOG_TAG,
                    "decrementUsage: mVideoUsage = $currentVal, which is less than 0!"
                )
            } else {
                Logger.d(LOG_TAG, "decrementUsage: mVideoUsage = $currentVal")
            }
        }
    }

    /**
     * Resets the usage count to 0.
     *
     * This method posts the task to the camera thread [executor].
     */
    fun reset() {
        executor.execute { resetDirectly() }
    }

    /**
     * Resets the usage count to 0 directly.
     *
     * This method should always be invoked from a code running on the camera thread [executor].
     */
    @ExecutedBy("executor")
    fun resetDirectly() {
        mVideoUsage.set(0)
        Logger.d(LOG_TAG, "resetDirectly: mVideoUsage reset!")
    }

    fun getUsage() = mVideoUsage.get()
}
