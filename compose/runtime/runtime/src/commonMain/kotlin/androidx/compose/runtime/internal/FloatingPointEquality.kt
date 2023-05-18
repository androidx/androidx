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

package androidx.compose.runtime.internal

/**
 * A backwards-compatible comparison check which returns true if the receiver and the [other]
 * operands are equal to each other. Equality as specified in the IEEE-754 standard. If at least
 * one operand is `NaN`, the result will always be `false`. The sign bit is ignored for operands
 * that are zero (i.e. +0 and -0 will always be equal to one another). All other comparisons check
 * the logical value of the float.
 *
 * This implementation is needed for proper behavior on x86 builds of Android SDK levels 21 and 22,
 * which contain a bug where [Float.NaN] is equal to every other [Float] value.
 *
 * See [issue 281205384](b/281205384).
 */
internal expect inline fun Float.equalsWithNanFix(other: Float): Boolean

/**
 * A backwards-compatible comparison check which returns true if the receiver and the [other]
 * operands are equal to each other. Equality as specified in the IEEE-754 standard. If at least
 * one operand is `NaN`, the result will always be `false`. The sign bit is ignored for operands
 * that are zero (i.e. +0 and -0 will always be equal to one another). All other comparisons check
 * the logical value of the double.
 *
 * This implementation is needed for proper behavior on x86 builds of Android SDK levels 21 and 22,
 * which contain a bug where [Double.NaN] is equal to every other [Double] value.
 *
 * See [issue 281205384](b/281205384).
 */
internal expect inline fun Double.equalsWithNanFix(other: Double): Boolean