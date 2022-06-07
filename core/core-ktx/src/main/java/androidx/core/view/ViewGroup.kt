/*
 * Copyright (C) 2017 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE") // Aliases to other public API.

package androidx.core.view

import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.annotation.RequiresApi

/**
 * Returns the view at [index].
 *
 * @throws IndexOutOfBoundsException if index is less than 0 or greater than or equal to the count.
 */
public operator fun ViewGroup.get(index: Int): View =
    getChildAt(index) ?: throw IndexOutOfBoundsException("Index: $index, Size: $childCount")

/** Returns `true` if [view] is found in this view group. */
public inline operator fun ViewGroup.contains(view: View): Boolean = indexOfChild(view) != -1

/** Adds [view] to this view group. */
public inline operator fun ViewGroup.plusAssign(view: View): Unit = addView(view)

/** Removes [view] from this view group. */
public inline operator fun ViewGroup.minusAssign(view: View): Unit = removeView(view)

/** Returns the number of views in this view group. */
public inline val ViewGroup.size: Int get() = childCount

/** Returns true if this view group contains no views. */
public inline fun ViewGroup.isEmpty(): Boolean = childCount == 0

/** Returns true if this view group contains one or more views. */
public inline fun ViewGroup.isNotEmpty(): Boolean = childCount != 0

/** Performs the given action on each view in this view group. */
public inline fun ViewGroup.forEach(action: (view: View) -> Unit) {
    for (index in 0 until childCount) {
        action(getChildAt(index))
    }
}

/** Performs the given action on each view in this view group, providing its sequential index. */
public inline fun ViewGroup.forEachIndexed(action: (index: Int, view: View) -> Unit) {
    for (index in 0 until childCount) {
        action(index, getChildAt(index))
    }
}

/**
 * Returns an [IntRange] of the valid indices for the children of this view group.
 *
 * This can be used for looping:
 * ```kotlin
 * for (i in viewGroup.indices.reversed) {
 *   if (viewGroup[i] is SomeView) {
 *     viewGroup.removeViewAt(i)
 *   }
 * }
 * ```
 *
 * Or to determine if an index is valid:
 * ```kotlin
 * if (2 in viewGroup.indices) {
 *   // Do something…
 * }
 * ```
 */
public inline val ViewGroup.indices: IntRange get() = 0 until childCount

/** Returns a [MutableIterator] over the views in this view group. */
public operator fun ViewGroup.iterator(): MutableIterator<View> = object : MutableIterator<View> {
    private var index = 0
    override fun hasNext() = index < childCount
    override fun next() = getChildAt(index++) ?: throw IndexOutOfBoundsException()
    override fun remove() = removeViewAt(--index)
}

/**
 * Returns a [Sequence] over the immediate child views in this view group.
 *
 * @see View.allViews
 * @see ViewGroup.descendants
 */
public val ViewGroup.children: Sequence<View>
    get() = object : Sequence<View> {
        override fun iterator() = this@children.iterator()
    }

/**
 * Returns a [Sequence] over the child views in this view group recursively.
 * This performs a depth-first traversal.
 * A view with no children will return a zero-element sequence.
 *
 * @see View.allViews
 * @see ViewGroup.children
 * @see View.ancestors
 */
public val ViewGroup.descendants: Sequence<View>
    get() = sequence {
        forEach { child ->
            yield(child)
            if (child is ViewGroup) {
                yieldAll(child.descendants)
            }
        }
    }

/**
 * Sets the margins in the ViewGroup's MarginLayoutParams. This version of the method sets all axes
 * to the provided size.
 *
 * @see ViewGroup.MarginLayoutParams.setMargins
 */
public inline fun ViewGroup.MarginLayoutParams.setMargins(@Px size: Int) {
    setMargins(size, size, size, size)
}

/**
 * Updates the margins in the [ViewGroup]'s [ViewGroup.MarginLayoutParams].
 * This version of the method allows using named parameters to just set one or more axes.
 *
 * @see ViewGroup.MarginLayoutParams.setMargins
 */
public inline fun ViewGroup.MarginLayoutParams.updateMargins(
    @Px left: Int = leftMargin,
    @Px top: Int = topMargin,
    @Px right: Int = rightMargin,
    @Px bottom: Int = bottomMargin
) {
    setMargins(left, top, right, bottom)
}

/**
 * Updates the relative margins in the ViewGroup's MarginLayoutParams.
 * This version of the method allows using named parameters to just set one or more axes.
 *
 * @see ViewGroup.MarginLayoutParams.setMargins
 */
@RequiresApi(17)
public inline fun ViewGroup.MarginLayoutParams.updateMarginsRelative(
    @Px start: Int = marginStart,
    @Px top: Int = topMargin,
    @Px end: Int = marginEnd,
    @Px bottom: Int = bottomMargin
) {
    marginStart = start
    topMargin = top
    marginEnd = end
    bottomMargin = bottom
}
