/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.action

import android.app.PendingIntent
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.actions.Action as CreationAction
import androidx.compose.remote.creation.actions.HostAction as CreationHostAction
import androidx.compose.remote.creation.compose.capture.WriterEvents
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.creationState
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.creation.profile.WidgetsProfileWriterV6
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Creates an [Action] that triggers a [PendingIntent].
 *
 * The [PendingIntent] is not serialized into the document. It is stored in
 * [androidx.compose.remote.creation.compose.capture.CapturedDocument.pendingIntents], keyed by its
 * index, and the host sends it when the action runs. `RemoteViews` hosts send it once it is
 * registered with `setOnClickPendingIntent(PendingIntentAction.FIRST_ACTION_ID + index, it)`.
 *
 * @param pendingIntent A lambda that returns the [PendingIntent] to trigger. The [Context] is
 *   provided to the lambda to ensure it's available during serialization.
 */
@Composable
public fun pendingIntentAction(pendingIntent: (Context) -> PendingIntent): Action {
    if (LocalInspectionMode.current) {
        // Use the dedicated empty action for IDE previews as a safe no-op.
        return Action.Empty
    }

    // Using a lambda defers PendingIntent access until serialization, preventing potential
    // crashes in environments like IDE previews where PendingIntent might not be available.
    val context = LocalContext.current
    return remember(context, pendingIntent) { PendingIntentAction({ pendingIntent(context) }) }
}

/**
 * Send the [PendingIntent] when invoked.
 *
 * How the action is written depends on where the document is played, because hosts outside androidx
 * dispatch it:
 * - Launcher widgets ([RcProfiles.PROFILE_WIDGETS], or the API level 6 widgets writer): an id
 *   action with id [FIRST_ACTION_ID] + index. This is the only host action `RemoteViews`
 *   dispatches, to the `PendingIntent` registered with `setOnClickPendingIntent` for that id.
 * - Every other profile, including Wear widgets: a named action [ACTION_NAME] with the index as an
 *   int value, the encoding existing hosts dispatch.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PendingIntentAction(public val pendingIntent: () -> PendingIntent) : RemoteAction() {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRemoteAction(): CreationAction {
        val writerCallback = document.writerCallback
        if (writerCallback !is WriterEvents) {
            error(
                "Could not store the pendingIntent, " +
                    "a WriterEvents is required for writing a PendingIntentAction."
            )
        }
        val index = writerCallback.storePendingIntent(pendingIntent())
        return if (usesIdActions()) {
            with(HostIdAction(FIRST_ACTION_ID + index)) { toRemoteAction() }
        } else {
            val valueId = document.addInteger(index)
            CreationHostAction(
                ACTION_NAME,
                HostAction.Type.INT.value,
                Utils.idFromLong(valueId).toInt(),
            )
        }
    }

    public companion object {
        /** The name of the named action used for [PendingIntent]s outside launcher widgets. */
        public const val ACTION_NAME: String = "SendPendingIntent"

        /**
         * The id of the id action for the [PendingIntent] at index 0, for launcher widgets. Chosen
         * to stay clear of explicit action and component ids, and of every id the writer allocates.
         */
        public const val FIRST_ACTION_ID: Int = 0x7E000000

        /** Whether [PendingIntentAction] is written as an id action in this scope. */
        internal fun RemoteStateScope.usesIdActions(): Boolean = isLauncherWidget()

        /**
         * Whether the document is captured for launcher widgets. [RcPlatformProfiles.WIDGETS_V6]
         * predates [RcProfiles.PROFILE_WIDGETS], so it is identified by its writer.
         */
        internal fun RemoteStateScope.isLauncherWidget(): Boolean =
            creationState.profile.operationsProfiles and RcProfiles.PROFILE_WIDGETS != 0 ||
                document is WidgetsProfileWriterV6

        /**
         * Returns the index of the [PendingIntent] that a host action triggers, or `null` if the
         * action is not a [PendingIntentAction].
         *
         * @param actionId The id of an id action.
         */
        public fun indexForActionId(actionId: Int): Int? =
            (actionId - FIRST_ACTION_ID).takeIf { actionId >= FIRST_ACTION_ID }

        /**
         * Returns the index of the [PendingIntent] that a named host action triggers, or `null` if
         * the action is not a [PendingIntentAction].
         *
         * @param name The name of a named action.
         * @param value The value of a named action.
         */
        public fun indexForNamedAction(name: String, value: Any?): Int? =
            if (name == ACTION_NAME) value as? Int else null
    }
}
