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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.text.input

import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.node.ObservationTarget
import androidx.compose.ui.platform.AndroidPlatformTextInputSession
import androidx.compose.ui.platform.ComposeUiHost
import androidx.compose.ui.platform.ExperimentalComposeUiHostApi
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.semantics.SemanticsPropertyProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.fail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalComposeUiHostApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidPlatformTextInputSessionTest {

    private var startInputCalls = 0
    private var stopInputCalls = 0
    private var showKeyboardCalls = 0
    private var hideKeyboardCalls = 0

    private val view = View(InstrumentationRegistry.getInstrumentation().context)
    private val host = RecordingInputMethodHost(view)
    private var activeHostRequestsAtStartInput = -1
    private val inputService =
        TextInputService(
            object : PlatformTextInputService {
                override fun startInput() {
                    startInputCalls++
                    activeHostRequestsAtStartInput = host.activeRequests.size
                }

                override fun stopInput() {
                    stopInputCalls++
                }

                override fun showSoftwareKeyboard() {
                    showKeyboardCalls++
                }

                override fun hideSoftwareKeyboard() {
                    hideKeyboardCalls++
                }

                override fun startInput(
                    value: TextFieldValue,
                    imeOptions: ImeOptions,
                    onEditCommand: (List<EditCommand>) -> Unit,
                    onImeActionPerformed: (ImeAction) -> Unit,
                ) {
                    fail("Not supported")
                }

                override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
                    fail("not supported")
                }
            }
        )

    @Test
    fun keyboardNotShown_whenStartInputMethodNotCalled() = runTest {
        AndroidPlatformTextInputSession(host, inputService, coroutineScope = this)
        assertThat(startInputCalls).isEqualTo(0)
        assertThat(stopInputCalls).isEqualTo(0)
    }

    @Test
    fun keyboardShown_whenStartInputMethodCalled() = runTest {
        val session = AndroidPlatformTextInputSession(host, inputService, coroutineScope = this)

        launch { session.startInputMethod(TestInputMethodRequest(view)) }
        advanceUntilIdle()

        assertThat(startInputCalls).isEqualTo(1)
        assertThat(stopInputCalls).isEqualTo(0)

        coroutineContext.job.cancelChildren()
    }

    @Test
    fun keyboardHidden_whenInnerSessionCanceled() = runTest {
        val session = AndroidPlatformTextInputSession(host, inputService, coroutineScope = this)
        val sessionJob = launch { session.startInputMethod(TestInputMethodRequest(view)) }
        advanceUntilIdle()

        sessionJob.cancel()
        advanceUntilIdle()

        assertThat(startInputCalls).isEqualTo(1)
        assertThat(stopInputCalls).isEqualTo(1)
    }

    @Test
    fun keyboardHidden_whenOuterSessionCanceled() = runTest {
        val sessionJob = launch {
            val session = AndroidPlatformTextInputSession(host, inputService, coroutineScope = this)
            session.startInputMethod(TestInputMethodRequest(view))
        }
        advanceUntilIdle()

        sessionJob.cancel()
        advanceUntilIdle()

        assertThat(startInputCalls).isEqualTo(1)
        assertThat(stopInputCalls).isEqualTo(1)
    }

    @Test
    fun keyboardNotHidden_whenInnerSessionInterrupted() = runTest {
        val session = AndroidPlatformTextInputSession(host, inputService, coroutineScope = this)
        launch { session.startInputMethod(TestInputMethodRequest(view)) }
        advanceUntilIdle()

        launch { session.startInputMethod(TestInputMethodRequest(view)) }
        advanceUntilIdle()

        assertThat(startInputCalls).isEqualTo(2)
        assertThat(stopInputCalls).isEqualTo(1)

        coroutineContext.job.cancelChildren()
    }

    @Test
    fun keyboardNotHidden_whenOuterSessionInterrupted_innerSessionImmediatelyStarted() = runTest {
        val session1Job = launch {
            val session = AndroidPlatformTextInputSession(host, inputService, coroutineScope = this)
            session.startInputMethod(TestInputMethodRequest(view))
        }
        advanceUntilIdle()

        // Interrupt outer session.
        launch {
            val session = AndroidPlatformTextInputSession(host, inputService, coroutineScope = this)
            session1Job.cancelAndJoin()
            session.startInputMethod(TestInputMethodRequest(view))
        }
        advanceUntilIdle()

        assertThat(startInputCalls).isEqualTo(2)
        assertThat(stopInputCalls).isEqualTo(1)

        coroutineContext.job.cancelChildren()
    }

    @Test
    fun keyboardHidden_whenOuterSessionInterrupted_innerSessionNotRestarted() = runTest {
        val session1Job = launch {
            val session = AndroidPlatformTextInputSession(host, inputService, coroutineScope = this)
            session.startInputMethod(TestInputMethodRequest(view))
        }
        advanceUntilIdle()

        // Interrupt outer session.
        launch {
            AndroidPlatformTextInputSession(host, inputService, coroutineScope = this)
            session1Job.cancelAndJoin()
        }
        advanceUntilIdle()

        assertThat(startInputCalls).isEqualTo(1)
        assertThat(stopInputCalls).isEqualTo(1)

        coroutineContext.job.cancelChildren()
    }

    @Test
    fun hostCallStartsBeforeStartInput() = runTest {
        val session = AndroidPlatformTextInputSession(host, inputService, coroutineScope = this)
        launch { session.startInputMethod(TestInputMethodRequest(view)) }
        advanceUntilIdle()

        assertThat(host.activeRequests).hasSize(1)
        assertThat(activeHostRequestsAtStartInput).isEqualTo(1)

        coroutineContext.job.cancelChildren()
    }

    @Test
    fun hostCallCancelled_whenInnerSessionCanceled() = runTest {
        val session = AndroidPlatformTextInputSession(host, inputService, coroutineScope = this)
        val sessionJob = launch { session.startInputMethod(TestInputMethodRequest(view)) }
        advanceUntilIdle()

        sessionJob.cancel()

        // Cleared in the same cancellation pass as stopInput, without dispatching.
        assertThat(host.activeRequests).isEmpty()
        assertThat(stopInputCalls).isEqualTo(1)
    }

    @Test
    fun hostCallReplaced_whenInnerSessionInterrupted() = runTest {
        val session = AndroidPlatformTextInputSession(host, inputService, coroutineScope = this)
        launch { session.startInputMethod(TestInputMethodRequest(view)) }
        advanceUntilIdle()
        val first = host.activeRequests.single()

        launch { session.startInputMethod(TestInputMethodRequest(view)) }
        advanceUntilIdle()

        assertThat(host.startCalls).isEqualTo(2)
        assertThat(host.activeRequests.single()).isNotSameInstanceAs(first)

        coroutineContext.job.cancelChildren()
    }

    @Test
    fun hostRequest_createsConnectionThroughSession() = runTest {
        val session = AndroidPlatformTextInputSession(host, inputService, coroutineScope = this)
        launch { session.startInputMethod(TestInputMethodRequest(view)) }
        advanceUntilIdle()

        val connection = host.activeRequests.single().createInputConnection(EditorInfo())

        assertThat(connection).isInstanceOf(NullableInputConnectionWrapper::class.java)

        coroutineContext.job.cancelChildren()
    }
}

