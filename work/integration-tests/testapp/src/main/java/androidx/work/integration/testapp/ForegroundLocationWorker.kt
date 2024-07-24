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

package androidx.work.integration.testapp

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay

class ForegroundLocationWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var currentLocation: Data = Data.EMPTY

    override suspend fun doWork(): Result {
        val permissionStatus =
            ActivityCompat.checkSelfPermission(applicationContext, ACCESS_COARSE_LOCATION)
        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            return Result.failure()
        }

        val notificationId = inputData.getInt(InputNotificationId, NotificationId)
        val delayTime = inputData.getLong(InputDelayTime, Delay)

        setForeground(getForegroundInfo(notificationId))

        repeat(20) {
            val location =
                locationManager.getLastKnownLocation(locationManager.allProviders.first())
            currentLocation =
                workDataOf(Lat to (location?.latitude ?: 0.0), Lon to (location?.longitude ?: 0.0))
            setProgress(currentLocation)
            if (Build.VERSION.SDK_INT < 31) {
                // No need for notifications starting S.
                notificationManager.notify(
                    notificationId,
                    getForegroundInfo(notificationId).notification
                )
            }
            delay(delayTime)
        }

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationId = inputData.getInt(InputNotificationId, NotificationId)
        return getForegroundInfo(notificationId)
    }

    private fun getForegroundInfo(notificationId: Int): ForegroundInfo {
        val latitude = currentLocation.getDouble(Lat, 0.0)
        val longitude = currentLocation.getDouble(Lon, 0.0)
        val id = applicationContext.getString(R.string.channel_id)
        val title = applicationContext.getString(R.string.notification_title)
        val content = "Location ($latitude,$longitude) %"
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

        return ForegroundInfo(notificationId, notification, FOREGROUND_SERVICE_TYPE_LOCATION)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClassVerificationFailure")
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
        private const val Delay = 1000L
        private const val Lat = "Location.lat"
        private const val Lon = "Location.Lon"
        const val InputNotificationId = "NotificationId"
        const val InputDelayTime = "DelayTime"
    }
}
