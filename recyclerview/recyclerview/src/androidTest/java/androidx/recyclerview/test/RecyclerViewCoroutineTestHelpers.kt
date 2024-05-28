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

package androidx.recyclerview.test

import androidx.recyclerview.widget.RecyclerView
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

suspend fun RecyclerView.awaitScrollIdle() {
    val rv = this
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { continuation ->
            val listener =
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            rv.removeOnScrollListener(this)
                            continuation.resume(Unit)
                        }
                    }
                }

            rv.addOnScrollListener(listener)

            continuation.invokeOnCancellation { rv.removeOnScrollListener(listener) }

            if (rv.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                rv.removeOnScrollListener(listener)
                continuation.resume(Unit)
            }
        }
    }
}
