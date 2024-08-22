/*
 * Copyright 2022 The Android Open Source Project
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
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Display
import android.view.SurfaceControlViewHost
import android.view.SurfaceView
import android.view.View
import android.window.SurfaceSyncGroup
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.client.RemoteCallManager.addBinderDeathListener
import androidx.privacysandbox.ui.client.RemoteCallManager.closeRemoteSession
import androidx.privacysandbox.ui.client.RemoteCallManager.tryToCallRemoteObject
import androidx.privacysandbox.ui.core.IRemoteSessionClient
import androidx.privacysandbox.ui.core.IRemoteSessionController
import androidx.privacysandbox.ui.core.ISandboxedUiAdapter
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionObserverFactory
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.Executor

/**
 * Provides an adapter created from a supplied Bundle which acts as a proxy between the host app and
 * the Binder provided by the provider of content.
 */
object SandboxedUiAdapterFactory {

    private const val TAG = "PrivacySandboxUiLib"
    // Bundle key is a binary compatibility requirement
    private const val UI_ADAPTER_BINDER = "uiAdapterBinder"
    private const val TEST_ONLY_USE_REMOTE_ADAPTER = "testOnlyUseRemoteAdapter"

    /**
     * @throws IllegalArgumentException if {@code coreLibInfo} does not contain a Binder with the
     *   key UI_ADAPTER_BINDER
     */
    fun createFromCoreLibInfo(coreLibInfo: Bundle): SandboxedUiAdapter {
        val uiAdapterBinder =
            requireNotNull(coreLibInfo.getBinder(UI_ADAPTER_BINDER)) {
                "Invalid bundle, missing $UI_ADAPTER_BINDER."
            }
        val adapterInterface = ISandboxedUiAdapter.Stub.asInterface(uiAdapterBinder)

        val forceUseRemoteAdapter = coreLibInfo.getBoolean(TEST_ONLY_USE_REMOTE_ADAPTER)
        val isLocalBinder =
            uiAdapterBinder.queryLocalInterface(ISandboxedUiAdapter.DESCRIPTOR) != null
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
     * [LocalAdapter] fetches UI from a provider living on same process as the client but on a
     * different class loader.
     */
    @SuppressLint("BanUncheckedReflection") // using reflection on library classes
    private class LocalAdapter(adapterInterface: ISandboxedUiAdapter) : SandboxedUiAdapter {
        private val uiProviderBinder = adapterInterface.asBinder()

        private val targetSessionClientClass =
            Class.forName(
                SandboxedUiAdapter.SessionClient::class.java.name,
                /* initialize = */ false,
                uiProviderBinder.javaClass.classLoader
            )

        // The adapterInterface provided must have a openSession method on its class.
        // Since the object itself has been instantiated on a different classloader, we
        // need reflection to get hold of it.
        private val openSessionMethod: Method =
            Class.forName(
                    SandboxedUiAdapter::class.java.name,
                    /*initialize=*/ false,
                    uiProviderBinder.javaClass.classLoader
                )
                .getMethod(
                    "openSession",
                    Context::class.java,
                    IBinder::class.java,
                    Int::class.java,
                    Int::class.java,
                    Boolean::class.java,
                    Executor::class.java,
                    targetSessionClientClass
                )

        override fun openSession(
            context: Context,
            windowInputToken: IBinder,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient
        ) {
            try {
                // We can't pass the client object as-is since it's been created on a different
                // classloader.
                val sessionClientProxy =
                    Proxy.newProxyInstance(
                        uiProviderBinder.javaClass.classLoader,
                        arrayOf(targetSessionClientClass),
                        SessionClientProxyHandler(client)
                    )
                openSessionMethod.invoke(
                    uiProviderBinder,
                    context,
                    windowInputToken,
                    initialWidth,
                    initialHeight,
                    isZOrderOnTop,
                    clientExecutor,
                    sessionClientProxy
                )
            } catch (exception: Throwable) {
                client.onSessionError(exception)
            }
        }

        override fun addObserverFactory(sessionObserverFactory: SessionObserverFactory) {}

        override fun removeObserverFactory(sessionObserverFactory: SessionObserverFactory) {}

        private class SessionClientProxyHandler(
            private val origClient: SandboxedUiAdapter.SessionClient,
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
                    "onResizeRequested" -> {
                        args!! // This method will always have an argument, so safe to !!
                        val width = args[0] as Int
                        val height = args[1] as Int
                        origClient.onResizeRequested(width, height)
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

        /** Create [SandboxedUiAdapter.Session] that proxies to [origSession] */
        private class SessionProxy(
            private val origSession: Any,
        ) : SandboxedUiAdapter.Session {

            private val targetClass =
                Class.forName(
                        SandboxedUiAdapter.Session::class.java.name,
                        /* initialize = */ false,
                        origSession.javaClass.classLoader
                    )
                    .also { it.cast(origSession) }

            private val getViewMethod = targetClass.getMethod("getView")
            private val notifyResizedMethod =
                targetClass.getMethod("notifyResized", Int::class.java, Int::class.java)
            private val getSignalOptionsMethod = targetClass.getMethod("getSignalOptions")
            private val notifyZOrderChangedMethod =
                targetClass.getMethod("notifyZOrderChanged", Boolean::class.java)
            private val notifyConfigurationChangedMethod =
                targetClass.getMethod("notifyConfigurationChanged", Configuration::class.java)
            private val notifyUiChangedMethod =
                targetClass.getMethod("notifyUiChanged", Bundle::class.java)
            private val closeMethod = targetClass.getMethod("close")

            override val view: View
                get() = getViewMethod.invoke(origSession) as View

            override val signalOptions: Set<String>
                @Suppress("UNCHECKED_CAST") // using reflection on library classes
                get() = getSignalOptionsMethod.invoke(origSession) as Set<String>

            override fun notifyResized(width: Int, height: Int) {
                view.layout(0, 0, width, height)
                notifyResizedMethod.invoke(origSession, width, height)
            }

            override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
                notifyZOrderChangedMethod.invoke(origSession, isZOrderOnTop)
            }

            override fun notifyConfigurationChanged(configuration: Configuration) {
                notifyConfigurationChangedMethod.invoke(origSession, configuration)
            }

            override fun notifyUiChanged(uiContainerInfo: Bundle) {
                notifyUiChangedMethod.invoke(origSession, uiContainerInfo)
            }

            override fun close() {
                closeMethod.invoke(origSession)
            }
        }
    }

    /** [RemoteAdapter] fetches content from a provider living on a different process. */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private class RemoteAdapter(private val adapterInterface: ISandboxedUiAdapter) :
        SandboxedUiAdapter {

        override fun openSession(
            context: Context,
            windowInputToken: IBinder,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient
        ) {
            val mDisplayManager =
                context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val displayId = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY).displayId

            tryToCallRemoteObject(adapterInterface) {
                this.openRemoteSession(
                    windowInputToken,
                    displayId,
                    initialWidth,
                    initialHeight,
                    isZOrderOnTop,
                    RemoteSessionClient(context, client, clientExecutor)
                )
            }
        }

        override fun addObserverFactory(sessionObserverFactory: SessionObserverFactory) {}

        override fun removeObserverFactory(sessionObserverFactory: SessionObserverFactory) {}

        class RemoteSessionClient(
            val context: Context,
            val client: SandboxedUiAdapter.SessionClient,
            val clientExecutor: Executor
        ) : IRemoteSessionClient.Stub() {

            lateinit var surfaceView: SurfaceView

            override fun onRemoteSessionOpened(
                surfacePackage: SurfaceControlViewHost.SurfacePackage,
                remoteSessionController: IRemoteSessionController,
                isZOrderOnTop: Boolean,
                hasObservers: Boolean
            ) {
                surfaceView = SurfaceView(context)
                surfaceView.setChildSurfacePackage(surfacePackage)
                surfaceView.setZOrderOnTop(isZOrderOnTop)
                surfaceView.addOnAttachStateChangeListener(
                    object : View.OnAttachStateChangeListener {

                        private var hasViewBeenPreviouslyAttached = false

                        override fun onViewAttachedToWindow(v: View) {
                            if (hasViewBeenPreviouslyAttached) {
                                tryToCallRemoteObject(remoteSessionController) {
                                    this.notifyFetchUiForSession()
                                }
                            } else {
                                hasViewBeenPreviouslyAttached = true
                            }
                        }

                        override fun onViewDetachedFromWindow(v: View) {}
                    }
                )

                clientExecutor.execute {
                    client.onSessionOpened(
                        SessionImpl(
                            surfaceView,
                            remoteSessionController,
                            surfacePackage,
                            hasObservers
                        )
                    )
                }
                addBinderDeathListener(remoteSessionController) {
                    onRemoteSessionError("Remote process died")
                }
            }

            override fun onRemoteSessionError(errorString: String) {
                clientExecutor.execute { client.onSessionError(Throwable(errorString)) }
            }

            override fun onResizeRequested(width: Int, height: Int) {
                clientExecutor.execute { client.onResizeRequested(width, height) }
            }

            override fun onSessionUiFetched(surfacePackage: SurfaceControlViewHost.SurfacePackage) {
                surfaceView.setChildSurfacePackage(surfacePackage)
            }
        }

        private class SessionImpl(
            val surfaceView: SurfaceView,
            val remoteSessionController: IRemoteSessionController,
            val surfacePackage: SurfaceControlViewHost.SurfacePackage,
            hasObservers: Boolean
        ) : SandboxedUiAdapter.Session {

            override val view: View = surfaceView

            // While there are no more refined signal options, just use hasObservers as a signal
            // for whether to start measurement.
            // TODO(b/341895747): Add structured signal options.
            override val signalOptions =
                if (hasObservers) {
                    setOf("someOptions")
                } else {
                    setOf()
                }

            override fun notifyConfigurationChanged(configuration: Configuration) {
                tryToCallRemoteObject(remoteSessionController) {
                    this.notifyConfigurationChanged(configuration)
                }
            }

            @SuppressLint("ClassVerificationFailure")
            override fun notifyResized(width: Int, height: Int) {

                val clientResizeRunnable = Runnable {
                    surfaceView.layout(
                        /* left = */ 0,
                        /* top = */ 0,
                        /* right = */ width,
                        /* bottom = */ height
                    )
                }

                val providerResizeRunnable = Runnable {
                    tryToCallRemoteObject(remoteSessionController) {
                        this.notifyResized(width, height)
                    }
                }

                val syncGroup = SurfaceSyncGroup("AppAndSdkViewsSurfaceSync")

                syncGroup.add(surfaceView.rootSurfaceControl, clientResizeRunnable)
                syncGroup.add(surfacePackage, providerResizeRunnable)
                syncGroup.markSyncReady()
            }

            override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
                surfaceView.setZOrderOnTop(isZOrderOnTop)
                tryToCallRemoteObject(remoteSessionController) {
                    this.notifyZOrderChanged(isZOrderOnTop)
                }
            }

            override fun notifyUiChanged(uiContainerInfo: Bundle) {
                tryToCallRemoteObject(remoteSessionController) {
                    this.notifyUiChanged(uiContainerInfo)
                }
            }

            override fun close() {
                closeRemoteSession(remoteSessionController)
            }
        }
    }
}
