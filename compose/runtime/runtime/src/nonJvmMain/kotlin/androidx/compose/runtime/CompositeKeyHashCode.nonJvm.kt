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

@file:Suppress("NOTHING_TO_INLINE", "EXTENSION_SHADOWED_BY_MEMBER")

package androidx.compose.runtime

import kotlin.text.toString as stdlibToString

public actual typealias CompositeKeyHashCode = Long

public actual inline fun CompositeKeyHashCode.toLong(): CompositeKeyHashCode = this

public actual inline fun CompositeKeyHashCode.toString(radix: Int): String =
    this.stdlibToString(radix)

internal actual inline fun CompositeKeyHashCode(initial: Int) = initial.toLong()

internal actual inline fun CompositeKeyHashCode.compoundWith(segment: Int, shift: Int) =
    (this rol shift) xor segment.toLong()

internal actual inline fun CompositeKeyHashCode.unCompoundWith(segment: Int, shift: Int) =
    (this xor segment.toLong()) ror shift

internal actual inline fun CompositeKeyHashCode.bottomUpCompoundWith(segment: Int, shift: Int) =
    this xor (segment.toLong() rol shift)

internal actual inline fun CompositeKeyHashCode.bottomUpCompoundWith(
    segment: CompositeKeyHashCode,
    shift: Int,
) = this xor (segment rol shift)

internal actual const val CompositeKeyHashSizeBits: Int = 64

public actual const val EmptyCompositeKeyHashCode: Long = 0L
