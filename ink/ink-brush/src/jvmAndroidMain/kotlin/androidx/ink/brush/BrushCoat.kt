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
 * A [BrushCoat] represents one coat of paint applied by a brush. It includes a single [BrushPaint],
 * as well as one or more [BrushTip]s used to apply that paint. Multiple [BrushCoat] can be combined
 * within a single brush; when a stroke drawn by a multi-coat brush is rendered, each coat of paint
 * will be drawn entirely atop the previous coat, even if the stroke crosses over itself, as though
 * each coat were painted in its entirety one at a time.
 */
@ExperimentalInkCustomBrushApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class BrushCoat
@JvmOverloads
constructor(
    // The [tips] val below is a defensive copy of this parameter.
    tips: List<BrushTip>,
    /** The paint to be applied in this coat. */
    public val paint: BrushPaint = BrushPaint(),
) {

    /**
     * The tip(s) used to apply the paint.
     *
     * For now, there must be exactly one tip. This restriction is expected to be lifted in a future
     * release.
     */
    // TODO: b/285594469 - More than one tip.
    public val tips: List<BrushTip> = unmodifiableList(tips.toList())

    @JvmOverloads
    public constructor(
        tip: BrushTip = BrushTip(),
        paint: BrushPaint = BrushPaint(),
    ) : this(listOf(tip), paint)

    /** A handle to the underlying native [BrushCoat] object. */
    internal val nativePointer: Long =
        nativeCreateBrushCoat(tips.map { it.nativePointer }.toLongArray(), paint.nativePointer)

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @JvmSynthetic
    public fun copy(tips: List<BrushTip> = this.tips, paint: BrushPaint = this.paint): BrushCoat {
        return if (tips == this.tips && paint == this.paint) {
            this
        } else {
            BrushCoat(tips, paint)
        }
    }

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @JvmSynthetic
    public fun copy(tip: BrushTip, paint: BrushPaint = this.paint): BrushCoat {
        return if (this.tips.size == 1 && tip == this.tips[0] && paint == this.paint) {
            this
        } else {
            BrushCoat(tip, paint)
        }
    }

    /**
     * Returns a [Builder] with values set equivalent to `this`. Java developers, use the returned
     * builder to build a copy of a BrushCoat.
     */
    public fun toBuilder(): Builder = Builder().setTips(tips).setPaint(paint)

    /**
     * Builder for [BrushCoat].
     *
     * For Java developers, use BrushCoat.Builder to construct [BrushCoat] with default values,
     * overriding only as needed. For example: `BrushCoat family = new
     * BrushCoat.Builder().tip(presetBrushTip).build();`
     */
    public class Builder {
        private var tips: List<BrushTip> = listOf(BrushTip())
        private var paint: BrushPaint = BrushPaint()

        public fun setTip(tip: BrushTip): Builder {
            this.tips = listOf(tip)
            return this
        }

        public fun setTips(tips: List<BrushTip>): Builder {
            this.tips = tips.toList()
            return this
        }

        public fun setPaint(paint: BrushPaint): Builder {
            this.paint = paint
            return this
        }

        public fun build(): BrushCoat = BrushCoat(tips, paint)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BrushCoat) return false
        return tips == other.tips && paint == other.paint
    }

    override fun hashCode(): Int {
        var result = tips.hashCode()
        result = 31 * result + paint.hashCode()
        return result
    }

    override fun toString(): String = "BrushCoat(tips=$tips, paint=$paint)"

    /** Deletes native BrushCoat memory. */
    protected fun finalize() {
        // NOMUTANTS -- Not tested post garbage collection.
        nativeFreeBrushCoat(nativePointer)
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeCreateBrushCoat(
        tipNativePointers: LongArray,
        paintNativePointer: Long,
    ): Long

    /** Release the underlying memory allocated in [nativeCreateBrushCoat]. */
    private external fun nativeFreeBrushCoat(
        nativePointer: Long
    ) // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    // Companion object gets initialized before anything else.
    public companion object {
        init {
            NativeLoader.load()
        }

        /** Returns a new [BrushCoat.Builder]. */
        @JvmStatic public fun builder(): Builder = Builder()
    }
}
