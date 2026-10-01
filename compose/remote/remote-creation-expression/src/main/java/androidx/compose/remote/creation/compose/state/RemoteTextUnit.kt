/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.state.RemoteTextUnit.OperationKey
import androidx.compose.remote.creation.compose.text.RemoteFontScaleConverter
import androidx.compose.runtime.Stable

/** The type of a [RemoteTextUnit]. */
public enum class RemoteTextUnitType {
    Sp
}

/**
 * Represents a TextUnit value backed by a [RemoteFloat].
 *
 * @property value The [RemoteFloat] that holds the scalar value.
 * @property type The [RemoteTextUnitType].
 */
@Stable
public class RemoteTextUnit
public constructor(
    public val value: RemoteFloat,
    public val type: RemoteTextUnitType = RemoteTextUnitType.Sp,
) : BaseRemoteState<Float>(RemoteStateInstanceKey()) {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override val cacheKey: RemoteStateCacheKey
        get() = RemoteOperationCacheKey.create(OperationKey.Sp, value, type)

    /**
     * Creates a [RemoteTextUnit] with the given literal [Float] [value] and [type].
     *
     * @param value The literal float value.
     * @param type The [RemoteTextUnitType]. Defaults to [RemoteTextUnitType.Sp].
     */
    public constructor(
        value: Float,
        type: RemoteTextUnitType = RemoteTextUnitType.Sp,
    ) : this(value.rf, type)

    internal enum class OperationKey : RemoteOperation {
        Sp,
        ToPx;

        override fun toDebugString(args: List<RemoteStateCacheKey>): String {
            return when (this) {
                Sp -> "${args[0].toOperandString(100)}.sp"
                ToPx -> "${args[0].toOperandString(100)}.toPx()"
            }
        }
    }

    @get:Suppress("AutoBoxing")
    override val constantValueOrNull: Float?
        get() = value.constantValueOrNull

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        return toPx(creationState.remoteDensity).writeToDocument(creationState)
    }

    /**
     * Converts this [RemoteTextUnit] to pixels using the provided [density].
     *
     * @param density The [RemoteDensity] used to convert this [RemoteTextUnit] to pixels.
     */
    public fun toPx(density: RemoteDensity): RemoteFloat {
        checkTextUnit()
        val constValue = value.constantValueOrNull
        val constDensity = density.density.constantValueOrNull
        val constFontScale = density.fontScale.constantValueOrNull

        if (constValue != null && constDensity != null && constFontScale != null) {
            val dp = RemoteFontScaleConverter.NonLinear.convertSpToDp(this, density.fontScale)
            return dp * density.density
        }

        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey =
                RemoteOperationCacheKey.create(
                    OperationKey.ToPx,
                    value,
                    type,
                    density.density,
                    density.fontScale,
                ),
        ) { creationState ->
            val dp = RemoteFontScaleConverter.NonLinear.convertSpToDp(this, density.fontScale)
            (dp * density.density).arrayForCreationState(creationState)
        }
    }

    /** Converts this [RemoteTextUnit] to pixels using the screen's density. */
    public fun toPx(): RemoteFloat {
        checkTextUnit()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.ToPx, value, type),
        ) { creationState ->
            val density = creationState.remoteDensity
            val dp = RemoteFontScaleConverter.NonLinear.convertSpToDp(this, density.fontScale)
            (dp * density.density).arrayForCreationState(creationState)
        }
    }

    private fun checkTextUnit() =
        check(type == RemoteTextUnitType.Sp) { "Only Sp is supported for conversion to pixels" }
}

/** Extension property to convert an [Int] to a [RemoteTextUnit] in Sp. */
public val Int.rsp: RemoteTextUnit
    get() = RemoteTextUnit(this.rf, RemoteTextUnitType.Sp)

/** Extension property to convert a [Float] to a [RemoteTextUnit] in Sp. */
public val Float.rsp: RemoteTextUnit
    get() = RemoteTextUnit(this.rf, RemoteTextUnitType.Sp)

/**
 * Extension property to convert a [RemoteFloat] to a [RemoteTextUnit] in Sp.
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteTextDynamicFontSizeSample
 */
public val RemoteFloat.rsp: RemoteTextUnit
    get() = RemoteTextUnit(this, RemoteTextUnitType.Sp)
