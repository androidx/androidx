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

package androidx.window

import android.graphics.Rect

/**
 * A wrapper for [Rect] to handle compatibility issues with API 15. In API 15, equals and
 * hashCode operate on the reference as opposed to the attributes. This leads to test failures
 * because the data matches but the equals check fails.
 *
 * Also useful in unit tests since you can instantiate [Bounds] in a JVM test but when you
 * instantiate [Rect] you are using the class from the mockable jar file. The mockable jar does
 * not contain any behavior or calculations.
 */
internal class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {

    constructor(rect: Rect) : this(rect.left, rect.top, rect.right, rect.bottom)

    /**
     * Return the [Rect] representation of the bounds
     */
    fun toRect(): Rect = Rect(left, top, right, bottom)

    /**
     * The width of the bounds, may be negative.
     */
    val width: Int
        get() = right - left

    /**
     * The height of the bounds, may be negative.
     */
    val height: Int
        get() = bottom - top

    /**
     * Determines if the bounds has empty area.
     */
    val isEmpty: Boolean
        get() = height == 0 || width == 0

    override fun toString(): String {
        return "${Bounds::class.java.simpleName} { [$left,$top,$right,$bottom] }"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bounds

        if (left != other.left) return false
        if (top != other.top) return false
        if (right != other.right) return false
        if (bottom != other.bottom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = left
        result = 31 * result + top
        result = 31 * result + right
        result = 31 * result + bottom
        return result
    }
}