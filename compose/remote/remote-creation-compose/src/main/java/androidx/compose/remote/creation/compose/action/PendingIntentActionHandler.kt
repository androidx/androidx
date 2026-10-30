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

package androidx.compose.remote.creation.compose.action

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.collection.IntObjectMap

/**
 * Handles the host actions written by [rememberPendingIntentAction], by sending the matching
 * [PendingIntent] with its extras filled in.
 *
 * A host action either sends a [PendingIntent] or should be delivered to the host's own callback,
 * never both. Players forward every host action here first and only call their own callback when
 * this returns `false`:
 * ```
 * val handler = PendingIntentActionHandler(context, capturedDocument.pendingIntents)
 * player.addIdActionListener { id, metadata ->
 *     if (!handler.onAction(id, metadata)) onAction(id, metadata)
 * }
 * ```
 *
 * The embedded player does this for
 * [androidx.compose.remote.creation.compose.capture.CapturedDocument.pendingIntents].
 *
 * @param context The context used to send the [PendingIntent]s.
 * @param pendingIntents The [PendingIntent]s, keyed by index.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PendingIntentActionHandler(
    private val context: Context,
    private val pendingIntents: IntObjectMap<PendingIntent>,
) {
    /**
     * Sends the [PendingIntent] for the id action [id], with the extras from [metadata] filled in,
     * if [id] belongs to a [rememberPendingIntentAction].
     *
     * @param id The id of the action.
     * @param metadata The metadata delivered with the action.
     * @return `true` if a [PendingIntent] was sent, `false` if the action is for the host.
     */
    public fun onAction(id: Int, metadata: String?): Boolean {
        val pendingIntent =
            PendingIntentAction.indexForActionId(id)?.let { pendingIntents[it] } ?: return false
        send(pendingIntent, fillInIntent(metadata))
        return true
    }

    /**
     * Sends the [PendingIntent] for a named action, if it belongs to a
     * [rememberPendingIntentAction]. This is the encoding used for Wear widgets, which carries no
     * extras.
     *
     * @param name The name of the action.
     * @param value The value of the action.
     * @return `true` if a [PendingIntent] was sent, `false` if the action is for the host.
     */
    public fun onNamedAction(name: String, value: Any?): Boolean {
        val pendingIntent =
            PendingIntentAction.indexForNamedAction(name, value)?.let { pendingIntents[it] }
                ?: return false
        send(pendingIntent, Intent())
        return true
    }

    private fun send(pendingIntent: PendingIntent, fillIn: Intent) {
        if (Build.VERSION.SDK_INT >= 31 && pendingIntent.isImmutable && fillIn.extras != null) {
            Log.w(TAG, "PendingIntent is immutable, its extras will be dropped")
        }
        try {
            pendingIntent.send(context, 0, fillIn)
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "Failed to send PendingIntent", e)
        }
    }

    public companion object {
        private const val TAG = "PendingIntentAction"

        /**
         * Creates the fill-in [Intent] for the metadata of a [rememberPendingIntentAction] id
         * action, with each extra as a [String] extra.
         *
         * @param metadata The metadata delivered with the action.
         */
        public fun fillInIntent(metadata: String?): Intent =
            Intent().apply {
                for ((key, value) in PendingIntentAction.decodeExtras(metadata)) {
                    putExtra(key, value)
                }
            }
    }
}
