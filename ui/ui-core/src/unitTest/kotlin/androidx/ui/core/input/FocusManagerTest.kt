/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.input

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FocusManagerTest {
    @Test
    fun requestFocusTest() {
        val focusNode1 = FocusNode()
        val observer: FocusTransitionObserver = mock()

        val fm = FocusManagerImpl()
        fm.registerObserver(focusNode1, observer)
        fm.requestFocus(focusNode1)

        verify(observer, times(1)).invoke(eq(null), eq(focusNode1))
    }

    @Test
    fun requestFocus_CallingOrderTest() {
        val focusNode1 = FocusNode()
        val focusNode2 = FocusNode()

        val observer1: FocusTransitionObserver = mock()
        val observer2: FocusTransitionObserver = mock()

        val fm = FocusManagerImpl()
        fm.registerObserver(focusNode1, observer1)
        fm.registerObserver(focusNode2, observer2)
        fm.requestFocus(focusNode1)
        fm.requestFocus(focusNode2)

        verify(observer1, times(1)).invoke(eq(focusNode1), eq(focusNode2))
    }

    @Test
    fun requestFocus_doNothingIfAlreadyFocused() {
        val focusNode1 = FocusNode()
        val observer: FocusTransitionObserver = mock()

        val fm = FocusManagerImpl()
        fm.registerObserver(focusNode1, observer)
        fm.requestFocus(focusNode1)
        verify(observer, times(1)).invoke(eq(null), eq(focusNode1))

        // Either onFocus or onBlur must not be called for the already focused object.
        fm.requestFocus(focusNode1)
        verify(observer, never()).invoke(any(), any())
    }

    @Test
    fun blur() {
        val focusNode: FocusNode = mock()
        val observer = mock<FocusTransitionObserver>()

        val fm = FocusManagerImpl()
        fm.registerObserver(focusNode, observer)
        fm.requestFocus(focusNode)

        // onBlur must be called to currently focused object, then onFocus is called to the next
        // object.
        fm.blur(focusNode)
        verify(observer, times(1)).invoke(eq(focusNode), eq(null))
    }
}