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
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.LambdaAction
import androidx.glance.action.StartActivityAction
import androidx.glance.action.StartActivityClassAction
import androidx.glance.action.StartActivityComponentAction
import androidx.glance.action.toMutableParameters
import androidx.glance.appwidget.GlanceAppWidgetTag
import androidx.glance.appwidget.TranslationContext

internal fun applyAction(
    translationContext: TranslationContext,
    rv: RemoteViews,
    action: Action,
    @IdRes viewId: Int,
) {
    // CheckBox is wrapped in a FrameLayout, so the viewId passed to this function is the ID of the
    // FrameLayout, not the CheckBox itself. CheckBoxTranslator sets actionTargetId on the
    // translationContext which allows us to call setOnCheckedChangeResponse() on the correct
    // target.
    val targetId = translationContext.actionTargetId ?: viewId
    try {
        if (translationContext.isLazyCollectionDescendant) {
            val fillInIntent = getFillInIntentForAction(action, translationContext, targetId)
            if (action is CompoundButtonAction && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ApplyActionApi31Impl.setOnCheckedChangeResponse(rv, targetId, fillInIntent)
            } else {
                rv.setOnClickFillInIntent(targetId, fillInIntent)
            }
        } else {
            val pendingIntent = getPendingIntentForAction(action, translationContext, targetId)
            if (action is CompoundButtonAction && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ApplyActionApi31Impl.setOnCheckedChangeResponse(rv, targetId, pendingIntent)
            } else {
                rv.setOnClickPendingIntent(targetId, pendingIntent)
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
    mutability: Int = PendingIntent.FLAG_IMMUTABLE,
): PendingIntent {
    when (action) {
        is StartActivityAction -> {
            val params = editParams(action.parameters)
            return PendingIntent.getActivity(
                translationContext.context,
                0,
                getStartActivityIntent(action, translationContext, params).apply {
                    // If there is no data URI set already, add a unique URI to ensure we get a
                    // distinct PendingIntent.
                    if (data == null) {
                        data =
                            createUniqueUri(
                                translationContext,
                                viewId,
                                ActionTrampolineType.CALLBACK,
                                flags.toString()
                            )
                    }
                },
                mutability or PendingIntent.FLAG_UPDATE_CURRENT,
                action.activityOptions,
            )
        }
        is StartServiceAction -> {
            val intent =
                getServiceIntent(action, translationContext).apply {
                    if (data == null) {
                        data =
                            createUniqueUri(
                                translationContext,
                                viewId,
                                ActionTrampolineType.CALLBACK,
                                flags.toString(),
                            )
                    }
                }
            return if (
                action.isForegroundService && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
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
                    mutability or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }
        }
        is SendBroadcastAction -> {
            return PendingIntent.getBroadcast(
                translationContext.context,
                0,
                getBroadcastReceiverIntent(action, translationContext).apply {
                    if (data == null) {
                        data =
                            createUniqueUri(
                                translationContext,
                                viewId,
                                ActionTrampolineType.CALLBACK,
                                flags.toString(),
                            )
                    }
                },
                mutability or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        is RunCallbackAction -> {
            return PendingIntent.getBroadcast(
                translationContext.context,
                0,
                ActionCallbackBroadcastReceiver.createIntent(
                        translationContext,
                        action.callbackClass,
                        editParams(action.parameters)
                    )
                    .apply {
                        data =
                            createUniqueUri(
                                translationContext,
                                viewId,
                                ActionTrampolineType.CALLBACK,
                            )
                    },
                mutability or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        is LambdaAction -> {
            requireNotNull(translationContext.actionBroadcastReceiver) {
                "In order to use LambdaAction, actionBroadcastReceiver must be provided"
            }
            return PendingIntent.getBroadcast(
                translationContext.context,
                0,
                LambdaActionBroadcasts.createIntent(
                        translationContext.actionBroadcastReceiver,
                        action.key,
                        translationContext.appWidgetId,
                    )
                    .apply {
                        data =
                            createUniqueUri(
                                translationContext,
                                viewId,
                                ActionTrampolineType.CALLBACK,
                                action.key,
                            )
                    },
                mutability or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        is CompoundButtonAction -> {
            return getPendingIntentForAction(
                action.innerAction,
                translationContext,
                viewId,
                action.getActionParameters(),
                mutability =
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            action.innerAction !is LambdaAction
                    ) {
                        // RemoteViews.setOnCheckedChangedResponse (API 31+) requires a mutable
                        // PendingIntent in order to set the EXTRA_CHECKED extra with the current
                        // state
                        // of the button. Lambda actions do not use this extra so they can be
                        // immutable.
                        PendingIntent.FLAG_MUTABLE
                    } else {
                        mutability
                    }
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
): Intent =
    when (action) {
        is StartActivityAction -> {
            getStartActivityIntent(
                    action = action,
                    translationContext = translationContext,
                    params = editParams(action.parameters)
                )
                .apply {
                    if (data == null) {
                        data =
                            createUniqueUri(
                                translationContext,
                                viewId,
                                ActionTrampolineType.CALLBACK,
                                flags.toString(),
                            )
                    }
                }
        }
        is StartServiceAction -> {
            getServiceIntent(action = action, translationContext = translationContext)
                .applyTrampolineIntent(
                    translationContext,
                    viewId = viewId,
                    type =
                        if (action.isForegroundService) {
                            ActionTrampolineType.FOREGROUND_SERVICE
                        } else {
                            ActionTrampolineType.SERVICE
                        },
                )
        }
        is SendBroadcastAction -> {
            getBroadcastReceiverIntent(action = action, translationContext = translationContext)
                .applyTrampolineIntent(
                    translationContext,
                    viewId = viewId,
                    type = ActionTrampolineType.BROADCAST,
                )
        }
        is RunCallbackAction -> {
            ActionCallbackBroadcastReceiver.createIntent(
                    translationContext,
                    action.callbackClass,
                    editParams(action.parameters)
                )
                .applyTrampolineIntent(
                    translationContext,
                    viewId = viewId,
                    type = ActionTrampolineType.BROADCAST,
                )
        }
        is LambdaAction -> {
            requireNotNull(translationContext.actionBroadcastReceiver) {
                "In order to use LambdaAction, actionBroadcastReceiver must be provided"
            }
            LambdaActionBroadcasts.createIntent(
                    receiver = translationContext.actionBroadcastReceiver,
                    actionKey = action.key,
                    appWidgetId = translationContext.appWidgetId,
                )
                .applyTrampolineIntent(
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
            params.toMutableParameters().apply { set(ToggleableStateKey, !checked) }
        } else {
            params
        }
    }

private fun getBroadcastReceiverIntent(
    action: SendBroadcastAction,
    translationContext: TranslationContext,
): Intent =
    when (action) {
        is SendBroadcastComponentAction -> Intent().setComponent(action.componentName)
        is SendBroadcastClassAction -> Intent(translationContext.context, action.receiverClass)
        is SendBroadcastIntentAction -> action.intent
        is SendBroadcastActionAction -> Intent(action.action).setComponent(action.componentName)
    }

private fun getServiceIntent(
    action: StartServiceAction,
    translationContext: TranslationContext,
): Intent =
    when (action) {
        is StartServiceComponentAction -> Intent().setComponent(action.componentName)
        is StartServiceClassAction -> Intent(translationContext.context, action.serviceClass)
        is StartServiceIntentAction -> action.intent
    }

private fun getStartActivityIntent(
    action: StartActivityAction,
    translationContext: TranslationContext,
    params: ActionParameters,
): Intent {
    val activityIntent =
        when (action) {
            is StartActivityComponentAction -> Intent().setComponent(action.componentName)
            is StartActivityClassAction -> Intent(translationContext.context, action.activityClass)
            is StartActivityIntentAction -> action.intent
            else -> error("Action type not defined in app widget package: $action")
        }

    val parametersPairs = params.asMap().map { (key, value) -> key.name to value }.toTypedArray()

    activityIntent.putExtras(bundleOf(*parametersPairs))
    return activityIntent
}

@RequiresApi(Build.VERSION_CODES.S)
private object ApplyActionApi31Impl {

    fun setOnCheckedChangeResponse(rv: RemoteViews, viewId: Int, intent: PendingIntent) {
        rv.setOnCheckedChangeResponse(viewId, RemoteViews.RemoteResponse.fromPendingIntent(intent))
    }

    fun setOnCheckedChangeResponse(rv: RemoteViews, viewId: Int, intent: Intent) {
        rv.setOnCheckedChangeResponse(viewId, RemoteViews.RemoteResponse.fromFillInIntent(intent))
    }

    fun unsetOnCheckedChangeResponse(rv: RemoteViews, viewId: Int) {
        rv.setOnCheckedChangeResponse(viewId, RemoteViews.RemoteResponse())
    }

    fun unsetOnClickResponse(rv: RemoteViews, viewId: Int) {
        rv.setOnClickResponse(viewId, RemoteViews.RemoteResponse())
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private object ApplyActionApi29Impl {
    fun setIntentIdentifier(intent: Intent, viewId: Int): Intent =
        intent.apply { identifier = viewId.toString() }
}

@RequiresApi(Build.VERSION_CODES.O)
private object ApplyActionApi26Impl {
    fun getForegroundServicePendingIntent(context: Context, intent: Intent): PendingIntent {
        return PendingIntent.getForegroundService(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
