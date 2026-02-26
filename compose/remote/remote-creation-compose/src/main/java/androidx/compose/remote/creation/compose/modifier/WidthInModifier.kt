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

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isUnspecified

internal class WidthInModifier(val min: RemoteDp? = null, val max: RemoteDp? = null) :
    RemoteModifier.Element {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        var minValue = 0f
        var maxValue = Float.MAX_VALUE
        if (min != null) {
            // specified in Dp values
            minValue = min.value.floatId
        }
        if (max != null) {
            // specified in Dp values
            maxValue = max.value.floatId
        }
        return androidx.compose.remote.creation.modifiers.WidthInModifier(minValue, maxValue)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.widthIn(
    min: Dp = Dp.Unspecified,
    max: Dp = Dp.Unspecified,
): RemoteModifier {
    return then(
        WidthInModifier(
            min = if (min.isUnspecified) null else min.asRdp(),
            max = if (max.isUnspecified) null else max.asRdp(),
        )
    )
}

/**
 * Sets the minimum and maximum width of the content.
 *
 * @param min The minimum width.
 * @param max The maximum width.
 */
@Composable
public fun RemoteModifier.widthIn(min: RemoteDp? = null, max: RemoteDp? = null): RemoteModifier {
    return then(WidthInModifier(min = min, max = max))
}
