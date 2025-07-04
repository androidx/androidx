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
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.IRemoteSharedUiSessionClient
import androidx.privacysandbox.ui.core.IRemoteSharedUiSessionController
import androidx.privacysandbox.ui.core.ISharedUiAdapter
import androidx.privacysandbox.ui.core.ProtocolConstants
import androidx.privacysandbox.ui.core.RemoteCallManager.addBinderDeathListener
import androidx.privacysandbox.ui.core.RemoteCallManager.tryToCallRemoteObject
import androidx.privacysandbox.ui.core.SdkRuntimeUiLibVersions
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
public object SharedUiAdapterFactory {

    private val uiAdapterFactoryDelegate =
        object : UiAdapterFactoryDelegate() {
            override val uiAdapterBinderKey: String = ProtocolConstants.sharedUiAdapterBinderKey
            override val adapterDescriptor: String = ISharedUiAdapter.DESCRIPTOR
        }

    /**
     * Creates a [SharedUiAdapter] from a supplied [coreLibInfo] that acts as a proxy between the
     * host app and the Binder provided by the UI provider.
     *
     * @throws IllegalArgumentException if `coreLibInfo` does not contain a Binder corresponding to
     *   [SharedUiAdapter]
     */
    @SuppressLint("NullAnnotationGroup")
    @ExperimentalFeatures.SharedUiPresentationApi
    public fun createFromCoreLibInfo(coreLibInfo: Bundle): SharedUiAdapter {
        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                !uiAdapterFactoryDelegate.shouldUseLocalAdapter(coreLibInfo)
        ) {
            RemoteAdapter(coreLibInfo)
        } else {
            LocalAdapter(coreLibInfo)
        }
    }

    /**
     * [LocalAdapter] communicates with a provider living on same process as the client but on a
     * different class loader. We should also perform ui-provider version check before calling any
     * newly introduced api / modified api.
     */
    @SuppressLint("BanUncheckedReflection") // using reflection on library classes
    private class LocalAdapter(adapterBundle: Bundle) :
        SharedUiAdapter, ClientAdapter(adapterBundle) {
        private val uiProviderBinder =
            uiAdapterFactoryDelegate.requireNotNullAdapterBinder(adapterBundle)
        private val uiProviderVersion =
            uiAdapterFactoryDelegate.requireNotNullUiProviderVersion(adapterBundle)

        private val targetSharedSessionClientClass =
            Class.forName(
                "androidx.privacysandbox.ui.core.SharedUiAdapter\$SessionClient",
                /* initialize = */ false,
                uiProviderBinder.javaClass.classLoader,
            )

        // The adapterInterface provided must have a openSession method on its class.
        // Since the object itself has been instantiated on a different classloader, we
        // need reflection to get hold of it.
        private val openSessionMethod: Method =
            Class.forName(
                    "androidx.privacysandbox.ui.core.LocalSharedUiAdapter",
                    /* initialize = */ false,
                    uiProviderBinder.javaClass.classLoader,
                )
                .getMethod(
                    "openLocalSession",
                    Int::class.java,
                    Executor::class.java,
                    targetSharedSessionClientClass,
                )

        override fun openSession(clientExecutor: Executor, client: SharedUiAdapter.SessionClient) {
            try {
                val sessionClientProxy =
                    Proxy.newProxyInstance(
                        uiProviderBinder.javaClass.classLoader,
                        arrayOf(targetSharedSessionClientClass),
                        SessionClientProxyHandler(uiProviderVersion, client),
                    )
                openSessionMethod.invoke(
                    uiProviderBinder,
                    SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel,
                    clientExecutor,
                    sessionClientProxy,
                )
            } catch (exception: Throwable) {
                client.onSessionError(exception)
            }
        }

        private class SessionClientProxyHandler(
            private val uiProviderVersion: Int,
            private val origClient: SharedUiAdapter.SessionClient,
        ) : InvocationHandler {
            override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any {
                return when (method.name) {
                    "onSessionOpened" -> {
                        // We have to forward the call to original client, but it won't
                        // recognize Session class on targetClassLoader. We need proxy for it
                        // on local ClassLoader.
                        args!! // This method will always have an argument, so safe to !!
                        origClient.onSessionOpened(SharedUiSessionProxy(uiProviderVersion, args[0]))
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
    }

    /**
     * [RemoteAdapter] maintains a shared session with a UI provider living in a different process.
     * We should also perform ui-provider version check before calling any newly introduced api /
     * modified api.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private class RemoteAdapter(adapterBundle: Bundle) :
        SharedUiAdapter, ClientAdapter(adapterBundle) {
        val uiAdapterBinder = uiAdapterFactoryDelegate.requireNotNullAdapterBinder(adapterBundle)
        val adapterInterface: ISharedUiAdapter = ISharedUiAdapter.Stub.asInterface(uiAdapterBinder)
        val uiProviderVersion =
            uiAdapterFactoryDelegate.requireNotNullUiProviderVersion(adapterBundle)

        override fun openSession(clientExecutor: Executor, client: SharedUiAdapter.SessionClient) {
            tryToCallRemoteObject(adapterInterface) {
                this.openRemoteSession(
                    SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel,
                    RemoteSharedUiSessionClient(uiProviderVersion, client, clientExecutor),
                )
            }
        }

        class RemoteSharedUiSessionClient(
            private val uiProviderVersion: Int,
            private val client: SharedUiAdapter.SessionClient,
            private val clientExecutor: Executor,
        ) : IRemoteSharedUiSessionClient.Stub() {
            override fun onRemoteSessionOpened(
                remoteSessionController: IRemoteSharedUiSessionController
            ) {
                val remoteSessionControllerWithVersionCheck =
                    RemoteSharedUiSessionController(uiProviderVersion, remoteSessionController)
                clientExecutor.execute {
                    client.onSessionOpened(SessionImpl(remoteSessionControllerWithVersionCheck))
                }
                addBinderDeathListener(remoteSessionController) {
                    onRemoteSessionError("Remote process died")
                }
            }

            override fun onRemoteSessionError(errorString: String) {
                clientExecutor.execute { client.onSessionError(Throwable(errorString)) }
            }

            private class SessionImpl(
                val remoteSessionController:
                    androidx.privacysandbox.ui.client.IRemoteSharedUiSessionController
            ) : SharedUiAdapter.Session {
                override fun close() {
                    remoteSessionController.close()
                }
            }
        }
    }
}
