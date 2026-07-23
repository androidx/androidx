/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.savedstate

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * A scope that owns a [SavedStateRegistry].
 *
 * Pass this owner to create a [SavedStateRegistryController] through which the owner accesses and
 * performs operations on its [SavedStateRegistry].
 *
 * The typical lifecycle flow and method call sequence of these components is:
 * ```
 * Lifecycle.State                 SavedStateRegistryController
 * ---------------                 ----------------------------
 * INITIALIZED ------------------> performAttach
 *                                 |
 *                                 performRestore
 *                                 |
 * CREATED ------------------------+
 *                                 |
 * STARTED ----------------------> (register providers, consume state)
 *                                 |
 * CREATED ----------------------> performSave
 *                                 |
 * DESTROYED <---------------------+
 * ```
 *
 * [SavedStateRegistryController.performAttach] must be called once (and only once) on the main
 * thread during the owner's [Lifecycle.State.INITIALIZED] state. Call it before calling
 * [SavedStateRegistryController.performRestore].
 *
 * [SavedStateRegistryController.performRestore] can be called with a nullable if nothing needs to
 * be restored, or with the saved state to be restored. Call it in one of two places:
 * 1. Directly before the [Lifecycle] moves to [Lifecycle.State.CREATED].
 * 2. Before [Lifecycle.State.STARTED] is reached, as part of the [LifecycleObserver] added during
 *    owner initialization.
 *
 * [SavedStateRegistryController.performSave] should be called after the owner has been stopped but
 * before it reaches [Lifecycle.State.DESTROYED] state. Call it only once the owner has received the
 * [Lifecycle.Event.ON_STOP] event. The saved state passed to performSave will be the saved state
 * restored by performRestore.
 */
public interface SavedStateRegistryOwner : LifecycleOwner {
    /** The [SavedStateRegistry] owned by this [SavedStateRegistryOwner]. */
    public val savedStateRegistry: SavedStateRegistry
}
