/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.annotation.RestrictTo
import androidx.ink.nativeloader.NativePointer

/** Determines how the raw data of a [Mesh] is represented. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
public class MeshFormat private constructor(pointerAlloc: () -> Long) {

    /**
     * Only for use within the ink library. Returns the native address held by this [MeshFormat].
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val nativePointer: Long by NativePointer(pointerAlloc, MeshFormatNative::free)

    /** Returns whether this format and [other] mesh format have the same packed representation. */
    public fun isPackedEquivalent(other: MeshFormat): Boolean {
        return this === other ||
            this.nativePointer == other.nativePointer ||
            MeshFormatNative.isPackedEquivalent(this.nativePointer, other.nativePointer)
    }

    /**
     * Returns whether this format and [other] mesh format have the same unpacked representation.
     */
    public fun isUnpackedEquivalent(other: MeshFormat): Boolean {
        return this === other ||
            this.nativePointer == other.nativePointer ||
            MeshFormatNative.isUnpackedEquivalent(this.nativePointer, other.nativePointer)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun attributeCount(): Int = MeshFormatNative.attributeCount(nativePointer)

    public companion object {
        /**
         * Construct a [MeshFormat], taking a callback that heap-allocates and returns a pointer to
         * a C++ `MeshFormat`.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(pointerAlloc: () -> Long): MeshFormat = MeshFormat(pointerAlloc)
    }
}

/** Singleton wrapper around native JNI calls. */
expect internal object MeshFormatNative {

    fun isPackedEquivalent(nativePointer: Long, otherNativePointer: Long): Boolean

    fun isUnpackedEquivalent(nativePointer: Long, otherNativePointer: Long): Boolean

    fun attributeCount(nativePointer: Long): Int

    fun free(nativePointer: Long)
}
