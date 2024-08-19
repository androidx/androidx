/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.client

import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ui.core.IRemoteSessionController

/** Utility class for remote objects called by the UI library adapter factories. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object RemoteCallManager {

    const val TAG = "PrivacySandboxUiLib"

    fun addBinderDeathListener(
        remoteSessionController: IRemoteSessionController,
        recipient: IBinder.DeathRecipient
    ) {
        tryToCallRemoteObject(remoteSessionController) { this.asBinder().linkToDeath(recipient, 0) }
    }

    fun closeRemoteSession(remoteSessionController: IRemoteSessionController) {
        tryToCallRemoteObject(remoteSessionController) { close() }
    }

    /** Tries to call the remote object and handles exceptions if the remote object has died. */
    inline fun <RemoteObject> tryToCallRemoteObject(
        remoteObject: RemoteObject,
        function: RemoteObject.() -> Unit
    ) {
        try {
            remoteObject.function()
        } catch (e: RemoteException) {
            Log.e(TAG, "Calling remote object failed: $e")
        }
    }
}
