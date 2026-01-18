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
import androidx.compose.foundation.layout.padding
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PaddingModifier(
    public val left: RemoteFloat,
    public val top: RemoteFloat,
    public val right: RemoteFloat,
    public val bottom: RemoteFloat,
) : RemoteModifier.Element {
    init {
        require(
            (!left.hasConstantValue || left.constantValue >= 0f) and
                (!top.hasConstantValue || top.constantValue >= 0f) and
                (!right.hasConstantValue || right.constantValue >= 0f) and
                (!bottom.hasConstantValue || bottom.constantValue >= 0f)
        ) {
            "Padding must be non-negative"
        }
    }

    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.PaddingModifier(
            left.floatId,
            top.floatId,
            right.floatId,
            bottom.floatId,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.padding(
    left: RemoteFloat = 0f.rf,
    top: RemoteFloat = 0f.rf,
    right: RemoteFloat = 0f.rf,
    bottom: RemoteFloat = 0f.rf,
): RemoteModifier = then(PaddingModifier(left, top, right, bottom))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.padding(all: RemoteFloat): RemoteModifier = padding(all, all, all, all)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.padding(
    horizontal: RemoteFloat = 0f.rf,
    vertical: RemoteFloat = 0f.rf,
): RemoteModifier =
    padding(left = horizontal, top = vertical, right = horizontal, bottom = vertical)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun RemoteModifier.padding(all: Dp): RemoteModifier =
    padding(left = all, top = all, right = all, bottom = all)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun RemoteModifier.padding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
): RemoteModifier {
    return with(LocalDensity.current) {
        padding(
            RemoteFloat(left.toPx()),
            RemoteFloat(top.toPx()),
            RemoteFloat(right.toPx()),
            RemoteFloat(bottom.toPx()),
        )
    }
}

@Composable
public fun RemoteModifier.padding(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): RemoteModifier =
    padding(left = horizontal, top = vertical, right = horizontal, bottom = vertical)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun RemoteModifier.padding(padding: RemotePaddingValues): RemoteModifier =
    then(
        with(LocalDensity.current) {
            // TODO(b/466078229): uses padding modifiers that takes RemoteDp
            PaddingModifier(
                padding.leftPadding.value * density,
                padding.topPadding.value * density,
                padding.rightPadding.value * density,
                padding.bottomPadding.value * density,
            )
        }
    )
