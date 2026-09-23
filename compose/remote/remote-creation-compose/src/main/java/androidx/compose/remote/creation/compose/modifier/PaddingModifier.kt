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
import androidx.compose.remote.creation.compose.capture.RemoteDensityBehavior
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.modifiers.PaddingModifier as CreationPaddingModifier
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.unit.LayoutDirection

internal class PaddingModifier(
    public val start: RemoteFloat,
    public val top: RemoteFloat,
    public val end: RemoteFloat,
    public val bottom: RemoteFloat,
    internal val startDp: RemoteDp? = null,
    internal val topDp: RemoteDp? = null,
    internal val endDp: RemoteDp? = null,
    internal val bottomDp: RemoteDp? = null,
) : RemoteModifier.Element {

    public constructor(
        start: RemoteDp,
        top: RemoteDp,
        end: RemoteDp,
        bottom: RemoteDp,
    ) : this(
        start = start.toPx(),
        top = top.toPx(),
        end = end.toPx(),
        bottom = bottom.toPx(),
        startDp = start,
        topDp = top,
        endDp = end,
        bottomDp = bottom,
    )

    init {
        require(
            (!start.hasConstantValue || start.constantValue >= 0f) and
                (!top.hasConstantValue || top.constantValue >= 0f) and
                (!end.hasConstantValue || end.constantValue >= 0f) and
                (!bottom.hasConstantValue || bottom.constantValue >= 0f)
        ) {
            "Padding must be non-negative"
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        val resolvedLeft = resolveLeft(this)
        val resolvedTop = resolveTop(this)
        val resolvedRight = resolveRight(this)
        val resolvedBottom = resolveBottom(this)
        return CreationPaddingModifier(
            resolvedLeft.floatId,
            resolvedTop.floatId,
            resolvedRight.floatId,
            resolvedBottom.floatId,
        )
    }
}

internal fun PaddingModifier.resolveStart(scope: RemoteStateScope): RemoteFloat =
    if (scope.densityBehavior == RemoteDensityBehavior.Dp) {
        startDp?.value ?: (start / scope.remoteDensity.density)
    } else {
        start
    }

internal fun PaddingModifier.resolveTop(scope: RemoteStateScope): RemoteFloat =
    if (scope.densityBehavior == RemoteDensityBehavior.Dp) {
        topDp?.value ?: (top / scope.remoteDensity.density)
    } else {
        top
    }

internal fun PaddingModifier.resolveEnd(scope: RemoteStateScope): RemoteFloat =
    if (scope.densityBehavior == RemoteDensityBehavior.Dp) {
        endDp?.value ?: (end / scope.remoteDensity.density)
    } else {
        end
    }

internal fun PaddingModifier.resolveBottom(scope: RemoteStateScope): RemoteFloat =
    if (scope.densityBehavior == RemoteDensityBehavior.Dp) {
        bottomDp?.value ?: (bottom / scope.remoteDensity.density)
    } else {
        bottom
    }

internal fun PaddingModifier.resolveLeft(scope: RemoteStateScope): RemoteFloat =
    if (scope.layoutDirection == LayoutDirection.Ltr) resolveStart(scope) else resolveEnd(scope)

internal fun PaddingModifier.resolveRight(scope: RemoteStateScope): RemoteFloat =
    if (scope.layoutDirection == LayoutDirection.Ltr) resolveEnd(scope) else resolveStart(scope)

/**
 * Adds padding to each edge of the content.
 *
 * @param start Padding at the start edge.
 * @param top Padding at the top edge.
 * @param end Padding at the end edge.
 * @param bottom Padding at the bottom edge.
 */
public fun RemoteModifier.padding(
    start: RemoteFloat = 0f.rf,
    top: RemoteFloat = 0f.rf,
    end: RemoteFloat = 0f.rf,
    bottom: RemoteFloat = 0f.rf,
): RemoteModifier = then(PaddingModifier(start = start, top = top, end = end, bottom = bottom))

/** Adds [all] padding to each edge of the content. */
public fun RemoteModifier.padding(all: RemoteFloat): RemoteModifier = padding(all, all, all, all)

/**
 * Adds [horizontal] padding to the start and end edges, and [vertical] padding to the top and
 * bottom edges.
 */
public fun RemoteModifier.padding(
    horizontal: RemoteFloat = 0f.rf,
    vertical: RemoteFloat = 0f.rf,
): RemoteModifier = padding(start = horizontal, top = vertical, end = horizontal, bottom = vertical)

/** Adds padding defined by the [padding] object. */
public fun RemoteModifier.padding(padding: RemotePaddingValues): RemoteModifier =
    padding(
        start = padding.leftPadding,
        top = padding.topPadding,
        end = padding.rightPadding,
        bottom = padding.bottomPadding,
    )

/** Adds [all] padding to each edge of the content. */
public fun RemoteModifier.padding(all: RemoteDp): RemoteModifier =
    padding(start = all, top = all, end = all, bottom = all)

/**
 * Adds padding to each edge of the content using [RemoteDp] values.
 *
 * @param start Padding at the start edge.
 * @param top Padding at the top edge.
 * @param end Padding at the end edge.
 * @param bottom Padding at the bottom edge.
 */
public fun RemoteModifier.padding(
    start: RemoteDp = 0.rdp,
    top: RemoteDp = 0.rdp,
    end: RemoteDp = 0.rdp,
    bottom: RemoteDp = 0.rdp,
): RemoteModifier {
    return then(PaddingModifier(start = start, top = top, end = end, bottom = bottom))
}

/**
 * Adds [horizontal] padding to the start and end edges, and [vertical] padding to the top and
 * bottom edges.
 */
public fun RemoteModifier.padding(
    horizontal: RemoteDp = 0.rdp,
    vertical: RemoteDp = 0.rdp,
): RemoteModifier = padding(start = horizontal, top = vertical, end = horizontal, bottom = vertical)
