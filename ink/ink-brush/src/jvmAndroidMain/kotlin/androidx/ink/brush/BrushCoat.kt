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
import androidx.ink.nativeloader.UsedByNative
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * A [BrushCoat] represents one coat of paint applied by a brush. It includes a [BrushPaint] and a
 * [BrushTip] used to apply that paint. Multiple [BrushCoat] can be combined within a single brush;
 * when a stroke drawn by a multi-coat brush is rendered, each coat of paint will be drawn entirely
 * atop the previous coat, even if the stroke crosses over itself, as though each coat were painted
 * in its entirety one at a time.
 */
@ExperimentalInkCustomBrushApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class BrushCoat
private constructor(
    /** A handle to the underlying native [BrushCoat] object. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val nativePointer: Long,
    /** The tip used to apply the paint. */
    public val tip: BrushTip,
    /** The paint to be applied in this coat. */
    public val paint: BrushPaint = BrushPaint(),
) {

    /**
     * Creates a [BrushCoat] with the given [tip] and [paint].
     *
     * @param tip The tip used to apply the paint.
     * @param paint The paint to be applied for this coat.
     */
    @JvmOverloads
    public constructor(
        tip: BrushTip = BrushTip(),
        paint: BrushPaint = BrushPaint(),
    ) : this(BrushCoatNative.create(tip.nativePointer, paint.nativePointer), tip, paint)

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @JvmSynthetic
    public fun copy(tip: BrushTip = this.tip, paint: BrushPaint = this.paint): BrushCoat {
        return if (tip == this.tip && paint == this.paint) {
            this
        } else {
            BrushCoat(tip, paint)
        }
    }

    /**
     * Returns a [Builder] with values set equivalent to `this`. Java developers, use the returned
     * builder to build a copy of a BrushCoat.
     */
    public fun toBuilder(): Builder = Builder().setTip(tip).setPaint(paint)

    /**
     * Builder for [BrushCoat].
     *
     * For Java developers, use BrushCoat.Builder to construct [BrushCoat] with default values,
     * overriding only as needed. For example: `BrushCoat family = new
     * BrushCoat.Builder().tip(presetBrushTip).build();`
     */
    public class Builder {
        private var tip: BrushTip = BrushTip()
        private var paint: BrushPaint = BrushPaint()

        public fun setTip(tip: BrushTip): Builder {
            this.tip = tip
            return this
        }

        public fun setPaint(paint: BrushPaint): Builder {
            this.paint = paint
            return this
        }

        public fun build(): BrushCoat = BrushCoat(tip, paint)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BrushCoat) return false
        return tip == other.tip && paint == other.paint
    }

    override fun hashCode(): Int {
        var result = tip.hashCode()
        result = 31 * result + paint.hashCode()
        return result
    }

    override fun toString(): String = "BrushCoat(tip=$tip, paint=$paint)"

    /** Deletes native BrushCoat memory. */
    // NOMUTANTS -- Not tested post garbage collection.
    protected fun finalize() {
        // TODO: b/423019041 - Investigate why this is failing in native code with nativePointer=0
        if (nativePointer != 0L) {
            BrushCoatNative.free(nativePointer)
        }
    }

    // Companion object gets initialized before anything else.
    public companion object {
        /** Returns a new [BrushCoat.Builder]. */
        @JvmStatic public fun builder(): Builder = Builder()

        /**
         * Construct a [BrushCoat] from an unowned heap-allocated native pointer to a C++
         * `BrushCoat`. Kotlin wrapper objects nested under the [BrushCoat] are initialized
         * similarly using their own [wrapNative] methods, passing those pointers to newly
         * copy-constructed heap-allocated objects. That avoids the need to call Kotlin constructors
         * for those objects from C++.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(unownedNativePointer: Long): BrushCoat =
            BrushCoat(
                unownedNativePointer,
                BrushTip.wrapNative(BrushCoatNative.newCopyOfBrushTip(unownedNativePointer)),
                BrushPaint.wrapNative(BrushCoatNative.newCopyOfBrushPaint(unownedNativePointer)),
            )
    }
}

private object BrushCoatNative {
    init {
        NativeLoader.load()
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    @UsedByNative external fun create(tipNativePointer: Long, paintNativePointer: Long): Long

    /** Release the underlying memory allocated in [create]. */
    @UsedByNative external fun free(nativePointer: Long)

    /**
     * Returns a new, unowned native pointer to a copy of the `BrushTip` in the pointed-at
     * `BrushCoat`.
     */
    @UsedByNative external fun newCopyOfBrushTip(nativePointer: Long): Long

    /**
     * Returns a new, unowned native pointer to a copy of the `BrushPaint` in the pointed-at
     * `BrushCoat`.
     */
    @UsedByNative external fun newCopyOfBrushPaint(nativePointer: Long): Long
}
