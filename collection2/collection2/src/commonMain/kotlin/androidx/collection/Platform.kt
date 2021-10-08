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

package androidx.collection

internal expect fun indexOutOfBounds(): IndexOutOfBoundsException

/**
 * Internal class and a temporary solution for the lack of native synchronization intrinsics across
 * all platforms (read: Apple platforms). On JVM and Android this is just a simple class and the
 * actual synchronization block use the JVM/Android built-in intrinsics. On other platforms we are
 * using whatever is currently available.
 *
 * TODO(b/172658775): Replace once a solution becomes available.
 */
internal expect class SynchronizedObject

internal expect fun createSynchronizedObject(): SynchronizedObject

internal expect inline fun <R> synchronizedOperation(lock: SynchronizedObject, block: () -> R): R
