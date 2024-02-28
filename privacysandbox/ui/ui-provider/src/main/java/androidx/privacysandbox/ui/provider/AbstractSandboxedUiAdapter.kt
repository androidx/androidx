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
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionObserverFactory

/**
 * An abstract class that implements [SandboxedUiAdapter] while abstracting away methods that do not
 * need to be implemented by a UI provider.
 *
 * UI providers should use this class rather than implementing [SandboxedUiAdapter] directly.
 */
abstract class AbstractSandboxedUiAdapter : SandboxedUiAdapter {

    /** The list of [SessionObserverFactory] instances that have been added to this adapter. */
    val sessionObserverFactories: List<SessionObserverFactory>
        get() {
            synchronized(_sessionObserverFactories) {
                return _sessionObserverFactories.toList()
            }
        }

    private val _sessionObserverFactories: MutableList<SessionObserverFactory> = mutableListOf()

    final override fun addObserverFactory(sessionObserverFactory: SessionObserverFactory) {
        synchronized(_sessionObserverFactories) {
            _sessionObserverFactories.add(sessionObserverFactory)
        }
    }

    final override fun removeObserverFactory(sessionObserverFactory: SessionObserverFactory) {
        synchronized(_sessionObserverFactories) {
            _sessionObserverFactories.remove(sessionObserverFactory)
        }
    }

    /**
     * An abstract class that implements [SandboxedUiAdapter.Session] so that a UI provider does not
     * need to implement the entire interface.
     *
     * UI providers should use this class rather than implementing [SandboxedUiAdapter.Session].
     */
    abstract class AbstractSession : SandboxedUiAdapter.Session {

        final override val signalOptions: Set<String>
            get() = setOf()

        override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {}

        override fun notifyResized(width: Int, height: Int) {}

        override fun notifyConfigurationChanged(configuration: Configuration) {}

        override fun notifyUiChanged(uiContainerInfo: Bundle) {}

        override fun close() {}
    }
}
