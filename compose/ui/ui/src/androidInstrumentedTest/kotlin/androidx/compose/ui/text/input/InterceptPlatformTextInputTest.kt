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

package androidx.compose.ui.text.input

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.AtomicInt
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.establishTextInputSession
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class InterceptPlatformTextInputTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var testNode: TestNode
    private lateinit var coroutineScope: CoroutineScope

    @Test
    fun interceptor_getsCoroutineContextFromTextInputNode() {
        var lastRequest: PlatformTextInputMethodRequest? = null
        var lastContinuation: CancellableContinuation<Nothing>? = null
        val testRequest = TaggedRequest()
        val interceptor = PlatformTextInputInterceptor { request, _ ->
            lastRequest = request
            suspendCancellableCoroutine { lastContinuation = it }
        }
        setContent { InterceptPlatformTextInput(interceptor) { FakeTextField() } }

        rule.runOnIdle {
            coroutineScope.launch {
                testNode.establishTextInputSession {
                    // This context should be propagated to startInputMethod.
                    withContext(CoroutineName("original")) { startInputMethod(testRequest) }
                }
            }
        }
        // Let the session start.
        rule.runOnIdle {
            assertThat(lastRequest).isSameInstanceAs(testRequest)
            assertThat(lastContinuation!!.context[CoroutineName]?.name).isEqualTo("original")
        }
    }

    @Test
    fun interceptor_interceptsCoroutineContext() {
        var lastContinuation: CancellableContinuation<Nothing>? = null
        val testRequest = TaggedRequest("original")
        val childInterceptor = PlatformTextInputInterceptor { request, nextHandler ->
            val originalName = currentCoroutineContext()[CoroutineName]?.name
            withContext(CoroutineName("wrapped $originalName")) {
                nextHandler.startInputMethod(request)
            }
        }
        val parentInterceptor = PlatformTextInputInterceptor { _, _ ->
            suspendCancellableCoroutine { lastContinuation = it }
        }
        setContent {
            InterceptPlatformTextInput(parentInterceptor) {
                InterceptPlatformTextInput(childInterceptor) { FakeTextField() }
            }
        }

        rule.runOnIdle {
            coroutineScope.launch {
                testNode.establishTextInputSession {
                    // This context should be propagated to startInputMethod.
                    withContext(CoroutineName("original")) { startInputMethod(testRequest) }
                }
            }
        }
        // Let the session start.
        rule.runOnIdle {
            assertThat(lastContinuation!!.context[CoroutineName]?.name)
                .isEqualTo("wrapped original")
        }
    }

    @Test
    fun interceptor_propagatesExceptions() {
        class ExpectedException : RuntimeException()

        lateinit var thrownException: ExpectedException
        val interceptor = PlatformTextInputInterceptor { _, _ ->
            throw ExpectedException().also { thrownException = it }
        }
        setContent { InterceptPlatformTextInput(interceptor) { FakeTextField() } }

        var exceptionFromInputNode: ExpectedException? = null
        rule.runOnIdle {
            coroutineScope.launch {
                testNode.establishTextInputSession {
                    try {
                        startInputMethod(TaggedRequest())
                    } catch (e: ExpectedException) {
                        exceptionFromInputNode = e
                        awaitCancellation()
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(exceptionFromInputNode).isNotNull()
            assertThat(exceptionFromInputNode).isSameInstanceAs(thrownException)
        }
    }

    @Test
    fun interceptor_interceptsEditorInfoAndInputConnection() {
        var lastRequest: PlatformTextInputMethodRequest? = null
        val testRequest = TaggedRequest("original")
        val childInterceptor = PlatformTextInputInterceptor { request, nextHandler ->
            val wrappedRequest = PlatformTextInputMethodRequest { outAttributes ->
                val originalInputConnection =
                    request.createInputConnection(outAttributes) as TaggedInputConnection
                outAttributes.privateImeOptions = "intercepted"
                TaggedInputConnection("wrapped ${originalInputConnection.tag}")
            }
            nextHandler.startInputMethod(wrappedRequest)
        }
        val parentInterceptor = PlatformTextInputInterceptor { request, _ ->
            lastRequest = request
            awaitCancellation()
        }
        setContent {
            InterceptPlatformTextInput(parentInterceptor) {
                InterceptPlatformTextInput(childInterceptor) { FakeTextField() }
            }
        }

        rule.runOnIdle {
            coroutineScope.launch {
                testNode.establishTextInputSession { startInputMethod(testRequest) }
            }
        }
        // Let the session start.
        rule.runOnIdle {
            val editorInfo = EditorInfo()
            val inputConnection = assertNotNull(lastRequest).createInputConnection(editorInfo)
            assertThat(editorInfo.privateImeOptions).isEqualTo("intercepted")
            assertIs<TaggedInputConnection>(inputConnection)
            assertThat(inputConnection.tag).isEqualTo("wrapped original")
        }
    }

    @Test
    fun interceptor_handlesRequestsSequentially_whenChangedDuringSession() {
        val currentCount = AtomicInt(0)
        fun expect(expectedCount: Int) {
            assertThat(currentCount.getAndIncrement()).isEqualTo(expectedCount)
        }

        /** Used to simulate a finally block that takes a long time to run. */
        val finishInterceptor1FinallyTrigger = Job()
        val interceptor1 = PlatformTextInputInterceptor { _, _ ->
            try {
                awaitCancellation()
            } finally {
                expect(1)
                // If we don't use NonCancellable, join will immediately throw when it tries to
                // suspend since this context is already cancelled.
                withContext(NonCancellable) { finishInterceptor1FinallyTrigger.join() }
                expect(3)
            }
        }
        val interceptor2 = PlatformTextInputInterceptor { _, _ ->
            expect(4)
            awaitCancellation()
        }
        var interceptor by mutableStateOf(interceptor1)
        setContent { InterceptPlatformTextInput(interceptor) { FakeTextField() } }

        rule.runOnIdle {
            coroutineScope.launch {
                testNode.establishTextInputSession { startInputMethod(TaggedRequest()) }
            }
        }

        rule.runOnIdle {
            expect(0)
            interceptor = interceptor2
        }

        rule.runOnIdle {
            expect(2)
            finishInterceptor1FinallyTrigger.complete()
        }

        rule.runOnIdle { expect(5) }
    }

    @Test
    fun interceptor_doesntCancelDownstream_whenChangedDuringSession() {
        var downstreamSessionCancelled = false
        var session2Started = false
        val interceptor1 = PlatformTextInputInterceptor { _, _ -> awaitCancellation() }
        val interceptor2 = PlatformTextInputInterceptor { _, _ ->
            session2Started = true
            awaitCancellation()
        }
        var interceptor by mutableStateOf(interceptor1)
        setContent { InterceptPlatformTextInput(interceptor) { FakeTextField() } }

        val sessionJob =
            rule.runOnIdle {
                coroutineScope.launch {
                    testNode.establishTextInputSession {
                        try {
                            startInputMethod(TaggedRequest())
                        } finally {
                            downstreamSessionCancelled = true
                        }
                    }
                }
            }

        rule.runOnIdle {
            assertTrue(sessionJob.isActive)
            interceptor = interceptor2
        }

        rule.runOnIdle {
            assertFalse(downstreamSessionCancelled)
            assertTrue(session2Started)
            assertTrue(sessionJob.isActive)
        }
    }

    @Test
    fun interceptor_wrapsRequestsInCorrectOrder() {
        lateinit var lastRequest: TaggedRequest
        val interceptor1 = PlatformTextInputInterceptor { request, nextHandler ->
            val tag = (request as TaggedRequest).tag
            nextHandler.startInputMethod(TaggedRequest("interceptor1 -> $tag"))
        }
        val interceptor2 = PlatformTextInputInterceptor { request, nextHandler ->
            val tag = (request as TaggedRequest).tag
            nextHandler.startInputMethod(TaggedRequest("interceptor2 -> $tag"))
        }
        val rootInterceptor = PlatformTextInputInterceptor { request, _ ->
            lastRequest = request as TaggedRequest
            awaitCancellation()
        }
        setContent {
            InterceptPlatformTextInput(rootInterceptor) {
                InterceptPlatformTextInput(interceptor1) {
                    InterceptPlatformTextInput(interceptor2) { FakeTextField() }
                }
            }
        }
        rule.runOnIdle {
            coroutineScope.launch {
                testNode.establishTextInputSession { startInputMethod(TaggedRequest("original")) }
            }
        }

        rule.runOnIdle {
            assertThat(lastRequest.tag).isEqualTo("interceptor1 -> interceptor2 -> original")
        }
    }

    @Test
    fun interceptor_interceptsRequest_fromDialog() {
        lateinit var lastRequest: PlatformTextInputMethodRequest
        val interceptor = PlatformTextInputInterceptor { request, _ ->
            lastRequest = request
            awaitCancellation()
        }
        setContent {
            InterceptPlatformTextInput(interceptor) {
                Dialog(onDismissRequest = {}) { FakeTextField() }
            }
        }

        rule.runOnIdle {
            coroutineScope.launch {
                testNode.establishTextInputSession {
                    startInputMethod(TaggedRequest("from dialog"))
                }
            }
        }

        rule.runOnIdle {
            val lastRequestName = assertIs<TaggedRequest>(lastRequest).tag
            assertThat(lastRequestName).isEqualTo("from dialog")
        }
    }

    @Test
    fun interceptor_interceptsRequest_fromComposeViewInsideAndroidView() {
        class FakeTextFieldAndroidView(context: Context) : FrameLayout(context) {
            init {
                ComposeView(context).also {
                    addView(it)
                    it.setContent { FakeTextField() }
                }
            }
        }

        lateinit var lastRequest: PlatformTextInputMethodRequest
        lateinit var innerAndroidView: FakeTextFieldAndroidView
        lateinit var viewFromSession: View
        val interceptor = PlatformTextInputInterceptor { request, _ ->
            lastRequest = request
            awaitCancellation()
        }
        setContent {
            InterceptPlatformTextInput(interceptor) {
                AndroidView(
                    factory = { context ->
                        FakeTextFieldAndroidView(context).also { innerAndroidView = it }
                    }
                )
            }
        }

        rule.runOnIdle {
            coroutineScope.launch {
                testNode.establishTextInputSession {
                    viewFromSession = view
                    startInputMethod(TaggedRequest("from nested"))
                }
            }
        }

        rule.runOnIdle {
            val lastRequestName = assertIs<TaggedRequest>(lastRequest).tag
            assertThat(lastRequestName).isEqualTo("from nested")
            // View should be the view hosting the node establishing the session, not the one
            // hosting the root interceptor.
            assertTrue(viewFromSession.isSameOrDescendentOf(innerAndroidView))
        }
    }

    @Test
    fun interceptor_canBlockSystemHandler() {
        lateinit var hostView: View
        val interceptor = PlatformTextInputInterceptor { _, _ -> awaitCancellation() }
        setContent {
            hostView = LocalView.current
            InterceptPlatformTextInput(interceptor) { FakeTextField() }
        }

        rule.runOnIdle {
            coroutineScope.launch {
                testNode.establishTextInputSession { startInputMethod(TaggedRequest()) }
            }
        }
        // Let the session start.
        rule.waitForIdle()

        assertFailsWith<ComposeTimeoutException> {
            // This should never return true.
            rule.waitUntil { hostView.onCheckIsTextEditor() }
        }
    }

    @Test
    fun interceptor_restartsSession_whenChanged() {
        val requests = mutableListOf<String>()
        val parentInterceptor = PlatformTextInputInterceptor { request, _ ->
            requests += (request as TaggedRequest).tag
            awaitCancellation()
        }
        val interceptor1 = PlatformTextInputInterceptor { request, next ->
            next.startInputMethod(TaggedRequest("one wrapping ${(request as TaggedRequest).tag}"))
        }
        val interceptor2 = PlatformTextInputInterceptor { request, next ->
            next.startInputMethod(TaggedRequest("two wrapping ${(request as TaggedRequest).tag}"))
        }
        var currentInterceptor by mutableStateOf(interceptor1)
        setContent {
            InterceptPlatformTextInput(parentInterceptor) {
                InterceptPlatformTextInput(currentInterceptor) { FakeTextField() }
            }
        }

        val testJob =
            rule.runOnIdle {
                coroutineScope.launch {
                    testNode.establishTextInputSession {
                        // This context should be propagated to startInputMethod.
                        startInputMethod(TaggedRequest("root"))
                    }
                }
            }
        // Let the session start.
        rule.runOnIdle { assertThat(requests).containsExactly("one wrapping root").inOrder() }

        currentInterceptor = interceptor2

        // Let the session restart.
        rule.waitUntil { requests.size == 2 }
        rule.runOnIdle {
            assertThat(requests).containsExactly("one wrapping root", "two wrapping root").inOrder()

            // Root request shouldn't be cancelled.
            assertTrue(testJob.isActive)
        }
    }

    @Test
    fun interceptor_restartsSession_whenMultipleStartCalls() {
        val requests = mutableListOf<String>()
        val parentInterceptor = PlatformTextInputInterceptor { request, _ ->
            requests += (request as TaggedRequest).tag
            awaitCancellation()
        }
        val requestTrigger = Channel<Int>()
        val interceptor = PlatformTextInputInterceptor { request, next ->
            coroutineScope {
                requestTrigger.consumeEach { id ->
                    launch {
                        next.startInputMethod(
                            TaggedRequest("$id wrapping ${(request as TaggedRequest).tag}")
                        )
                    }
                }
            }
            awaitCancellation()
        }
        setContent {
            InterceptPlatformTextInput(parentInterceptor) {
                InterceptPlatformTextInput(interceptor) { FakeTextField() }
            }
        }
        val testJob =
            rule.runOnIdle {
                coroutineScope.launch {
                    testNode.establishTextInputSession {
                        // This context should be propagated to startInputMethod.
                        startInputMethod(TaggedRequest("root"))
                    }
                }
            }
        // Let the session start.
        rule.runOnIdle { assertThat(requests).isEmpty() }

        assertTrue(requestTrigger.trySend(0).isSuccess)

        rule.waitUntil { requests.size == 1 }
        rule.runOnIdle {
            assertThat(requests)
                .containsExactly(
                    "0 wrapping root",
                )
                .inOrder()

            // Root request shouldn't be cancelled.
            assertTrue(testJob.isActive)
        }

        assertTrue(requestTrigger.trySend(1).isSuccess)

        rule.waitUntil { requests.size == 2 }
        rule.runOnIdle {
            assertThat(requests)
                .containsExactly(
                    "0 wrapping root",
                    "1 wrapping root",
                )
                .inOrder()

            // Root request shouldn't be cancelled.
            assertTrue(testJob.isActive)
        }
    }

    @Test
    fun interceptor_restartsSession_whenNewCapturingLambda() {
        @Composable
        fun NamedSessionInterceptor(name: String, content: @Composable () -> Unit) {
            InterceptPlatformTextInput(
                content = content,
                interceptor = { request, nextHandler ->
                    withContext(CoroutineName(name)) { nextHandler.startInputMethod(request) }
                }
            )
        }

        lateinit var lastSessionContext: CoroutineContext
        val rootInterceptor = PlatformTextInputInterceptor { _, _ ->
            lastSessionContext = currentCoroutineContext()
            awaitCancellation()
        }
        var name by mutableStateOf("one")
        setContent {
            InterceptPlatformTextInput(rootInterceptor) {
                NamedSessionInterceptor(name) { FakeTextField() }
            }
        }
        rule.runOnIdle {
            coroutineScope.launch {
                testNode.establishTextInputSession { startInputMethod(TaggedRequest()) }
            }
        }

        rule.runOnIdle {
            assertThat(lastSessionContext[CoroutineName]?.name).isEqualTo("one")
            name = "two"
        }

        rule.runOnIdle { assertThat(lastSessionContext[CoroutineName]?.name).isEqualTo("two") }
    }

    private fun setContent(content: @Composable () -> Unit) {
        rule.setContent {
            this.coroutineScope = rememberCoroutineScope()
            content()
        }
    }

    @Composable
    private fun FakeTextField(modifier: Modifier = Modifier) {
        Box(modifier.then(TestElement { testNode = it }).size(1.dp))
    }

    private tailrec fun View.isSameOrDescendentOf(expectedParent: ViewGroup): Boolean {
        if (this === expectedParent) return true
        val myParent = this.parent
        if (myParent !is View) return false
        return myParent.isSameOrDescendentOf(expectedParent)
    }

    private data class TestElement(val onNode: (TestNode) -> Unit) :
        ModifierNodeElement<TestNode>() {
        override fun create(): TestNode = TestNode(onNode)

        override fun update(node: TestNode) {
            node.onNode = onNode
        }
    }

    private class TestNode(var onNode: (TestNode) -> Unit) :
        Modifier.Node(), PlatformTextInputModifierNode {

        override fun onAttach() {
            onNode(this)
        }
    }

    private data class TaggedRequest(
        val tag: String = "",
    ) : PlatformTextInputMethodRequest {
        override fun createInputConnection(outAttributes: EditorInfo): InputConnection =
            TaggedInputConnection(tag)
    }

    private data class TaggedInputConnection(val tag: String = "") : InputConnection {
        override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence =
            TODO("Not yet implemented")

        override fun getTextAfterCursor(n: Int, flags: Int): CharSequence =
            TODO("Not yet implemented")

        override fun getSelectedText(flags: Int): CharSequence = TODO("Not yet implemented")

        override fun getCursorCapsMode(reqModes: Int): Int = TODO("Not yet implemented")

        override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText =
            TODO("Not yet implemented")

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean =
            TODO("Not yet implemented")

        override fun deleteSurroundingTextInCodePoints(
            beforeLength: Int,
            afterLength: Int
        ): Boolean = TODO("Not yet implemented")

        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean =
            TODO("Not yet implemented")

        override fun setComposingRegion(start: Int, end: Int): Boolean = TODO("Not yet implemented")

        override fun finishComposingText(): Boolean = TODO("Not yet implemented")

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean =
            TODO("Not yet implemented")

        override fun commitCompletion(text: CompletionInfo?): Boolean = TODO("Not yet implemented")

        override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean =
            TODO("Not yet implemented")

        override fun setSelection(start: Int, end: Int): Boolean = TODO("Not yet implemented")

        override fun performEditorAction(editorAction: Int): Boolean = TODO("Not yet implemented")

        override fun performContextMenuAction(id: Int): Boolean = TODO("Not yet implemented")

        override fun beginBatchEdit(): Boolean = TODO("Not yet implemented")

        override fun endBatchEdit(): Boolean = TODO("Not yet implemented")

        override fun sendKeyEvent(event: KeyEvent?): Boolean = TODO("Not yet implemented")

        override fun clearMetaKeyStates(states: Int): Boolean = TODO("Not yet implemented")

        override fun reportFullscreenMode(enabled: Boolean): Boolean = TODO("Not yet implemented")

        override fun performPrivateCommand(action: String?, data: Bundle?): Boolean =
            TODO("Not yet implemented")

        override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean =
            TODO("Not yet implemented")

        override fun getHandler(): Handler = TODO("Not yet implemented")

        override fun closeConnection(): Unit = TODO("Not yet implemented")

        override fun commitContent(
            inputContentInfo: InputContentInfo,
            flags: Int,
            opts: Bundle?
        ): Boolean = TODO("Not yet implemented")
    }
}
