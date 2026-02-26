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
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal class PaddingModifier(
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.PaddingModifier(
            left.floatId,
            top.floatId,
            right.floatId,
            bottom.floatId,
        )
    }
}

/**
 * Adds padding to each edge of the content.
 *
 * @param left Padding at the left edge.
 * @param top Padding at the top edge.
 * @param right Padding at the right edge.
 * @param bottom Padding at the bottom edge.
 */
public fun RemoteModifier.padding(
    left: RemoteFloat = 0f.rf,
    top: RemoteFloat = 0f.rf,
    right: RemoteFloat = 0f.rf,
    bottom: RemoteFloat = 0f.rf,
): RemoteModifier = then(PaddingModifier(left = left, top = top, right = right, bottom = bottom))

/** Adds [all] padding to each edge of the content. */
public fun RemoteModifier.padding(all: RemoteFloat): RemoteModifier = padding(all, all, all, all)

/**
 * Adds [horizontal] padding to the left and right edges, and [vertical] padding to the top and
 * bottom edges.
 */
public fun RemoteModifier.padding(
    horizontal: RemoteFloat = 0f.rf,
    vertical: RemoteFloat = 0f.rf,
): RemoteModifier =
    padding(left = horizontal, top = vertical, right = horizontal, bottom = vertical)

/** Adds [all] padding to each edge of the content. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.padding(all: Dp): RemoteModifier =
    padding(left = all, top = all, right = all, bottom = all)

/**
 * Adds padding to each edge of the content using [Dp] values.
 *
 * @param left Padding at the left edge.
 * @param top Padding at the top edge.
 * @param right Padding at the right edge.
 * @param bottom Padding at the bottom edge.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.padding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
): RemoteModifier {
    return padding(
        left = left.asRdp(),
        top = top.asRdp(),
        right = right.asRdp(),
        bottom = bottom.asRdp(),
    )
}

/**
 * Adds [horizontal] padding to the left and right edges, and [vertical] padding to the top and
 * bottom edges.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.padding(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): RemoteModifier =
    padding(left = horizontal, top = vertical, right = horizontal, bottom = vertical)

/** Adds padding defined by the [padding] object. */
public fun RemoteModifier.padding(padding: RemotePaddingValues): RemoteModifier =
    padding(
        left = padding.leftPadding,
        top = padding.topPadding,
        right = padding.rightPadding,
        bottom = padding.bottomPadding,
    )

/** Adds [all] padding to each edge of the content. */
public fun RemoteModifier.padding(all: RemoteDp): RemoteModifier =
    padding(left = all, top = all, right = all, bottom = all)

/**
 * Adds padding to each edge of the content using [Dp] values.
 *
 * @param left Padding at the left edge.
 * @param top Padding at the top edge.
 * @param right Padding at the right edge.
 * @param bottom Padding at the bottom edge.
 */
public fun RemoteModifier.padding(
    left: RemoteDp = 0.rdp,
    top: RemoteDp = 0.rdp,
    right: RemoteDp = 0.rdp,
    bottom: RemoteDp = 0.rdp,
): RemoteModifier {
    return padding(
        left = left.toPx(),
        top = top.toPx(),
        right = right.toPx(),
        bottom = bottom.toPx(),
    )
}

/**
 * Adds [horizontal] padding to the left and right edges, and [vertical] padding to the top and
 * bottom edges.
 */
public fun RemoteModifier.padding(
    horizontal: RemoteDp = 0.rdp,
    vertical: RemoteDp = 0.rdp,
): RemoteModifier =
    padding(left = horizontal, top = vertical, right = horizontal, bottom = vertical)
