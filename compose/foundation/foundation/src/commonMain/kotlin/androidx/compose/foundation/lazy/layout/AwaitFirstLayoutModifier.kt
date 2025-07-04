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

package androidx.compose.foundation.lazy.layout

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.util.fastForEach
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Internal modifier which allows to delay some interactions (e.g. scroll) until layout is ready.
 */
internal class AwaitFirstLayoutModifier : OnGloballyPositionedModifier {
    private var wasPositioned = false
    private val continuations = mutableListOf<Continuation<Unit>>()

    suspend fun waitForFirstLayout() {
        if (!wasPositioned) {
            var continuation: Continuation<Unit>? = null
            try {
                suspendCancellableCoroutine<Unit> {
                    continuation = it
                    continuations.add(it)
                }
            } finally {
                continuations.remove(continuation)
            }
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (!wasPositioned) {
            wasPositioned = true
            continuations.fastForEach { it.resume(Unit) }
            continuations.clear()
        }
    }
}
