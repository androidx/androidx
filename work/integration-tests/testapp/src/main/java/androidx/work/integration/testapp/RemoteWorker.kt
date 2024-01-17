/*
 * Copyright 2021 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.multiprocess.RemoteListenableWorker
import androidx.work.workDataOf
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RemoteWorker(private val context: Context, private val parameters: WorkerParameters) :
    RemoteListenableWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var job: Job? = null
    private var progress: Data = Data.EMPTY

    override fun startRemoteWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer ->
            Log.d(TAG, "Starting Remote Worker.")
            if (Looper.getMainLooper().thread != Thread.currentThread()) {
                completer.set(Result.failure())
                return@getFuture "startRemoteWork"
            }
            val scope = CoroutineScope(Dispatchers.Default)
            job = scope.launch {
                for (i in 1..10) {
                    delay(10000)
                    progress = workDataOf(Progress to i * 10)
                    setForegroundAsync(getForegroundInfo(NotificationId))
                    setProgressAsync(progress).await()
                }
            }

            job?.invokeOnCompletion {
                Log.d(TAG, "Done.")
                completer.set(Result.success())
            }
            return@getFuture "startRemoteWork"
        }
    }

    override fun onStopped() {
        super.onStopped()
        job?.cancel()
    }

    private fun getForegroundInfo(notificationId: Int): ForegroundInfo {
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

        return ForegroundInfo(notificationId, notification)
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
        private const val TAG = "WM-RemoteWorker"
        private const val NotificationId = 20
        private const val Progress = "Progress"
    }
}
