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
import androidx.appactions.interaction.capabilities.core.ActionExecutor
import androidx.appactions.interaction.capabilities.core.BaseSession
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
    @GuardedBy("lock") private val uiCacheList = mutableListOf<UiCache>()

    fun removeUiCache(uiHandle: Any): Boolean {
        synchronized(lock) {
            return uiCacheList.remove(getUiCacheOrNull(uiHandle))
        }
    }

    fun getOrCreateUiCache(uiHandle: Any): UiCache {
        synchronized(lock) {
            return uiCacheList.find { it.uiHandle === uiHandle } ?: createUiCache(uiHandle)
        }
    }

    fun getUiCacheOrNull(uiHandle: Any): UiCache? {
        synchronized(lock) {
            return uiCacheList.find { it.uiHandle === uiHandle }
        }
    }

    private fun createUiCache(uiHandle: Any): UiCache {
        val uiSession = UiCache(uiHandle)
        uiCacheList.add(uiSession)
        return uiSession
    }
}

/** Return a UI associated with this [BaseSession]. */
fun BaseSession<*, *>.updateUi(uiResponse: UiResponse) =
    UiSessions.getOrCreateUiCache(this).updateUiInternal(uiResponse)

/** Return a UI associated with this [ActionExecutor]. */
fun ActionExecutor<*, *>.updateUi(uiResponse: UiResponse) =
    UiSessions.getOrCreateUiCache(this).updateUiInternal(uiResponse)
