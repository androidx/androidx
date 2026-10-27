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
import androidx.compose.remote.core.RcPlatformServices.LogCategory
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.actions.Action as CreationAction
import androidx.compose.remote.creation.actions.HostAction as CreationHostAction
import androidx.compose.remote.creation.compose.capture.WriterEvents
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.creationState
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.WidgetsProfileWriterV6
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastJoinToString

/**
 * Creates an [Action] that sends a [PendingIntent], filling in [extras] when the action runs.
 *
 * The [PendingIntent] is not serialized into the document. It is stored in
 * [androidx.compose.remote.creation.compose.capture.CapturedDocument.pendingIntents], keyed by its
 * index, and the host sends it when the action runs: the embedded player through
 * [PendingIntentActionHandler], and `RemoteViews` hosts once it is registered with
 * `setOnClickPendingIntent(PendingIntentAction.FIRST_ACTION_ID + index, it)`.
 *
 * [extras] are for values that are only known during playback, such as remote state. Each one is
 * added to the sent [android.content.Intent] as a [String] extra. Put constant values in the
 * [android.content.Intent] of the [PendingIntent] instead, where every host delivers them; constant
 * entries in [extras] are ignored. Extras are only delivered when:
 * - the [PendingIntent] is created with [PendingIntent.FLAG_MUTABLE],
 * - the host fills them in, as [PendingIntentActionHandler] does, and
 * - the document is not captured for Wear widgets, whose hosts don't support them.
 *
 * Otherwise the [PendingIntent] is sent without them, so receivers should handle a missing key.
 * Don't also put these keys in the [android.content.Intent]: filled in extras never replace extras
 * the [android.content.Intent] already has (see [android.content.Intent.fillIn]).
 *
 * @param extras The [String] extras to fill in, as key to value pairs.
 * @param pendingIntent A lambda that returns the [PendingIntent] to trigger. The [Context] is
 *   provided to the lambda to ensure it's available during serialization.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun pendingIntentAction(
    vararg extras: Pair<String, RemoteString>,
    pendingIntent: (Context) -> PendingIntent,
): Action = rememberPendingIntentAction(extras.toList(), pendingIntent)

/**
 * Creates an [Action] that triggers a [PendingIntent].
 *
 * The [PendingIntent] is not serialized into the document. It is stored in
 * [androidx.compose.remote.creation.compose.capture.CapturedDocument.pendingIntents], keyed by its
 * index, and the host sends it when the action runs.
 *
 * @param pendingIntent A lambda that returns the [PendingIntent] to trigger. The [Context] is
 *   provided to the lambda to ensure it's available during serialization.
 */
@Composable
public fun pendingIntentAction(pendingIntent: (Context) -> PendingIntent): Action =
    rememberPendingIntentAction(emptyList(), pendingIntent)

@Composable
private fun rememberPendingIntentAction(
    extras: List<Pair<String, RemoteString>>,
    pendingIntent: (Context) -> PendingIntent,
): Action {
    if (LocalInspectionMode.current) {
        // Use the dedicated empty action for IDE previews as a safe no-op.
        return Action.Empty
    }

    // Using a lambda defers PendingIntent access until serialization, preventing potential
    // crashes in environments like IDE previews where PendingIntent might not be available.
    val context = LocalContext.current
    return remember(context, pendingIntent, extras) {
        PendingIntentAction({ pendingIntent(context) }, extras)
    }
}

