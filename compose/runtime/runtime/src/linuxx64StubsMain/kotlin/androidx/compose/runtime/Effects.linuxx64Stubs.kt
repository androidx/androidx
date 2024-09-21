/*
 * Copyright 2024 The Android Open Source Project
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

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope

internal actual class RememberedCoroutineScope
actual constructor(
    parentContext: CoroutineContext,
    overlayContext: CoroutineContext,
) : CoroutineScope, RememberObserver {
    // Implementation note:
    // This implementation should initialize the context lazily,
    // already cancelled if cancelIfCreated was previously invoked.
    override val coroutineContext: CoroutineContext
        get() = implementedInJetBrainsFork()

    // Implementation note:
    // This implementation should be nearly free if coroutineContext has not yet been accessed.
    actual fun cancelIfCreated() {
        implementedInJetBrainsFork()
    }

    override fun onRemembered() {
        // Do nothing
    }

    override fun onForgotten() {
        cancelIfCreated()
    }

    override fun onAbandoned() {
        cancelIfCreated()
    }
}
