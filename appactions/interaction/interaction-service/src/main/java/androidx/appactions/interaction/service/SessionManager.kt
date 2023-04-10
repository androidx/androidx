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
import javax.annotation.concurrent.ThreadSafe

/** Global object for managing capability sessions. */
@ThreadSafe
internal object SessionManager {

    private val lock = Any()

    @GuardedBy("lock")
    private val sessions = mutableMapOf<String, CapabilitySession>()

    fun putSession(sessionId: String, session: CapabilitySession) {
        synchronized(lock) {
            sessions[sessionId] = session
        }
    }

    fun getSession(sessionId: String): CapabilitySession? {
        synchronized(lock) {
            return sessions[sessionId]
        }
    }

    fun removeSession(sessionId: String) {
        synchronized(lock) {
            sessions.remove(sessionId)
        }
    }
}
