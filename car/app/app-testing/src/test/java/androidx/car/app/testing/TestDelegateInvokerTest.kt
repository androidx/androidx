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

package androidx.car.app.testing

import androidx.car.app.serialization.ListDelegateImpl
import androidx.car.app.testing.TestDelegateInvoker.requestAllItemsForTest
import androidx.car.app.testing.TestDelegateInvoker.requestItemRangeForTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

/** Tests for {@link TestDelegateInvoker}. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
class TestDelegateInvokerTest {
    @Test
    fun requestAllItemsForTest() {
        val numbers = List(10) { it }
        val listDelegate = ListDelegateImpl(numbers)

        assertThat(listDelegate.requestAllItemsForTest()).containsExactlyElementsIn(numbers)
    }

    @Test
    fun requestItemRangeForTest() {
        val numbers = List(10) { it }
        val listDelegate = ListDelegateImpl(numbers)

        assertThat(listDelegate.requestItemRangeForTest(3, 5))
            .containsExactlyElementsIn(numbers.subList(3, 6)) // indices 3, 4, 5
    }
}
