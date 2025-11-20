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

package androidx.core.telecom.test

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.util.ExperimentalAppActions

@ExperimentalAppActions
@RequiresApi(Build.VERSION_CODES.S)
class NotificationsUtilities {
    companion object {
        val TAG: String = NotificationsUtilities::class.java.getSimpleName()
        val NOTIFICATION_CHANNEL_ID = "CoreTelecomTestVoipApp"
        val CHANNEL_NAME = "Core Telecom Test Voip App Channel"
        val CHANNEL_DESCRIPTION =
            "Core Telecom Test Voip calls will post call-style" + " notifications to this channel"
        val CALL_STYLE_INITIAL_TEXT: String = "New Call"
        val CALL_STYLE_ONGOING_TEXT: String = "Ongoing Call"
        val CALL_STYLE_TITLE: String = "Core-Telecom Test VoIP App"
        val NOTIFICATION_ACTION_ANSWER = "androidx.core.telecom.test.ANSWER"
        val NOTIFICATION_ACTION_DECLINE = "androidx.core.telecom.test.DECLINE"
        val NOTIFICATION_ID = "androidx.core.telecom.test.ID"
        val IS_ANSWER_ACTION = "androidx.core.telecom.test.IS_ANSWER_ACTION"

        fun createInitialCallStyleNotification(
            context: Context,
            uniqueId: Int,
            channelId: String?,
            callerName: String?,
            isOutgoing: Boolean,
        ): Notification {
            val fullScreenIntent = getDeclinePendingIntent(context, uniqueId)
            val person: Person = Person.Builder().setName(callerName).setImportant(true).build()
            return Notification.Builder(context, channelId)
                .setContentText(CALL_STYLE_INITIAL_TEXT)
                .setContentTitle(CALL_STYLE_TITLE)
                .setSmallIcon(R.drawable.sym_def_app_icon)
                .setStyle(getCallStyle(context, isOutgoing, person, fullScreenIntent, uniqueId))
                .setFullScreenIntent(fullScreenIntent, true)
                .setOngoing(isOutgoing)
                .build()
        }

        private fun getCallStyle(
            c: Context,
            isOutgoing: Boolean,
            person: Person,
            fullScreenIntent: PendingIntent,
            notificationId: Int,
        ): Notification.CallStyle {
            return if (isOutgoing) {
                Notification.CallStyle.forOngoingCall(person, fullScreenIntent)
            } else {
                Notification.CallStyle.forIncomingCall(
                    person,
                    getDeclinePendingIntent(c, notificationId),
                    getAnswerPendingIntent(c, notificationId),
                )
            }
        }

        fun updateNotificationToOngoing(
            context: Context,
            notificationId: Int,
            channelId: String?,
            callerName: String?,
        ) {
            val endCallAction = getDeclinePendingIntent(context, notificationId)
            val callStyleNotification =
                Notification.Builder(context, channelId)
                    .setContentText(CALL_STYLE_ONGOING_TEXT)
                    .setContentTitle(CALL_STYLE_TITLE)
                    .setSmallIcon(R.drawable.sym_def_app_icon)
                    .setStyle(
                        Notification.CallStyle.forOngoingCall(
                            Person.Builder().setName(callerName).setImportant(true).build(),
                            endCallAction,
                        )
                    )
                    .setFullScreenIntent(endCallAction, true)
                    .setOngoing(true)
                    .build()

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.notify(notificationId, callStyleNotification)
        }

        private fun getDeclinePendingIntent(context: Context, notificationId: Int): PendingIntent {
            return PendingIntent.getActivity(
                context,
                notificationId,
                getDeclineIntent(context, notificationId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        private fun getDeclineIntent(c: Context, notificationId: Int): Intent {
            val declineIntent =
                Intent(c, CallingMainActivity::class.java).apply {
                    action = NOTIFICATION_ACTION_DECLINE
                    putExtra(NOTIFICATION_ID, notificationId)
                    putExtra(IS_ANSWER_ACTION, false)
                }
            declineIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return declineIntent
        }

        private fun getAnswerPendingIntent(c: Context, notificationId: Int): PendingIntent {
            return PendingIntent.getActivity(
                c,
                notificationId,
                getAnswerIntent(c, notificationId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        private fun getAnswerIntent(c: Context, notificationId: Int): Intent {
            val answerIntent =
                Intent(c, CallingMainActivity::class.java).apply {
                    action = NOTIFICATION_ACTION_ANSWER
                    putExtra(NOTIFICATION_ID, notificationId)
                    putExtra(IS_ANSWER_ACTION, true)
                }
            answerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return answerIntent
        }

        fun clearNotification(c: Context, notificationId: Int) {
            val notificationManager = c.getSystemService(NotificationManager::class.java)
            notificationManager?.cancel(notificationId)
        }

        fun initNotificationChannel(c: Context) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(NOTIFICATION_CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESCRIPTION
                }
            val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        fun deleteNotificationChannel(c: Context) {
            val notificationManager = c.getSystemService(NotificationManager::class.java)
            try {
                notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)
            } catch (e: Exception) {
                Log.i(
                    TAG,
                    String.format(
                        "notificationManager: hit exception=[%s] while deleting the" +
                            " call channel with id=[%s]",
                        e,
                        NOTIFICATION_CHANNEL_ID,
                    ),
                )
            }
        }
    }
}
