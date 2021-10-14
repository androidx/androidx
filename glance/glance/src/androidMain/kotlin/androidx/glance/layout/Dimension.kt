/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.layout

import androidx.annotation.DimenRes
import androidx.annotation.RestrictTo
import androidx.glance.Modifier
import androidx.glance.unit.Dp

/**
 * Dimension types. This contains all the dimension types which are supported by androidx.glance.
 *
 * These should only be used internally; developers should be using the width/height Modifiers
 * below rather than this class directly.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed class Dimension {
    public class Dp(public val dp: androidx.glance.unit.Dp) : Dimension()
    public object Wrap : Dimension()
    public object Fill : Dimension()
    public object Expand : Dimension()
    public class Resource(@DimenRes public val res: Int) : Dimension()
}

/**
 * Modifier to represent the width of an element.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WidthModifier(public val width: Dimension) : Modifier.Element

/** Sets the absolute width of an element, in [Dp]. */
public fun Modifier.width(width: Dp): Modifier = this.then(WidthModifier(Dimension.Dp(width)))

/** Set the width of a view from the value of a resource. */
public fun Modifier.width(@DimenRes width: Int): Modifier =
    this.then(WidthModifier(Dimension.Resource(width)))

/** Specifies that the width of the element should wrap its contents. */
public fun Modifier.wrapContentWidth(): Modifier = this.then(WidthModifier(Dimension.Wrap))

/**
 * Specifies that the width of the element should expand to the size of its parent. Note that if
 * multiple elements within a linear container (e.g. Row or Column) have their width as
 * [fillMaxWidth], then they will all share the remaining space.
 */
public fun Modifier.fillMaxWidth(): Modifier = this.then(WidthModifier(Dimension.Fill))

/**
 * Modifier to represent the height of an element.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HeightModifier(public val height: Dimension) : Modifier.Element

/** Sets the absolute height of an element, in [Dp]. */
public fun Modifier.height(height: Dp): Modifier = this.then(HeightModifier(Dimension.Dp(height)))

/** Set the height of the view from a resource. */
public fun Modifier.height(@DimenRes height: Int): Modifier =
    this.then(HeightModifier(Dimension.Resource(height)))

/** Specifies that the height of the element should wrap its contents. */
public fun Modifier.wrapContentHeight(): Modifier = this.then(HeightModifier(Dimension.Wrap))

/**
 * Specifies that the height of the element should expand to the size of its parent. Note that if
 * multiple elements within a linear container (e.g. Row or Column) have their height as
 * expandHeight, then they will all share the remaining space.
 */
public fun Modifier.fillMaxHeight(): Modifier = this.then(HeightModifier(Dimension.Fill))

/** Sets both the width and height of an element, in [Dp]. */
public fun Modifier.size(size: Dp): Modifier = this.width(size).height(size)

/** Sets both width and height of an element from a resource. */
public fun Modifier.size(@DimenRes size: Int): Modifier = this.width(size).height(size)

/** Sets both the width and height of an element, in [Dp]. */
public fun Modifier.size(width: Dp, height: Dp): Modifier = this.width(width).height(height)

/** Sets both the width and height of an element from resources. */
public fun Modifier.size(@DimenRes width: Int, @DimenRes height: Int): Modifier =
    this.width(width).height(height)

/** Wrap both the width and height's content. */
public fun Modifier.wrapContentSize(): Modifier = this.wrapContentHeight().wrapContentWidth()

/** Set both the width and height to the maximum available space. */
public fun Modifier.fillMaxSize(): Modifier = this.fillMaxWidth().fillMaxHeight()