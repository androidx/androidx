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
import android.content.pm.ServiceInfo
import androidx.work.ForegroundInfo

/**
 * A container class holding information related to the notifications for the
 * [UserInitiatedTaskRequest].
 */
class UitForegroundInfo(
    /**
     * The notification id of the notification to be associated with the foreground service.
     */
    val notificationId: Int,
    /**
     * The notification object to be associated with the foreground service.
     */
    val notification: Notification,
    /**
     * The foreground service type for the foreground service associated with the task request.
     *
     * This is not required to be specified on API versions below 29.
     *
     * The default type here will be [ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE]. However, on
     * API versions 34 and above, a different type must be specified otherwise an
     * [InvalidForegroundServiceTypeException] will be thrown.
     */
    @Suppress("DEPRECATION")
    val fgsType: Int = ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE,
    /**
     * Indicates what should be done with the notification after the foreground service is finished.
     *
     * **By default, the notification will be removed
     * (see [TaskEndNotificationPolicy.NOTIFICATION_REMOVE])**
     */
    val taskEndNotificationPolicy: TaskEndNotificationPolicy =
        TaskEndNotificationPolicy.NOTIFICATION_REMOVE
) {
    /**
     * Internal container variable pointing to the [ForegroundInfo] object in WorkManager.
     */
    private val foregroundInfo: ForegroundInfo =
                                ForegroundInfo(notificationId, notification, fgsType)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as UitForegroundInfo
        return foregroundInfo == that.foregroundInfo &&
            taskEndNotificationPolicy == that.taskEndNotificationPolicy
    }

    override fun hashCode(): Int {
        var result = foregroundInfo.hashCode()
        result = 31 * result + taskEndNotificationPolicy.hashCode()
        return result
    }
}

enum class TaskEndNotificationPolicy {
    /**
     * This indicates that the notification will be removed when the task is finished.
     *
     * **This is the default behavior.**
     */
    NOTIFICATION_REMOVE,
    /**
     * This indicates that the notification will be detached from the foreground service,
     * but not removed so it can still be modified if needed.
     */
    NOTIFICATION_DETACH
}
