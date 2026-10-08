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

@file:JvmName("HeightModifierKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.compose.state.RemoteFloat

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.height(height: IntrinsicSize): RemoteModifier {
    return if (height == IntrinsicSize.Min) {
        then(HeightModifier(Type.INTRINSIC_MIN, RemoteFloat(0f)))
    } else {
        then(HeightModifier(Type.INTRINSIC_MAX, RemoteFloat(0f)))
    }
}
