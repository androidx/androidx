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
        verify(FocusNode1, never()).onBlur(eq(true /* hasNextClient */))
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
            verify(FocusNode1, times(1)).onBlur(eq(true /* hasNextClient */))
            verify(FocusNode2, times(1)).onFocus()
        }
    }

    @Test
    fun requestFocus_doNothingIfAlreadyFocused() {
        val FocusNode1: FocusManager.FocusNode = mock()

        val fm = FocusManager()
        fm.requestFocus(FocusNode1)
        verify(FocusNode1, times(1)).onFocus()
        verify(FocusNode1, never()).onBlur(any())

        // Either onFocus or onBlur must not be called for the already focused object.
        fm.requestFocus(FocusNode1)
        verify(FocusNode1, times(1)).onFocus()
        verify(FocusNode1, never()).onBlur(any())
    }

    @Test
    fun register_and_focusNodeById() {
        val id = "Focus Node ID"
        val node = mock<FocusManager.FocusNode>()

        val fm = FocusManager()

        fm.registerFocusNode(id, node)
        fm.requestFocusById(id)
        verify(node, times(1)).onFocus()
        verify(node, never()).onBlur(any())
    }

    @Test
    fun unregister() {
        val id = "Focus Node ID"
        val node = mock<FocusManager.FocusNode>()

        val fm = FocusManager()

        fm.registerFocusNode(id, node)

        fm.unregisterFocusNode(id)
        fm.requestFocusById(id)
        verify(node, never()).onFocus()
        verify(node, never()).onBlur(any())
    }

    @Test
    fun blur() {
        val focusNode: FocusManager.FocusNode = mock()

        val fm = FocusManager()
        fm.requestFocus(focusNode)

        // onBlur must be called to currently focused object, then onFocus is called to the next
        // object.
        fm.blur(focusNode)
        verify(focusNode, times(1)).onBlur(eq(false /* hasNextClient */))
    }
}