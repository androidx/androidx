/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.work.integration.testapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

/** Foreground worker that calls [setForegroundAsync] and then waits. */
class SetForegroundAsyncWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val delayTime = inputData.getLong(InputDelayTime, Delay)
        setForegroundAsync(makeForegroundInfo())
        delay(delayTime)
        return Result.success()
    }

    private fun makeForegroundInfo(): ForegroundInfo {
        val id = applicationContext.getString(R.string.channel_id)
        val title = applicationContext.getString(R.string.notification_title)
        val content = "Content"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notification =
            NotificationCompat.Builder(applicationContext, id)
                .setContentTitle(title)
                .setTicker(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_work_notification)
                .setOngoing(true)
                .build()

        return ForegroundInfo(
            /* notificationId= */ 1,
            notification,
            FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val id = applicationContext.getString(R.string.channel_id)
        val name = applicationContext.getString(R.string.channel_name)
        val description = applicationContext.getString(R.string.channel_description)
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW)
        channel.description = description
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val Delay = 100L
        const val InputDelayTime = "DelayTime"
    }
}
