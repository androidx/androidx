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

package androidx.compose.runtime.composer

import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.RememberObserverHolder

/**
 * An interface used during [androidx.compose.runtime.ControlledComposition.applyChanges] and
 * [androidx.compose.runtime.Composition.dispose] to track when
 * [androidx.compose.runtime.RememberObserver] instances and leave the composition an also allows
 * recording [androidx.compose.runtime.SideEffect] calls.
 */
internal interface RememberManager {
    /**
     * The [androidx.compose.runtime.RememberObserver] is being remembered by a slot in the slot
     * table.
     */
    fun remembering(instance: RememberObserverHolder)

    /**
     * The [androidx.compose.runtime.RememberObserver] is being forgotten by a slot in the slot
     * table.
     */
    fun forgetting(instance: RememberObserverHolder)

    /**
     * The [effect] should be called when changes are being applied but after the remember/forget
     * notifications are sent.
     */
    fun sideEffect(effect: () -> Unit)

    /** The [androidx.compose.runtime.ComposeNodeLifecycleCallback] is being deactivated. */
    fun deactivating(instance: ComposeNodeLifecycleCallback)

    /** The [ComposeNodeLifecycleCallback] is being released. */
    fun releasing(instance: ComposeNodeLifecycleCallback)

    /** The restart scope is pausing */
    fun rememberPausingScope(scope: RecomposeScopeImpl)

    /** The restart scope is resuming */
    fun startResumingScope(scope: RecomposeScopeImpl)

    /** The restart scope is finished resuming */
    fun endResumingScope(scope: RecomposeScopeImpl)
}
