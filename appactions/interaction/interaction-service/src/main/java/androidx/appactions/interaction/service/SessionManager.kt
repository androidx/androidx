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

package androidx.appactions.interaction.service

import androidx.annotation.GuardedBy
import androidx.appactions.interaction.capabilities.core.impl.CapabilitySession
import java.util.IdentityHashMap
import javax.annotation.concurrent.ThreadSafe

/** Global object for managing capability sessions. */
@ThreadSafe
internal object SessionManager {

    private val lock = Any()

    @GuardedBy("lock")
    private val sessions = mutableMapOf<String, CapabilitySession>()

    /**
     * stores a map of uiHandle reference to sessionId, in order to get/set UiCache entries with
     * sessionId key.
     *
     * if sessionId is the key, we can release the lock as soon as the ActionExecutor finishes.
     */
    @GuardedBy("lock")
    private val uiHandleToSessionId = IdentityHashMap<Any, String>()

    fun putSession(sessionId: String, session: CapabilitySession) {
        synchronized(lock) {
            sessions[sessionId] = session
            uiHandleToSessionId[session.uiHandle] = sessionId
        }
    }

    fun getSession(sessionId: String): CapabilitySession? {
        synchronized(lock) {
            return sessions[sessionId]
        }
    }

    fun getLatestSessionIdFromUiHandle(uiHandle: Any): String? {
        synchronized(lock) {
            return uiHandleToSessionId[uiHandle]
        }
    }

    fun removeSession(sessionId: String) {
        synchronized(lock) {
            val session = sessions[sessionId]
            session?.let { uiHandleToSessionId.remove(it.uiHandle) }
            sessions.remove(sessionId)
        }
    }
}
