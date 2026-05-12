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
import androidx.ink.brush.ImmutableCollections.unmodifiableList
import androidx.ink.nativeloader.NativePointer
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * A [BrushFamily] describes a family of brushes (e.g. “highlighter” or “pressure pen”),
 * irrespective of their size or color. It can be thought of as roughly analogous to a font family.
 *
 * [BrushFamily] objects are immutable.
 */
public class BrushFamily
private constructor(
    nativeAlloc: () -> Long,
    coats: List<BrushCoat>? = null,
    inputModel: InputModel? = null,
) {

    /** A handle to the underlying native [BrushFamily] object. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val nativePointer: Long by NativePointer(nativeAlloc, BrushFamilyNative::free)

    /** An immutable list of the [BrushCoat]s that make up this [BrushFamily]. */
    public val coats: List<BrushCoat> =
        unmodifiableList(
            // If we just constructed this in Kotlin, use the coats provided.
            coats
                // Otherwise, copy them from the native object.
                ?: (0 until BrushFamilyNative.getBrushCoatCount(nativePointer)).map { i ->
                    BrushCoat.wrapNative { BrushFamilyNative.newCopyOfBrushCoat(nativePointer, i) }
                }
        )

    /** The [InputModel] that will be used by a [Brush] in this [BrushFamily]. */
    public val inputModel: InputModel =
        // If we just constructed this in Kotlin, use the input model object provided.
        inputModel
            // Otherwise, copy it from the native object.
            ?: InputModel.wrapNative(BrushFamilyNative.getInputModelType(nativePointer)) {
                BrushFamilyNative.newCopyOfInputModel(nativePointer)
            }

    /** Client-provided identifier for this [BrushFamily]. */
    // Cached to avoid converting C++ string to JVM string every time.
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    public val clientBrushFamilyId: String = BrushFamilyNative.getClientBrushFamilyId(nativePointer)

    /**
     * A multi-line, human-readable string with a description of the brush and how it works, with
     * the intended audience being designers/developers who are editing the brush definition. This
     * string is not generally intended to be displayed to end users.
     */
    public val developerComment: String = BrushFamilyNative.getDeveloperComment(nativePointer)

    /**
     * Returns true if this [BrushFamily] contains serialized fallback data representing similar
     * [BrushFamily]s that are compatible with other versions of Ink. If true, the stored data will
     * be used when serializing this [BrushFamily] to a proto instead of recomputing it from the
     * [BrushFamily]. This data is created by serializing multiple [BrushFamily]s together with
     * `encodeMultiple`, and stored in a [BrushFamily] created by `decode`, if it exists in the
     * proto. Fallback data is not preserved when modifying a [BrushFamily] with [copy] or
     * [toBuilder].
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @get:JvmName("hasFallbacks")
    public val hasFallbacks: Boolean = BrushFamilyNative.hasFallbacks(nativePointer)

    /**
     * Returns the minimum required [Version] for this [BrushFamily].
     *
     * By default, decoding a [BrushFamily] with a minimum required version higher than
     * [Version.MAX_SUPPORTED] will fail.
     */
    public fun calculateMinimumRequiredVersion(): Version =
        Version.fromInt(BrushFamilyNative.calculateMinimumRequiredVersion(nativePointer))

    /**
     * Creates a [BrushFamily] with the given [BrushCoat]s.
     *
     * @param coats The [BrushCoat]s that make up this [BrushFamily].
     * @param inputModel The [InputModel] that will be used by a [Brush] in this [BrushFamily].
     * @param developerComment A non-user-facing human-readable description of the brush family.
     */
    @JvmOverloads
    public constructor(
        coats: List<BrushCoat>,
        inputModel: InputModel = InputModel.DEFAULT_INPUT_MODEL,
        developerComment: String = "",
    ) : this(
        coats = coats,
        inputModel = inputModel,
        developerComment = developerComment,
        clientBrushFamilyId = "",
    )

    /**
     * Creates a [BrushFamily] with the given [BrushCoat]s.
     *
     * @param coats The [BrushCoat]s that make up this [BrushFamily].
     * @param inputModel The [InputModel] that will be used by a [Brush] in this [BrushFamily].
     * @param developerComment A non-user-facing human-readable description of the brush family.
     * @param clientBrushFamilyId Optional-provided identifier for this [BrushFamily].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    public constructor(
        coats: List<BrushCoat>,
        inputModel: InputModel = InputModel.DEFAULT_INPUT_MODEL,
        developerComment: String = "",
        clientBrushFamilyId: String = "",
    ) : this(
        nativeAlloc = {
            BrushFamilyNative.create(
                coatNativePointers = coats.map { it.nativePointer }.toLongArray(),
                inputModelPointer = inputModel.nativePointer,
                clientBrushFamilyId = clientBrushFamilyId,
                developerComment = developerComment,
            )
        },
        coats = coats,
        inputModel = inputModel,
    )

    /**
     * Creates a [BrushFamily] with a single [BrushCoat] that consists of the given [BrushTip] and
     * [BrushPaint].
     *
     * @param tip The [BrushTip] to use in the single [BrushCoat] for this [BrushFamily].
     * @param paint The [BrushPaint] to use in the single [BrushCoat] for this [BrushFamily].
     * @param inputModel The [InputModel] that will be used by a [Brush] in this [BrushFamily].
     * @param developerComment A non-user-facing human-readable description of the brush family.
     */
    @JvmOverloads
    public constructor(
        tip: BrushTip = BrushTip(),
        paint: BrushPaint = BrushPaint(),
        inputModel: InputModel = InputModel.DEFAULT_INPUT_MODEL,
        developerComment: String = "",
    ) : this(
        coats = listOf(BrushCoat(tip, paint)),
        inputModel = inputModel,
        developerComment = developerComment,
        clientBrushFamilyId = "",
    )

    /**
     * Creates a [BrushFamily] with a single [BrushCoat] that consists of the given [BrushTip] and
     * [BrushPaint].
     *
     * @param tip The [BrushTip] to use in the single [BrushCoat] for this [BrushFamily].
     * @param paint The [BrushPaint] to use in the single [BrushCoat] for this [BrushFamily].
     * @param inputModel The [InputModel] that will be used by a [Brush] in this [BrushFamily].
     * @param developerComment A non-user-facing human-readable description of the brush family.
     * @param clientBrushFamilyId Optional-provided identifier for this [BrushFamily].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    public constructor(
        tip: BrushTip = BrushTip(),
        paint: BrushPaint = BrushPaint(),
        inputModel: InputModel = InputModel.DEFAULT_INPUT_MODEL,
        developerComment: String = "",
        clientBrushFamilyId: String = "",
    ) : this(
        coats = listOf(BrushCoat(tip, paint)),
        inputModel = inputModel,
        developerComment = developerComment,
        clientBrushFamilyId = clientBrushFamilyId,
    )

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     *
     * Java callers should use [Builder] instead.
     */
    @Suppress("MissingJvmstatic") // no @JvmOverloads; not intended for Java callers
    public fun copy(
        coats: List<BrushCoat> = this.coats,
        inputModel: InputModel = this.inputModel,
        developerComment: String = this.developerComment,
    ): BrushFamily =
        copy(
            coats = coats,
            inputModel = inputModel,
            developerComment = developerComment,
            clientBrushFamilyId = this.clientBrushFamilyId,
        )

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     *
     * Java callers should use [Builder] instead.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @Suppress("MissingJvmstatic") // no @JvmOverloads; not intended for Java callers
    public fun copy(
        coats: List<BrushCoat> = this.coats,
        inputModel: InputModel = this.inputModel,
        developerComment: String = this.developerComment,
        clientBrushFamilyId: String = this.clientBrushFamilyId,
    ): BrushFamily {
        return if (
            coats == this.coats &&
                inputModel == this.inputModel &&
                developerComment == this.developerComment &&
                clientBrushFamilyId == this.clientBrushFamilyId
        ) {
            this
        } else {
            BrushFamily(
                coats = coats,
                inputModel = inputModel,
                developerComment = developerComment,
                clientBrushFamilyId = clientBrushFamilyId,
            )
        }
    }

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     *
     * Java callers should use [Builder] instead.
     */
    @Suppress("MissingJvmstatic") // no @JvmOverloads; not intended for Java callers
    public fun copy(
        coat: BrushCoat,
        inputModel: InputModel = this.inputModel,
        developerComment: String = this.developerComment,
    ): BrushFamily =
        copy(
            coat = coat,
            inputModel = inputModel,
            developerComment = developerComment,
            clientBrushFamilyId = this.clientBrushFamilyId,
        )

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     *
     * Java callers should use [Builder] instead.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @Suppress("MissingJvmstatic") // no @JvmOverloads; not intended for Java callers
    public fun copy(
        coat: BrushCoat,
        inputModel: InputModel = this.inputModel,
        developerComment: String = this.developerComment,
        clientBrushFamilyId: String = this.clientBrushFamilyId,
    ): BrushFamily =
        copy(
            coats = listOf(coat),
            inputModel = inputModel,
            developerComment = developerComment,
            clientBrushFamilyId = clientBrushFamilyId,
        )

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     *
     * Java callers should use [Builder] instead.
     */
    @Suppress("MissingJvmstatic") // no @JvmOverloads; not intended for Java callers
    public fun copy(
        tip: BrushTip,
        paint: BrushPaint,
        inputModel: InputModel = this.inputModel,
        developerComment: String = this.developerComment,
    ): BrushFamily =
        copy(
            tip = tip,
            paint = paint,
            inputModel = inputModel,
            developerComment = developerComment,
            clientBrushFamilyId = this.clientBrushFamilyId,
        )

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     *
     * Java callers should use [Builder] instead.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @Suppress("MissingJvmstatic") // no @JvmOverloads; not intended for Java callers
    public fun copy(
        tip: BrushTip,
        paint: BrushPaint,
        inputModel: InputModel = this.inputModel,
        developerComment: String = this.developerComment,
        clientBrushFamilyId: String = this.clientBrushFamilyId,
    ): BrushFamily =
        copy(
            coat = BrushCoat(tip, paint),
            inputModel = inputModel,
            developerComment = developerComment,
            clientBrushFamilyId = clientBrushFamilyId,
        )

    /**
     * Returns a [Builder] with values set equivalent to `this`. Java developers, use the returned
     * builder to build a copy of a BrushFamily.
     */
    public fun toBuilder(): Builder =
        Builder()
            .setCoats(coats)
            .setInputModel(inputModel)
            .setClientBrushFamilyId(clientBrushFamilyId)
            .setDeveloperComment(developerComment)

    /**
     * Builder for [BrushFamily].
     *
     * For Java developers, use `BrushFamily.Builder` to construct a [BrushFamily] with default
     * values, overriding only as needed. For example: `BrushFamily family =
     * BrushFamily.builder().setCoat(presetBrushCoat).build();`
     */
    public class Builder {
        private var coats: List<BrushCoat> = listOf(BrushCoat(BrushTip(), BrushPaint()))
        private var inputModel: InputModel = InputModel.DEFAULT_INPUT_MODEL
        private var clientBrushFamilyId: String = ""
        private var developerComment: String = ""

        /** Sets the list of brush coats for this brush family to a single coat. */
        @Suppress("MissingGetterMatchingBuilder")
        public fun setCoat(coat: BrushCoat): Builder = setCoats(listOf(coat))

        /** Sets the list of brush coats for this brush family. */
        public fun setCoats(coats: List<BrushCoat>): Builder {
            this.coats = coats.toList()
            return this
        }

        /** Sets the input model for this brush family. */
        public fun setInputModel(inputModel: InputModel): Builder {
            this.inputModel = inputModel
            return this
        }

        /** Sets the client ID for this brush family. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
        public fun setClientBrushFamilyId(clientBrushFamilyId: String): Builder {
            this.clientBrushFamilyId = clientBrushFamilyId
            return this
        }

        /** Sets the developer comment for this brush family. */
        public fun setDeveloperComment(developerComment: String): Builder {
            this.developerComment = developerComment
            return this
        }

        /** Constructs a [BrushFamily] from this [Builder]. */
        public fun build(): BrushFamily =
            BrushFamily(
                coats = coats,
                inputModel = inputModel,
                clientBrushFamilyId = clientBrushFamilyId,
                developerComment = developerComment,
            )
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BrushFamily) return false
        // NOMUTANTS -- Check the instance first to short circuit faster.
        if (other === this) return true
        return coats == other.coats &&
            inputModel == other.inputModel &&
            clientBrushFamilyId == other.clientBrushFamilyId &&
            developerComment == other.developerComment
    }

    override fun hashCode(): Int {
        var result = coats.hashCode()
        result = 31 * result + inputModel.hashCode()
        result = 31 * result + clientBrushFamilyId.hashCode()
        result = 31 * result + developerComment.hashCode()
        return result
    }

    override fun toString(): String =
        "BrushFamily(developerComment=$developerComment, coats=$coats, inputModel=$inputModel, " +
            "clientBrushFamilyId=$clientBrushFamilyId)"

    // Companion object gets initialized before anything else.
    public companion object {
        /** Construct a [BrushFamily], taking a callback that heap-allocates a C++ `BrushFamily`. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(nativeAlloc: () -> Long): BrushFamily = BrushFamily(nativeAlloc)

        /** Returns a new [BrushFamily.Builder]. */
        @JvmStatic public fun builder(): Builder = Builder()
    }

    /**
     * Specifies a model for turning a sequence of raw hardware inputs (e.g. from a stylus,
     * touchscreen, or mouse) into a sequence of smoothed, modeled inputs. Raw hardware inputs tend
     * to be noisy, and must be smoothed before being passed into a brush's behaviors and extruded
     * into a mesh in order to get a good-looking stroke.
     */
    public abstract class InputModel internal constructor(nativeAlloc: () -> Long) {

        internal val nativePointer: Long by NativePointer(nativeAlloc, InputModelNative::free)

        public companion object {
            internal fun wrapNative(inputModelType: Int, nativeAlloc: () -> Long): InputModel {
                return when (inputModelType) {
                    4 -> SlidingWindowModel(nativeAlloc)
                    else -> NoParametersModel.fromInputModelType(inputModelType)
                }
            }

            /**
             * A naive input model that passes raw inputs through unchanged, performing only
             * base-minimal modeling to derive velocity and acceleration values for the modeled
             * inputs. This can be useful as a point of comparison for other input models, or for
             * callers who wish to do their own input modeling prior to passing inputs into Ink.
             */
            @JvmField public val PASSTHROUGH_MODEL: InputModel = NoParametersModel.PASSTHROUGH_MODEL

            /**
             * The default [InputModel] that will be used by a [BrushFamily] when none is specified.
             * Currently, this is the [SlidingWindowModel], with default parameters.
             */
            @JvmField public val DEFAULT_INPUT_MODEL: InputModel = SlidingWindowModel()
        }

        internal class NoParametersModel private constructor(type: Int, private val name: String) :
            InputModel({ InputModelNative.createNoParametersModel(type) }) {
            init {
                check(type !in TYPE_TO_INSTANCE) { "Duplicate NoParametersModel type: $type" }
                TYPE_TO_INSTANCE[type] = this
            }

            override fun toString(): String = name

            internal companion object {
                private val TYPE_TO_INSTANCE = MutableIntObjectMap<NoParametersModel>()

                fun fromInputModelType(type: Int): NoParametersModel =
                    checkNotNull(TYPE_TO_INSTANCE[type]) { "Invalid NoParametersModel type: $type" }

                val PASSTHROUGH_MODEL = NoParametersModel(3, "PassthroughModel")
                // SlidingWindowModel, below, uses type 4.
            }
        }

        /** An [InputModel] that averages nearby inputs together within a sliding time window. */
        public class SlidingWindowModel internal constructor(nativeAlloc: () -> Long) :
            InputModel(nativeAlloc) {
            /**
             * The duration over which to average together nearby raw inputs. Typically this should
             * be somewhere in the 1 ms to 100 ms range.
             */
            public val windowDurationMillis: Long =
                InputModelNative.getSlidingWindowDurationMillis(nativePointer)

            /**
             * The minimum frequency at which modeled inputs should occur, or zero to disable
             * upsampling.
             */
            public val upsamplingFrequencyHz: Int =
                InputModelNative.getSlidingUpsamplingFrequencyHz(nativePointer)

            /** Constructs a `SlidingWindowModel` with default parameters. */
            public constructor() :
                this({ InputModelNative.createSlidingWindowModelWithDefaultParameters() })

            /** Constructs a `SlidingWindowModel` with the given parameters. */
            public constructor(
                windowDurationMillis: Long,
                upsamplingFrequencyHz: Int,
            ) : this({
                InputModelNative.createSlidingWindowModel(
                    windowDurationMillis,
                    upsamplingFrequencyHz,
                )
            })

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
}

expect internal object BrushFamilyNative {
    fun create(
        coatNativePointers: LongArray,
        inputModelPointer: Long,
        clientBrushFamilyId: String,
        developerComment: String,
    ): Long

    fun free(nativePointer: Long)

    fun getBrushCoatCount(nativePointer: Long): Int

    fun getClientBrushFamilyId(nativePointer: Long): String

    fun getDeveloperComment(nativePointer: Long): String

    fun calculateMinimumRequiredVersion(nativePointer: Long): Int

    fun hasFallbacks(nativePointer: Long): Boolean

    fun newCopyOfBrushCoat(nativePointer: Long, index: Int): Long

    fun getInputModelType(nativePointer: Long): Int

    fun newCopyOfInputModel(nativePointer: Long): Long
}

expect internal object InputModelNative {
    fun createNoParametersModel(type: Int): Long

    fun createSlidingWindowModel(windowDurationMillis: Long, upsamplingFrequencyHz: Int): Long

    fun createSlidingWindowModelWithDefaultParameters(): Long

    fun free(nativePointer: Long)

    fun getSlidingWindowDurationMillis(nativePointer: Long): Long

    fun getSlidingUpsamplingFrequencyHz(nativePointer: Long): Int
}
