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

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ParameterizedHelperTest {
    @Test
    fun testIncrement() {
        val number = listOf(RadixDigit(2, 0), RadixDigit(3, 0))

        assertThat(::increment.invoke(number, 1))
            .isEqualTo(listOf(RadixDigit(2, 0), RadixDigit(3, 1)))
        assertThat(::increment.invoke(number, 2))
            .isEqualTo(listOf(RadixDigit(2, 0), RadixDigit(3, 2)))
        assertThat(::increment.invoke(number, 3))
            .isEqualTo(listOf(RadixDigit(2, 1), RadixDigit(3, 0)))
        assertThat(::increment.invoke(number, 4))
            .isEqualTo(listOf(RadixDigit(2, 1), RadixDigit(3, 1)))
        assertThat(::increment.invoke(number, 5))
            .isEqualTo(listOf(RadixDigit(2, 1), RadixDigit(3, 2)))
        assertThat(::increment.invoke(number, 6))
            .isEqualTo(listOf(RadixDigit(2, 0), RadixDigit(3, 0)))
        assertThat(::increment.invoke(number, 7))
            .isEqualTo(listOf(RadixDigit(2, 0), RadixDigit(3, 1)))
    }

    @Test
    fun testProduct() {
        assertThat(listOf<Int>().product()).isEqualTo(1)
        assertThat(listOf(0).product()).isEqualTo(0)
        assertThat(listOf(2).product()).isEqualTo(2)
        assertThat(listOf(2, 3).product()).isEqualTo(6)
    }

    @Test
    fun testEnumerations() {
        assertThat(generateAllEnumerations()).isEmpty()

        // Comparing List of Arrays doesn't work(https://github.com/google/truth/issues/928), so
        // we're mapping it to List of Lists
        assertThat(generateAllEnumerations(listOf(false)).map { it.toList() })
            .isEqualTo(listOf(listOf<Any>(false)))
        assertThat(generateAllEnumerations(listOf(false, true)).map { it.toList() })
            .isEqualTo(listOf(listOf<Any>(false), listOf<Any>(true)))
        assertThat(generateAllEnumerations(listOf(false, true), listOf())).isEmpty()
        assertThat(
                generateAllEnumerations(listOf(false, true), listOf(false, true)).map {
                    it.toList()
                }
            )
            .isEqualTo(
                listOf(
                    listOf(false, false),
                    listOf(false, true),
                    listOf(true, false),
                    listOf(true, true)
                )
            )
        assertThat(
                generateAllEnumerations(listOf(false, true), (0..2).toList(), listOf("low", "hi"))
                    .map { it.toList() }
            )
            .isEqualTo(
                listOf(
                    listOf(false, 0, "low"),
                    listOf(false, 0, "hi"),
                    listOf(false, 1, "low"),
                    listOf(false, 1, "hi"),
                    listOf(false, 2, "low"),
                    listOf(false, 2, "hi"),
                    listOf(true, 0, "low"),
                    listOf(true, 0, "hi"),
                    listOf(true, 1, "low"),
                    listOf(true, 1, "hi"),
                    listOf(true, 2, "low"),
                    listOf(true, 2, "hi")
                )
            )
    }

    // `::f.invoke(0, 3)` is equivalent to `f(f(f(0)))`
    private fun <T> ((T) -> T).invoke(argument: T, repeat: Int): T {
        var result = argument
        for (i in 0 until repeat) {
            result = this(result)
        }
        return result
    }

    @Test
    fun testInvoke() {
        val addOne = { i: Int -> i + 1 }
        assertThat(addOne.invoke(42, 0)).isEqualTo(42)
        assertThat(addOne.invoke(42, 1)).isEqualTo(addOne(42))
        assertThat(addOne.invoke(42, 2)).isEqualTo(addOne(addOne(42)))

        val appendA = { str: String -> str + "a" }
        assertThat(appendA.invoke("a", 2)).isEqualTo(appendA(appendA("a")))
    }
}
