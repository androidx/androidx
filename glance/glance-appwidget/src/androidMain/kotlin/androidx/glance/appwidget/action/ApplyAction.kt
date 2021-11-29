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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.LaunchActivityAction
import androidx.glance.action.LaunchActivityClassAction
import androidx.glance.action.LaunchActivityComponentAction
import androidx.glance.action.toMutableParameters
import androidx.glance.appwidget.GlanceAppWidgetTag
import androidx.glance.appwidget.TranslationContext

internal fun applyAction(
    translationContext: TranslationContext,
    rv: RemoteViews,
    action: Action,
    @IdRes viewId: Int,
) {
    try {
        if (translationContext.isLazyCollectionDescendant) {
            val fillInIntent =
                getFillInIntentForAction(action, translationContext, viewId)
            if (action is CompoundButtonAction && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ApplyActionApi31Impl.setOnCheckedChangeResponse(rv, viewId, fillInIntent)
            } else {
                rv.setOnClickFillInIntent(viewId, fillInIntent)
            }
        } else {
            val pendingIntent =
                getPendingIntentForAction(action, translationContext, viewId)
            if (action is CompoundButtonAction && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ApplyActionApi31Impl.setOnCheckedChangeResponse(rv, viewId, pendingIntent)
            } else {
                rv.setOnClickPendingIntent(viewId, pendingIntent)
            }
        }
    } catch (t: Throwable) {
        Log.e(GlanceAppWidgetTag, "Unrecognized Action: $action", t)
    }
}

