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
@file:OptIn(ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.creation.compose.action

import android.app.PendingIntent
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.actions.Action as CreationAction
import androidx.compose.remote.creation.actions.HostAction as CreationHostAction
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.capture.WriterEvents
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.runtime.Composable

/** Create a [PendingIntentAction] to send the [PendingIntent] when invoked. */
@Composable
public fun pendingIntentAction(pendingIntent: PendingIntent): Action =
    PendingIntentAction(pendingIntent)

/** Send the [PendingIntent] when invoked. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PendingIntentAction(public val pendingIntent: PendingIntent) : Action {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRemoteAction(): CreationAction {
        val writerCallback = document.writerCallback
        if (writerCallback is WriterEvents) {
            val index = writerCallback.storePendingIntent(pendingIntent)
            val valueId = document.addInteger(index)
            return CreationHostAction(
                ACTION_NAME,
                HostAction.Type.INT.ordinal,
                Utils.idFromLong(valueId).toInt(),
            )
        } else {
            error(
                "Could not store the pendingIntent, " +
                    "a WriterEvents is required for writing a PendingIntentAction."
            )
        }
    }

    public companion object {
        public const val ACTION_NAME: String = "SendPendingIntent"
    }
}
