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

package androidx.core.telecom.extensions

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.telecom.internal.CallIconStateListener
import androidx.core.telecom.internal.CapabilityExchangeListenerRemote
import androidx.core.telecom.util.ExperimentalAppActions
import kotlin.coroutines.resume
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Remote implementation of the [CallIconExtensionRemote] interface.
 *
 * This class handles communication with the remote call icon extension, receiving updates to the
 * call icon URI and providing them to the application through a flow.
 *
 * @param context The Android context.
 * @param callScope The coroutine scope for launching flows.
 */
@ExperimentalAppActions
internal class CallIconExtensionRemoteImpl(
    private val context: Context,
    private val callScope: CoroutineScope,
    private val onCallIconChanged: suspend (Uri) -> Unit
) : CallIconExtensionRemote {

    /** Indicates whether the remote call icon extension is supported. */
    override var isSupported by Delegates.notNull<Boolean>()

    companion object {
        /** The tag used for logging. */
        val TAG: String = CallIconExtensionRemoteImpl::class.java.simpleName
    }

    /** Returns an empty array of actions, as no actions are supported. */
    internal val actions
        get() = IntArray(0) // No actions supported

    /**
     * Called when the capability exchange is complete.
     *
     * This method determines whether the remote extension is supported based on the negotiated
     * capability and connects to the remote actions interface if supported.
     *
     * @param negotiatedCapability The negotiated capability.
     * @param remote The remote capability exchange listener.
     */
    internal suspend fun onExchangeComplete(
        negotiatedCapability: Capability?,
        remote: CapabilityExchangeListenerRemote?
    ) {
        if (negotiatedCapability == null || remote == null) {
            Log.i(TAG, "onNegotiated: remote is not capable")
            isSupported = false
            return
        }

        isSupported = true
        connectToRemote(negotiatedCapability, remote)
    }

    /**
     * Connects to the remote call icon actions interface.
     *
     * This method establishes communication with the remote extension using the provided capability
     * and listener. It sets up a listener to receive call icon URI updates and resumes the
     * continuation with the remote actions interface.
     *
     * @param negotiatedCapability The negotiated capability.
     * @param remote The remote capability exchange listener.
     */
    private suspend fun connectToRemote(
        negotiatedCapability: Capability,
        remote: CapabilityExchangeListenerRemote
    ): Unit = suspendCancellableCoroutine { continuation ->
        val stateListener =
            CallIconStateListener(
                callIconUriUpdater = {
                    callScope.launch {
                        // Called when the remote extension updates the URI.
                        onCallIconChanged(it)
                    }
                },
                finishSync = { callScope.launch { continuation.resume(Unit) } }
            )
        remote.onCreateCallIconExtension(
            negotiatedCapability.featureVersion,
            negotiatedCapability.supportedActions,
            context.packageName,
            stateListener
        )
    }
}
