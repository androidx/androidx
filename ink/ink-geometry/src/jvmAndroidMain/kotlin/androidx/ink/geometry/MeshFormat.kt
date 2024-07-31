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
import androidx.ink.nativeloader.NativeLoader

/** Determines how the raw data of a [Mesh] is represented. */
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
public class MeshFormat
/** Only for use within the ink library. Constructs a [MeshFormat] from native pointer. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(private var nativeAddress: Long) {

    /**
     * Only for use within the ink library. Returns the native address held by this [MeshFormat].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun getNativeAddress(): Long = nativeAddress

    /** Returns whether this format and [other] mesh format have the same packed representation. */
    public fun isPackedEquivalent(other: MeshFormat): Boolean {
        return this === other ||
            this.nativeAddress == other.nativeAddress ||
            nativeIsPackedEquivalent(this.nativeAddress, other.nativeAddress)
    }

    /**
     * Returns whether this format and [other] mesh format have the same unpacked representation.
     */
    public fun isUnpackedEquivalent(other: MeshFormat): Boolean {
        return this === other ||
            this.nativeAddress == other.nativeAddress ||
            nativeIsUnpackedEquivalent(this.nativeAddress, other.nativeAddress)
    }

    protected fun finalize() {
        nativeFree(nativeAddress)
    }

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeIsPackedEquivalent(
        firstNativeAddress: Long,
        secondNativeAddress: Long,
    ): Boolean

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeIsUnpackedEquivalent(
        firstNativeAddress: Long,
        secondNativeAddress: Long,
    ): Boolean

    private external fun nativeFree(
        nativeAddress: Long
    ) // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    public companion object {
        init {
            NativeLoader.load()
        }
    }
}
