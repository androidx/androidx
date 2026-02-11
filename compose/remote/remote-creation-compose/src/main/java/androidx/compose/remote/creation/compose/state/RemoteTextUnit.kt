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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Represents a TextUnit value (Sp or Em) backed by a [RemoteFloat].
 *
 * @property value The [RemoteFloat] that holds the scalar value.
 * @property type The [TextUnitType] (Sp or Em).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteTextUnit(public val value: RemoteFloat, public val type: TextUnitType) :
    BaseRemoteState<TextUnit>() {

    override val constantValueOrNull: TextUnit?
        get() {
            val constValue = value.constantValueOrNull ?: return null
            return when (type) {
                TextUnitType.Sp -> constValue.sp
                TextUnitType.Em -> constValue.em
                else -> throw IllegalStateException("Unsupported TextUnitType: $type")
            }
        }

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        return toPx(creationState.remoteDensity).writeToDocument(creationState)
    }

    /**
     * Converts this [RemoteTextUnit] to pixels using the provided [density].
     *
     * Note: Only [TextUnitType.Sp] is supported for conversion to pixels via [RemoteDensity].
     * [TextUnitType.Em] requires current font size context which is not available here.
     */
    public fun toPx(density: RemoteDensity): RemoteFloat {
        return when (type) {
            TextUnitType.Sp -> value * density.fontScale * density.density
            TextUnitType.Em -> value
            else -> throw IllegalStateException("Unsupported TextUnitType: $type")
        }
    }
}

/** Extension property to convert an [Int] to a [RemoteTextUnit] in Sp. */
public val Int.rsp: RemoteTextUnit
    get() = RemoteTextUnit(this.rf, TextUnitType.Sp)

/** Extension function to convert a [TextUnit] to a [RemoteTextUnit]. */
public val TextUnit.asRemote: RemoteTextUnit
    get() = RemoteTextUnit(this.value.rf, this.type)
