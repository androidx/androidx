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

package androidx.compose.ui.node

import androidx.compose.runtime.collection.mutableVectorOf
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NestedVectorStackTest {

    @Test
    fun testEnumerationOrder() {
        val stack = NestedVectorStack<Int>()
        stack.push(mutableVectorOf(1, 2, 3))
        stack.push(mutableVectorOf(4, 5, 6))

        Truth
            .assertThat(stack.enumerate())
            .isEqualTo(listOf(6, 5, 4, 3, 2, 1))
    }

    @Test
    fun testEnumerationOrderPartiallyPoppingMiddleVectors() {
        val stack = NestedVectorStack<Int>()
        stack.push(mutableVectorOf(1, 2, 3))

        Truth.assertThat(stack.pop()).isEqualTo(3)

        stack.push(mutableVectorOf(4, 5, 6))

        Truth.assertThat(stack.pop()).isEqualTo(6)

        Truth
            .assertThat(stack.enumerate())
            .isEqualTo(listOf(5, 4, 2, 1))
    }

    @Test
    fun testEnumerationOrderFullyPoppingMiddleVectors() {
        val stack = NestedVectorStack<Int>()
        stack.push(mutableVectorOf(1, 2, 3))

        Truth.assertThat(stack.pop()).isEqualTo(3)
        Truth.assertThat(stack.pop()).isEqualTo(2)
        Truth.assertThat(stack.pop()).isEqualTo(1)

        stack.push(mutableVectorOf(4, 5, 6))

        Truth.assertThat(stack.pop()).isEqualTo(6)

        Truth
            .assertThat(stack.enumerate())
            .isEqualTo(listOf(5, 4))
    }
}

internal fun <T> NestedVectorStack<T>.enumerate(): List<T> {
    val result = mutableListOf<T>()
    var item: T? = pop()
    while (item != null) {
        result.add(item)
        item = if (isNotEmpty()) pop() else null
    }
    return result
}
