/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.foundation.change_notifier

import androidx.ui.VoidCallback
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify

@RunWith(JUnit4::class)
class ValueNotifierTest {

    data class Data(val data: String)

    @Test
    fun `changed notification of primitive type`() {
        val notifier = ValueNotifier<Int>(0)
        val listener: VoidCallback = mock()

        notifier.addListener(listener)
        notifier.value = 0 // same value. ChangeNotifier won't be notified
        verify(listener, times(0)).invoke()

        notifier.value = 1
        verify(listener, times(1)).invoke()
    }

    @Test
    fun `changed notification of object type`() {
        val obj = Object()
        val notifier = ValueNotifier<Object>(obj)
        val listener: VoidCallback = mock()

        notifier.addListener(listener)
        notifier.value = obj // same value. ChangeNotifier won't be notified
        verify(listener, times(0)).invoke()

        notifier.value = Object()
        verify(listener, times(1)).invoke()
    }

    @Test
    fun `changed notification of nullable object type`() {
        val obj = Object()
        val notifier = ValueNotifier<Object?>(null)
        val listener: VoidCallback = mock()

        notifier.addListener(listener)
        notifier.value = null // same value. ChangeNotifier won't be notified
        verify(listener, times(0)).invoke()

        notifier.value = Object()
        verify(listener, times(1)).invoke()

        notifier.value = null
        verify(listener, times(2)).invoke()
    }

    @Test
    fun `changed notification of structual equality`() {
        val data1 = Data("Data1")
        val data2 = Data("Data2")
        val anotherData2 = Data("Data2")

        val notifier = ValueNotifier<Data?>(null)
        val listener: VoidCallback = mock()

        notifier.addListener(listener)
        notifier.value = data1
        verify(listener, times(1)).invoke()

        notifier.value = data2
        verify(listener, times(2)).invoke()

        // data2 and anotherData2 are structually equal. Should not notify the event.
        notifier.value = anotherData2
        verify(listener, times(2)).invoke()
    }
}
