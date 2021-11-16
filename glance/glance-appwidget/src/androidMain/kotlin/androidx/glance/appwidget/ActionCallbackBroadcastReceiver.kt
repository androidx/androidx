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
import androidx.core.os.bundleOf
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.RunCallbackAction
import androidx.glance.action.mutableActionParametersOf
import kotlinx.coroutines.CancellationException
import java.util.UUID

/**
 * Responds to broadcasts from [RunCallbackAction] clicks by executing the associated action.
 */
internal class ActionCallbackBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        goAsync {
            try {
                val extras = requireNotNull(intent.extras) {
                    "The intent must have action parameters extras."
                }
                val paramsBundle = requireNotNull(extras.getBundle(ExtraParameters)) {
                    "The intent must contain a parameters bundle using extra: $ExtraParameters"
                }
                val parameters = mutableActionParametersOf().apply {
                    paramsBundle.keySet().forEach { key ->
                        set(ActionParameters.Key(key), paramsBundle[key])
                    }
                }
                val className = requireNotNull(extras.getString(ExtraCallbackClassName)) {
                    "The intent must contain a work class name string using " +
                        "extra: $ExtraCallbackClassName"
                }
                require(intent.hasExtra(AppWidgetId)) {
                    "To update the widget, the intent must contain the AppWidgetId integer using" +
                        " extra: $AppWidgetId"
                }
                val glanceId = AppWidgetId(extras.getInt(AppWidgetId))

                RunCallbackAction.run(context, className, glanceId, parameters)
            } catch (ex: CancellationException) {
                throw ex
            } catch (throwable: Throwable) {
                logException(throwable)
            }
        }
    }

    companion object {
        private const val AppWidgetId = "ActionCallbackBroadcastReceiver:appWidgetId"
        private const val ExtraCallbackClassName = "ActionCallbackBroadcastReceiver:callbackClass"
        private const val ExtraParameters = "ActionCallbackBroadcastReceiver:parameters"
        private const val ExtraParametersUUID = "ActionCallbackBroadcastReceiver:uuid"

        fun createPendingIntent(
            context: Context,
            callbackClass: Class<out ActionCallback>,
            appWidgetId: Int,
            parameters: ActionParameters
        ): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ActionCallbackBroadcastReceiver::class.java)
                .setPackage(context.packageName)
                .putExtra(ExtraCallbackClassName, callbackClass.canonicalName)
                .putExtra(ExtraParametersUUID, UUID.randomUUID().leastSignificantBits)
                .putExtra(AppWidgetId, appWidgetId)
                .putParameterExtras(parameters).apply {
                    data = Uri.parse(toUri(0))
                        .buildUpon()
                        .scheme("remoteAction")
                        .build()
                },
            PendingIntent.FLAG_MUTABLE
        )

        private fun Intent.putParameterExtras(parameters: ActionParameters): Intent {
            val parametersPairs = parameters.asMap().map { (key, value) ->
                key.name to value
            }.toTypedArray()
            putExtra(ExtraParameters, bundleOf(*parametersPairs))
            return this
        }
    }
}