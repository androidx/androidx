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

internal class HeightModifier(val type: Type, val value: RemoteFloat) : RemoteModifier.Element {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.HeightModifier(type, value.floatId)
    }
}

/** Sets the height of the content using [RemoteDp]. */
public fun RemoteModifier.height(height: RemoteDp): RemoteModifier =
    then(HeightModifier(Type.EXACT_DP, height.value))

/** Sets the height of the content using [RemoteFloat]. */
public fun RemoteModifier.height(height: RemoteFloat): RemoteModifier =
    then(HeightModifier(Type.EXACT, height))

/**
 * Fills the maximum available height.
 *
 * @param fraction The fraction of the maximum height to use.
 */
public fun RemoteModifier.fillMaxHeight(fraction: RemoteFloat = 1f.rf): RemoteModifier =
    then(HeightModifier(Type.FILL, fraction))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.fillMaxHeight(fraction: Float): RemoteModifier =
    then(HeightModifier(Type.FILL, RemoteFloat(fraction)))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.fillParentMaxHeight(fraction: Float): RemoteModifier =
    then(HeightModifier(Type.FILL_PARENT_MAX_HEIGHT, RemoteFloat(fraction)))

/**
 * Fills the parent's maximum available height.
 *
 * @param fraction The fraction of the parent's maximum height to use.
 */
public fun RemoteModifier.fillParentMaxHeight(fraction: RemoteFloat = 1f.rf): RemoteModifier =
    then(HeightModifier(Type.FILL_PARENT_MAX_HEIGHT, fraction))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.height(height: Int): RemoteModifier =
    then(HeightModifier(Type.EXACT, height.rf))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.height(height: IntrinsicSize): RemoteModifier {
    return if (height == IntrinsicSize.Min) {
        then(HeightModifier(Type.INTRINSIC_MIN, RemoteFloat(0f)))
    } else {
        then(HeightModifier(Type.INTRINSIC_MAX, RemoteFloat(0f)))
    }
}
