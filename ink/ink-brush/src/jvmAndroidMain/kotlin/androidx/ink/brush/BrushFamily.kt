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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
@ExperimentalInkCustomBrushApi
@JvmOverloads
constructor(
    // The [coats] val below is a defensive copy of this parameter.
    coats: List<BrushCoat>,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public val uri: String? = null,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public val inputModel: InputModel = DEFAULT_INPUT_MODEL,
) {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public val coats: List<BrushCoat> = unmodifiableList(coats.toList())

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    @ExperimentalInkCustomBrushApi
    @JvmOverloads
    public constructor(
        tip: BrushTip = BrushTip(),
        paint: BrushPaint = BrushPaint(),
        uri: String? = null,
        inputModel: InputModel = DEFAULT_INPUT_MODEL,
    ) : this(listOf(BrushCoat(tip, paint)), uri, inputModel)

    /** A handle to the underlying native [BrushFamily] object. */
    internal val nativePointer: Long =
        nativeCreateBrushFamily(
            coats.map { it.nativePointer }.toLongArray(),
            uri,
            inputModel === SpringModel,
        )

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    @ExperimentalInkCustomBrushApi
    @JvmSynthetic
    public fun copy(
        coats: List<BrushCoat> = this.coats,
        uri: String? = this.uri,
        inputModel: InputModel = this.inputModel,
    ): BrushFamily {
        return if (coats == this.coats && uri == this.uri && inputModel == this.inputModel) {
            this
        } else {
            BrushFamily(coats, uri, inputModel)
        }
    }

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    @ExperimentalInkCustomBrushApi
    @JvmSynthetic
    public fun copy(
        coat: BrushCoat,
        uri: String? = this.uri,
        inputModel: InputModel = this.inputModel,
    ): BrushFamily {
        return copy(coats = listOf(coat), uri = uri, inputModel = inputModel)
    }

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    @ExperimentalInkCustomBrushApi
    @JvmSynthetic
    public fun copy(
        tip: BrushTip,
        paint: BrushPaint,
        uri: String? = this.uri,
        inputModel: InputModel = this.inputModel,
    ): BrushFamily {
        return copy(coat = BrushCoat(tip, paint), uri = uri, inputModel = inputModel)
    }

    /**
     * Returns a [Builder] with values set equivalent to `this`. Java developers, use the returned
     * builder to build a copy of a BrushFamily.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    @ExperimentalInkCustomBrushApi
    public fun toBuilder(): Builder =
        Builder().setCoats(coats).setUri(uri).setInputModel(inputModel)

    /**
     * Builder for [BrushFamily].
     *
     * For Java developers, use BrushFamily.Builder to construct [BrushFamily] with default values,
     * overriding only as needed. For example: `BrushFamily family = new
     * BrushFamily.Builder().coat(presetBrushCoat).build();`
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    @ExperimentalInkCustomBrushApi
    public class Builder {
        private var coats: List<BrushCoat> = listOf(BrushCoat(BrushTip(), BrushPaint()))
        private var uri: String? = null
        private var inputModel: InputModel = DEFAULT_INPUT_MODEL

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

        public fun setInputModel(inputModel: InputModel): Builder {
            this.inputModel = inputModel
            return this
        }

        public fun build(): BrushFamily = BrushFamily(coats, uri, inputModel)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BrushFamily) return false
        // NOMUTANTS -- Check the instance first to short circuit faster.
        if (other === this) return true
        return coats == other.coats && uri == other.uri && inputModel == other.inputModel
    }

    override fun hashCode(): Int {
        var result = coats.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + inputModel.hashCode()
        return result
    }

    override fun toString(): String = "BrushFamily(coats=$coats, uri=$uri, inputModel=$inputModel)"

    /** Deletes native BrushFamily memory. */
    protected fun finalize() {
        // NOMUTANTS -- Not tested post garbage collection.
        nativeFreeBrushFamily(nativePointer)
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeCreateBrushFamily(
        coatNativePointers: LongArray,
        uri: String?,
        useSpringModelV2: Boolean,
    ): Long

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
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
        @ExperimentalInkCustomBrushApi
        @JvmStatic
        public fun builder(): Builder = Builder()

        /** The recommended spring-based input modeler. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
        @ExperimentalInkCustomBrushApi
        @JvmField
        public val SPRING_MODEL: InputModel = SpringModel

        /**
         * The legacy spring-based input modeler, provided for backwards compatibility with existing
         * Ink clients.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
        @ExperimentalInkCustomBrushApi
        @JvmField
        public val LEGACY_SPRING_MODEL: InputModel = LegacySpringModel

        /** The default [InputModel] that will be used by a [BrushFamily] when none is specified. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
        @ExperimentalInkCustomBrushApi
        @JvmField
        public val DEFAULT_INPUT_MODEL: InputModel = SPRING_MODEL
    }

    /**
     * Specifies a model for turning a sequence of raw hardware inputs (e.g. from a stylus,
     * touchscreen, or mouse) into a sequence of smoothed, modeled inputs. Raw hardware inputs tend
     * to be noisy, and must be smoothed before being passed into a brush's behaviors and extruded
     * into a mesh in order to get a good-looking stroke.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    @ExperimentalInkCustomBrushApi
    public abstract class InputModel internal constructor() {}

    internal object LegacySpringModel : InputModel() {
        override fun toString(): String = "LegacySpringModel"
    }

    internal object SpringModel : InputModel() {
        override fun toString(): String = "SpringModel"
    }
}
