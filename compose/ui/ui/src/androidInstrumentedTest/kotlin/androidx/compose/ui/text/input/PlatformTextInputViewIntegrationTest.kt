/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text.input

import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.establishTextInputSession
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PlatformTextInputViewIntegrationTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var hostView: AndroidComposeView
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var node1: PlatformTextInputModifierNode
    private lateinit var node2: PlatformTextInputModifierNode

    // Used for ordering tests.
    private var expected = 0

    private fun expect(value: Int) {
        assertThat(expected).isEqualTo(value)
        expected++
    }

    @Test
    fun hostViewIsPassedToFactory() {
        setupContent()
        lateinit var view1: View
        lateinit var view2: View

        coroutineScope.launch {
            node1.establishTextInputSession {
                view1 = view
                throw CancellationException()
            }
        }

        rule.runOnIdle {
            coroutineScope.launch {
                node2.establishTextInputSession {
                    view2 = view
                    throw CancellationException()
                }
            }
            assertThat(view1).isSameInstanceAs(view2)
        }
    }

    @Test
    fun checkIsTextEditor_returnsFalse_whenNoSessionActive() {
        setupContent()
        rule.runOnIdle { assertThat(hostView.onCheckIsTextEditor()).isFalse() }
    }

    @Test
    fun checkIsTextEditor_returnsFalse_whenNoInnerSessionActive() {
        setupContent()

        coroutineScope.launch { node1.establishTextInputSession { awaitCancellation() } }

        rule.runOnIdle { assertThat(hostView.onCheckIsTextEditor()).isFalse() }
    }

    @Test
    fun checkIsTextEditor_returnsTrue_whenInnerSessionActive() {
        setupContent()

        coroutineScope.launch {
            node1.establishTextInputSession { startInputMethod(TestInputMethodRequest(view)) }
        }

        rule.runOnIdle { assertThat(hostView.onCheckIsTextEditor()).isTrue() }

        // Handoff session to another node.
        val sessionJob =
            coroutineScope.launch {
                node2.establishTextInputSession { startInputMethod(TestInputMethodRequest(view)) }
            }

        rule.runOnIdle {
            assertThat(hostView.onCheckIsTextEditor()).isTrue()
            sessionJob.cancel()
        }

        rule.runOnIdle { assertThat(hostView.onCheckIsTextEditor()).isFalse() }
    }

    @Test
    fun createInputConnection_returnsNull_whenNoSessionActive() {
        setupContent()
        rule.runOnIdle { assertThat(hostView.onCreateInputConnection(EditorInfo())).isNull() }
    }

    @Test
    fun createInputConnection_returnsNull_whenNoInnerSessionActive() {
        setupContent()
        coroutineScope.launch { node1.establishTextInputSession { awaitCancellation() } }

        rule.runOnIdle { assertThat(hostView.onCreateInputConnection(EditorInfo())).isNull() }
    }

    @Test
    fun createInputConnection_returnsConnection_whenInnerSessionActive() {
        setupContent()
        val editorInfo = EditorInfo()
        val request1Texts = mutableListOf<String>()
        val request2Texts = mutableListOf<String>()
        coroutineScope.launch {
            node2.establishTextInputSession {
                startInputMethod(
                    object : TestInputMethodRequest(view) {
                        override fun commitText(
                            text: CharSequence?,
                            newCursorPosition: Int
                        ): Boolean {
                            request1Texts += text.toString()
                            return true
                        }
                    }
                )
            }
        }

        rule.runOnIdle {
            val connection1 = hostView.onCreateInputConnection(editorInfo)
            assertNotNull(connection1)
            connection1.commitText("hello", 1)
            assertThat(request1Texts).containsExactly("hello").inOrder()
            assertThat(request2Texts).isEmpty()
        }

        val sessionJob =
            coroutineScope.launch {
                node1.establishTextInputSession {
                    startInputMethod(
                        object : TestInputMethodRequest(view) {
                            override fun commitText(
                                text: CharSequence?,
                                newCursorPosition: Int
                            ): Boolean {
                                request2Texts += text.toString()
                                return true
                            }
                        }
                    )
                }
            }
        rule.runOnIdle {
            val connection2 = hostView.onCreateInputConnection(editorInfo)
            assertNotNull(connection2)
            connection2.commitText("world", 1)
            assertThat(request1Texts).containsExactly("hello").inOrder()
            assertThat(request2Texts).containsExactly("world").inOrder()

            sessionJob.cancel()
        }

        rule.runOnIdle { assertThat(hostView.onCreateInputConnection(editorInfo)).isNull() }
    }

    @Test
    fun outerSessionCanceled_whenOuterSessionInterrupted() {
        setupContent()
        coroutineScope.launch {
            expect(0)
            try {
                node1.establishTextInputSession {
                    expect(1)
                    try {
                        startInputMethod(
                            object : TestInputMethodRequest(view) {
                                override fun createInputConnection(
                                    outAttributes: EditorInfo
                                ): InputConnection {
                                    expect(2)
                                    return super.createInputConnection(outAttributes)
                                }
                            }
                        )
                    } catch (e: CancellationException) {
                        expect(4)
                        throw e
                    }
                }
            } catch (e: CancellationException) {
                expect(5)
                throw e
            }
        }

        rule.runOnIdle { assertThat(hostView.onCreateInputConnection(EditorInfo())).isNotNull() }

        coroutineScope.launch {
            expect(3)
            node1.establishTextInputSession {
                expect(6)
                startInputMethod(TestInputMethodRequest(view))
            }
        }

        rule.runOnIdle { expect(7) }
    }

    // closeConnection is only supported on API 24+
    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun connectionClosed_whenOuterSessionCanceled() {
        setupContent()
        // keep a strong reference to created InputConnection so it's not collected by GC
        var ic: InputConnection?
        val sessionJob =
            coroutineScope.launch {
                try {
                    node1.establishTextInputSession {
                        try {
                            startInputMethod(
                                object : TestInputMethodRequest(view) {
                                    override fun closeConnection() {
                                        expect(1)
                                    }
                                }
                            )
                        } catch (e: CancellationException) {
                            expect(2)
                            throw e
                        }
                    }
                } catch (e: CancellationException) {
                    expect(3)
                    throw e
                }
            }
        expect(0)

        rule.runOnIdle {
            ic = hostView.onCreateInputConnection(EditorInfo())
            assertThat(ic).isNotNull()
        }

        rule.runOnIdle {
            sessionJob.cancel()
            expect(4)
        }
    }

    // closeConnection is only supported on API 24+
    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun connectionClosed_whenOuterSessionInterrupted() {
        setupContent()
        coroutineScope.launch {
            expect(0)
            try {
                node1.establishTextInputSession {
                    expect(1)
                    try {
                        startInputMethod(
                            object : TestInputMethodRequest(view) {
                                override fun createInputConnection(
                                    outAttributes: EditorInfo
                                ): InputConnection {
                                    expect(2)
                                    return super.createInputConnection(outAttributes)
                                }

                                override fun closeConnection() {
                                    expect(4)
                                }
                            }
                        )
                    } catch (e: CancellationException) {
                        expect(5)
                        throw e
                    }
                }
            } catch (e: CancellationException) {
                expect(6)
                throw e
            }
        }

        rule.runOnIdle { assertThat(hostView.onCreateInputConnection(EditorInfo())).isNotNull() }

        coroutineScope.launch {
            expect(3)
            node1.establishTextInputSession {
                expect(7)
                startInputMethod(TestInputMethodRequest(view))
            }
        }

        rule.runOnIdle { expect(8) }
    }

    @Test
    fun innerSessionCanceled_whenInnerSessionInterrupted() {
        setupContent()
        coroutineScope.launch {
            expect(0)
            node1.establishTextInputSession {
                expect(1)
                launch(start = CoroutineStart.UNDISPATCHED) {
                    expect(2)
                    try {
                        startInputMethod(
                            object : TestInputMethodRequest(view) {
                                override fun createInputConnection(
                                    outAttributes: EditorInfo
                                ): InputConnection {
                                    expect(3)
                                    return super.createInputConnection(outAttributes)
                                }
                            }
                        )
                    } catch (e: CancellationException) {
                        expect(5)
                        throw e
                    }
                }

                assertThat(hostView.onCreateInputConnection(EditorInfo())).isNotNull()

                launch {
                    expect(4)
                    startInputMethod(TestInputMethodRequest(view))
                }

                awaitCancellation()
            }
        }

        rule.runOnIdle { expect(6) }
    }

    // closeConnection is only supported on API 24+
    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun connectionClosed_whenInnerSessionCanceled() {
        setupContent()
        lateinit var sessionJob: Job
        // keep a strong reference to created InputConnection so it's not collected by GC
        var ic: InputConnection?
        coroutineScope.launch {
            node1.establishTextInputSession {
                sessionJob = launch {
                    try {
                        startInputMethod(
                            object : TestInputMethodRequest(view) {
                                override fun closeConnection() {
                                    expect(1)
                                }
                            }
                        )
                    } catch (e: CancellationException) {
                        expect(2)
                        throw e
                    }
                }
                awaitCancellation()
            }
        }
        expect(0)

        rule.runOnIdle {
            ic = hostView.onCreateInputConnection(EditorInfo())
            assertThat(ic).isNotNull()
        }

        rule.runOnIdle {
            sessionJob.cancel()
            expect(3)
        }
    }

    // closeConnection is only supported on API 24+
    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun connectionClosed_whenInnerSessionInterrupted() {
        setupContent()
        coroutineScope.launch {
            expect(0)
            node1.establishTextInputSession {
                expect(1)
                launch(start = CoroutineStart.UNDISPATCHED) {
                    expect(2)
                    try {
                        startInputMethod(
                            object : TestInputMethodRequest(view) {
                                override fun createInputConnection(
                                    outAttributes: EditorInfo
                                ): InputConnection {
                                    expect(3)
                                    return super.createInputConnection(outAttributes)
                                }

                                override fun closeConnection() {
                                    expect(5)
                                }
                            }
                        )
                    } catch (e: CancellationException) {
                        expect(6)
                        throw e
                    }
                }

                assertThat(hostView.onCreateInputConnection(EditorInfo())).isNotNull()

                launch {
                    expect(4)
                    startInputMethod(TestInputMethodRequest(view))
                }

                awaitCancellation()
            }
        }

        rule.runOnIdle { expect(7) }
    }

    // closeConnection is only supported on API 24+
    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun connectionNotClosed_whenCreateConnectionCalledAgain() {
        class TestConnection(view: View) : BaseInputConnection(view, true) {
            var closeCalls = 0

            override fun closeConnection() {
                closeCalls++
                super.closeConnection()
            }
        }

        setupContent()
        val connections = mutableListOf<TestConnection>()
        val sessionJob =
            coroutineScope.launch {
                node1.establishTextInputSession {
                    startInputMethod { TestConnection(view).also { connections += it } }
                }
            }

        rule.runOnIdle {
            assertThat(connections).isEmpty()

            hostView.onCreateInputConnection(EditorInfo())

            assertThat(connections).hasSize(1)
            val connection1 = connections.last()
            assertThat(connection1.closeCalls).isEqualTo(0)

            hostView.onCreateInputConnection(EditorInfo())

            assertThat(connections).hasSize(2)
            val connection2 = connections.last()
            assertThat(connection1.closeCalls).isEqualTo(0)
            assertThat(connection2.closeCalls).isEqualTo(0)

            hostView.onCreateInputConnection(EditorInfo())

            assertThat(connections).hasSize(3)
            val connection3 = connections.last()
            assertThat(connection1.closeCalls).isEqualTo(0)
            assertThat(connection2.closeCalls).isEqualTo(0)
            assertThat(connection3.closeCalls).isEqualTo(0)
        }

        rule.runOnIdle { assertThat(sessionJob.isActive).isTrue() }
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun innerSessionNotCanceled_whenIsolatedFromOuterSession_whenConnectionClosed() {
        setupContent()
        lateinit var innerJob: Job
        val outerJob =
            coroutineScope.launch {
                node1.establishTextInputSession {
                    innerJob = launch { startInputMethod(TestInputMethodRequest(view)) }
                    awaitCancellation()
                }
            }

        rule.runOnIdle {
            val connection = checkNotNull(hostView.onCreateInputConnection(EditorInfo()))
            assertThat(outerJob.isActive).isTrue()
            assertThat(innerJob.isActive).isTrue()
            connection.closeConnection()
        }

        rule.runOnIdle {
            assertThat(outerJob.isActive).isTrue()
            assertThat(innerJob.isActive).isTrue()
        }
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun cancellationDoesNotPropagate_whenConnectionClosed() {
        setupContent()
        val sessionJob =
            coroutineScope.launch {
                node1.establishTextInputSession { startInputMethod(TestInputMethodRequest(view)) }
            }

        rule.runOnIdle {
            val connection = checkNotNull(hostView.onCreateInputConnection(EditorInfo()))
            assertThat(sessionJob.isActive).isTrue()
            connection.closeConnection()
        }

        rule.runOnIdle { assertThat(sessionJob.isActive).isTrue() }
    }

    @Test
    fun createInputConnection_queriesNewRequest_forNewInnerSession() {
        setupContent()
        coroutineScope.launch {
            node1.establishTextInputSession {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    startInputMethod(
                        object : TestInputMethodRequest(view) {
                            override fun createInputConnection(
                                outAttributes: EditorInfo
                            ): InputConnection {
                                expect(1)
                                return super.createInputConnection(outAttributes)
                            }
                        }
                    )
                }

                expect(0)
                val connection1 = hostView.onCreateInputConnection(EditorInfo())

                launch(start = CoroutineStart.UNDISPATCHED) {
                    startInputMethod(
                        object : TestInputMethodRequest(view) {
                            override fun createInputConnection(
                                outAttributes: EditorInfo
                            ): InputConnection {
                                expect(3)
                                return super.createInputConnection(outAttributes)
                            }
                        }
                    )
                }

                expect(2)
                val connection2 = hostView.onCreateInputConnection(EditorInfo())

                assertThat(connection2).isNotSameInstanceAs(connection1)
                awaitCancellation()
            }
        }

        rule.runOnIdle { expect(4) }
    }

    @Test
    fun createInputConnection_returnsDifferentConnections_forSameInnerSession() {
        setupContent()
        coroutineScope.launch {
            node1.establishTextInputSession {
                launch {
                    startInputMethod(
                        object : TestInputMethodRequest(view) {
                            override fun createInputConnection(
                                outAttributes: EditorInfo
                            ): InputConnection = BaseInputConnection(view, true)
                        }
                    )
                }
                awaitCancellation()
            }
        }

        rule.runOnIdle {
            val connection1 = hostView.onCreateInputConnection(EditorInfo())
            val connection2 = hostView.onCreateInputConnection(EditorInfo())
            assertThat(connection1).isNotSameInstanceAs(connection2)
        }
    }

    private fun setupContent() {
        rule.setContent {
            hostView = LocalView.current as AndroidComposeView
            coroutineScope = rememberCoroutineScope()

            Box(TestElement { node1 = it })
            Box(TestElement { node2 = it })
        }
    }

    private data class TestElement(val onNode: (PlatformTextInputModifierNode) -> Unit) :
        ModifierNodeElement<TestNode>() {
        override fun create(): TestNode = TestNode(onNode)

        override fun update(node: TestNode) {
            node.onNode = onNode
        }
    }

    private class TestNode(var onNode: (PlatformTextInputModifierNode) -> Unit) :
        Modifier.Node(), PlatformTextInputModifierNode {

        override fun onAttach() {
            onNode(this)
        }
    }
}
