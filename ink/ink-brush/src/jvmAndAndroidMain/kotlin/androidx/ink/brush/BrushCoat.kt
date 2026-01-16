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
import androidx.annotation.Size
import androidx.ink.geometry.MeshFormat
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.util.Collections.unmodifiableList
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * A [BrushCoat] represents one coat of ink applied by a brush. It includes a `BrushTip` that
 * describes the structure of that coat, and a non-empty list of possible [BrushPaint] objects -
 * each one describes how to render the coat structure, and the one [BrushPaint] that is actually
 * used is the first one in the list that is compatible with the device and renderer. Multiple
 * [BrushCoat]s can be combined within a single brush; when a stroke drawn by a multi-coat brush is
 * rendered, each coat of ink will be drawn entirely atop the previous coat, even if the stroke
 * crosses over itself, as though each coat were painted in its entirety one at a time.
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
    // The [paintPreferences] val below is a defensive copy of this parameter.
    paintPreferences: List<BrushPaint>,
) {

    /**
     * The [BrushPaint] instances to try to use for rendering, in preference order. The first one
     * that is compatible with the renderer and the device will be used. Compatibility may be
     * determined by various parameters of a [BrushPaint] and its underlying
     * [BrushPaint.TextureLayer] objects. Alternative paints add portability to brushes, so that
     * fallback versions of strokes can be rendered on devices or renderers that have more limited
     * functionality. If no paints are compatible, then this [BrushCoat] will not be rendered.
     */
    @Size(min = 1)
    public val paintPreferences: List<BrushPaint> = unmodifiableList(paintPreferences)

    /**
     * Creates a [BrushCoat] with the given [BrushTip] and ordered preferences for [BrushPaint].
     *
     * @param tip The tip used to apply the paint.
     * @param paintPreferences The paint options to try to use for rendering, in preference order.
     *   The first one that is compatible with the renderer and the device will be used.
     */
    @JvmOverloads
    public constructor(
        tip: BrushTip = BrushTip(),
        @Size(min = 1) paintPreferences: List<BrushPaint> = listOf(BrushPaint()),
    ) : this(
        BrushCoatNative.create(
            tip.nativePointer,
            paintPreferences.let {
                require(it.isNotEmpty()) { "BrushCoat.paintPreferences cannot be empty" }
                it.map { paint -> paint.nativePointer }.toLongArray()
            },
        ),
        tip,
        paintPreferences,
    )

    /**
     * Creates a [BrushCoat] with the given [tip] and [paint].
     *
     * @param tip The tip used to apply the paint.
     * @param paint The paint to be applied for this coat.
     */
    @JvmOverloads
    public constructor(tip: BrushTip = BrushTip(), paint: BrushPaint) : this(tip, listOf(paint))

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @JvmSynthetic
    public fun copy(
        tip: BrushTip = this.tip,
        @Size(min = 1) paintPreferences: List<BrushPaint> = this.paintPreferences,
    ): BrushCoat {
        return if (tip == this.tip && paintPreferences == this.paintPreferences) {
            this
        } else {
            BrushCoat(tip, paintPreferences)
        }
    }

    /**
     * Whether the brush can be supported by the attributes in the given [MeshFormat]. For use in
     * Stroke.copy to determine if mesh regeneration is needed when the brush is changed.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun isCompatibleWithMeshFormat(meshFormat: MeshFormat): Boolean {
        return BrushCoatNative.isCompatibleWithMeshFormat(nativePointer, meshFormat.nativePointer)
    }

    /**
     * Returns a [Builder] with values set equivalent to `this`. Java developers, use the returned
     * builder to build a copy of a BrushCoat.
     */
    public fun toBuilder(): Builder = Builder().setTip(tip).setPaintPreferences(paintPreferences)

    /**
     * Builder for [BrushCoat].
     *
     * For Java developers, use BrushCoat.Builder to construct [BrushCoat] with default values,
     * overriding only as needed. For example: `BrushCoat family = new
     * BrushCoat.Builder().tip(presetBrushTip).build();`
     */
    @Suppress("ScopeReceiverThis")
    public class Builder {
        private var tip: BrushTip = BrushTip()
        private var paintPreferences = mutableListOf<BrushPaint>()

        public fun setTip(tip: BrushTip): Builder = apply { this.tip = tip }

        public fun addPaintPreference(paint: BrushPaint): Builder = apply {
            this.paintPreferences.add(paint)
        }

        public fun setPaintPreferences(@Size(min = 1) paintPreferences: List<BrushPaint>): Builder =
            apply {
                this.paintPreferences.clear()
                this.paintPreferences.addAll(paintPreferences)
            }

        public fun build(): BrushCoat = BrushCoat(tip, paintPreferences)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BrushCoat) return false
        return this === other || (tip == other.tip && paintPreferences == other.paintPreferences)
    }

    override fun hashCode(): Int {
        var result = tip.hashCode()
        result = 31 * result + paintPreferences.hashCode()
        return result
    }

    override fun toString(): String = "BrushCoat(tip=$tip, paintPreferences=$paintPreferences)"

    /** Deletes native BrushCoat memory. */
    // NOMUTANTS -- Not tested post garbage collection.
    protected fun finalize() {
        // Note that the instance becomes finalizable at the conclusion of the Object constructor,
        // which
        // in Kotlin is always before any non-default field initialization has been done by a
        // derived
        // class constructor.
        if (nativePointer == 0L) return
        BrushCoatNative.free(nativePointer)
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
                List(BrushCoatNative.getBrushPaintPreferencesCount(unownedNativePointer)) { index ->
                    BrushPaint.wrapNative(
                        BrushCoatNative.newCopyOfBrushPaintPreference(unownedNativePointer, index)
                    )
                },
            )
    }
}

private object BrushCoatNative {
    init {
        NativeLoader.load()
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    @UsedByNative
    external fun create(tipNativePointer: Long, paintPreferencesNativePointers: LongArray): Long

    /** Release the underlying memory allocated in [create]. */
    @UsedByNative external fun free(nativePointer: Long)

    @UsedByNative
    external fun isCompatibleWithMeshFormat(
        nativePointer: Long,
        meshFormatNativePointer: Long,
    ): Boolean

    /**
     * Returns a new, unowned native pointer to a copy of the `BrushTip` in the pointed-at
     * `BrushCoat`.
     */
    @UsedByNative external fun newCopyOfBrushTip(nativePointer: Long): Long

    @UsedByNative external fun getBrushPaintPreferencesCount(nativePointer: Long): Int

    /**
     * Returns a new, unowned native pointer to a copy of the `BrushPaint` in the pointed-at
     * `BrushCoat`.
     */
    @UsedByNative external fun newCopyOfBrushPaintPreference(nativePointer: Long, index: Int): Long
}
