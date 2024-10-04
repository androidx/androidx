/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.materialcore

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toolingGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Icon component that draws a [painter] using [tint].
 *
 * @param painter [Painter] to draw inside this Icon
 * @param contentDescription Text used by accessibility services to describe what this icon
 *   represents. This should always be provided unless this icon is used for decorative purposes,
 *   and does not represent a meaningful action that a user can take. This text should be localized,
 *   such as by using [androidx.compose.ui.res.stringResource] or similar
 * @param modifier Optional [Modifier] for this Icon
 * @param tint Tint to be applied to [painter]. If [Color.Unspecified] is provided, then no tint is
 *   applied
 */
// TODO: Add link to Chip for a clickable icon
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun Icon(painter: Painter, contentDescription: String?, modifier: Modifier, tint: Color) {
    val colorFilter =
        remember(tint) { if (tint == Color.Unspecified) null else ColorFilter.tint(tint) }
    val semantics =
        if (contentDescription != null) {
            Modifier.semantics {
                this.contentDescription = contentDescription
                this.role = Role.Image
            }
        } else {
            Modifier
        }
    Box(
        modifier
            .toolingGraphicsLayer()
            .defaultSizeFor(painter)
            .paint(painter, colorFilter = colorFilter, contentScale = ContentScale.Fit)
            .then(semantics)
    )
}

internal fun Modifier.defaultSizeFor(painter: Painter) =
    this.then(
        if (painter.intrinsicSize == Size.Unspecified || painter.intrinsicSize.isInfinite()) {
            DefaultIconSizeModifier
        } else {
            Modifier
        }
    )

internal fun Size.isInfinite() = width.isInfinite() && height.isInfinite()

// Default icon size, for icons with no intrinsic size information
internal val DefaultIconSizeModifier = Modifier.size(24.dp)
