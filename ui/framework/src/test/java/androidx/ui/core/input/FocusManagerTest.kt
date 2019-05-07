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

import com.nhaarman.mockitokotlin2.inOrder
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
        val FocusNode1: FocusManager.FocusNode = mock()

        val fm = FocusManager()
        fm.requestFocus(FocusNode1)

        verify(FocusNode1, times(1)).onFocus()
        verify(FocusNode1, never()).onBlur()
    }

    @Test
    fun requestFocus_CallingOrderTest() {
        val FocusNode1: FocusManager.FocusNode = mock()
        val FocusNode2: FocusManager.FocusNode = mock()

        val fm = FocusManager()
        fm.requestFocus(FocusNode1)
        fm.requestFocus(FocusNode2)

        // onBlur must be called to currently focused object, then onFocus is called to the next
        // object.
        inOrder(FocusNode1, FocusNode2) {
            verify(FocusNode1, times(1)).onBlur()
            verify(FocusNode2, times(1)).onFocus()
        }
    }

    @Test
    fun requestFocus_doNothingIfAlreadyFocused() {
        val FocusNode1: FocusManager.FocusNode = mock()

        val fm = FocusManager()
        fm.requestFocus(FocusNode1)
        verify(FocusNode1, times(1)).onFocus()
        verify(FocusNode1, never()).onBlur()

        // Either onFocus or onBlur must not be called for the already focused object.
        fm.requestFocus(FocusNode1)
        verify(FocusNode1, times(1)).onFocus()
        verify(FocusNode1, never()).onBlur()
    }
}