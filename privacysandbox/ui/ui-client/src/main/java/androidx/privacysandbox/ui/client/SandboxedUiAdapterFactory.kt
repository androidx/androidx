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
import android.view.Display
import android.view.SurfaceControlViewHost
import android.view.View
import android.window.SurfaceSyncGroup
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.core.IDelegatingSandboxedUiAdapter
import androidx.privacysandbox.ui.core.IRemoteSessionClient
import androidx.privacysandbox.ui.core.IRemoteSessionController
import androidx.privacysandbox.ui.core.ISandboxedUiAdapter
import androidx.privacysandbox.ui.core.ProtocolConstants
import androidx.privacysandbox.ui.core.RemoteCallManager.addBinderDeathListener
import androidx.privacysandbox.ui.core.RemoteCallManager.tryToCallRemoteObject
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SdkRuntimeUiLibVersions
import androidx.privacysandbox.ui.core.SessionData
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.Executor

/**
 * Provides an adapter created from a supplied Bundle which acts as a proxy between the host app and
 * the Binder provided by the provider of content.
 */
public object SandboxedUiAdapterFactory {

    private val uiAdapterFactoryDelegate =
        object : UiAdapterFactoryDelegate() {
            override val uiAdapterBinderKey: String = ProtocolConstants.uiAdapterBinderKey
            override val adapterDescriptor: String = ISandboxedUiAdapter.DESCRIPTOR
        }

