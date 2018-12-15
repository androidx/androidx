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

package androidx.ui.widgets.focusmanager

import androidx.ui.VoidCallback
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FocusManagerTest {
    var deferredTask: VoidCallback? = null

    fun deferredTaskExecutor(task: VoidCallback) {
        assertThat(deferredTask).isNull()
        deferredTask = task
    }

    fun runPendingDeferredTask() {
        deferredTask?.invoke()
        deferredTask = null
    }

    lateinit var focusManager: FocusManager

    @Before
    fun setUp() {
        focusManager = FocusManager(::deferredTaskExecutor)
    }

    @Test
    fun `FocusManager requestFocus`() {
        // By default, there is no focus
        assertThat(focusManager.currentFocus).isNull()

        // By default, the node has no focus
        val node = FocusNode()
        val nodeCallback: VoidCallback = mock()
        node.addListener(nodeCallback)
        assertThat(node.hasFocus).isFalse()

        // Request focus gives focus to the node
        focusManager.rootScope.requestFocus(node)
        runPendingDeferredTask()
        assertThat(focusManager.currentFocus).isEqualTo(node)
        assertThat(node.hasFocus).isTrue()
        verify(nodeCallback, times(1)).invoke()
    }

    @Test
    fun `FocusManager unfocus`() {
        // By default, there is no focus
        assertThat(focusManager.currentFocus).isNull()

        // By default, the node has no focus
        val node = FocusNode()
        val nodeCallback: VoidCallback = mock()
        node.addListener(nodeCallback)
        assertThat(node.hasFocus).isFalse()

        // Request focus gives focus to the node
        focusManager.rootScope.requestFocus(node)
        runPendingDeferredTask()

        // Check unfocus releases focus.
        node.unfocus()
        runPendingDeferredTask()
        assertThat(focusManager.currentFocus).isNull()
        assertThat(node.hasFocus).isFalse()
        verify(nodeCallback, times(2)).invoke()
    }

    @Test
    fun `FocusManager moveFocus`() {
        // By default, there is no focus
        assertThat(focusManager.currentFocus).isNull()

        // By default, the node has no focus
        val node1 = FocusNode()
        val node1Callback: VoidCallback = mock()
        node1.addListener(node1Callback)
        assertThat(node1.hasFocus).isFalse()
        val node2 = FocusNode()
        val node2Callback: VoidCallback = mock()
        node2.addListener(node2Callback)
        assertThat(node2.hasFocus).isFalse()

        focusManager.rootScope.requestFocus(node1)
        runPendingDeferredTask()
        verify(node1Callback, times(1)).invoke()
        verify(node2Callback, times(0)).invoke()

        // Request another focus, then the focus has moved to new one.
        focusManager.rootScope.requestFocus(node2)
        runPendingDeferredTask()
        assertThat(focusManager.currentFocus).isEqualTo(node2)
        assertThat(node1.hasFocus).isFalse()
        assertThat(node2.hasFocus).isTrue()
        verify(node1Callback, times(2)).invoke()
        verify(node2Callback, times(1)).invoke()
    }

    @Test
    fun `FocusManager moveFocus and unfocus`() {
        val node1 = FocusNode()
        val node2 = FocusNode()

        // Request focus gives focus to the node
        focusManager.rootScope.requestFocus(node1)
        runPendingDeferredTask()

        // Request another focus, then the focus has moved to new one.
        focusManager.rootScope.requestFocus(node2)
        runPendingDeferredTask()

        val node1Callback: VoidCallback = mock()
        node1.addListener(node1Callback)
        val node2Callback: VoidCallback = mock()
        node2.addListener(node2Callback)

        // Calling unfocus from moved focus does nothing
        node1.unfocus()
        runPendingDeferredTask()
        assertThat(focusManager.currentFocus).isEqualTo(node2)
        assertThat(node1.hasFocus).isFalse()
        assertThat(node2.hasFocus).isTrue()
        verify(node1Callback, times(0)).invoke()
        verify(node2Callback, times(0)).invoke()
    }

    @Test
    fun `FocusManager autofocus`() {
        val node1 = FocusNode()
        val node2 = FocusNode()

        // Request focus gives focus to the node
        focusManager.rootScope.requestFocus(node1)
        runPendingDeferredTask()

        // autofocus takes focus if nobody has focus. In this case, node1 has a focus, do nothing.
        focusManager.rootScope.autofocus(node2)
        runPendingDeferredTask()

        node1.unfocus()
        runPendingDeferredTask()

        val node1Callback: VoidCallback = mock()
        node1.addListener(node1Callback)
        val node2Callback: VoidCallback = mock()
        node2.addListener(node2Callback)

        // autofocus gives focus to node2 since nobody has focus.
        focusManager.rootScope.autofocus(node2)
        runPendingDeferredTask()
        assertThat(focusManager.currentFocus).isEqualTo(node2)
        assertThat(node1.hasFocus).isFalse()
        assertThat(node2.hasFocus).isTrue()
        verify(node1Callback, times(0)).invoke()
        verify(node2Callback, times(1)).invoke()
    }

    @Test
    fun `FocusManager unfocus then focus again`() {
        val node = FocusNode()

        // Request focus gives focus to the node
        focusManager.rootScope.requestFocus(node)
        runPendingDeferredTask()

        val nodeCallback: VoidCallback = mock()
        node.addListener(nodeCallback)

        node.unfocus()
        runPendingDeferredTask()
        assertThat(focusManager.currentFocus).isNull()
        assertThat(node.hasFocus).isFalse()
        verify(nodeCallback, times(1)).invoke()

        focusManager.rootScope.requestFocus(node)
        runPendingDeferredTask()
        assertThat(focusManager.currentFocus).isEqualTo(node)
        assertThat(node.hasFocus).isTrue()
        verify(nodeCallback, times(2)).invoke()
    }

    @Test
    fun `FocusManager subScopeTree focus resolution`() {
        val node = FocusNode()
        val scope = FocusScopeNode()

        scope.requestFocus(node)
        // The node is not focused since the focus scope node is not attached to focus anywhere
        assertThat(node.hasFocus).isFalse()

        focusManager.rootScope.setFirstFocus(scope)
        runPendingDeferredTask()

        // Now the focus scope is attached to focus manager, the node should have a node.
        assertThat(node.hasFocus).isTrue()
    }

    @Test
    fun `FocusManager subScopeTree focus moving`() {
        val node1 = FocusNode()
        val scope1 = FocusScopeNode()
        scope1.requestFocus(node1)

        val node2 = FocusNode()
        val scope2 = FocusScopeNode()
        scope2.requestFocus(node2)

        focusManager.rootScope.setFirstFocus(scope1)
        runPendingDeferredTask()
        // Now the focus scope is attached to focus manager, the node should have a node.
        assertThat(node1.hasFocus).isTrue()
        assertThat(node2.hasFocus).isFalse()
    }

    @Test
    fun `FocusManager subScopeTree focus transition`() {
        val node1 = FocusNode()
        val scope1 = FocusScopeNode()
        scope1.requestFocus(node1)

        val node2 = FocusNode()
        val scope2 = FocusScopeNode()
        scope2.requestFocus(node2)

        focusManager.rootScope.setFirstFocus(scope1)
        runPendingDeferredTask()

        focusManager.rootScope.setFirstFocus(scope2)
        runPendingDeferredTask()
        // Now the first focus scope is scope2, node1 is no longer has a focus
        assertThat(node1.hasFocus).isFalse()
        assertThat(node2.hasFocus).isTrue()

        scope2.detach()
        runPendingDeferredTask()
        // Now the node2 has detached from the scope tree. The first scope node is revived.
        assertThat(node1.hasFocus).isTrue()
        assertThat(node2.hasFocus).isFalse()
    }

    @Test
    fun `FocusManager subScopeTree detach`() {
        val node1 = FocusNode()
        val scope1 = FocusScopeNode()
        scope1.requestFocus(node1)

        val node2 = FocusNode()
        val scope2 = FocusScopeNode()
        scope2.requestFocus(node2)

        focusManager.rootScope.setFirstFocus(scope1)
        runPendingDeferredTask()

        focusManager.rootScope.setFirstFocus(scope2)
        runPendingDeferredTask()

        scope2.detach()
        runPendingDeferredTask()
        // Now the node2 has detached from the scope tree. The first scope node is revived.
        assertThat(node1.hasFocus).isTrue()
        assertThat(node2.hasFocus).isFalse()
    }

    @Test
    fun `FocusManager subScopeTree reparent Node`() {
        val node1 = FocusNode()
        val scope1 = FocusScopeNode()
        scope1.requestFocus(node1)

        val node2 = FocusNode()
        val scope2 = FocusScopeNode()
        scope2.requestFocus(node2)

        focusManager.rootScope.setFirstFocus(scope1)
        runPendingDeferredTask()

        focusManager.rootScope.reparentIfNeeded(node2)
        runPendingDeferredTask()
        // Different from setFirstFocus, reparent doesn't go the first focus scope
        assertThat(node1.hasFocus).isTrue()
        assertThat(node2.hasFocus).isFalse()
    }

    @Test
    fun `FocusManager subScopeTree reparent Scope`() {
        val node1 = FocusNode()
        val scope1 = FocusScopeNode()
        scope1.requestFocus(node1)

        val node2 = FocusNode()
        val scope2 = FocusScopeNode()
        scope2.requestFocus(node2)

        focusManager.rootScope.setFirstFocus(scope1)
        runPendingDeferredTask()

        focusManager.rootScope.reparentScopeIfNeeded(scope2)
        runPendingDeferredTask()
        // Different from reparentIfNecessary, reparentScopeIfNeeded doesn't attach given scope
        // if there it is not attached anywhere.
        assertThat(node1.hasFocus).isTrue()
        assertThat(node2.hasFocus).isFalse()
        assertThat(scope2.parent).isNull()
    }

    @Test
    fun `FocusManager subScopeTree reparentIfNeeded make parent null`() {
        val node1 = FocusNode()
        val scope1 = FocusScopeNode()
        scope1.requestFocus(node1)

        val node2 = FocusNode()
        val scope2 = FocusScopeNode()
        scope2.requestFocus(node2)

        focusManager.rootScope.setFirstFocus(scope1)
        runPendingDeferredTask()

        focusManager.rootScope.reparentScopeIfNeeded(scope2)
        runPendingDeferredTask()

        val scope3 = FocusScopeNode()
        scope3.setFirstFocus(scope2)
        // Make scope2 not a first child otherwise assert fails.
        // TODO(Migration/nona): Check if this is a correct things in flutter.
        scope3.setFirstFocus(FocusScopeNode())
        runPendingDeferredTask()
        focusManager.rootScope.reparentScopeIfNeeded(scope2)
        runPendingDeferredTask()
        // According to the method comment in reparentScopeIfNeeded detaches the given scope if that
        // is not a firstFocusNode.
        assertThat(scope2.parent).isNull()
    }
}