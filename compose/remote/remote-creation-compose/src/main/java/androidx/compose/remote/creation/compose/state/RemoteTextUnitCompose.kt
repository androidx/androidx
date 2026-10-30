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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/** Creates a [RemoteTextUnit] from a [RemoteFloat] and a Compose [TextUnitType]. */
public fun RemoteTextUnit(
    value: RemoteFloat,
    textUnitType: TextUnitType,
): RemoteTextUnit =
    RemoteTextUnit(
        value,
        when (textUnitType) {
            TextUnitType.Sp -> RemoteTextUnitType.Sp
            else ->
                throw IllegalArgumentException(
                    "Only Sp is supported for RemoteTextUnit, but was $textUnitType"
                )
        },
    )

/**
 * Converts this [RemoteTextUnit] to a [TextUnit] if it has a constant value.
 *
 * @throws IllegalStateException if this state does not have a constant value.
 */
public fun RemoteTextUnit.asConstantTextUnit(): TextUnit =
    when (type) {
        RemoteTextUnitType.Sp -> constantValue.sp
    }

/** Converts this [RemoteTextUnit] to a [TextUnit] if it has a constant value, or null otherwise. */
public fun RemoteTextUnit.asConstantTextUnitOrNull(): TextUnit? {
    val constValue = constantValueOrNull ?: return null
    return when (type) {
        RemoteTextUnitType.Sp -> constValue.sp
    }
}

/** Extension function to convert a [TextUnit] to a [RemoteTextUnit]. */
public fun TextUnit.asRemoteTextUnit(): RemoteTextUnit {
    val unitType =
        when (type) {
            TextUnitType.Sp -> RemoteTextUnitType.Sp
            else -> throw IllegalArgumentException("Unsupported TextUnitType: $type")
        }
    return RemoteTextUnit(this.value.rf, unitType)
}

/** Extension function to convert a [Dp] to a [RemoteTextUnit] in Sp. */
public fun Dp.toRsp(): RemoteTextUnit {
    check(isSpecified) { "Dp conversion not possible for unspecified Dp" }
    return RemoteTextUnit(
        value =
            RemoteFloatExpression(
                constantValueOrNull = null,
                cacheKey = RemoteOperationCacheKey.create(TextUnitComposeOp.DpToSp, value),
            ) { creationState ->
                val fontScale = creationState.remoteDensity.fontScale
                (value.rf / fontScale).arrayForCreationState(creationState)
            },
        type = RemoteTextUnitType.Sp,
    )
}

internal enum class TextUnitComposeOp : RemoteOperation {
    DpToSp;

    override fun toDebugString(args: List<RemoteStateCacheKey>): String =
        "${args[0].toDebugString()}.dpToSp()"
}
