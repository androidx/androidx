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

// / How much space should be occupied in the main axis.
// /
// / During a flex layout, available space along the main axis is allocated to
// / children. After allocating space, there might be some remaining free space.
// / This value controls whether to maximize or minimize the amount of free
// / space, subject to the incoming layout constraints.
// /
// / See also:
// /
// /  * [Column], [Row], and [Flex], the flex widgets.
// /  * [Expanded] and [Flexible], the widgets that controls a flex widgets'
// /    children's flex.
// /  * [RenderFlex], the flex render object.
// /  * [MainAxisAlignment], which controls how the free space is distributed.
enum class MainAxisSize {
    // / Minimize the amount of free space along the main axis, subject to the
    // / incoming layout constraints.
    // /
    // / If the incoming layout constraints have a large enough
    // / [BoxConstraints.minWidth] or [BoxConstraints.minHeight], there might still
    // / be a non-zero amount of free space.
    // /
    // / If the incoming layout constraints are unbounded, and any children have a
    // / non-zero [FlexParentData.flex] and a [FlexFit.TIGHT] fit (as applied by
    // / [Expanded]), the [RenderFlex] will assert, because there would be infinite
    // / remaining free space and boxes cannot be given infinite size.
    MIN,

    // / Maximize the amount of free space along the main axis, subject to the
    // / incoming layout constraints.
    // /
    // / If the incoming layout constraints have a small enough
    // / [BoxConstraints.maxWidth] or [BoxConstraints.maxHeight], there might still
    // / be no free space.
    // /
    // / If the incoming layout constraints are unbounded, the [RenderFlex] will
    // / assert, because there would be infinite remaining free space and boxes
    // / cannot be given infinite size.
    MAX
}

// / How the children should be placed along the main axis in a flex layout.
// /
// / See also:
// /
// /  * [Column], [Row], and [Flex], the flex widgets.
// /  * [RenderFlex], the flex render object.
enum class MainAxisAlignment {
    // / Place the children as close to the start of the main axis as possible.
    // /
    // / If this value is used in a horizontal direction, a [TextDirection] must be
    // / available to determine if the start is the left or the right.
    // /
    // / If this value is used in a vertical direction, a [VerticalDirection] must be
    // / available to determine if the start is the top or the bottom.
    START,

    // / Place the children as close to the end of the main axis as possible.
    // /
    // / If this value is used in a horizontal direction, a [TextDirection] must be
    // / available to determine if the end is the left or the right.
    // /
    // / If this value is used in a vertical direction, a [VerticalDirection] must be
    // / available to determine if the end is the top or the bottom.
    END,

    // / Place the children as close to the middle of the main axis as possible.
    CENTER,

    // / Place the free space evenly between the children.
    SPACE_BETWEEN,

    // / Place the free space evenly between the children as well as half of that
    // / space before and after the first and last child.
    SPACE_AROUND,

    // / Place the free space evenly between the children as well as before and
    // / after the first and last child.
    SPACE_EVENLY
}