/**
 * Send the [PendingIntent] when invoked.
 *
 * How the action is written depends on where the document is played, because hosts outside androidx
 * dispatch it:
 * - Launcher widgets ([RcProfiles.PROFILE_WIDGETS], or the API level 6 widgets writer): an id
 *   action with id [FIRST_ACTION_ID] + index. This is the only host action `RemoteViews`
 *   dispatches, to the `PendingIntent` registered with `setOnClickPendingIntent` for that id.
 * - With dynamic [extras], outside Wear widgets: the same id action, carrying the extras as its
 *   metadata, for players that use [PendingIntentActionHandler].
 * - Otherwise, including Wear widgets: a named action [ACTION_NAME] with the index as an int value,
 *   the encoding existing hosts dispatch. It cannot carry extras.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PendingIntentAction(
    public val pendingIntent: () -> PendingIntent,
    public val extras: List<Pair<String, RemoteString>> = emptyList(),
) : RemoteAction() {

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
        val dynamicExtras = dynamicExtras()
        val isWearWidget =
            creationState.profile.operationsProfiles and RcProfiles.PROFILE_WEAR_WIDGETS != 0
        return if (isLauncherWidget() || (dynamicExtras.isNotEmpty() && !isWearWidget)) {
            with(HostIdAction(FIRST_ACTION_ID + index, encodeExtras(dynamicExtras))) {
                toRemoteAction()
            }
        } else {
            if (dynamicExtras.isNotEmpty()) {
                creationState.platform.log(
                    LogCategory.WARN,
                    "Wear widget hosts don't support PendingIntent extras, ignoring them",
                )
            }
            val valueId = document.addInteger(index)
            CreationHostAction(
                ACTION_NAME,
                HostAction.Type.INT.value,
                Utils.idFromLong(valueId).toInt(),
            )
        }
    }

    /** The [extras] to encode: those with a valid key and a value only known during playback. */
    private fun RemoteStateScope.dynamicExtras(): List<Pair<String, RemoteString>> =
        extras.fastFilter { (key, value) ->
            when {
                key.isEmpty() || key.any { it in SEPARATORS } -> {
                    creationState.platform.log(
                        LogCategory.WARN,
                        "Ignoring PendingIntent extra with invalid key '$key'",
                    )
                    false
                }
                value.constantValueOrNull != null -> {
                    creationState.platform.log(
                        LogCategory.WARN,
                        "Ignoring constant PendingIntent extra '$key', put it in the Intent",
                    )
                    false
                }
                else -> true
            }
        }

    public companion object {
        // ASCII control characters, not expected in extra keys or values.
        private const val KEY_SEPARATOR = '\u001F'
        private const val VALUE_SEPARATOR = '\u001E'
        private const val HEADER_SEPARATOR = '\u001D'
        private const val SEPARATORS = "$KEY_SEPARATOR$VALUE_SEPARATOR$HEADER_SEPARATOR"

        /**
         * Encodes [extras] as `key1 US key2 GS value1 RS value2`, or returns `null` when there are
         * none.
         *
         * The keys form a constant header, so the keys that [decodeExtras] returns are always the
         * ones written here. Values are dynamic and cannot be escaped during playback, so a value
         * that contains [VALUE_SEPARATOR] shifts the values after it to later keys; the last key
         * then receives the remainder. That is accepted: it can only mix up values between declared
         * keys, never add a key.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun encodeExtras(extras: List<Pair<String, RemoteString>>): RemoteString? {
            if (extras.isEmpty()) return null
            val header = extras.fastJoinToString("$KEY_SEPARATOR") { it.first } + HEADER_SEPARATOR
            var result: RemoteString = header.rs
            extras.fastForEachIndexed { index, (_, value) ->
                result = if (index == 0) result + value else result + "$VALUE_SEPARATOR".rs + value
            }
            return result
        }

        /**
         * Decodes metadata written by [encodeExtras]. Never throws: malformed metadata decodes to
         * fewer, or no, entries.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun decodeExtras(metadata: String?): Map<String, String> {
            if (metadata == null) return emptyMap()
            // The header is constant and its keys cannot contain HEADER_SEPARATOR, so the first
            // one always ends it.
            val headerEnd = metadata.indexOf(HEADER_SEPARATOR)
            if (headerEnd <= 0) return emptyMap()
            val keys = metadata.substring(0, headerEnd).split(KEY_SEPARATOR)
            val values = metadata.substring(headerEnd + 1).split(VALUE_SEPARATOR, limit = keys.size)
            val result = LinkedHashMap<String, String>()
            for (i in keys.indices) {
                if (i < values.size && keys[i].isNotEmpty()) {
                    result.putIfAbsent(keys[i], values[i])
                }
            }
            return result
        }

        /** The name of the named action used for [PendingIntent]s without extras. */
        public const val ACTION_NAME: String = "SendPendingIntent"

        /**
         * The id of the id action for the [PendingIntent] at index 0. Chosen to stay clear of
         * explicit action and component ids, and of every id the writer allocates.
         */
        public const val FIRST_ACTION_ID: Int = 0x7E000000

        /**
         * Whether the document is captured for launcher widgets.
         * [androidx.compose.remote.creation.profile.RcPlatformProfiles.WIDGETS_V6] predates
         * [RcProfiles.PROFILE_WIDGETS], so it is identified by its writer.
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