/** A [ComposeUiHost] that records [startInputMethod] calls; other members are inert. */
@OptIn(ExperimentalComposeUiHostApi::class)
private class RecordingInputMethodHost(override val view: View) : ComposeUiHost {
    val activeRequests = mutableListOf<PlatformTextInputMethodRequest>()
    var startCalls = 0

    override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
        startCalls++
        suspendCancellableCoroutine<Nothing> { continuation ->
            activeRequests += request
            continuation.invokeOnCancellation { activeRequests -= request }
        }
    }

    override val density: Density = Density(1f)
    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override val window: Window? = null
    override val graphicsContext: GraphicsContext
        get() = error("not used")

    override fun localToScreen(relativeToLocal: Offset): Offset = relativeToLocal

    override fun screenToLocal(relativeToScreen: Offset): Offset = relativeToScreen

    override fun transformToScreen(matrix: Matrix) {}

    override fun <T : ObservationTarget> observeReads(
        target: T,
        onChanged: (T) -> Unit,
        block: () -> Unit,
    ) = block()

    override fun invalidateDraw() {}

    override fun invalidateMeasurement() {}

    override fun voteFrameRate(frameRate: Float) {}

    override fun voteFrameRateCategory(category: FrameRateCategory) {}

    override fun requestHostFocus(
        focusDirection: FocusDirection,
        previouslyFocusedRect: Rect?,
    ): Boolean = false

    override fun clearHostFocus() {}

    override fun addAndroidView(view: View, semanticsId: Int, invalidateMeasurement: () -> Unit) {}

    override fun removeAndroidView(view: View, semanticsId: Int) {}

    override fun drawAndroidView(view: View, canvas: Canvas) {}

    override fun onLayoutChanged(node: LayoutInfo) {}

    override fun onSemanticsChanged(node: LayoutInfo, previousConfig: SemanticsPropertyProvider?) {}

    override fun onFocusChanged(previous: LayoutInfo?, current: LayoutInfo?) {}

    override fun onNodeAttached(node: LayoutInfo) {}

    override fun onNodeDetached(node: LayoutInfo) {}

    override fun onNodeDeactivated(node: LayoutInfo) {}

    override fun onNodeReused(node: LayoutInfo, previousSemanticsId: Int) {}

    override fun onAutofillRequested(node: LayoutInfo) {}

    override fun onChangesApplied() {}
}
