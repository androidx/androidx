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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.glance.appwidget.TranslationContext

internal enum class ActionTrampolineType {
    ACTIVITY, BROADCAST, SERVICE, FOREGROUND_SERVICE, CALLBACK
}

private const val ActionTrampolineScheme = "glance-action"

/**
 * Wraps the "action intent" into an activity trampoline intent, where it will be invoked based on
 * the type, with modifying its content.
 *
 * @see launchTrampolineAction
 */
internal fun Intent.applyTrampolineIntent(
    translationContext: TranslationContext,
    viewId: Int,
    type: ActionTrampolineType
): Intent {
    val target = if (type == ActionTrampolineType.ACTIVITY) {
        ActionTrampolineActivity::class.java
    } else {
        InvisibleActionTrampolineActivity::class.java
    }
    return Intent(translationContext.context, target).also { intent ->
        intent.data = createUniqueUri(translationContext, viewId, type)
        intent.putExtra(ActionTypeKey, type.name)
        intent.putExtra(ActionIntentKey, this)
    }
}

internal fun createUniqueUri(
    translationContext: TranslationContext,
    viewId: Int,
    type: ActionTrampolineType,
): Uri = Uri.Builder().apply {
    scheme(ActionTrampolineScheme)
    path(type.name)
    appendQueryParameter("appWidgetId", translationContext.appWidgetId.toString())
    appendQueryParameter("viewId", viewId.toString())
    appendQueryParameter("viewSize", translationContext.layoutSize.toString())
    if (translationContext.isLazyCollectionDescendant) {
        appendQueryParameter(
            "lazyCollection",
            translationContext.layoutCollectionViewId.toString()
        )
        appendQueryParameter(
            "lazeViewItem",
            translationContext.layoutCollectionItemId.toString()
        )
    }
}.build()

/**
 * Unwraps and launches the action intent based on its type.
 *
 * @see applyTrampolineIntent
 */
@Suppress("DEPRECATION")
internal fun Activity.launchTrampolineAction(intent: Intent) {
    val actionIntent = requireNotNull(intent.getParcelableExtra<Intent>(ActionIntentKey)) {
        "List adapter activity trampoline invoked without specifying target intent."
    }
    if (intent.hasExtra(RemoteViews.EXTRA_CHECKED)) {
        actionIntent.putExtra(
            RemoteViews.EXTRA_CHECKED,
            intent.getBooleanExtra(RemoteViews.EXTRA_CHECKED, false)
        )
    }
    val type = requireNotNull(intent.getStringExtra(ActionTypeKey)) {
        "List adapter activity trampoline invoked without trampoline type"
    }
    when (ActionTrampolineType.valueOf(type)) {
        ActionTrampolineType.ACTIVITY -> startActivity(actionIntent)
        ActionTrampolineType.BROADCAST, ActionTrampolineType.CALLBACK -> sendBroadcast(actionIntent)
        ActionTrampolineType.SERVICE -> startService(actionIntent)
        ActionTrampolineType.FOREGROUND_SERVICE -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ListAdapterTrampolineApi26Impl.startForegroundService(
                    context = this,
                    intent = actionIntent
                )
            } else {
                startService(actionIntent)
            }
        }
    }
    finish()
}

private const val ActionTypeKey = "ACTION_TYPE"
private const val ActionIntentKey = "ACTION_INTENT"

@RequiresApi(Build.VERSION_CODES.O)
private object ListAdapterTrampolineApi26Impl {
    @DoNotInline
    fun startForegroundService(context: Context, intent: Intent) {
        context.startForegroundService(intent)
    }
}
