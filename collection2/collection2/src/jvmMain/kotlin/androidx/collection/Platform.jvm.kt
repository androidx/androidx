/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.collection

internal actual fun indexOutOfBounds(): IndexOutOfBoundsException {
    // Throw AIOOB on JVM for behavioral compatibility with 1.x.
    return ArrayIndexOutOfBoundsException()
}

/**
 * A class for synchronization monitors. Note: do not make this a subclass of any other class from
 * a non-native synchronization library (such as AtomicFU). It's not needed on JVM/Android, and past
 * experience showed some difficulties with effective dead-code elimination with R8.
 */
internal actual class SynchronizedObject

internal actual fun createSynchronizedObject() = SynchronizedObject()

internal actual inline fun <R> synchronizedOperation(lock: SynchronizedObject, block: () -> R): R {
    synchronized(lock) {
        return block()
    }
}
