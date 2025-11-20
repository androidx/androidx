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

package androidx.compose.runtime.retain

import androidx.compose.runtime.RememberObserver

internal class RetainedValueHolder<out T>
internal constructor(
    val key: Any,
    val value: T,
    owner: RetainedValuesStore,
    private var isNewlyRetained: Boolean,
) : RememberObserver {

    var owner: RetainedValuesStore = owner
        private set

    init {
        if (value is RememberObserver && value !is RetainObserver) {
            throw IllegalArgumentException(
                "Retained a value that implements RememberObserver but not RetainObserver. " +
                    "To receive the correct callbacks, the retained value '$value' must also " +
                    "implement RetainObserver."
            )
        }
    }

    internal fun readoptUnder(newStore: RetainedValuesStore) {
        owner = newStore
    }

    override fun onRemembered() {
        if (value is RetainObserver) {
            if (isNewlyRetained) {
                isNewlyRetained = false
                value.onRetained()
            }
            value.onEnteredComposition()
        }
    }

    override fun onForgotten() {
        if (value is RetainObserver) {
            value.onExitedComposition()
        }
        owner.saveExitingValue(key, value)
    }

    override fun onAbandoned() {
        if (value is RetainObserver) value.onUnused()
    }
}
