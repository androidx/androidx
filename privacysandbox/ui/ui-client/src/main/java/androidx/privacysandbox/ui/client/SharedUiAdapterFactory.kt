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

package androidx.privacysandbox.ui.client

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.IRemoteSharedUiSessionClient
import androidx.privacysandbox.ui.core.IRemoteSharedUiSessionController
import androidx.privacysandbox.ui.core.ISharedUiAdapter
import androidx.privacysandbox.ui.core.RemoteCallManager.addBinderDeathListener
import androidx.privacysandbox.ui.core.RemoteCallManager.closeRemoteSession
import androidx.privacysandbox.ui.core.RemoteCallManager.tryToCallRemoteObject
import androidx.privacysandbox.ui.core.SharedUiAdapter
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.Executor

/**
 * Provides an implementation of [SharedUiAdapter] created from a supplied Bundle which acts as a
 * proxy between the host app and the Binder provided by the UI provider.
 */
@SuppressLint("NullAnnotationGroup")
@ExperimentalFeatures.SharedUiPresentationApi
object SharedUiAdapterFactory {

    private const val TAG = "PrivacySandboxUiLib"
    // Bundle key is a binary compatibility requirement
    private const val SHARED_UI_ADAPTER_BINDER = "sharedUiAdapterBinder"
    private const val TEST_ONLY_USE_REMOTE_ADAPTER = "testOnlyUseRemoteAdapter"

    /**
     * Creates a [SharedUiAdapter] from a supplied [coreLibInfo] that acts as a proxy between the
     * host app and the Binder provided by the UI provider.
     *
     * @throws IllegalArgumentException if `coreLibInfo` does not contain a Binder corresponding to
     *   [SharedUiAdapter]
     */
    // TODO(b/365553832): add shim support to generate client proxy.
    @SuppressLint("NullAnnotationGroup")
    @ExperimentalFeatures.SharedUiPresentationApi
    fun createFromCoreLibInfo(coreLibInfo: Bundle): SharedUiAdapter {
        val uiAdapterBinder =
            requireNotNull(coreLibInfo.getBinder(SHARED_UI_ADAPTER_BINDER)) {
                "Invalid bundle, missing $SHARED_UI_ADAPTER_BINDER."
            }
        val adapterInterface = ISharedUiAdapter.Stub.asInterface(uiAdapterBinder)

        val forceUseRemoteAdapter = coreLibInfo.getBoolean(TEST_ONLY_USE_REMOTE_ADAPTER)
        val isLocalBinder = uiAdapterBinder.queryLocalInterface(ISharedUiAdapter.DESCRIPTOR) != null
        val useLocalAdapter = !forceUseRemoteAdapter && isLocalBinder
        Log.d(TAG, "useLocalAdapter=$useLocalAdapter")

        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !useLocalAdapter
        ) {
            RemoteAdapter(adapterInterface)
        } else {
            LocalAdapter(adapterInterface)
        }
    }

    /**
     * [LocalAdapter] communicates with a provider living on same process as the client but on a
     * different class loader.
     */
    @SuppressLint("BanUncheckedReflection") // using reflection on library classes
    private class LocalAdapter(adapterInterface: ISharedUiAdapter) : SharedUiAdapter {
        private val uiProviderBinder = adapterInterface.asBinder()

        private val targetSharedSessionClientClass =
            Class.forName(
                "androidx.privacysandbox.ui.core.SharedUiAdapter\$SessionClient",
                /* initialize = */ false,
                uiProviderBinder.javaClass.classLoader
            )

        // The adapterInterface provided must have a openSession method on its class.
        // Since the object itself has been instantiated on a different classloader, we
        // need reflection to get hold of it.
        private val openSessionMethod: Method =
            Class.forName(
                    "androidx.privacysandbox.ui.core.SharedUiAdapter",
                    /* initialize = */ false,
                    uiProviderBinder.javaClass.classLoader
                )
                .getMethod("openSession", Executor::class.java, targetSharedSessionClientClass)

        override fun openSession(clientExecutor: Executor, client: SharedUiAdapter.SessionClient) {
            try {
                val sessionClientProxy =
                    Proxy.newProxyInstance(
                        uiProviderBinder.javaClass.classLoader,
                        arrayOf(targetSharedSessionClientClass),
                        SessionClientProxyHandler(client)
                    )
                openSessionMethod.invoke(uiProviderBinder, clientExecutor, sessionClientProxy)
            } catch (exception: Throwable) {
                client.onSessionError(exception)
            }
        }

        private class SessionClientProxyHandler(
            private val origClient: SharedUiAdapter.SessionClient
        ) : InvocationHandler {
            override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any {
                return when (method.name) {
                    "onSessionOpened" -> {
                        // We have to forward the call to original client, but it won't
                        // recognize Session class on targetClassLoader. We need proxy for it
                        // on local ClassLoader.
                        args!! // This method will always have an argument, so safe to !!
                        origClient.onSessionOpened(SessionProxy(args[0]))
                    }
                    "onSessionError" -> {
                        args!! // This method will always have an argument, so safe to !!
                        val throwable = args[0] as Throwable
                        origClient.onSessionError(throwable)
                    }
                    "toString" -> origClient.toString()
                    "equals" -> proxy === args?.get(0)
                    "hashCode" -> hashCode()
                    else -> {
                        throw UnsupportedOperationException(
                            "Unexpected method call object:$proxy, method: $method, args: $args"
                        )
                    }
                }
            }
        }

        /** Create [SharedUiAdapter.Session] that proxies to [origSession] */
        private class SessionProxy(private val origSession: Any) : SharedUiAdapter.Session {
            private val targetClass =
                Class.forName(
                        "androidx.privacysandbox.ui.core.SharedUiAdapter\$Session",
                        /* initialize = */ false,
                        origSession.javaClass.classLoader
                    )
                    .also { it.cast(origSession) }

            private val closeMethod = targetClass.getMethod("close")

            override fun close() {
                closeMethod.invoke(origSession)
            }
        }
    }

    /**
     * [RemoteAdapter] maintains a shared session with a UI provider living in a different process.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private class RemoteAdapter(private val adapterInterface: ISharedUiAdapter) : SharedUiAdapter {
        override fun openSession(clientExecutor: Executor, client: SharedUiAdapter.SessionClient) {
            tryToCallRemoteObject(adapterInterface) {
                this.openRemoteSession(RemoteSharedUiSessionClient(client, clientExecutor))
            }
        }

        class RemoteSharedUiSessionClient(
            val client: SharedUiAdapter.SessionClient,
            val clientExecutor: Executor
        ) : IRemoteSharedUiSessionClient.Stub() {
            override fun onRemoteSessionOpened(
                remoteSessionController: IRemoteSharedUiSessionController
            ) {
                clientExecutor.execute {
                    client.onSessionOpened(SessionImpl(remoteSessionController))
                }
                addBinderDeathListener(remoteSessionController) {
                    onRemoteSessionError("Remote process died")
                }
            }

            override fun onRemoteSessionError(errorString: String) {
                clientExecutor.execute { client.onSessionError(Throwable(errorString)) }
            }

            private class SessionImpl(
                val remoteSessionController: IRemoteSharedUiSessionController
            ) : SharedUiAdapter.Session {
                override fun close() {
                    closeRemoteSession(remoteSessionController)
                }
            }
        }
    }
}
