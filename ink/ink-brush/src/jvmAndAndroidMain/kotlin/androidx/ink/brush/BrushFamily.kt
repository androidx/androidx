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
import androidx.collection.MutableIntObjectMap
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.util.Collections.unmodifiableList
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * A [BrushFamily] describes a family of brushes (e.g. “highlighter” or “pressure pen”),
 * irrespective of their size or color.
 *
 * For now, [BrushFamily] is an opaque type that can only be instantiated via [StockBrushes]. A
 * future version of this module will allow creating fully custom [BrushFamily] objects.
 *
 * [BrushFamily] objects are immutable.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class BrushFamily
private constructor(
    /** A handle to the underlying native [BrushFamily] object. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val nativePointer: Long,
    coats: List<BrushCoat>,
    /** The [InputModel] that will be used by a [Brush] in this [BrushFamily]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public val inputModel: InputModel,
) {

    /** The [BrushCoat]s that make up this [BrushFamily]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public val coats: List<BrushCoat> = unmodifiableList(coats.toList())

    /** Client-provided identifier for this [BrushFamily]. */
    // Cached to avoid converting C++ string to JVM string every time.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public val clientBrushFamilyId: String = BrushFamilyNative.getClientBrushFamilyId(nativePointer)

    /**
     * Creates a [BrushFamily] with the given [BrushCoat]s.
     *
     * @param coats The [BrushCoat]s that make up this [BrushFamily].
     * @param clientBrushFamilyId Optional-provided identifier for this [BrushFamily].
     * @param inputModel The [InputModel] that will be used by a [Brush] in this [BrushFamily].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @ExperimentalInkCustomBrushApi
    @JvmOverloads
    public constructor(
        coats: List<BrushCoat>,
        clientBrushFamilyId: String = "",
        inputModel: InputModel = DEFAULT_INPUT_MODEL,
    ) : this(
        BrushFamilyNative.create(
            coats.map { it.nativePointer }.toLongArray(),
            clientBrushFamilyId,
            inputModel.nativePointer,
        ),
        coats,
        inputModel,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @ExperimentalInkCustomBrushApi
    @JvmOverloads
    public constructor(
        tip: BrushTip = BrushTip(),
        paint: BrushPaint = BrushPaint(),
        clientBrushFamilyId: String = "",
        inputModel: InputModel = DEFAULT_INPUT_MODEL,
    ) : this(listOf(BrushCoat(tip, paint)), clientBrushFamilyId, inputModel)

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @ExperimentalInkCustomBrushApi
    @JvmSynthetic
    public fun copy(
        coats: List<BrushCoat> = this.coats,
        clientBrushFamilyId: String = this.clientBrushFamilyId,
        inputModel: InputModel = this.inputModel,
    ): BrushFamily {
        return if (
            coats == this.coats &&
                clientBrushFamilyId == this.clientBrushFamilyId &&
                inputModel == this.inputModel
        ) {
            this
        } else {
            BrushFamily(coats, clientBrushFamilyId, inputModel)
        }
    }

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @ExperimentalInkCustomBrushApi
    @JvmSynthetic
    public fun copy(
        coat: BrushCoat,
        clientBrushFamilyId: String = this.clientBrushFamilyId,
        inputModel: InputModel = this.inputModel,
    ): BrushFamily {
        return copy(
            coats = listOf(coat),
            clientBrushFamilyId = clientBrushFamilyId,
            inputModel = inputModel,
        )
    }

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @ExperimentalInkCustomBrushApi
    @JvmSynthetic
    public fun copy(
        tip: BrushTip,
        paint: BrushPaint,
        clientBrushFamilyId: String = this.clientBrushFamilyId,
        inputModel: InputModel = this.inputModel,
    ): BrushFamily {
        return copy(
            coat = BrushCoat(tip, paint),
            clientBrushFamilyId = clientBrushFamilyId,
            inputModel = inputModel,
        )
    }

    /**
     * Returns a [Builder] with values set equivalent to `this`. Java developers, use the returned
     * builder to build a copy of a BrushFamily.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @ExperimentalInkCustomBrushApi
    public fun toBuilder(): Builder =
        Builder()
            .setCoats(coats)
            .setClientBrushFamilyId(clientBrushFamilyId)
            .setInputModel(inputModel)

    /**
     * Builder for [BrushFamily].
     *
     * For Java developers, use BrushFamily.Builder to construct [BrushFamily] with default values,
     * overriding only as needed. For example: `BrushFamily family = new
     * BrushFamily.Builder().coat(presetBrushCoat).build();`
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @ExperimentalInkCustomBrushApi
    public class Builder {
        private var coats: List<BrushCoat> = listOf(BrushCoat(BrushTip(), BrushPaint()))
        private var clientBrushFamilyId: String = ""
        private var inputModel: InputModel = DEFAULT_INPUT_MODEL

        public fun setCoat(tip: BrushTip, paint: BrushPaint): Builder =
            setCoat(BrushCoat(tip, paint))

        public fun setCoat(coat: BrushCoat): Builder = setCoats(listOf(coat))

        public fun setCoats(coats: List<BrushCoat>): Builder {
            this.coats = coats.toList()
            return this
        }

        public fun setClientBrushFamilyId(clientBrushFamilyId: String): Builder {
            this.clientBrushFamilyId = clientBrushFamilyId
            return this
        }

        public fun setInputModel(inputModel: InputModel): Builder {
            this.inputModel = inputModel
            return this
        }

        public fun build(): BrushFamily = BrushFamily(coats, clientBrushFamilyId, inputModel)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BrushFamily) return false
        // NOMUTANTS -- Check the instance first to short circuit faster.
        if (other === this) return true
        return coats == other.coats &&
            clientBrushFamilyId == other.clientBrushFamilyId &&
            inputModel == other.inputModel
    }

    override fun hashCode(): Int {
        var result = coats.hashCode()
        result = 31 * result + clientBrushFamilyId.hashCode()
        result = 31 * result + inputModel.hashCode()
        return result
    }

    override fun toString(): String =
        "BrushFamily(coats=$coats, clientBrushFamilyId=$clientBrushFamilyId, inputModel=$inputModel)"

    /** Deletes native BrushFamily memory. */
    // NOMUTANTS -- Not tested post garbage collection.
    protected fun finalize() {
        // Note that the instance becomes finalizable at the conclusion of the Object constructor,
        // which
        // in Kotlin is always before any non-default field initialization has been done by a
        // derived
        // class constructor.
        if (nativePointer == 0L) return
        BrushFamilyNative.free(nativePointer)
    }

    // Companion object gets initialized before anything else.
    public companion object {
        /**
         * Construct a [BrushFamily] from an unowned heap-allocated native pointer to a C++
         * `BrushFamily`. Kotlin wrapper objects nested under the [BrushFamily] are initialized
         * similarly using their own [wrapNative] methods, passing those pointers to newly
         * copy-constructed heap-allocated objects. That avoids the need to call Kotlin constructors
         * for those objects from C++.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(unownedNativePointer: Long): BrushFamily =
            BrushFamily(
                unownedNativePointer,
                (0 until BrushFamilyNative.getBrushCoatCount(unownedNativePointer)).map { i ->
                    BrushCoat.wrapNative(
                        BrushFamilyNative.newCopyOfBrushCoat(unownedNativePointer, i)
                    )
                },
                InputModel.wrapNative(BrushFamilyNative.newCopyOfInputModel(unownedNativePointer)),
            )

        /** Returns a new [BrushFamily.Builder]. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
        @ExperimentalInkCustomBrushApi
        @JvmStatic
        public fun builder(): Builder = Builder()

        /** The recommended spring-based input modeler. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
        @ExperimentalInkCustomBrushApi
        @JvmField
        public val SPRING_MODEL: InputModel = NoParametersModel.SPRING_MODEL

        /**
         * A naive model that passes through raw inputs mostly unchanged. This is an experimental
         * configuration which may be adjusted or removed later.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
        @ExperimentalInkCustomBrushApi
        @JvmField
        public val EXPERIMENTAL_NAIVE_MODEL: InputModel = NoParametersModel.EXPERIMENTAL_NAIVE_MODEL

        /** The default [InputModel] that will be used by a [BrushFamily] when none is specified. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
        @ExperimentalInkCustomBrushApi
        @JvmField
        public val DEFAULT_INPUT_MODEL: InputModel = SlidingWindowModel()
    }

    /**
     * Specifies a model for turning a sequence of raw hardware inputs (e.g. from a stylus,
     * touchscreen, or mouse) into a sequence of smoothed, modeled inputs. Raw hardware inputs tend
     * to be noisy, and must be smoothed before being passed into a brush's behaviors and extruded
     * into a mesh in order to get a good-looking stroke.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @ExperimentalInkCustomBrushApi
    public abstract class InputModel internal constructor(internal val nativePointer: Long) {
        // NOMUTANTS -- Not tested post garbage collection.
        protected fun finalize() {
            // Note that the instance becomes finalizable at the conclusion of the Object
            // constructor,
            // which in Kotlin is always before any non-default field initialization has been done
            // by a
            // derived class constructor.
            if (nativePointer == 0L) return
            InputModelNative.free(nativePointer)
        }

        internal companion object {
            internal fun wrapNative(unownedNativePointer: Long): InputModel {
                val type = InputModelNative.getType(unownedNativePointer)
                when (type) {
                    4 -> {
                        return SlidingWindowModel(unownedNativePointer)
                    }
                    else -> {
                        InputModelNative.free(unownedNativePointer)
                        return NoParametersModel.fromInputModelType(type)
                    }
                }
            }
        }
    }

    internal class NoParametersModel private constructor(type: Int, private val name: String) :
        InputModel(InputModelNative.createNoParametersModel(type)) {
        init {
            check(type !in TYPE_TO_INSTANCE) { "Duplicate NoParametersModel type: $type" }
            TYPE_TO_INSTANCE[type] = this
        }

        override public fun toString(): String = name

        internal companion object {
            private val TYPE_TO_INSTANCE = MutableIntObjectMap<NoParametersModel>()

            fun fromInputModelType(type: Int): NoParametersModel =
                checkNotNull(TYPE_TO_INSTANCE[type]) { "Invalid NoParametersModel type: $type" }

            val SPRING_MODEL = NoParametersModel(1, "SpringModel")
            val EXPERIMENTAL_NAIVE_MODEL = NoParametersModel(3, "ExperimentalNaiveModel")
            // SlidingWindowModel, below, uses type 4.
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @ExperimentalInkCustomBrushApi
    public class SlidingWindowModel internal constructor(nativePointer: Long) :
        InputModel(nativePointer) {
        public val windowDurationMillis: Long =
            InputModelNative.getSlidingWindowDurationMillis(nativePointer)
        public val upsamplingFrequencyHz: Int =
            InputModelNative.getSlidingUpsamplingFrequencyHz(nativePointer)

        /** Constructs a `SlidingWindowModel` with default parameters. */
        public constructor() :
            this(InputModelNative.createSlidingWindowModelWithDefaultParameters())

        public constructor(
            windowDurationMillis: Long,
            upsamplingFrequencyHz: Int,
        ) : this(
            InputModelNative.createSlidingWindowModel(windowDurationMillis, upsamplingFrequencyHz)
        )

        override public fun toString(): String =
            "SlidingWindowModel(windowDurationMillis=${windowDurationMillis}, " +
                "upsamplingFrequencyHz=${upsamplingFrequencyHz})"

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is SlidingWindowModel) return false
            return windowDurationMillis == other.windowDurationMillis &&
                upsamplingFrequencyHz == other.upsamplingFrequencyHz
        }

        override fun hashCode(): Int {
            var result = windowDurationMillis.hashCode()
            result = 31 * result + upsamplingFrequencyHz.hashCode()
            return result
        }
    }
}

