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

package androidx.glance.appwidget.action

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import androidx.glance.action.ActionParameters
import androidx.glance.action.mutableActionParametersOf
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.goAsync
import androidx.glance.appwidget.logException
import kotlinx.coroutines.CancellationException

/** Responds to broadcasts from [RunCallbackAction] clicks by executing the associated action. */
open class ActionCallbackBroadcastReceiver : BroadcastReceiver() {

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context?, intent: Intent?) {
        goAsync {
            try {
                requireNotNull(context) { "Context is null" }
                requireNotNull(intent) { "Intent is null" }
                val extras =
                    requireNotNull(intent.extras) {
                        "The intent must have action parameters extras."
                    }
                val paramsBundle =
                    requireNotNull(extras.getBundle(ExtraParameters)) {
                        "The intent must contain a parameters bundle using extra: $ExtraParameters"
                    }
                val parameters =
                    mutableActionParametersOf().apply {
                        paramsBundle.keySet().forEach { key ->
                            set(ActionParameters.Key(key), paramsBundle[key])
                        }
                        if (extras.containsKey(RemoteViews.EXTRA_CHECKED)) {
                            set(ToggleableStateKey, extras.getBoolean(RemoteViews.EXTRA_CHECKED))
                        }
                    }
                val className =
                    requireNotNull(extras.getString(ExtraCallbackClassName)) {
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal companion object {
        private const val AppWidgetId = "ActionCallbackBroadcastReceiver:appWidgetId"
        private const val ExtraCallbackClassName = "ActionCallbackBroadcastReceiver:callbackClass"
        private const val ExtraParameters = "ActionCallbackBroadcastReceiver:parameters"

        internal fun createIntent(
            translationContext: TranslationContext,
            callbackClass: Class<out ActionCallback>,
            parameters: ActionParameters
        ) =
            Intent()
                .setComponent(translationContext.glanceComponents.actionCallbackBroadcastReceiver)
                .putExtra(ExtraCallbackClassName, callbackClass.canonicalName)
                .putExtra(AppWidgetId, translationContext.appWidgetId)
                .putParameterExtras(parameters)

        private fun Intent.putParameterExtras(parameters: ActionParameters): Intent {
            val parametersPairs =
                parameters.asMap().map { (key, value) -> key.name to value }.toTypedArray()
            putExtra(ExtraParameters, bundleOf(*parametersPairs))
            return this
        }
    }
}
