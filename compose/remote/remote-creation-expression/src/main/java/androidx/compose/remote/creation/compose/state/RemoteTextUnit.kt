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
internal constructor(
    public val value: RemoteFloat,
    public val type: RemoteTextUnitType,
    cacheKey: RemoteStateCacheKey,
) : BaseRemoteState<Float>(cacheKey) {

    internal enum class OperationKey : RemoteOperation {
        Sp,
        DpToSp,
        ToPx;

        override fun toDebugString(args: List<RemoteStateCacheKey>): String {
            return when (this) {
                Sp -> "${args[0].toOperandString(100)}.sp"
                DpToSp -> "${args[0].toDebugString()}.dpToSp()"
                ToPx -> "${args[0].toOperandString(100)}.toPx()"
            }
        }
    }

    @get:Suppress("AutoBoxing")
    override val constantValueOrNull: Float?
        get() = value.constantValueOrNull

    internal override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        return toPx(creationState.remoteDensity).writeToDocument(creationState)
    }

    /**
     * Converts this [RemoteTextUnit] to pixels using the provided [density].
     *
     * @param density The [RemoteDensity] used to convert this [RemoteTextUnit] to pixels.
     * @return A [RemoteFloat] representing this text size in pixels.
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

        return RemoteFloat(
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

    /**
     * Converts this [RemoteTextUnit] to pixels using the current [RemoteDensity].
     *
     * @return A [RemoteFloat] representing this text size in pixels.
     */
    public fun toPx(): RemoteFloat {
        checkTextUnit()
        return RemoteFloat(
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

/**
 * Creates a [RemoteTextUnit] backed by a [RemoteFloat].
 *
 * @param value The [RemoteFloat] that holds the scalar value.
 * @param type The [RemoteTextUnitType]. Defaults to [RemoteTextUnitType.Sp].
 * @return A [RemoteTextUnit] wrapping the given [RemoteFloat].
 */
public fun RemoteTextUnit(
    value: RemoteFloat,
    type: RemoteTextUnitType = RemoteTextUnitType.Sp,
): RemoteTextUnit =
    RemoteTextUnit(
        value = value,
        type = type,
        cacheKey = RemoteOperationCacheKey.create(RemoteTextUnit.OperationKey.Sp, value, type),
    )

/**
 * Creates a [RemoteTextUnit] with the given literal [Float] [value] and [type].
 *
 * @param value The literal float value.
 * @param type The [RemoteTextUnitType]. Defaults to [RemoteTextUnitType.Sp].
 * @return A [RemoteTextUnit] representing the constant text unit.
 */
public fun RemoteTextUnit(
    value: Float,
    type: RemoteTextUnitType = RemoteTextUnitType.Sp,
): RemoteTextUnit = RemoteTextUnit(value.rf, type)

/** Extension property to convert an [Int] to a [RemoteTextUnit] in Sp. */
public val Int.rsp: RemoteTextUnit
    get() = RemoteTextUnit(this.rf, RemoteTextUnitType.Sp)

/** Extension property to convert a [Float] to a [RemoteTextUnit] in Sp. */
public val Float.rsp: RemoteTextUnit
    get() = RemoteTextUnit(this.rf, RemoteTextUnitType.Sp)

/**
 * Extension property to convert a [RemoteFloat] to a [RemoteTextUnit] in Sp.
 *
 * Example of computing a dynamic font size from a [RemoteFloat] expression:
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteTextDynamicFontSizeSample
 */
public val RemoteFloat.rsp: RemoteTextUnit
    get() = RemoteTextUnit(this, RemoteTextUnitType.Sp)

/**
 * Converts this [RemoteDp] to a [RemoteTextUnit] in Sp.
 *
 * @return A [RemoteTextUnit] in Sp converted from this [RemoteDp].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteDp.toRsp(): RemoteTextUnit =
    RemoteTextUnit(
        value =
            RemoteFloat(
                constantValueOrNull = null,
                cacheKey = RemoteOperationCacheKey.create(OperationKey.DpToSp, value),
            ) { creationState ->
                (value / creationState.remoteDensity.fontScale).arrayForCreationState(creationState)
            },
        type = RemoteTextUnitType.Sp,
    )
