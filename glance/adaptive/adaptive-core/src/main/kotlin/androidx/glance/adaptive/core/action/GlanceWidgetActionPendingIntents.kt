/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.core.action

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.RestrictTo

/**
 * Converts this [GlanceAdaptiveAction] into the immutable [PendingIntent] to send when its element
 * is tapped on the widget instance identified by [widgetName] and [widgetId].
 *
 * - [GlanceAdaptiveAction.CallbackAction]: a broadcast to [ActionCallbackBroadcastReceiver].
 * - [GlanceAdaptiveAction.LaunchAction]: starts
 *   [GlanceAdaptiveAction.LaunchAction.targetActivityClass] with
 *   [GlanceAdaptiveAction.LaunchAction.parameters] as extras.
 * - [GlanceAdaptiveAction.DeeplinkAction]: starts an [Intent.ACTION_VIEW] activity for
 *   [GlanceAdaptiveAction.DeeplinkAction.uri], in the widget's own app when one of its activities
 *   handles it (see [deeplinkIntent]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun GlanceAdaptiveAction.toPendingIntent(
    context: Context,
    widgetName: String,
    widgetId: String? = null,
): PendingIntent {
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    return when (this) {
        is GlanceAdaptiveAction.CallbackAction ->
            PendingIntent.getBroadcast(
                context,
                /* requestCode= */ 0,
                ActionCallbackBroadcastReceiver.createIntent(context, this, widgetName, widgetId),
                flags,
            )
        is GlanceAdaptiveAction.LaunchAction ->
            PendingIntent.getActivity(
                context,
                // Extras are not part of PendingIntent identity, so the request code keeps
                // launches with different parameters apart. It is used rather than the intent
                // data, which belongs to the developer's activity.
                /* requestCode= */ hashCode(),
                Intent(context, targetActivityClass)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtras(parameters),
                flags,
            )
        is GlanceAdaptiveAction.DeeplinkAction ->
            PendingIntent.getActivity(
                context,
                /* requestCode= */ 0,
                deeplinkIntent(context, uri),
                flags,
            )
        // GlanceAdaptiveAction is not sealed, so new action types can be added without breaking
        // callers; this is only reachable if one is added without being handled here.
        else -> throw IllegalArgumentException("Unsupported GlanceAdaptiveAction: $this")
    }
}

/**
 * The [Intent.ACTION_VIEW] intent for [uri], limited to the app of [context] when one of its
 * activities handles [uri], or left implicit otherwise, so a URI only another app (such as a
 * browser) handles still opens there.
 *
 * Limiting it to the app lets it reach the app's own non-exported deep link activities: from
 * Android 14 (API 34), an implicit intent, with neither a component nor a package, is only
 * delivered to exported activities, even within the app that sends it.
 */
private fun deeplinkIntent(context: Context, uri: Uri): Intent {
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // Resolves as startActivity does: with the app's own package set, its non-exported activities
    // are included.
    intent.setPackage(context.packageName)
    if (intent.resolveActivity(context.packageManager) == null) {
        intent.setPackage(null)
    }
    return intent
}
