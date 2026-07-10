/*
 * Copyright 2026 The Android Open Source Project
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

@file:OptIn(ExperimentalWasmJsInterop::class)

package androidx.datastore.core.okio

import kotlin.js.Promise
import kotlinx.coroutines.await
import kotlinx.coroutines.promise

/**
 * The actual implementations are separated per platform because the kotlin.js.Promise APIs are not
 * available in common web source sets, but rather reside in the individual target platforms.
 */
internal actual class AsyncWebCoordinator actual constructor(private val name: String) :
    BaseAsyncWebCoordinator(name) {
    actual override suspend fun <T> lock(block: suspend () -> T): T {
        if (isExclusiveLockHeld) {
            return block()
        }
        val lockName = "datastore-opfs-lock-$name"

        var result: Result<T>? = null

        val callback: (Lock?) -> Promise<JsAny?> = { _ ->
            scope.promise {
                isExclusiveLockHeld = true
                try {
                    result = runCatching { block() }
                } finally {
                    isExclusiveLockHeld = false
                }
                null
            }
        }

        getJsNavigator().locks.request(lockName, callback).await<JsAny?>()
        return result!!.getOrThrow()
    }

    actual override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
        if (isExclusiveLockHeld) {
            return block(true)
        }
        val lockName = "datastore-opfs-lock-$name"
        val options = tryLockOptions()

        var result: Result<T>? = null
        val callback: (Lock?) -> Promise<JsAny?> = { lock ->
            scope.promise {
                result = runCatching { block(lock != null) }
                null
            }
        }

        getJsNavigator().locks.request(lockName, options, callback).await<JsAny?>()
        return result!!.getOrThrow()
    }
}
