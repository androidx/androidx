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

package androidx.compose.ui.geometry

// Masks everything but the sign bit
@PublishedApi internal const val DualUnsignedFloatMask = 0x7fffffff_7fffffffL

// Any value greater than this is a NaN
@PublishedApi internal const val FloatInfinityBase = 0x7f800000

// Same as above, but for floats packed in a Long
@PublishedApi internal const val DualFloatInfinityBase = 0x7f800000_7f800000L

// Same as Offset/Size.Unspecified.packedValue, but avoids a getstatic
@PublishedApi internal const val UnspecifiedPackedFloats = 0x7fc00000_7fc00000L // NaN_NaN

// 0x80000000_80000000UL.toLong() but expressed as a const value
// Mask for the sign bit of the two floats packed in a long
@PublishedApi internal const val DualFloatSignBit = -0x7fffffff_80000000L

// Set the highest bit of each 32 bit chunk in a 64 bit word
@PublishedApi internal const val Uint64High32 = -0x7fffffff_80000000L

// Set the lowest bit of each 32 bit chunk in a 64 bit word
@PublishedApi internal const val Uint64Low32 = 0x00000001_00000001L

// Encodes the first valid NaN in each of the 32 bit chunk of a 64 bit word
@PublishedApi internal const val DualFirstNaN = 0x7f800001_7f800001L
