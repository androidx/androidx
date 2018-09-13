/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.rendering.flex

// / How the children should be placed along the cross axis in a flex layout.
// /
// / See also:
// /
// /  * [Column], [Row], and [Flex], the flex widgets.
// /  * [RenderFlex], the flex render object.
enum class CrossAxisAlignment {
    // / Place the children with their start edge aligned with the start side of
    // / the cross axis.
    // /
    // / For example, in a column (a flex with a vertical axis) whose
    // / [TextDirection] is [TextDirection.ltr], this aligns the left edge of the
    // / children along the left edge of the column.
    // /
    // / If this value is used in a horizontal direction, a [TextDirection] must be
    // / available to determine if the start is the left or the right.
    // /
    // / If this value is used in a vertical direction, a [VerticalDirection] must be
    // / available to determine if the start is the top or the bottom.
    START,

    // / Place the children as close to the end of the cross axis as possible.
    // /
    // / For example, in a column (a flex with a vertical axis) whose
    // / [TextDirection] is [TextDirection.ltr], this aligns the right edge of the
    // / children along the right edge of the column.
    // /
    // / If this value is used in a horizontal direction, a [TextDirection] must be
    // / available to determine if the end is the left or the right.
    // /
    // / If this value is used in a vertical direction, a [VerticalDirection] must be
    // / available to determine if the end is the top or the bottom.
    END,

    // / Place the children so that their centers align with the middle of the
    // / cross axis.
    // /
    // / This is the default cross-axis alignment.
    CENTER,

    // / Require the children to fill the cross axis.
    // /
    // / This causes the constraints passed to the children to be tight in the
    // / cross axis.
    STRETCH,

    // / Place the children along the cross axis such that their baselines match.
    // /
    // / If the main axis is vertical, then this value is treated like [start]
    // / (since baselines are always horizontal).
    BASELINE
}