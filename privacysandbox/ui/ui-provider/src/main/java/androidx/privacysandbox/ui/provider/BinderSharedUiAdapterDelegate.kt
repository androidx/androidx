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
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.IRemoteSharedUiSessionClient
import androidx.privacysandbox.ui.core.IRemoteSharedUiSessionController
import androidx.privacysandbox.ui.core.ISharedUiAdapter
import androidx.privacysandbox.ui.core.RemoteCallManager.tryToCallRemoteObject
import androidx.privacysandbox.ui.core.SharedUiAdapter
import java.util.concurrent.Executor

/**
 * Provides a [Bundle] containing a Binder which represents a [SharedUiAdapter]. The Bundle is sent
 * to the client in order for the [SharedUiAdapter] to be used to maintain a connection with a UI
 * provider.
 */
@SuppressLint("NullAnnotationGroup")
@ExperimentalFeatures.SharedUiPresentationApi
fun SharedUiAdapter.toCoreLibInfo(): Bundle {
    val binderAdapter = BinderSharedUiAdapterDelegate(this)
    // TODO(b/350445624): Add version info
    val bundle = Bundle()

    // Bundle key is a binary compatibility requirement
    bundle.putBinder("sharedUiAdapterBinder", binderAdapter)
    return bundle
}

@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
private class BinderSharedUiAdapterDelegate(private val adapter: SharedUiAdapter) :
    ISharedUiAdapter.Stub(), SharedUiAdapter {

    override fun openSession(clientExecutor: Executor, client: SharedUiAdapter.SessionClient) {
        adapter.openSession(clientExecutor, client)
    }

    // TODO(b/365614954): try to improve method's performance.
    override fun openRemoteSession(remoteSessionClient: IRemoteSharedUiSessionClient) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            tryToCallRemoteObject(remoteSessionClient) {
                onRemoteSessionError("openRemoteSession() requires API34+")
            }
            return
        }

        try {
            val sessionClient = SessionClientProxy(remoteSessionClient)
            openSession(Runnable::run, sessionClient)
        } catch (exception: Throwable) {
            tryToCallRemoteObject(remoteSessionClient) { onRemoteSessionError(exception.message) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private class SessionClientProxy(
        private val remoteSessionClient: IRemoteSharedUiSessionClient
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
