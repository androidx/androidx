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
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.inOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever

@RunWith(JUnit4::class)
class ChangeNotifierTest {

    // notifyListners is protected. Use this class to call notifyListner from outside.
    class TestableChangeNotifier : ChangeNotifier() {
        fun callNotifyListeners() {
            super.notifyListeners()
        }
    }

    @Test
    fun `hasListner returns true if listener is registered`() {
        val notifier = ChangeNotifier()
        val listener = { }
        assertThat(notifier.hasListeners()).isFalse()
        notifier.addListener(listener)
        assertThat(notifier.hasListeners()).isTrue()
        notifier.removeListener(listener)
        assertThat(notifier.hasListeners()).isFalse()
    }

    @Test
    fun `dispose can be called`() {
        val notifier = ChangeNotifier()
        notifier.dispose()
    }

    @Test
    fun `notifyListener calls listener`() {
        val notifier = TestableChangeNotifier()
        val listener: VoidCallback = mock()

        notifier.addListener(listener)
        notifier.callNotifyListeners()
        verify(listener, times(1)).invoke()
    }

    @Test
    fun `notifyListener not called after removed`() {
        val notifier = TestableChangeNotifier()
        val listener: VoidCallback = mock()

        notifier.addListener(listener)
        notifier.callNotifyListeners()

        notifier.removeListener(listener)
        notifier.callNotifyListeners()
        verify(listener, times(1)).invoke()
    }

    @Test
    fun `notifyListener multiple listeners`() {
        val notifier = TestableChangeNotifier()
        val listener1: VoidCallback = mock()
        val listener2: VoidCallback = mock()

        notifier.addListener(listener1)
        notifier.addListener(listener2)
        notifier.callNotifyListeners()
        verify(listener1, times(1)).invoke()
        verify(listener2, times(1)).invoke()
    }

    @Test
    fun `notifyListener multiple listeners not called added listener during iteration`() {
        val notifier = TestableChangeNotifier()
        val listener: VoidCallback = mock()
        val nextListener: VoidCallback = mock()

        whenever(listener.invoke()).then {
            notifier.addListener(nextListener)
        }

        notifier.addListener(listener)
        notifier.callNotifyListeners()
        verify(listener, times(1)).invoke()
        verify(nextListener, times(0)).invoke()
    }

    @Test
    fun `notifyListener multiple listeners calling order preserved`() {
        // Any comments in the original ChangeNotifier doesn't imply any calling order but calling
        // order is one of the important aspect of the listeners. Here, expects the calling order is
        // the same as the added order as the original implementation is.
        val notifier = TestableChangeNotifier()
        val listener1: VoidCallback = mock()
        val listener2: VoidCallback = mock()

        notifier.addListener(listener1)
        notifier.addListener(listener2)
        notifier.callNotifyListeners()

        inOrder(listener1, listener2) {
            verify(listener1).invoke()
            verify(listener2).invoke()
        }
    }

    @Test
    fun `notifyListener multiple listeners not called removed during iteration`() {
        val notifier = TestableChangeNotifier()
        val listener1: VoidCallback = mock()
        val listener2: VoidCallback = mock()
        whenever(listener1.invoke()).then {
            notifier.removeListener(listener2)
        }

        // The listner2 won't be invoked since it is removed by listener1.
        // This test assumes listener1 is called first. There is no documentation about the calling
        // order but looks like
        notifier.addListener(listener1)
        notifier.addListener(listener2)
        notifier.callNotifyListeners()
        verify(listener1, times(1)).invoke()
        verify(listener2, times(0)).invoke()
    }
}
