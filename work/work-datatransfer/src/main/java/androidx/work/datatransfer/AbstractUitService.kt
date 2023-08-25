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

package androidx.work.datatransfer

import android.app.Notification
import android.app.Service
import android.content.Intent

/**
 * App developers should migrate their existing foreground service implementation to this new
 * base class instead of [Service].
 *
 * App developers are not supposed to call [Service.stopForeground] (or even [Service.stopService])
 * on their own, otherwise this library will crash unexpectedly.
 */
abstract class AbstractUitService : Service() {

    /**
     * This is an equivalent of the [Service.onStartCommand], however, developers should override
     * this method instead of the [Service.onStartCommand]. Its return value will be honored if
     * there are no pending/active [UserInitiatedTask]s, otherwise the return value will be ignored.
     */
    abstract fun handleOnStartCommand(intent: Intent?, flags: Int, startId: Int): Int

    /**
     * This is an equivalent of [Service.onDestroy]. Developers should override this method instead.
     */
    abstract fun handleOnDestroyCommand()

    /**
     * Optional method that can be overridden by apps. Apps can implement their own policy for
     * how multiplexing for task notifications will behave.
     *
     * **The default notification policy is FIFO.**
     */
    open fun handleTaskNotification(id: Int, notification: Notification) {
        TODO()
    }

    final override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    final override fun onDestroy() {
        // TODO: notify UserInitiatedTaskManager to stop all running tasks
        super.onDestroy()
    }
}