    /**
     * @throws IllegalArgumentException if {@code coreLibInfo} does not contain a Binder with the
     *   key UI_ADAPTER_BINDER
     */
    public fun createFromCoreLibInfo(coreLibInfo: Bundle): SandboxedUiAdapter {
        val uiAdapterBinder = uiAdapterFactoryDelegate.requireNotNullAdapterBinder(coreLibInfo)
        // the following check for DelegatingAdapter check must happen before the checks for
        // remote/local binder as the checks below have fallback to a RemoteAdapter if it's not
        // local.
        if (
            uiAdapterBinder.interfaceDescriptor?.equals(
                "androidx.privacysandbox.ui.core.IDelegatingSandboxedUiAdapter"
            ) == true
        ) {
            val delegate =
                coreLibInfo.getBundle(ProtocolConstants.delegateKey)
                    ?: throw UnsupportedOperationException(
                        "DelegatingAdapter must have a non null delegate"
                    )
            return ClientDelegatingAdapter(
                IDelegatingSandboxedUiAdapter.Stub.asInterface(uiAdapterBinder),
                createFromCoreLibInfo(delegate),
            )
        }

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
     * [LocalAdapter] fetches UI from a provider living on same process as the client but on a
     * different class loader. We should also perform ui-provider version check before calling any
     * newly introduced api / modified api.
     */
    @SuppressLint("BanUncheckedReflection") // using reflection on library classes
    private class LocalAdapter(adapterBundle: Bundle) :
        SandboxedUiAdapter, ClientAdapter(adapterBundle) {

        val uiProviderBinder = uiAdapterFactoryDelegate.requireNotNullAdapterBinder(adapterBundle)

        val uiProviderVersion =
            uiAdapterFactoryDelegate.requireNotNullUiProviderVersion(adapterBundle)

        private val targetSessionClientClass =
            Class.forName(
                "androidx.privacysandbox.ui.core.SandboxedUiAdapter\$SessionClient",
                /* initialize = */ false,
                uiProviderBinder.javaClass.classLoader,
            )

        private val targetSessionDataClass =
            Class.forName(
                "androidx.privacysandbox.ui.core.SessionData",
                /* initialize = */ false,
                uiProviderBinder.javaClass.classLoader,
            )

        private val targetSessionDataCompanionObject =
            targetSessionDataClass.getDeclaredField("Companion").get(null)

        // The adapterInterface provided must have a openSession method on its class.
        // Since the object itself has been instantiated on a different classloader, we
        // need reflection to get hold of it.
        private val openLocalSessionMethod: Method =
            Class.forName(
                    "androidx.privacysandbox.ui.core.LocalUiAdapter",
                    /*initialize=*/ false,
                    uiProviderBinder.javaClass.classLoader,
                )
                .getMethod(
                    "openLocalSession",
                    Int::class.java,
                    Context::class.java,
                    targetSessionDataClass,
                    Int::class.java,
                    Int::class.java,
                    Boolean::class.java,
                    Executor::class.java,
                    targetSessionClientClass,
                )

        private val fromBundleMethod: Method =
            targetSessionDataCompanionObject.javaClass.getMethod("fromBundle", Bundle::class.java)

        override fun openSession(
            context: Context,
            sessionData: SessionData,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient,
        ) {
            try {
                // We can't pass the client object as-is since it's been created on a different
                // classloader.
                val sessionClientProxy =
                    Proxy.newProxyInstance(
                        uiProviderBinder.javaClass.classLoader,
                        arrayOf(targetSessionClientClass),
                        SessionClientProxyHandler(uiProviderVersion, client),
                    )

                openLocalSessionMethod.invoke(
                    uiProviderBinder,
                    SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel,
                    context,
                    fromBundleMethod.invoke(
                        targetSessionDataCompanionObject,
                        SessionData.toBundle(sessionData),
                    ),
                    initialWidth,
                    initialHeight,
                    isZOrderOnTop,
                    clientExecutor,
                    sessionClientProxy,
                )
            } catch (exception: Throwable) {
                client.onSessionError(exception)
            }
        }

        private class SessionClientProxyHandler(
            private val uiProviderVersion: Int,
            private val origClient: SandboxedUiAdapter.SessionClient,
        ) : InvocationHandler {

            override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any {
                return when (method.name) {
                    "onSessionOpened" -> {
                        // We have to forward the call to original client, but it won't
                        // recognize Session class on targetClassLoader. We need proxy for it
                        // on local ClassLoader.
                        args!! // This method will always have an argument, so safe to !!
                        origClient.onSessionOpened(SessionProxy(uiProviderVersion, args[0]))
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
    }

    /**
     * [RemoteAdapter] fetches content from a provider living on a different process. We should also
     * perform ui-provider version check before calling any newly introduced api / modified api.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private class RemoteAdapter(adapterBundle: Bundle) :
        SandboxedUiAdapter, ClientAdapter(adapterBundle) {

        val uiAdapterBinder = uiAdapterFactoryDelegate.requireNotNullAdapterBinder(adapterBundle)
        val adapterInterface: ISandboxedUiAdapter =
            ISandboxedUiAdapter.Stub.asInterface(uiAdapterBinder)

        val uiProviderVersion =
            uiAdapterFactoryDelegate.requireNotNullUiProviderVersion(adapterBundle)

        override fun openSession(
            context: Context,
            sessionData: SessionData,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient,
        ) {
            val mDisplayManager =
                context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val displayId = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY).displayId

            tryToCallRemoteObject(adapterInterface) {
                this.openRemoteSession(
                    SdkRuntimeUiLibVersions.CURRENT_VERSION.apiLevel,
                    SessionData.toBundle(sessionData),
                    displayId,
                    initialWidth,
                    initialHeight,
                    isZOrderOnTop,
                    RemoteSessionClient(uiProviderVersion, context, client, clientExecutor),
                )
            }
        }

        class RemoteSessionClient(
            val uiProviderVersion: Int,
            val context: Context,
            val client: SandboxedUiAdapter.SessionClient,
            val clientExecutor: Executor,
        ) : IRemoteSessionClient.Stub() {

            lateinit var contentView: ContentView

            override fun onRemoteSessionOpened(
                surfacePackage: SurfaceControlViewHost.SurfacePackage,
                remoteSessionController: IRemoteSessionController,
                isZOrderOnTop: Boolean,
                signalOptions: List<String>,
            ) {
                val remoteSessionControllerWithVersionCheck =
                    RemoteSessionController(uiProviderVersion, remoteSessionController)
                contentView = ContentView(context, remoteSessionControllerWithVersionCheck)
                contentView.setChildSurfacePackage(surfacePackage)
                contentView.setZOrderOnTop(isZOrderOnTop)
                contentView.addOnAttachStateChangeListener(
                    object : View.OnAttachStateChangeListener {

                        private var hasViewBeenPreviouslyAttached = false

                        override fun onViewAttachedToWindow(v: View) {
                            if (hasViewBeenPreviouslyAttached) {
                                remoteSessionControllerWithVersionCheck.notifyFetchUiForSession()
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
                            contentView,
                            remoteSessionControllerWithVersionCheck,
                            surfacePackage,
                            signalOptions.toSet(),
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
                contentView.setChildSurfacePackage(surfacePackage)
            }
        }

        private class SessionImpl(
            val contentView: ContentView,
            val remoteSessionController: androidx.privacysandbox.ui.client.IRemoteSessionController,
            val surfacePackage: SurfaceControlViewHost.SurfacePackage,
            override val signalOptions: Set<String>,
        ) : SandboxedUiAdapter.Session {

            override val view: View = contentView

            override fun notifyConfigurationChanged(configuration: Configuration) {
                remoteSessionController.notifyConfigurationChanged(configuration)
            }

            override fun notifyResized(width: Int, height: Int) {

                val parentView = contentView.parent as View

                val clientResizeRunnable = Runnable {
                    contentView.layout(
                        /* left = */ parentView.paddingLeft,
                        /* top = */ parentView.paddingTop,
                        /* right = */ parentView.paddingLeft + width,
                        /* bottom = */ parentView.paddingTop + height,
                    )
                }

                val providerResizeRunnable = Runnable {
                    remoteSessionController.notifyResized(width, height)
                }

                val syncGroup = SurfaceSyncGroup("AppAndSdkViewsSurfaceSync")

                syncGroup.add(contentView.rootSurfaceControl, clientResizeRunnable)
                syncGroup.add(surfacePackage, providerResizeRunnable)
                syncGroup.markSyncReady()
            }

            override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
                contentView.setZOrderOnTop(isZOrderOnTop)
                remoteSessionController.notifyZOrderChanged(isZOrderOnTop)
            }

            override fun notifyUiChanged(uiContainerInfo: Bundle) {
                remoteSessionController.notifyUiChanged(uiContainerInfo)
            }

            override fun notifySessionRendered(supportedSignalOptions: Set<String>) {
                remoteSessionController.notifySessionRendered(supportedSignalOptions.toList())
            }

            override fun close() {
                remoteSessionController.close()
            }
        }
    }
}
