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

package androidx.testutils

/**
 * Generate all argument enumerations for Parameterized tests. For example,
 * `generateAllEnumerations(listOf(false, true), listOf(1, 2, 3))` would return:
 *
 * ```
 * [
 *   [false, 1],
 *   [false, 2],
 *   [false, 3],
 *   [true, 1],
 *   [true, 2],
 *   [true, 3]
 * ]
 * ```
 *
 * See [ParameterizedHelperTest] for more examples.
 */
// TODO(kuanyingchou): Remove and replace with TestParameterInjector"
fun generateAllEnumerations(vararg args: List<Any>): List<Array<Any>> =
    generateAllEnumerationsIteratively(args.toList()).map { it.toTypedArray() }

internal fun generateAllEnumerationsIteratively(elements: List<List<Any>>): List<List<Any>> {
    if (elements.isEmpty()) return emptyList()
    var number = elements.map { RadixDigit(it.size, 0) }
    val total = elements.map { it.size }.product()
    val result = mutableListOf<List<Any>>()
    for (i in 0 until total) {
        result.add(elements.mapIndexed { index, element -> element[number[index].digit] })
        number = increment(number)
    }
    return result
}

internal fun increment(number: List<RadixDigit>): List<RadixDigit> {
    var index = number.size - 1
    var carry = 1
    val result = mutableListOf<RadixDigit>()
    while (index >= 0) {
        val rd = number[index]
        if (carry > 0) {
            if (rd.digit < rd.radix - 1) {
                result.add(rd.copy(digit = rd.digit + 1))
                carry = 0
            } else {
                result.add(rd.copy(digit = 0))
            }
        } else {
            result.add(rd)
        }
        index--
    }
    return result.reversed()
}

internal fun List<Int>.product() = this.fold(1) { acc, elem -> acc * elem }

internal data class RadixDigit(val radix: Int, val digit: Int)
