/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.compose.runtime.internal.AtomicBoolean

/**
 * Implementations of this interface can be used to cancel an ongoing operation or unregister a
 * listener by calling [cancel].
 */
public fun interface CancellationHandle {

    /**
     * Cancels the operation that this handle was returned for. Implementations of this method
     * should be idempotent. Callers should be able to cancel the same handle a second time without
     * causing any externally visible effects.
     */
    public fun cancel()

    public companion object {
        internal val Empty = CancellationHandle { /* No-op. */ }
    }
}

internal class OneShotCancellationHandle(private val action: () -> Unit) : CancellationHandle {
    private val didFireCancellation = AtomicBoolean(false)

    override fun cancel() {
        if (!didFireCancellation.getAndSet(true)) {
            action()
        }
    }
}
