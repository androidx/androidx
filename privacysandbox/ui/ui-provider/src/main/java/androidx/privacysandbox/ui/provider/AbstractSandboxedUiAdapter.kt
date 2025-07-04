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

package androidx.privacysandbox.ui.provider

import android.content.res.Configuration
import android.os.Bundle
import androidx.privacysandbox.ui.core.SandboxedSdkViewUiInfo
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SandboxedUiAdapterSignalOptions
import androidx.privacysandbox.ui.core.SessionObserver
import androidx.privacysandbox.ui.core.SessionObserverContext
import androidx.privacysandbox.ui.core.SessionObserverFactory

/**
 * An abstract class that implements [SandboxedUiAdapter] while abstracting away methods that do not
 * need to be implemented by a UI provider.
 *
 * UI providers should use this class rather than implementing [SandboxedUiAdapter] directly.
 */
public abstract class AbstractSandboxedUiAdapter :
    SandboxedUiAdapter, SessionObserverFactoryRegistry {
    private val registryProvider = SessionObserverFactoryRegistryProvider()

    private val delegateMap:
        MutableMap<SessionObserverFactory, SessionObserverFactorySignalDelegate> =
        mutableMapOf()

    final override val sessionObserverFactories: List<SessionObserverFactory>
        get() = registryProvider.sessionObserverFactories

    final override fun addObserverFactory(sessionObserverFactory: SessionObserverFactory) {
        val delegateFactory = SessionObserverFactorySignalDelegate(sessionObserverFactory)
        delegateMap.put(sessionObserverFactory, delegateFactory)
        registryProvider.addObserverFactory(delegateFactory)
    }

    final override fun removeObserverFactory(sessionObserverFactory: SessionObserverFactory) {
        val proxy = delegateMap[sessionObserverFactory]
        proxy?.let {
            registryProvider.removeObserverFactory(proxy)
            delegateMap.remove(sessionObserverFactory)
        }
    }

    /**
     * A wrapper class of [SessionObserverFactory] that delegates calls to the underlying
     * [SessionObserver]s based on the [signalOptions] specified by the factory.
     */
    private class SessionObserverFactorySignalDelegate(
        val sessionObserverFactory: SessionObserverFactory
    ) : SessionObserverFactory {
        override val signalOptions: Set<String> = sessionObserverFactory.signalOptions

        override fun create(): SessionObserver {
            return SessionObserverSignalDelegate(sessionObserverFactory.create())
        }

        private inner class SessionObserverSignalDelegate(val sessionObserver: SessionObserver) :
            SessionObserver {
            override fun onSessionOpened(sessionObserverContext: SessionObserverContext) {
                sessionObserver.onSessionOpened(sessionObserverContext)
            }

            override fun onUiContainerChanged(uiContainerInfo: Bundle) {
                SandboxedSdkViewUiInfo.pruneBundle(uiContainerInfo, signalOptions)
                if (
                    signalOptions.contains(SandboxedUiAdapterSignalOptions.GEOMETRY) ||
                        signalOptions.contains(SandboxedUiAdapterSignalOptions.OBSTRUCTIONS)
                ) {
                    sessionObserver.onUiContainerChanged(uiContainerInfo)
                }
            }

            override fun onSessionClosed() {
                sessionObserver.onSessionClosed()
            }
        }
    }

    /**
     * An abstract class that implements [SandboxedUiAdapter.Session] so that a UI provider does not
     * need to implement the entire interface.
     *
     * UI providers should use this class rather than implementing [SandboxedUiAdapter.Session].
     */
    public abstract class AbstractSession : SandboxedUiAdapter.Session {

        final override val signalOptions: Set<String>
            get() = setOf()

        override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {}

        override fun notifyResized(width: Int, height: Int) {}

        override fun notifyConfigurationChanged(configuration: Configuration) {}

        override fun notifyUiChanged(uiContainerInfo: Bundle) {}

        override fun notifySessionRendered(supportedSignalOptions: Set<String>) {}

        override fun close() {}
    }
}
