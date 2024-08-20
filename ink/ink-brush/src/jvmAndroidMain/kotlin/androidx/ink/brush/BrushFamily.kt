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

package androidx.ink.brush

import androidx.annotation.RestrictTo
import androidx.ink.nativeloader.NativeLoader
import java.util.Collections.unmodifiableList
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * A [BrushFamily] combines one or more [BrushCoat]s and an optional URI to describe a family of
 * brushes.
 *
 * The [uri] exists for the convenience of higher level serialization and asset management APIs.
 * Aside from being checked for valid formatting and being forwarded on copy or move, the [uri] will
 * not*** be used by Ink APIs that consume a [BrushFamily].
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public class BrushFamily
@ExperimentalInkCustomBrushApi
@JvmOverloads
constructor(
    // The [coats] val below is a defensive copy of this parameter.
    coats: List<BrushCoat>,
    public val uri: String? = null,
) {
    public val coats: List<BrushCoat> = unmodifiableList(coats.toList())

    @ExperimentalInkCustomBrushApi
    @JvmOverloads
    public constructor(
        tip: BrushTip = BrushTip(),
        paint: BrushPaint = BrushPaint(),
        uri: String? = null,
    ) : this(listOf(BrushCoat(tip, paint)), uri)

    /** A handle to the underlying native [BrushFamily] object. */
    internal val nativePointer: Long =
        nativeCreateBrushFamily(coats.map { it.nativePointer }.toLongArray(), uri)

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @ExperimentalInkCustomBrushApi
    @JvmSynthetic
    public fun copy(coats: List<BrushCoat> = this.coats, uri: String? = this.uri): BrushFamily {
        return if (coats == this.coats && uri == this.uri) {
            this
        } else {
            BrushFamily(coats, uri)
        }
    }

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @ExperimentalInkCustomBrushApi
    @JvmSynthetic
    public fun copy(coat: BrushCoat, uri: String? = this.uri): BrushFamily {
        return copy(coats = listOf(coat), uri = uri)
    }

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @ExperimentalInkCustomBrushApi
    @JvmSynthetic
    public fun copy(tip: BrushTip, paint: BrushPaint, uri: String? = this.uri): BrushFamily {
        return copy(coat = BrushCoat(tip, paint), uri = uri)
    }

    /**
     * Returns a [Builder] with values set equivalent to `this`. Java developers, use the returned
     * builder to build a copy of a BrushFamily.
     */
    @ExperimentalInkCustomBrushApi
    public fun toBuilder(): Builder = Builder().setCoats(coats).setUri(uri)

    /**
     * Builder for [BrushFamily].
     *
     * For Java developers, use BrushFamily.Builder to construct [BrushFamily] with default values,
     * overriding only as needed. For example: `BrushFamily family = new
     * BrushFamily.Builder().coat(presetBrushCoat).build();`
     */
    @ExperimentalInkCustomBrushApi
    public class Builder {
        private var coats: List<BrushCoat> = listOf(BrushCoat(BrushTip(), BrushPaint()))
        private var uri: String? = null

        public fun setCoat(tip: BrushTip, paint: BrushPaint): Builder =
            setCoat(BrushCoat(tip, paint))

        public fun setCoat(coat: BrushCoat): Builder = setCoats(listOf(coat))

        public fun setCoats(coats: List<BrushCoat>): Builder {
            this.coats = coats.toList()
            return this
        }

        public fun setUri(uri: String?): Builder {
            this.uri = uri
            return this
        }

        public fun build(): BrushFamily = BrushFamily(coats, uri)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BrushFamily) return false
        return coats == other.coats && uri == other.uri
    }

    override fun hashCode(): Int {
        var result = coats.hashCode()
        result = 31 * result + uri.hashCode()
        return result
    }

    override fun toString(): String = "BrushFamily(coats=$coats, uri=$uri)"

    /** Deletes native BrushFamily memory. */
    protected fun finalize() {
        // NOMUTANTS -- Not tested post garbage collection.
        nativeFreeBrushFamily(nativePointer)
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeCreateBrushFamily(coatNativePointers: LongArray, uri: String?): Long

    /** Release the underlying memory allocated in [nativeCreateBrushFamily]. */
    private external fun nativeFreeBrushFamily(
        nativePointer: Long
    ) // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    // Companion object gets initialized before anything else.
    public companion object {
        init {
            NativeLoader.load()
        }

        /** Returns a new [BrushFamily.Builder]. */
        @ExperimentalInkCustomBrushApi @JvmStatic public fun builder(): Builder = Builder()
    }
}
