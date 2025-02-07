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
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.internal.CallIconStateListenerRemote
import androidx.core.telecom.internal.CapabilityExchangeRepository
import androidx.core.telecom.util.ExperimentalAppActions
import kotlin.collections.mutableSetOf
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Implementation of the [CallIconExtension] interface for managing and sharing call icon URIs with
 * remote listeners.
 *
 * This class handles granting URI permissions to remote packages, syncing the current icon URI with
 * new listeners, and managing the flow of URI updates.
 *
 * @param mContext The Android context.
 * @param mCoroutineContext The coroutine context to use for asynchronous operations.
 * @param mInitialCallIcon The initial call icon URI.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalAppActions::class)
internal class CallIconExtensionImpl(
    val mContext: Context,
    val mCoroutineContext: CoroutineContext,
    val mInitialCallIcon: Uri
) : CallIconExtension {

    companion object {
        /** The current version of the Call Icon Extension. */
        internal const val VERSION = 1

        /** The tag used for logging. */
        val TAG: String = CallIconExtensionImpl::class.java.simpleName
    }

    /** A state flow holding the current call icon URI. Emits [Uri.EMPTY] initially. */
    internal val mUriFlow = MutableStateFlow<Uri>(Uri.EMPTY)

    /**
     * Called when a capability exchange is started.
     *
     * @param callbacks The [CapabilityExchangeRepository] to register callbacks.
     * @return A [Capability] object representing the Call Icon Extension.
     */
    internal fun onExchangeStarted(callbacks: CapabilityExchangeRepository): Capability {
        callbacks.onCreateCallIconExtension = ::onCreateCallIconExtension
        return Capability().apply {
            featureId = Extensions.CALL_ICON
            featureVersion = VERSION
            supportedActions = IntArray(0)
        }
    }

    /**
     * Updates the call icon URI.
     *
     * This method updates the current icon URI and notifies remote listeners of the change.
     *
     * @param iconUri The new call icon URI.
     */
    override suspend fun updateCallIconUri(iconUri: Uri) {
        Log.d(TAG, "updateCallIconUri: updatedUri=$iconUri")
        if (mUriFlow.value == iconUri) {
            // some clients may keep the same URI but modify the bitmap, in this case,
            // the remote listeners need to implement a content observer and will be updated
            // via the content observer
            mContext.contentResolver.notifyChange(iconUri, null)
        }
        mUriFlow.emit(iconUri)
    }

    /**
     * Called when a new remote listener is created.
     *
     * This method adds the remote listener to the list, grants the listener permission to access
     * the current icon URI, and starts a flow to propagate URI updates to the listener.
     *
     * @param coroutineScope The coroutine scope to launch the flow in.
     * @param remoteActions The set of remote actions.
     * @param remoteName The package name of the remote listener.
     * @param binder The [CallIconStateListenerRemote] binder for communication.
     */
    private fun onCreateCallIconExtension(
        coroutineScope: CoroutineScope,
        remoteActions: Set<Int>,
        remoteName: String,
        binder: CallIconStateListenerRemote
    ) {
        Log.d(TAG, "onCreateCallIconExtension: actions=$remoteActions")
        val urisToRevoke = mutableSetOf<Uri>()

        // Sync the new remote listener with the initial icon.
        mContext.grantUriPermission(remoteName, mInitialCallIcon, FLAG_GRANT_READ_URI_PERMISSION)
        binder.updateCallIconUri(uri = mInitialCallIcon)

        mUriFlow
            .filter { it != Uri.EMPTY } // Ignore initial empty URI
            .onEach { newUri ->
                mContext.grantUriPermission(remoteName, newUri, FLAG_GRANT_READ_URI_PERMISSION)
                binder.updateCallIconUri(uri = newUri)
                urisToRevoke.add(newUri)
            }
            .onCompletion { // When the flow completes (e.g., listener disconnects)
                urisToRevoke.forEach { uriSent ->
                    mContext.revokeUriPermission(
                        remoteName,
                        uriSent,
                        FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            .launchIn(coroutineScope)
        binder.finishSync()
    }
}
