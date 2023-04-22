/*
 * Copyright 2023 The Android Open Source Project
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
@file:JvmName("UiUpdater")

package androidx.appactions.interaction.service

import androidx.annotation.GuardedBy
import androidx.appactions.interaction.capabilities.core.ExecutionCallback
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.impl.UiHandleRegistry
import androidx.appactions.interaction.service.UiSessions.removeUiCache
import javax.annotation.concurrent.ThreadSafe

/**
 * A singleton object used to maintain UiCache objects per capability session.
 *
 * When the developer first calls `updateUi`, create a new [UiCache] for it. The Ui from the
 * developer is cached until the GRPC impl reads it. It's up to the GRPC impl to clean up the
 * UiCache (by calling [removeUiCache] before destroying the session.
 */
@ThreadSafe
internal object UiSessions {
    private val lock = Any()
    @GuardedBy("lock")
    private val sessionIdToUiCache = mutableMapOf<String, UiCache>()

    fun removeUiCache(sessionId: String): Boolean {
        synchronized(lock) {
            return sessionIdToUiCache.remove(sessionId) != null
        }
    }

    fun getOrCreateUiCache(sessionId: String): UiCache {
        synchronized(lock) {
            return sessionIdToUiCache[sessionId] ?: createUiCache(sessionId)
        }
    }

    fun getUiCacheOrNull(sessionId: String): UiCache? {
        synchronized(lock) {
            return sessionIdToUiCache[sessionId]
        }
    }

    private fun createUiCache(sessionId: String): UiCache {
        val uiCache = UiCache()
        synchronized(lock) {
            sessionIdToUiCache[sessionId] = uiCache
        }
        return uiCache
    }
}

/** Return a UI associated with this [BaseExecutionSession]. */
fun BaseExecutionSession<*, *>.updateUi(uiResponse: UiResponse) =
    UiSessions.getOrCreateUiCache(
        UiHandleRegistry.getSessionIdFromUiHandle(this)!!
    ).updateUiInternal(uiResponse)

/** Return a UI associated with this [ExecutionCallback]. */
fun ExecutionCallback<*, *>.updateUi(uiResponse: UiResponse) =
    UiSessions.getOrCreateUiCache(
        UiHandleRegistry.getSessionIdFromUiHandle(this)!!
    ).updateUiInternal(uiResponse)
