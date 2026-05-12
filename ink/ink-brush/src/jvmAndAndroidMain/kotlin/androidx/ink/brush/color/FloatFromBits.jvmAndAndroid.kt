/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.ink.brush.color

// This function is technically identical to Float.fromBits(). However,
// since they are declared as top-level functions, they do not incur the
// cost of a static fetch through the Companion class. Using these
// top-level functions, the generated arm64 code after dex2oat is exactly
// a single `fmov`
actual internal inline fun floatFromBits(bits: Int): Float = java.lang.Float.intBitsToFloat(bits)
