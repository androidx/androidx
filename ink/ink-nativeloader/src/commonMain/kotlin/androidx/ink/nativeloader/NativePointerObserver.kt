/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.nativeloader

import kotlin.concurrent.Volatile

/** Holder for test-only hooks to observe [NativePointer] initialization and cleanup. */
internal object NativePointerObserver {
    // Called with the raw native pointer value returned by the `pointerAlloc` functions passed to
    // each [NativePointer] constructor if that succeeds. Called with 0 if `pointerAlloc` throws an
    // exception.
    @Volatile var onAlloc: ((Long) -> Unit)? = null

    // Called with the raw native pointer value held by each [NativePointer] when it is cleaned up.
    // If a [NativePointer]'s `pointerAlloc` function throws an exception, this should be called
    // with
    // 0, since the cleanup is set up before the allocation is attempted.
    @Volatile var onCleanup: ((Long) -> Unit)? = null
}
