/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.modifiers.RecordingModifier

internal class WidthModifier(public val type: Type, public val value: RemoteFloat) :
    RemoteModifier.Element {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.WidthModifier(type, value.floatId)
    }
}

/** Sets the width of the content using [RemoteDp]. */
public fun RemoteModifier.width(width: RemoteDp): RemoteModifier =
    then(WidthModifier(Type.EXACT_DP, width.value))

/** Sets the width of the content using [RemoteFloat]. */
public fun RemoteModifier.width(width: RemoteFloat): RemoteModifier =
    then(WidthModifier(Type.EXACT, width))

/**
 * Fills the maximum available width.
 *
 * @param fraction The fraction of the maximum width to use.
 */
public fun RemoteModifier.fillMaxWidth(fraction: RemoteFloat = RemoteFloat(1f)): RemoteModifier =
    then(WidthModifier(Type.FILL, fraction))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.fillMaxWidth(fraction: Float): RemoteModifier =
    then(WidthModifier(Type.FILL, RemoteFloat(fraction)))

/**
 * Fills the parent's maximum available width.
 *
 * @param fraction The fraction of the parent's maximum width to use.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.fillParentMaxWidth(fraction: Float): RemoteModifier =
    then(WidthModifier(Type.FILL_PARENT_MAX_WIDTH, RemoteFloat(fraction)))

/**
 * Fills the parent's maximum available width.
 *
 * @param fraction The fraction of the parent's maximum width to use.
 */
public fun RemoteModifier.fillParentMaxWidth(fraction: RemoteFloat = 1f.rf): RemoteModifier =
    then(WidthModifier(Type.FILL_PARENT_MAX_WIDTH, fraction))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.width(width: Int): RemoteModifier =
    then(WidthModifier(Type.EXACT, width.rf))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.width(width: IntrinsicSize): RemoteModifier {
    return if (width == IntrinsicSize.Min) {
        then(WidthModifier(Type.INTRINSIC_MIN, 0f.rf))
    } else {
        then(WidthModifier(Type.INTRINSIC_MAX, 0f.rf))
    }
}
