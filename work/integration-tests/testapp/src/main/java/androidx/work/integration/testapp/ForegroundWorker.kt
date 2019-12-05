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

package androidx.work.integration.testapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay

class ForegroundWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var progress: Data = Data.EMPTY

    override suspend fun doWork(): Result {
        // Run in the context of a Foreground service
        setForeground(getNotification())

        val range = 20
        for (i in 1..range) {
            delay(1000)
            progress = workDataOf(Progress to i * (100 / range))
            setProgress(progress)
            setForeground(getNotification())
        }
        return Result.success()
    }

    private fun getNotification(): ForegroundInfo {
        val percent = progress.getInt(Progress, 0)
        val id = applicationContext.getString(R.string.channel_id)
        val title = applicationContext.getString(R.string.notification_title)
        val content = "Progress ($percent) %"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_work_notification)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NotificationId, notification)
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
        private const val NotificationId = 10
        private const val Progress = "Progress"
    }
}
