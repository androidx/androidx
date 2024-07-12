/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.car.app.serialization

import androidx.car.app.OnDoneCallback
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ListDelegateTest {
    private val testList = (100..199).toList()
    private val listDelegate = ListDelegateImpl(testList)
    private val onDoneCallback = mock<OnDoneCallback>()
    private val resultCaptor = argumentCaptor<Bundleable>()

    @Test
    fun requestItemRange_sendsCorrectItems() {
        // Single item, first and last
        assertThat(requestItemRange(0, 0)).containsExactly(100)
        assertThat(requestItemRange(99, 99)).containsExactly(199)

        // Random-ish ranges
        assertThat(requestItemRange(0, 3)).containsExactlyElementsIn((100..103).toList())
        assertThat(requestItemRange(1, 3)).containsExactlyElementsIn((101..103).toList())
        assertThat(requestItemRange(2, 3)).containsExactlyElementsIn((102..103).toList())
        assertThat(requestItemRange(3, 7)).containsExactlyElementsIn((103..107).toList())
    }

    @Test
    fun requestItemRange_invalidRange_crashes() {
        assertInvalidIndices(-1, 1) // start out of bounds
        assertInvalidIndices(1, testList.indices.last + 1) // end out of bounds
        assertInvalidIndices(2, 1) // end before start
    }

    @Test
    fun equalsAndHashCode_sameInstance_areEqual() =
        ListDelegateImpl(testList).let { assertEqual(it, it) }

    @Test
    fun equalsAndHashCode_equivalentItems_areEqual() =
        testList.let { assertEqual(ListDelegateImpl(it), ListDelegateImpl(it)) }

    @Test
    fun equalsAndHashCode_marshalledItem_areEqual() =
        ListDelegateImpl(testList).let { assertEqual(it, marshallUnmarshall(it)) }

    @Test
    fun equalsAndHashCode_differentItems_areNotEqual() =
        assertNotEqual(ListDelegateImpl((10..19).toList()), ListDelegateImpl((20..29).toList()))

    private fun assertInvalidIndices(startIndex: Int, endIndex: Int) {
        assertThrows(AssertionError::class.java) { requestItemRange(startIndex, endIndex) }
    }

    private fun requestItemRange(startIndex: Int, endIndex: Int): List<Int> {
        // Serialize and deserialize the ListDelegate
        @Suppress("UNCHECKED_CAST")
        val remoteListDelegate =
            Bundler.fromBundle(Bundler.toBundle(listDelegate)) as ListDelegate<Int>

        remoteListDelegate.requestItemRange(startIndex, endIndex, onDoneCallback)

        verify(onDoneCallback, atLeastOnce()).onSuccess(resultCaptor.capture())

        @Suppress("UNCHECKED_CAST") return resultCaptor.lastValue.get() as List<Int>
    }

    private fun <T : Any> assertEqual(a: T, b: T) {
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(a)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    private fun <T : Any> assertNotEqual(a: T, b: T) {
        assertThat(a).isNotEqualTo(b)
        assertThat(b).isNotEqualTo(a)
        assertThat(a.hashCode()).isNotEqualTo(b.hashCode())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> marshallUnmarshall(obj: T): T =
        Bundler.fromBundle(Bundler.toBundle(obj)) as T
}
