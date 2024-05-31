/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

// Make the content believe it's using the full dimensions of the parent, but limit it
// to the given bounds. This is used to limit the space used on screen for "full-screen" components
// like ScrollIndicator or HorizontalPageIndicator, so it doesn't interfere with a11y on the
// whole screen.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun BoundsLimiter(
    offset: Density.() -> IntOffset,
    size: Density.() -> IntSize,
    modifier: Modifier = Modifier,
    onSizeChanged: (IntSize) -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) =
    Box(
        modifier = Modifier.fillMaxSize().onSizeChanged(onSizeChanged).absoluteOffset(offset),
        contentAlignment = AbsoluteAlignment.TopLeft
    ) {
        // This Box has the position and size we need, so any modifiers passed in should be applied
        // here. We set the size using a custom modifier (that passes the constraints transparently
        // to
        // the content), and add a negative offset to make the content believe is drawing at the top
        // left (position 0, 0).
        Box(
            modifier.transparentSizeModifier(size).absoluteOffset { -offset() },
            content = content,
            contentAlignment = AbsoluteAlignment.TopLeft
        )
    }

// Sets the size of this element, but lets the child measure using the constraints
// of the element containing this.
private fun Modifier.transparentSizeModifier(size: Density.() -> IntSize): Modifier =
    this.then(
        object : LayoutModifier {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints
            ): MeasureResult {
                val placeable = measurable.measure(constraints)
                val actualSize = size()
                return layout(actualSize.width, actualSize.height) { placeable.place(0, 0) }
            }
        }
    )
