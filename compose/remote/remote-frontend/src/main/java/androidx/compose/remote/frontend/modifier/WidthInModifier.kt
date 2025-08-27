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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.modifier

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

class WidthInModifier(val min: Dp = Dp.Unspecified, val max: Dp = Dp.Unspecified) :
    RemoteLayoutModifier {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        var minValue = 0f
        var maxValue = Float.MAX_VALUE
        if (min != Dp.Unspecified) {
            minValue = min.value
        }
        if (max != Dp.Unspecified) {
            maxValue = max.value
        }
        return androidx.compose.remote.creation.modifiers.WidthInModifier(minValue, maxValue)
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        return widthIn(min, max)
    }
}

@Composable
fun RemoteModifier.widthIn(min: Dp = Dp.Unspecified, max: Dp = Dp.Unspecified): RemoteModifier {
    return then(WidthInModifier(min, max))
}
