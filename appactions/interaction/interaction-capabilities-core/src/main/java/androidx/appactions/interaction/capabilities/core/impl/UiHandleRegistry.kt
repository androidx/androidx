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

package androidx.appactions.interaction.capabilities.core.impl

import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import java.util.IdentityHashMap

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object UiHandleRegistry {
    private val lock = Any()

    @GuardedBy("lock")
    private val uiHandleToSessionId = IdentityHashMap<Any, String>()

    internal fun registerUiHandle(uiHandle: Any, sessionId: String) {
        synchronized(lock) {
            uiHandleToSessionId[uiHandle] = sessionId
        }
    }

    internal fun unregisterUiHandle(uiHandle: Any) {
        synchronized(lock) {
            uiHandleToSessionId.remove(uiHandle)
        }
    }

    fun getSessionIdFromUiHandle(uiHandle: Any): String? {
      synchronized(lock) {
        return uiHandleToSessionId[uiHandle]
      }
    }
}
