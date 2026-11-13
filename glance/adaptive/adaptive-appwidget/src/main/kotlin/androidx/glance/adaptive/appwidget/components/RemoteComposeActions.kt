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

package androidx.glance.adaptive.appwidget.components

import android.util.Log
import androidx.compose.remote.creation.compose.action.rememberPendingIntentAction
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.runtime.Composable
import androidx.glance.adaptive.core.action.GlanceWidgetAction
import androidx.glance.adaptive.core.action.toPendingIntent

// Tag length must not exceed 23 characters for pre-API 24 log compatibility.
private const val TAG = "RemoteComposeActions"

/**
 * Turns the [GlanceWidgetAction]s drawn for one widget into RemoteCompose `PendingIntent` click
 * modifiers — the RemoteCompose counterpart of [RemoteViewsActions].
 *
 * Components that ignore taps are not given a [clickable] modifier at all, so accessibility
 * services never announce an inert tap target.
 *
 * @param widgetName Name of the widget the components are drawn for, which a
 *   [GlanceWidgetAction.Callback] hands to its callback, or `null` when they are not drawn for a
 *   named widget.
 */
internal class RemoteComposeActions(private val widgetName: String? = null) {

    /**
     * Whether a component with [action] runs it when tapped: [action] is not `null`, and it is not
     * a [GlanceWidgetAction.Callback] with no widget name to deliver it to.
     */
    fun isTappable(action: GlanceWidgetAction?): Boolean =
        action != null && (widgetName != null || action !is GlanceWidgetAction.Callback)

    /**
     * Attaches a `PendingIntent` click action for [action] when [isTappable] is `true`, or returns
     * `this` unchanged when the component should ignore taps.
     */
    @RemoteComposable
    @Composable
    fun RemoteModifier.widgetClickable(action: GlanceWidgetAction?): RemoteModifier {
        if (action == null) return this
        if (!isTappable(action)) {
            Log.w(TAG, "Ignoring $action: a callback needs the name of the widget it is drawn for.")
            return this
        }
        val remoteAction = rememberPendingIntentAction { context ->
            // Only a callback reads the widget name.
            // TODO(b/580007927): Also pass widgetId to toPendingIntent once the renderer is given
            //  the widget instance it renders for.
            action.toPendingIntent(context, widgetName.orEmpty())
        }
        return clickable(remoteAction)
    }
}