/** Singleton wrapper around native JNI calls. */
@UsedByNative
@OptIn(ExperimentalInkCustomBrushApi::class)
private object BrushFamilyNative {
    init {
        NativeLoader.load()
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    @UsedByNative
    external fun create(
        coatNativePointers: LongArray,
        clientBrushFamilyId: String,
        inputModelPointer: Long,
    ): Long

    /** Release the underlying memory allocated in [create]. */
    @UsedByNative external fun free(nativePointer: Long)

    @UsedByNative external fun getClientBrushFamilyId(nativePointer: Long): String

    @UsedByNative external fun getBrushCoatCount(nativePointer: Long): Int

    /**
     * Returns a new, unowned native pointer to a copy of the `BrushCoat` at index for the
     * pointed-at native `BrushFamily`.
     */
    @UsedByNative external fun newCopyOfBrushCoat(nativePointer: Long, index: Int): Long

    /**
     * Returns a new, unowned native pointer to a copy of the [InputModel] for the pointed-at native
     * [BrushFamily].
     */
    @UsedByNative external fun newCopyOfInputModel(nativePointer: Long): Long
}

/** Singleton wrapper around native JNI calls. */
@UsedByNative
@OptIn(ExperimentalInkCustomBrushApi::class)
private object InputModelNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative external fun createNoParametersModel(type: Int): Long

    @UsedByNative
    external fun createSlidingWindowModel(
        windowDurationMillis: Long,
        upsamplingFrequencyHz: Int,
    ): Long

    @UsedByNative external fun createSlidingWindowModelWithDefaultParameters(): Long

    @UsedByNative external fun free(nativePointer: Long)

    @UsedByNative external fun getType(nativePointer: Long): Int

    @UsedByNative external fun getSlidingWindowDurationMillis(nativePointer: Long): Long

    @UsedByNative external fun getSlidingUpsamplingFrequencyHz(nativePointer: Long): Int
}
