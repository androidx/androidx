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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.action

import android.app.PendingIntent
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.PendingIntentAwareWriter
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.runtime.Composable

/** Create a [PendingIntentAction] to send the [PendingIntent] when invoked. */
@Composable
public fun pendingIntentAction(pendingIntent: PendingIntent): PendingIntentAction =
    PendingIntentAction(LocalRemoteComposeCreationState.current, pendingIntent)

/** Send the [PendingIntent] when invoked. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PendingIntentAction(
    public val remoteComposeCreationState: RemoteComposeCreationState,
    public val pendingIntent: PendingIntent,
) : Action {

    override fun toRemoteAction(): androidx.compose.remote.creation.actions.Action {
        val writer = remoteComposeCreationState.document
        if (writer is PendingIntentAwareWriter) {
            val index = writer.storePendingIntent(pendingIntent)
            val valueId = remoteComposeCreationState.document.addInteger(index)
            return androidx.compose.remote.creation.actions.HostAction(
                ACTION_NAME,
                HostAction.Type.INT.ordinal,
                Utils.idFromLong(valueId).toInt(),
            )
        } else {
            error(
                "Could not store the pendingIntent, " +
                    "a PendingIntentAwareWriter is required for writing a PendingIntentAction."
            )
        }
    }

    @Composable override fun toComposeUiAction(): () -> Unit = {}

    public companion object {
        public const val ACTION_NAME: String = "SendPendingIntent"
    }
}
