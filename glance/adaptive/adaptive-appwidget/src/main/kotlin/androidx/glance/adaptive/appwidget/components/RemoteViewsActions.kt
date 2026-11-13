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

import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.glance.adaptive.core.action.GlanceWidgetAction
import androidx.glance.adaptive.core.action.toPendingIntent

// Tag length must not exceed 23 characters for pre-API 24 log compatibility.
private const val TAG = "RemoteViewsActions"

/**
 * Turns the [GlanceWidgetAction]s drawn for one widget into the [PendingIntent]s its views send
 * when tapped.
 *
 * A [PendingIntent] is identified by its request code and its intent's action, data and component,
 * but not by its extras. [toPendingIntent] derives all of these from the action's value, so a view
 * keeps the same [PendingIntent] from one update to the next, and two different actions never
 * share, and so never overwrite, one. Where a view is drawn takes no part in it: the same action in
 * two places does the same thing, so it may share one [PendingIntent].
 *
 * A view that ignores taps must not be given a click at all, not even a `null` one: setting any
 * click makes the view clickable, and accessibility services then announce a tap that does nothing.
 * So whether a view is tappable has to be part of what the host compares when it reapplies an
 * update onto the previous hierarchy, its layout or its stable ID, or a view that lost its action
 * would keep the earlier one.
 *
 * @property context The context the views and their [PendingIntent]s are created with.
 * @param widgetName Name of the widget the views are drawn for, which a
 *   [GlanceWidgetAction.Callback] hands to its callback, or `null` when they are not drawn for a
 *   named widget. The target instance's `widgetId` is not yet passed to the renderer (b/580007927),
 *   so callbacks receive `widgetId = null` until the renderer is given the widget instance it
 *   renders for.
 */
internal class RemoteViewsActions(
    val context: Context,
    private val widgetName: String?,
) {
    /**
     * Whether a view with [action] runs it when tapped: [action] is not `null`, and it is not a
     * [GlanceWidgetAction.Callback] with no widget name to deliver it to.
     */
    fun isTappable(action: GlanceWidgetAction?): Boolean =
        action != null && (widgetName != null || action !is GlanceWidgetAction.Callback)

    /**
     * The [PendingIntent] a tap runs [action] with, or `null` when the view should ignore taps (see
     * [isTappable]).
     */
    fun pendingIntentFor(action: GlanceWidgetAction?): PendingIntent? {
        if (action == null) return null
        if (!isTappable(action)) {
            Log.w(TAG, "Ignoring $action: a callback needs the name of the widget it is drawn for.")
            return null
        }
        // Only a callback reads the widget name.
        // TODO(b/580007927): Also pass widgetId to toPendingIntent once the renderer is given the
        //  widget instance it renders for.
        return action.toPendingIntent(context, widgetName.orEmpty())
    }
}
