/*
 * Copyright (C) 2017 The Android Open Source Project
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

@file:Suppress("FacadeClassJvmName") // Cannot be updated, the Kt name has been released

package androidx.lifecycle

import kotlinx.coroutines.CoroutineScope

/**
 * A class that exposes a [Lifecycle].
 *
 * Allows custom components to observe and react to lifecycle changes without tight coupling to the
 * owner's implementation.
 *
 * @see Lifecycle
 */
public interface LifecycleOwner {
    /** The [Lifecycle] of this owner. */
    public val lifecycle: Lifecycle
}

/**
 * [CoroutineScope] tied to this [LifecycleOwner]'s [Lifecycle].
 *
 * Canceled when the [Lifecycle] is destroyed. Bound to `Dispatchers.Main.immediate`.
 */
public val LifecycleOwner.lifecycleScope: LifecycleCoroutineScope
    get() = lifecycle.coroutineScope
