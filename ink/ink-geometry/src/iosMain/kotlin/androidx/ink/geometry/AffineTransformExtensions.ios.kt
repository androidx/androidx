/*
 * Copyright (C) 2026 The Android Open Source Project
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

@file:OptIn(ExperimentalForeignApi::class)

package androidx.ink.geometry

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGAffineTransform
import platform.CoreGraphics.CGAffineTransformMake

/*
 * Extensions for [AffineTransform] for use on iOS.
 *
 * `CGAffineTransform` is row-major with row vectors, structured like:
 * ```
 *   ⎡a   b    0 ⎤
 *   ⎢c   d    0 ⎥
 *   ⎣tx  ty   1 ⎦
 * ```
 *
 * [AffineTransform] is column-major with column vectors. The column-major equivalent is:
 * ```
 *   ⎡m00=a  m10=c  m20=tx ⎤
 *   ⎢m01=b  m11=d  m21=ty ⎥
 *   ⎣0      0      1      ⎦
 * ```
 */

/** Returns a [CValue] holding a `CGAffineTransform` with the values from this [AffineTransform]. */
public fun AffineTransform.toCgAffineTransform(): CValue<CGAffineTransform> =
    CGAffineTransformMake(
        a = m00.toDouble(),
        c = m10.toDouble(),
        tx = m20.toDouble(),
        b = m01.toDouble(),
        d = m11.toDouble(),
        ty = m21.toDouble(),
    )

/**
 * Constructs an [ImmutableAffineTransform] with the values from [transform].
 *
 * Performance-sensitive code should use the [populateFrom] overload that takes a pre-allocated
 * [MutableAffineTransform], so that the instance can be reused across multiple calls.
 */
public fun ImmutableAffineTransform.Companion.from(
    transform: CValue<CGAffineTransform>
): ImmutableAffineTransform = transform.useContents {
    ImmutableAffineTransform(
        m00 = a.toFloat(),
        m10 = c.toFloat(),
        m20 = tx.toFloat(),
        m01 = b.toFloat(),
        m11 = d.toFloat(),
        m21 = ty.toFloat(),
    )
}

/**
 * Fills this [MutableAffineTransform] with the values from [transform].
 *
 * Leaves [transform] unchanged. Returns this modified instance to allow chaining calls.
 *
 * @return `this`
 */
public fun MutableAffineTransform.populateFrom(
    transform: CValue<CGAffineTransform>
): MutableAffineTransform {
    transform.useContents {
        m00 = a.toFloat()
        m10 = c.toFloat()
        m20 = tx.toFloat()
        m01 = b.toFloat()
        m11 = d.toFloat()
        m21 = ty.toFloat()
    }
    return this
}
