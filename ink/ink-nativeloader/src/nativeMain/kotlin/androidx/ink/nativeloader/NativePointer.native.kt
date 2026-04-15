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

import androidx.annotation.RestrictTo
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
@OptIn(ExperimentalNativeApi::class)
actual public class NativePointer
actual constructor(
    pointerAlloc: () -> Long, // Must not be retained
    pointerFree: (Long) -> Unit, // Must not capture a reference to this instance
) : ReadOnlyProperty<Any, Long> {

    private val pointerWrapper = PointerWrapper(pointerFree)

    // Both of these arguments outlive this object externally and must not capture a reference to
    // this object, or this will never be GC'd. The second argument is a non-capturing function
    // reference, so that is ensured if the pointerFree function passed to the constructor is
    // not capturing. (Unfortunately, there's not a way to enforce that statically.)
    private val cleaner = createCleaner(pointerWrapper, PointerWrapper::free)

    init {
        // alloc after cleanup is set up, to ensure cleanup happens any time alloc is reached.
        try {
            pointerWrapper.pointer = pointerAlloc()
        } finally {
            NativePointerObserver.onAlloc?.invoke(pointerWrapper.pointer)
        }
        check(pointerWrapper.pointer != 0L) {
            "Native allocation failed, should throw an exception in pointerAlloc in that case."
        }
    }

    actual override operator fun getValue(thisRef: Any, property: KProperty<*>): Long =
        pointerWrapper.pointer
}

private class PointerWrapper(private val pointerFree: (Long) -> Unit) {
    var pointer: Long = 0L

    fun free() {
        if (pointer != 0L) {
            pointerFree(pointer)
        }
        // This observer is used for tests to wait until cleanup is done, so it should happen after
        // pointerFree is called.
        NativePointerObserver.onCleanup?.invoke(pointer)
    }
}
