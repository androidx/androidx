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
package androidx.xr.compose.testapp.focuschange

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

const val CHANNEL_ID = "lifecycletest"
const val NOTIFICATION_ID = 1

fun createNotificationChannel(context: Context) {
    val importance = NotificationManager.IMPORTANCE_HIGH
    val channel =
        NotificationChannel(CHANNEL_ID, "Notification Channel", importance).apply {
            description = "Trying to make you lose focus, hope it works."
        }
    channel.setShowBadge(true)
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}

fun showSimpleNotification(context: Context, priority: Int) {
    val notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle("Taking your focus")
            .setContentText("Stealing focus from activity")
            .setPriority(priority)
            .setChannelId(CHANNEL_ID)
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(NOTIFICATION_ID, notification.build())
}

fun notificationTest(appContext: Context, priority: Int) {
    createNotificationChannel(appContext)
    showSimpleNotification(appContext, priority)
}
