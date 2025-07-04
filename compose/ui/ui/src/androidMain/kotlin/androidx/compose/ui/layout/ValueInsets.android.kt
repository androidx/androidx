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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.layout

import androidx.core.graphics.Insets

/**
 * A value class version of insets, made to reduce the number of allocations and State value reads.
 */
@JvmInline
internal value class ValueInsets(val packedValue: Long) {
    val left: Int
        inline get() = ((packedValue ushr 48) and 0xFFFF).toInt()

    val top: Int
        inline get() = ((packedValue ushr 32) and 0xFFFF).toInt()

    val right: Int
        inline get() = ((packedValue ushr 16) and 0xFFFF).toInt()

    val bottom: Int
        inline get() = (packedValue and 0xFFFF).toInt()

    override fun toString(): String {
        return "ValueInsets($left, $top, $right, $bottom)"
    }
}

/** Create a [ValueInsets] from a normal [Insets] type. */
internal inline fun ValueInsets(insets: Insets): ValueInsets =
    ValueInsets(
        (insets.left.toLong() shl 48) or
            (insets.top.toLong() shl 32) or
            (insets.right.toLong() shl 16) or
            (insets.bottom.toLong())
    )

/** Create a [ValueInsets] from individual values. */
internal inline fun ValueInsets(left: Int, top: Int, right: Int, bottom: Int): ValueInsets =
    ValueInsets(
        (left.toLong() shl 48) or
            (top.toLong() shl 32) or
            (right.toLong() shl 16) or
            (bottom.toLong())
    )

/** A [ValueInsets] with all values set to `0`. */
internal val ZeroValueInsets = ValueInsets(0L)

/** A [ValueInsets] representing `null` or unset values. */
internal val UnsetValueInsets = ValueInsets(0xFFFF_FFFF_FFFF_FFFFUL.toLong())
