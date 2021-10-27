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
import androidx.glance.action.ActionRunnable
import androidx.glance.action.UpdateContentAction
import androidx.glance.action.mutableActionParametersOf
import java.util.UUID

/**
 * Responds to broadcasts from [UpdateContentAction] clicks by executing the associated action.
 */
internal class ActionRunnableBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) {
            return
        }

        goAsync {
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

            runActionWork(context, intent, parameters)
            updateWidget(context, intent)
        }
    }

    companion object {
        private const val ExtraRunnableClassName = "ActionRunnableBroadcastReceiver:runnableClass"
        private const val ExtraWidgetClassName = "ActionRunnableBroadcastReceiver:appWidgetClass"
        private const val AppWidgetId = "ActionRunnableBroadcastReceiver:appWidgetId"
        private const val ExtraParameters = "ActionRunnableBroadcastReceiver:parameters"
        private const val ExtraParametersUUID = "ActionRunnableBroadcastReceiver:uuid"

        fun createPendingIntent(
            context: Context,
            runnableClass: Class<out ActionRunnable>,
            appWidgetClass: Class<out GlanceAppWidget>,
            appWidgetId: Int,
            parameters: ActionParameters
        ): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ActionRunnableBroadcastReceiver::class.java)
                    .setPackage(context.packageName)
                    .putExtra(ExtraRunnableClassName, runnableClass.canonicalName)
                    .putExtra(ExtraWidgetClassName, appWidgetClass.canonicalName)
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

    private suspend fun runActionWork(
        context: Context,
        intent: Intent,
        parameters: ActionParameters
    ) {
        val className = requireNotNull(intent.getStringExtra(ExtraRunnableClassName)) {
            "The intent must contain a work class name string using extra: " +
                "$ExtraRunnableClassName"
        }
        UpdateContentAction.run(context, className, parameters)
    }

    private suspend fun updateWidget(context: Context, intent: Intent) {
        val widgetClassName = requireNotNull(intent.getStringExtra(ExtraWidgetClassName)) {
            "The intent must contain a widget class name string using extra: " +
                "$ExtraWidgetClassName"
        }
        require(intent.hasExtra(AppWidgetId)) {
            "To update the widget, the intent must contain the AppWidgetId integer using extra: " +
                "$AppWidgetId"
        }

        (Class.forName(widgetClassName).newInstance() as GlanceAppWidget)
            .update(context, AppWidgetId(intent.getIntExtra(AppWidgetId, -1)))
    }
}