private fun getPendingIntentForAction(
    action: Action,
    translationContext: TranslationContext,
    @IdRes viewId: Int,
    editParams: (ActionParameters) -> ActionParameters = { it },
): PendingIntent {
    when (action) {
        is LaunchActivityAction -> {
            val params = editParams(action.parameters)
            val intent = getLaunchActivityIntent(action, translationContext, params)
            val finalIntent = if (action !is LaunchActivityIntentAction && !params.isEmpty()) {
                intent.applyTrampolineIntent(
                    translationContext,
                    viewId,
                    ActionTrampolineType.ACTIVITY,
                )
            } else {
                intent
            }
            return PendingIntent.getActivity(
                translationContext.context,
                0,
                finalIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        is LaunchServiceAction -> {
            val intent = getLaunchServiceIntent(action, translationContext)
            return if (action.isForegroundService &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ) {
                ApplyActionApi26Impl.getForegroundServicePendingIntent(
                    context = translationContext.context,
                    intent = intent
                )
            } else {
                PendingIntent.getService(
                    translationContext.context,
                    0,
                    intent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }
        }
        is LaunchBroadcastReceiverAction -> {
            return PendingIntent.getBroadcast(
                translationContext.context,
                0,
                getLaunchBroadcastReceiverIntent(action, translationContext),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        is RunCallbackAction -> {
            return PendingIntent.getBroadcast(
                translationContext.context,
                0,
                ActionCallbackBroadcastReceiver.createIntent(
                    translationContext.context,
                    action.callbackClass,
                    translationContext.appWidgetId,
                    editParams(action.parameters)
                ).apply {
                    data =
                        createUniqueUri(
                            translationContext,
                            viewId,
                            ActionTrampolineType.CALLBACK,
                        )
                },
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        is CompoundButtonAction -> {
            return getPendingIntentForAction(
                action.innerAction,
                translationContext,
                viewId,
                action.getActionParameters(),
            )
        }
        else -> error("Cannot create PendingIntent for action type: $action")
    }
}

private fun getFillInIntentForAction(
    action: Action,
    translationContext: TranslationContext,
    @IdRes viewId: Int,
    editParams: (ActionParameters) -> ActionParameters = { it }
): Intent = when (action) {
    is LaunchActivityAction -> {
        getLaunchActivityIntent(
            action = action,
            translationContext = translationContext,
            params = editParams(action.parameters)
        ).applyTrampolineIntent(
            translationContext,
            viewId = viewId,
            type = ActionTrampolineType.ACTIVITY,
        )
    }
    is LaunchServiceAction -> {
        getLaunchServiceIntent(
            action = action,
            translationContext = translationContext
        ).applyTrampolineIntent(
            translationContext,
            viewId = viewId,
            type = if (action.isForegroundService) {
                ActionTrampolineType.FOREGROUND_SERVICE
            } else {
                ActionTrampolineType.SERVICE
            },
        )
    }
    is LaunchBroadcastReceiverAction -> {
        getLaunchBroadcastReceiverIntent(
            action = action,
            translationContext = translationContext
        ).applyTrampolineIntent(
            translationContext,
            viewId = viewId,
            type = ActionTrampolineType.BROADCAST,
        )
    }
    is RunCallbackAction -> {
        ActionCallbackBroadcastReceiver.createIntent(
            context = translationContext.context,
            callbackClass = action.callbackClass,
            appWidgetId = translationContext.appWidgetId,
            parameters = editParams(action.parameters)
        ).applyTrampolineIntent(
            translationContext,
            viewId = viewId,
            type = ActionTrampolineType.BROADCAST,
        )
    }
    is CompoundButtonAction -> {
        getFillInIntentForAction(
            action.innerAction,
            translationContext,
            viewId,
            action.getActionParameters(),
        )
    }
    else -> error("Cannot create fill-in Intent for action type: $action")
}

private fun CompoundButtonAction.getActionParameters(): (ActionParameters) -> ActionParameters =
    { params: ActionParameters ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            params.toMutableParameters().apply {
                set(ToggleableStateKey, !checked)
            }
        } else {
            params
        }
    }

private fun getLaunchBroadcastReceiverIntent(
    action: LaunchBroadcastReceiverAction,
    translationContext: TranslationContext,
): Intent = when (action) {
    is LaunchBroadcastReceiverComponentAction -> Intent().setComponent(action.componentName)
    is LaunchBroadcastReceiverClassAction ->
        Intent(translationContext.context, action.receiverClass)
    is LaunchBroadcastReceiverIntentAction -> action.intent
    is LaunchBroadcastReceiverActionAction ->
        Intent(action.action).setComponent(action.componentName)
}

private fun getLaunchServiceIntent(
    action: LaunchServiceAction,
    translationContext: TranslationContext,
): Intent = when (action) {
    is LaunchServiceComponentAction -> Intent().setComponent(action.componentName)
    is LaunchServiceClassAction ->
        Intent(translationContext.context, action.serviceClass)
    is LaunchServiceIntentAction -> action.intent
}

private fun getLaunchActivityIntent(
    action: LaunchActivityAction,
    translationContext: TranslationContext,
    params: ActionParameters,
): Intent {
    val activityIntent = when (action) {
        is LaunchActivityComponentAction -> Intent().setComponent(action.componentName)
        is LaunchActivityClassAction -> Intent(translationContext.context, action.activityClass)
        is LaunchActivityIntentAction -> action.intent
        else -> error("Action type not defined in app widget package: $action")
    }

    val parametersPairs = params.asMap().map { (key, value) ->
        key.name to value
    }.toTypedArray()

    activityIntent.putExtras(bundleOf(*parametersPairs))
    return activityIntent
}

@RequiresApi(Build.VERSION_CODES.S)
private object ApplyActionApi31Impl {

    @DoNotInline
    fun setOnCheckedChangeResponse(rv: RemoteViews, viewId: Int, intent: PendingIntent) {
        rv.setOnCheckedChangeResponse(viewId, RemoteViews.RemoteResponse.fromPendingIntent(intent))
    }

    @DoNotInline
    fun setOnCheckedChangeResponse(rv: RemoteViews, viewId: Int, intent: Intent) {
        rv.setOnCheckedChangeResponse(viewId, RemoteViews.RemoteResponse.fromFillInIntent(intent))
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private object ApplyActionApi29Impl {
    @DoNotInline
    fun setIntentIdentifier(intent: Intent, viewId: Int): Intent = intent.apply {
        identifier = viewId.toString()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private object ApplyActionApi26Impl {
    @DoNotInline
    fun getForegroundServicePendingIntent(context: Context, intent: Intent): PendingIntent {
        return PendingIntent.getForegroundService(
            context,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}