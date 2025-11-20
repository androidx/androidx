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
@file:Suppress("DEPRECATION")

package androidx.privacysandbox.ui.provider

import androidx.privacysandbox.ui.core.SessionObserverFactory

internal class SessionObserverFactoryRegistryProvider : SessionObserverFactoryRegistry {
    private val _sessionObserverFactories: MutableList<SessionObserverFactory> = mutableListOf()

    override val sessionObserverFactories: List<SessionObserverFactory>
        get() = _sessionObserverFactories.toList()

    override fun addObserverFactory(sessionObserverFactory: SessionObserverFactory) {
        _sessionObserverFactories.add(sessionObserverFactory)
    }

    override fun removeObserverFactory(sessionObserverFactory: SessionObserverFactory) {
        _sessionObserverFactories.remove(sessionObserverFactory)
    }
}
