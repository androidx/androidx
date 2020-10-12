/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.activity.contextaware

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Run [onContextAvailable] when the [Context] becomes available and
 * resume with the result.
 *
 * If the [Context] is already available, [onContextAvailable] will be
 * synchronously called on the current coroutine context. Otherwise,
 * [onContextAvailable] will be called on the UI thread immediately when
 * the Context becomes available.
 */
public suspend inline fun <R> ContextAware.withContextAvailable(
    crossinline onContextAvailable: (Context) -> R
): R {
    val availableContext = peekAvailableContext()
    return if (availableContext != null) {
        onContextAvailable(availableContext)
    } else {
        suspendCancellableCoroutine { co ->
            val listener = object : OnContextAvailableListener {
                override fun onContextAvailable(context: Context) {
                    co.resumeWith(runCatching { onContextAvailable(context) })
                }
            }
            addOnContextAvailableListener(listener)
            co.invokeOnCancellation {
                removeOnContextAvailableListener(listener)
            }
        }
    }
}
