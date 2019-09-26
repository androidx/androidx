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

package androidx.work.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.NotificationMetadata
import androidx.work.NotificationProvider
import androidx.work.Worker
import androidx.work.WorkerParameters

class TestForegroundWorker(val context: Context, val parameters: WorkerParameters) :
    Worker(context, parameters), NotificationProvider {

    override fun doWork(): Result {
        return Result.success()
    }

    override fun getNotification(): NotificationMetadata {
        val notification = NotificationCompat.Builder(context, ChannelId)
            .setOngoing(true)
            .setTicker(Ticker)
            .setContentText(Content)
            .build()

        return NotificationMetadata.Builder(NotificationId, notification)
            .build()
    }

    companion object {
        // Channel id
        private const val ChannelId = "Channel"
        // The notification id
        private const val NotificationId = 1
        // Ticker
        private const val Ticker = "StopAwareForegroundWorker"
        // Content
        private const val Content = "Test Notification"
    }
}
