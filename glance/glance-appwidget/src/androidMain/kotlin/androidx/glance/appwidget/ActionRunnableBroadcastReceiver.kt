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

package androidx.glance.appwidget

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.action.ActionRunnable
import androidx.glance.action.UpdateContentAction

/**
 * Responds to broadcasts from [UpdateContentAction] clicks by executing the associated action.
 */
internal class ActionRunnableBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) {
            return
        }

        goAsync {
            val className = requireNotNull(intent.getStringExtra(ExtraClassName)) {
                "The custom work intent must contain a work class name string using extra: " +
                    ExtraClassName
            }
            UpdateContentAction.run(context, className)
        }
    }

    companion object {
        private const val ExtraClassName = "CustomWorkBroadcastReceiver:className"

        fun createPendingIntent(
            context: Context,
            runnableClass: Class<out ActionRunnable>
        ): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ActionRunnableBroadcastReceiver::class.java)
                    .setPackage(context.packageName)
                    .putExtra(ExtraClassName, runnableClass.canonicalName)
                    .setData(Uri.Builder()
                        .scheme("remoteAction")
                        .appendQueryParameter("className", runnableClass.canonicalName)
                        .build()),
                PendingIntent.FLAG_MUTABLE
            )
        }
    }
}
