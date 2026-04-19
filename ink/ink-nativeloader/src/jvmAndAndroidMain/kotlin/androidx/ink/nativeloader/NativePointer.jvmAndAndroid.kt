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
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
@Suppress("NotCloseable") // Finalize is only used to free a native peer object.
actual public class NativePointer
actual constructor(
    pointerAlloc: () -> Long, // Must not be retained
    private val pointerFree: (Long) -> Unit, // Must not capture a reference to this instance
) : ReadOnlyProperty<Any, Long> {

    private var pointer: Long = 0L

    init {
        try {
            pointer = pointerAlloc()
        } finally {
            NativePointerObserver.onAlloc?.invoke(pointer)
        }
        check(pointer != 0L) {
            "Native allocation failed, should throw an exception in pointerAlloc in that case."
        }
    }

    // This could use java.lang.ref.Cleaner, but that's not supported until API 33 and we're still
    // supporting older.
    protected fun finalize() {
        // This can be called before initialization is complete, since objects are finalizable as
        // soon
        // as the Object constructor completes, and in Kotlin derived class initialization always
        // happens after base class initialization. This does ensure that the class is finalizable
        // before the pointer is allocated.
        if (pointer != 0L) {
            pointerFree(pointer)
        }
        // This observer is used for tests to wait until cleanup is done, so it should happen after
        // pointerFree is called.
        NativePointerObserver.onCleanup?.invoke(pointer)
    }

    actual override operator fun getValue(thisRef: Any, property: KProperty<*>): Long = pointer
}
