/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.processing

import androidx.kruth.assertThat
import androidx.room.compiler.processing.util.MemoizedSequence
import org.junit.Test

class MemoizedSequenceTest {
    @Test
    fun test() {
        val list = listOf(1, 2, 3, 4, 5, 6)
        var startedSequence = false
        val memoized = MemoizedSequence {
            sequence<Int> {
                assertThat(startedSequence).isFalse()
                startedSequence = true
            } + list.asSequence()
        }
        assertThat(startedSequence).isFalse()
        val s1 = memoized.iterator()
        val s2 = memoized.iterator()
        assertThat(startedSequence).isFalse()
        val s1List = mutableListOf<Int>()
        val s2List = mutableListOf<Int>()
        while (s1.hasNext() || s2.hasNext()) {
            if (s1.hasNext()) {
                s1List.add(s1.next())
            }
            if (s2.hasNext()) {
                s2List.add(s2.next())
            }
        }
        assertThat(startedSequence).isTrue()
        val s3List = memoized.toList()
        assertThat(
            s1List
        ).containsExactlyElementsIn(list)
        assertThat(
            s2List
        ).containsExactlyElementsIn(list)
        assertThat(
            s3List
        ).containsExactlyElementsIn(list)
    }

    @Test
    fun empty() {
        val memoized = MemoizedSequence<Int> {
            emptySequence()
        }
        assertThat(memoized.toList()).isEmpty()
    }

    @Test
    fun noSuchElement_empty() {
        val memoized = MemoizedSequence<Int> {
            emptySequence()
        }
        val result = kotlin.runCatching {
            memoized.iterator().next()
        }
        assertThat(result.exceptionOrNull()).isInstanceOf<NoSuchElementException>()
    }

    @Test
    fun noSuchElement_notEmpty() {
        val iterator = MemoizedSequence {
            sequenceOf(1, 2, 3)
        }.iterator()
        val collected = mutableListOf<Int>()
        val result = kotlin.runCatching {
            while (true) {
                collected.add(iterator.next())
            }
        }
        assertThat(result.exceptionOrNull()).isInstanceOf<NoSuchElementException>()
        assertThat(collected).containsExactly(1, 2, 3)
    }
}
