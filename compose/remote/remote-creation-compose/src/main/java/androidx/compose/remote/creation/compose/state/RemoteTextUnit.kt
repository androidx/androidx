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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Represents a TextUnit value (Sp or Em) backed by a [RemoteFloat].
 *
 * @property value The [RemoteFloat] that holds the scalar value.
 * @property type The [TextUnitType] (Sp or Em).
 */
public class RemoteTextUnit
internal constructor(public val value: RemoteFloat, public val type: TextUnitType) :
    BaseRemoteState<TextUnit>() {
    internal override val cacheKey: RemoteStateCacheKey
        get() = toPx().cacheKey

    internal enum class OperationKey {
        ToPx,
        DpToSp,
    }

    init {
        require(type == TextUnitType.Sp || type == TextUnitType.Em) {
            "TextUnitType must be Sp or Em, but was $type"
        }
    }

    override val constantValueOrNull: TextUnit?
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get() {
            val constValue = value.constantValueOrNull ?: return null
            return when (type) {
                TextUnitType.Sp -> constValue.sp
                TextUnitType.Em -> constValue.em
                else -> throw IllegalStateException("Unsupported TextUnitType: $type")
            }
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        return toPx(creationState.remoteDensity).writeToDocument(creationState)
    }

    /** Converts this [RemoteTextUnit] to pixels using the provided [density]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toPx(density: RemoteDensity): RemoteFloat {
        checkTextUnit()
        val dp = RemoteFontScaleConverter.NonLinear.convertSpToDp(this, density.fontScale)
        return dp * density.density
    }

    /** Converts this [RemoteTextUnit] to pixels using the screen's density. */
    public fun toPx(): RemoteFloat {
        checkTextUnit()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.ToPx, value),
        ) { creationState ->
            val density = creationState.remoteDensity
            val dp = RemoteFontScaleConverter.NonLinear.convertSpToDp(this, density.fontScale)
            (dp * density.density).arrayForCreationState(creationState)
        }
    }

    private fun checkTextUnit() =
        check(type == TextUnitType.Sp) { "Only Sp is supported for conversion to pixels" }
}

/** Extension property to convert an [Int] to a [RemoteTextUnit] in Sp. */
public val Int.rsp: RemoteTextUnit
    get() = RemoteTextUnit(this.rf, TextUnitType.Sp)

/** Extension function to convert a [TextUnit] to a [RemoteTextUnit]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun TextUnit.asRemoteTextUnit(): RemoteTextUnit = RemoteTextUnit(this.value.rf, this.type)

/** Extension function to convert a [Dp] to a [RemoteTextUnit] in Sp. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Dp.toRsp(): RemoteTextUnit {
    check(isSpecified) { "Dp conversion not possible for unspecified Dp" }
    return RemoteTextUnit(
        value =
            RemoteFloatExpression(
                constantValueOrNull = null,
                cacheKey = RemoteOperationCacheKey.create(OperationKey.DpToSp, value),
            ) { creationState ->
                val fontScale = creationState.remoteDensity.fontScale
                (value.rf / fontScale).arrayForCreationState(creationState)
            },
        type = TextUnitType.Sp,
    )
}
