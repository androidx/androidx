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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RemoteTextUnitType {
    Sp
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteTextUnit(
    value: RemoteFloat,
    type: RemoteTextUnitType = RemoteTextUnitType.Sp,
): RemoteTextUnit =
    RemoteTextUnit(
        value = value,
        type = type,
        cacheKey = RemoteOperationCacheKey.create(RemoteTextUnit.OperationKey.Sp, value, type),
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteTextUnit(
    value: Float,
    type: RemoteTextUnitType = RemoteTextUnitType.Sp,
): RemoteTextUnit = RemoteTextUnit(value.rf, type)

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val Int.rsp: RemoteTextUnit
    get() = RemoteTextUnit(this.rf, RemoteTextUnitType.Sp)

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val Float.rsp: RemoteTextUnit
    get() = RemoteTextUnit(this.rf, RemoteTextUnitType.Sp)

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
