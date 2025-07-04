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
@file:JvmName("SharedUiAdapterProxy")

package androidx.privacysandbox.ui.provider

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.core.ClientAdapterWrapper
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.IRemoteSharedUiSessionClient
import androidx.privacysandbox.ui.core.IRemoteSharedUiSessionController
import androidx.privacysandbox.ui.core.ISharedUiAdapter
import androidx.privacysandbox.ui.core.LocalSharedUiAdapter
import androidx.privacysandbox.ui.core.ProtocolConstants
import androidx.privacysandbox.ui.core.SdkRuntimeUiLibVersions
import androidx.privacysandbox.ui.core.SharedUiAdapter
import androidx.privacysandbox.ui.core.SharedUiAdapter.SessionClient
import java.util.concurrent.Executor

/**
 * Provides a [Bundle] containing a Binder which represents a [SharedUiAdapter]. The Bundle is sent
 * to the client in order for the [SharedUiAdapter] to be used to maintain a connection with a UI
 * provider.
 */
@SuppressLint("NullAnnotationGroup")
@ExperimentalFeatures.SharedUiPresentationApi
public fun SharedUiAdapter.toCoreLibInfo(): Bundle {
    // If the ui adapter has already been wrapped as a client SharedUiAdapter
    // at some point it needs no further wrapping
    if (this is ClientAdapterWrapper) {
        return this.getSourceBundle()
    }
    val binderAdapter = BinderSharedUiAdapterDelegate(this)

    val bundle = Bundle()
    bundle.putInt(
        ProtocolConstants.uiProviderVersionKey,
        SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel,
    )

    // Bundle key is a binary compatibility requirement
    bundle.putBinder(ProtocolConstants.sharedUiAdapterBinderKey, binderAdapter)
    return bundle
}

@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
private class BinderSharedUiAdapterDelegate(private val adapter: SharedUiAdapter) :
    ISharedUiAdapter.Stub(), LocalSharedUiAdapter {

    override fun openLocalSession(
        clientVersion: Int,
        clientExecutor: Executor,
        client: SessionClient,
    ) {
        adapter.openSession(clientExecutor, LocalSharedUiSessionClient(clientVersion, client))
    }

    // TODO(b/365614954): try to improve method's performance.
    override fun openRemoteSession(
        clientVersion: Int,
        remoteSessionClient: IRemoteSharedUiSessionClient,
    ) {
        val remoteSessionClientWithVersionCheck =
            RemoteSharedUiSessionClient(clientVersion, remoteSessionClient)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            remoteSessionClientWithVersionCheck.onRemoteSessionError(
                "openRemoteSession() requires API34+"
            )
            return
        }

        try {
            val sessionClient = SessionClientProxy(remoteSessionClientWithVersionCheck)
            adapter.openSession(Runnable::run, sessionClient)
        } catch (exception: Throwable) {
            remoteSessionClientWithVersionCheck.onRemoteSessionError(exception.message)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private class SessionClientProxy(
        private val remoteSessionClient:
            androidx.privacysandbox.ui.provider.IRemoteSharedUiSessionClient
    ) : SharedUiAdapter.SessionClient {
        override fun onSessionOpened(session: SharedUiAdapter.Session) {
            remoteSessionClient.onRemoteSessionOpened(RemoteSharedUiSessionController(session))
        }

        override fun onSessionError(throwable: Throwable) {
            remoteSessionClient.onRemoteSessionError(throwable.message)
        }

        private class RemoteSharedUiSessionController(val session: SharedUiAdapter.Session) :
            IRemoteSharedUiSessionController.Stub() {
            override fun close() {
                session.close()
            }
        }
    }
}
