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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.os.bundleOf
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.action.ActionParameters
import androidx.glance.action.mutableActionParametersOf
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceComponents
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.UnmanagedSessionReceiver
import androidx.glance.appwidget.logException
import androidx.glance.appwidget.updatestrategy.BroadcastRequest
import androidx.glance.appwidget.updatestrategy.UpdateStrategy
import kotlinx.coroutines.CancellationException

/** Responds to broadcasts from [RunCallbackAction] clicks by executing the associated action. */
public open class ActionCallbackBroadcastReceiver : BroadcastReceiver() {

    @OptIn(ExperimentalGlanceApi::class)
    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            requireNotNull(context) { "Context is null" }
            requireNotNull(intent) { "Intent is null" }
            val extras =
                requireNotNull(intent.extras) { "The intent must have action parameters extras." }
            val parameters = getParameterExtras(extras)
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

            val maybeWidgetReceiver = createWidgetReceiver(extras.getString(ExtraReceiverClassName))
            // Find the clicked widget: a bound widget through its receiver, or a widget shown with
            // GlanceAppWidget.runComposition through the session it registered.
            val widget =
                maybeWidgetReceiver?.glanceAppWidget
                    ?: UnmanagedSessionReceiver.getSession(glanceId.appWidgetId)?.widget
            // Defer to the widget's UpdateStrategy, or fall back to IMMEDIATE when there is no
            // widget (for example a preview, or an intent created by an older version).
            val strategy = widget?.updateStrategy ?: UpdateStrategy.IMMEDIATE
            strategy.handleBroadcast(
                context,
                BroadcastRequest.RunCallback(
                    receiver = this,
                    widgetOwner = maybeWidgetReceiver,
                    className = className,
                    glanceId = glanceId,
                    parameters = parameters,
                ),
            )
        } catch (ex: CancellationException) {
            throw ex
        } catch (throwable: Throwable) {
            logException(throwable)
        }
    }

    internal companion object {
        internal const val AppWidgetId = "ActionCallbackBroadcastReceiver:appWidgetId"
        internal const val ExtraCallbackClassName = "ActionCallbackBroadcastReceiver:callbackClass"
        internal const val ExtraParameters = "ActionCallbackBroadcastReceiver:parameters"
        internal const val ExtraReceiverClassName = "ActionCallbackBroadcastReceiver:receiverClass"

        internal fun createIntent(
            translationContext: TranslationContext,
            callbackClass: Class<out ActionCallback>,
            parameters: ActionParameters,
        ) =
            createIntent(
                glanceComponents = translationContext.glanceComponents,
                appWidgetId = translationContext.appWidgetId,
                callbackClass = callbackClass,
                parameters = parameters,
                receiver = translationContext.actionBroadcastReceiver,
            )

        internal fun createIntent(
            glanceComponents: GlanceComponents,
            appWidgetId: Int,
            callbackClass: Class<out ActionCallback>,
            parameters: ActionParameters,
            receiver: ComponentName? = null,
        ) =
            Intent()
                .setComponent(glanceComponents.actionCallbackBroadcastReceiver)
                .putExtra(ExtraCallbackClassName, callbackClass.canonicalName)
                .putExtra(AppWidgetId, appWidgetId)
                .putExtra(ExtraReceiverClassName, receiver?.className)
                .putParameterExtras(parameters)

        internal fun Intent.putParameterExtras(parameters: ActionParameters): Intent {
            val parametersPairs =
                parameters.asMap().map { (key, value) -> key.name to value }.toTypedArray()
            @Suppress("DEPRECATION") // bundleOf is deprecated
            putExtra(ExtraParameters, bundleOf(*parametersPairs))
            return this
        }

        @Suppress("DEPRECATION")
        internal fun getParameterExtras(extras: Bundle): ActionParameters {
            val paramsBundle =
                requireNotNull(extras.getBundle(ExtraParameters)) {
                    "The intent must contain a parameters bundle using extra: $ExtraParameters"
                }
            return mutableActionParametersOf().apply {
                paramsBundle.keySet().forEach { key ->
                    set(ActionParameters.Key(key), paramsBundle[key])
                }
                if (extras.containsKey(RemoteViews.EXTRA_CHECKED) == true) {
                    set(ToggleableStateKey, extras.getBoolean(RemoteViews.EXTRA_CHECKED))
                }
            }
        }

        /**
         * Creates the [GlanceAppWidgetReceiver] named by [className], or returns null if
         * [className] is null, can't be instantiated, or isn't a [GlanceAppWidgetReceiver] (such as
         * [UnmanagedSessionReceiver]).
         */
        private fun createWidgetReceiver(className: String?): GlanceAppWidgetReceiver? {
            if (className == null) return null
            return runCatching { Class.forName(className).getDeclaredConstructor().newInstance() }
                .getOrNull() as? GlanceAppWidgetReceiver
        }
    }
}
