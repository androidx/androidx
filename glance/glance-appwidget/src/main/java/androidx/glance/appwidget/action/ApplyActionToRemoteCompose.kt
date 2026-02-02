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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.DpSize
import androidx.core.os.bundleOf
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.LambdaAction
import androidx.glance.action.StartActivityAction
import androidx.glance.action.StartActivityClassAction
import androidx.glance.action.StartActivityComponentAction
import androidx.glance.appwidget.GlanceAppWidgetTag
import androidx.glance.appwidget.remotecompose.TranslationContext as RCTranslationContext

@SuppressLint("NewApi") // remote compose only exists post api 35
internal fun applyActionForRemoteComposeElement(
    rcTranslationContext: RCTranslationContext,
    action: Action,
    @IdRes arbitraryId: Int,
) {
    try {
        val pendingIntent =
            getPendingIntentForAction(
                action = action,
                translationContext = rcTranslationContext,
                viewId = arbitraryId,
            )
        rcTranslationContext.actionMap.add(arbitraryId to pendingIntent)
    } catch (t: Throwable) {
        Log.e(GlanceAppWidgetTag, "Unrecognized Action: $action", t)
        throw IllegalStateException(t)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun getPendingIntentForAction(
    action: Action,
    translationContext: RCTranslationContext,
    @IdRes viewId: Int,
    mutability: Int = PendingIntent.FLAG_IMMUTABLE,
): PendingIntent {
    return when (action) {
        is StartActivityAction ->
            startActivityAction(action, translationContext, viewId, mutability)
        is StartServiceAction -> startServiceAction(action, translationContext, viewId, mutability)
        is SendBroadcastAction -> runBroadcastAction(action, translationContext, viewId, mutability)
        is RunCallbackAction -> runCallbackAction(action, translationContext, viewId, mutability)
        is LambdaAction -> runLambaAction(action, translationContext, viewId, mutability)
        else -> error("Cannot create PendingIntent for action type: $action")
    }
}

private fun runBroadcastAction(
    action: SendBroadcastAction,
    translationContext: RCTranslationContext,
    viewId: Int,
    mutability: Int,
): PendingIntent {
    val baseIntent = getBroadcastReceiverIntent(action, translationContext.context)
    setUniqueUriOnIntentIfNeeded(
        baseIntent = baseIntent,
        viewId = viewId,
        layoutSize = translationContext.layoutSize,
        appWidgetId = translationContext.appWidgetId,
    )

    return PendingIntent.getBroadcast(
        translationContext.context,
        0,
        baseIntent,
        mutability or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

private fun runLambaAction(
    action: LambdaAction,
    translationContext: RCTranslationContext,
    viewId: Int,
    mutability: Int,
): PendingIntent {
    requireNotNull(translationContext.actionBroadcastReceiver) {
        "In order to use LambdaAction, actionBroadcastReceiver must be provided"
    }

    val baseIntent =
        LambdaActionBroadcasts.createIntent(
            translationContext.actionBroadcastReceiver,
            action.key,
            translationContext.appWidgetId,
        )

    baseIntent.data =
        createUniqueUri(
            viewId = viewId,
            widgetId = translationContext.appWidgetId,
            type = ActionTrampolineType.CALLBACK,
            layoutSize = translationContext.layoutSize,
            extraData = baseIntent.flags.toString(),
        )

    return PendingIntent.getBroadcast(
        translationContext.context,
        0,
        baseIntent,
        mutability or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

private fun runCallbackAction(
    action: RunCallbackAction,
    translationContext: RCTranslationContext,
    @IdRes viewId: Int,
    mutability: Int,
): PendingIntent {
    val baseIntent: Intent =
        ActionCallbackBroadcastReceiver.createIntent(
            glanceComponents = translationContext.glanceComponents,
            callbackClass = action.callbackClass,
            appWidgetId = translationContext.appWidgetId,
            parameters = action.parameters,
        )

    baseIntent.data =
        createUniqueUri(
            viewId = viewId,
            widgetId = translationContext.appWidgetId,
            type = ActionTrampolineType.CALLBACK,
            layoutSize = translationContext.layoutSize,
            extraData = baseIntent.flags.toString(),
        )

    return PendingIntent.getBroadcast(
        translationContext.context,
        0,
        baseIntent,
        mutability or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

private fun startActivityAction(
    action: StartActivityAction,
    translationContext: RCTranslationContext,
    @IdRes viewId: Int,
    mutability: Int,
): PendingIntent {
    val params = action.parameters
    val baseIntent = getStartActivityIntent(action, translationContext.context, params)

    setUniqueUriOnIntentIfNeeded(
        baseIntent = baseIntent,
        viewId = viewId,
        layoutSize = translationContext.layoutSize,
        appWidgetId = translationContext.appWidgetId,
    )

    val pi: PendingIntent =
        PendingIntent.getActivity(
            translationContext.context,
            0,
            baseIntent,
            mutability or PendingIntent.FLAG_UPDATE_CURRENT,
            action.activityOptions,
        )

    return pi
}

@RequiresApi(Build.VERSION_CODES.O)
private fun startServiceAction(
    action: StartServiceAction,
    translationContext: RCTranslationContext,
    viewId: Int,
    mutability: Int,
): PendingIntent {
    val baseIntent = getServiceIntent(action = action, context = translationContext.context)

    setUniqueUriOnIntentIfNeeded(
        baseIntent = baseIntent,
        viewId = viewId,
        layoutSize = translationContext.layoutSize,
        appWidgetId = translationContext.appWidgetId,
    )
    return if (action.isForegroundService) {
        ApplyActionApi26Impl.getForegroundServicePendingIntent(
            context = translationContext.context,
            intent = baseIntent,
        )
    } else {
        PendingIntent.getService(
            translationContext.context,
            0,
            baseIntent,
            mutability or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

private fun setUniqueUriOnIntentIfNeeded(
    baseIntent: Intent,
    viewId: Int,
    layoutSize: DpSize,
    appWidgetId: Int,
) {
    // If there is no data URI set already, add a unique URI to ensure we get a
    // distinct PendingIntent.
    if (baseIntent.data == null) {
        baseIntent.data =
            createUniqueUri(
                viewId = viewId,
                widgetId = appWidgetId,
                type = ActionTrampolineType.CALLBACK,
                layoutSize = layoutSize,
                extraData = baseIntent.flags.toString(),
            )
    }
}

private fun getBroadcastReceiverIntent(action: SendBroadcastAction, context: Context): Intent =
    when (action) {
        is SendBroadcastComponentAction -> Intent().setComponent(action.componentName)
        is SendBroadcastClassAction -> Intent(context, action.receiverClass)
        is SendBroadcastIntentAction -> action.intent
        is SendBroadcastActionAction -> Intent(action.action).setComponent(action.componentName)
    }

private fun getServiceIntent(action: StartServiceAction, context: Context): Intent =
    when (action) {
        is StartServiceComponentAction -> Intent().setComponent(action.componentName)
        is StartServiceClassAction -> Intent(context, action.serviceClass)
        is StartServiceIntentAction -> action.intent
    }

@Suppress("Deprecation")
private fun getStartActivityIntent(
    action: StartActivityAction,
    context: Context,
    params: ActionParameters,
): Intent {
    val activityIntent =
        when (action) {
            is StartActivityComponentAction -> Intent().setComponent(action.componentName)
            is StartActivityClassAction -> Intent(context, action.activityClass)
            is StartActivityIntentAction -> action.intent
            else -> error("Action type not defined in app widget package: $action")
        }

    val parametersPairs = params.asMap().map { (key, value) -> key.name to value }.toTypedArray()

    // bundleOf() is deprecated because it is not type safe. Unfortunately, our class
    // ActionParameters.kt is is also not type safe. It states that for a Key<T>, T must be
    // primitive or parcelable, but there is no compile check for this
    activityIntent.putExtras(bundleOf(*parametersPairs))
    return activityIntent
}
