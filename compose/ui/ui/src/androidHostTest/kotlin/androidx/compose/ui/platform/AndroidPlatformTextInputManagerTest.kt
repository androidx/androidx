/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.view.View
import android.view.Window
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.input.pointer.MatrixPositionCalculator
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.node.ObservationTarget
import androidx.compose.ui.semantics.SemanticsPropertyProvider
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.InputMethodManager
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputServiceAndroid
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Tests that [AndroidPlatformTextInputManager] drives input methods through [ComposeUiHost]. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Config.TARGET_SDK])
@OptIn(
    ExperimentalComposeUiHostApi::class,
    ExperimentalCoroutinesApi::class,
    InternalTextApi::class,
)
class AndroidPlatformTextInputManagerTest {
    private val view = View(RuntimeEnvironment.getApplication())
    private val host = RecordingInputMethodHost(view)
    private val events = mutableListOf<String>()
    private val manager =
        AndroidPlatformTextInputManager(
            host = host,
            matrixPositionCalculator = NoOpMatrixPositionCalculator,
            afterFrameCommandExecutor = Executor {},
            nextFrameCommandExecutor = Executor {},
            coroutineContext = { Dispatchers.Unconfined },
        )

    @After
    fun tearDown() {
        platformTextInputServiceInterceptor = { it }
    }

    @Test
    fun legacyEditorFocus_holdsHostCallWithLegacyRequest() {
        manager.textInputService.startInput(TextFieldValue(), ImeOptions.Default, {}, {})

        assertThat(host.activeRequests).hasSize(1)
        assertThat(host.startCalls).isEqualTo(1)

        manager.textInputService.stopInput()

        assertThat(host.activeRequests).isEmpty()
    }

    @Test
    fun legacyEditorFocus_repeatedStartInput_keepsOneHostCall() {
        manager.textInputService.startInput(TextFieldValue(), ImeOptions.Default, {}, {})
        manager.textInputService.startInput(TextFieldValue("a"), ImeOptions.Default, {}, {})

        assertThat(host.startCalls).isEqualTo(1)
        assertThat(host.activeRequests).hasSize(1)

        manager.textInputService.stopInput()
        manager.textInputService.startInput(TextFieldValue(), ImeOptions.Default, {}, {})

        assertThat(host.startCalls).isEqualTo(2)
        assertThat(host.activeRequests).hasSize(1)
    }

    @Test
    fun legacyRequest_createsConnectionWhileFocused() {
        manager.textInputService.startInput(TextFieldValue("abc"), ImeOptions.Default, {}, {})

        val connection = host.activeRequests.single().createInputConnection(EditorInfo())

        assertThat(connection).isNotInstanceOf(BaseInputConnection::class.java)
    }

    @Test
    fun session_startsHostCallBeforeStartInput_andCancelsItWithTheSession() =
        runTest(UnconfinedTestDispatcher()) {
            platformTextInputServiceInterceptor = { delegate ->
                object : PlatformTextInputService by delegate {
                    override fun startInput() {
                        events += "startInput:${host.activeRequests.size}"
                        delegate.startInput()
                    }

                    override fun stopInput() {
                        events += "stopInput:${host.activeRequests.size}"
                        delegate.stopInput()
                    }
                }
            }
            host.onStart = { events += "host" }
            val request = PlatformTextInputMethodRequest { BaseInputConnection(view, false) }

            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    manager.textInputSession { startInputMethod(request) }
                }

            assertThat(host.activeRequests).hasSize(1)
            assertThat(events).containsExactly("host", "startInput:1").inOrder()

            job.cancel()

            // Cleared synchronously by the cancellation, before any dispatch.
            assertThat(host.activeRequests).isEmpty()
            assertThat(events.last()).startsWith("stopInput")
            advanceUntilIdle()
        }

    @Test
    fun sessionWithoutStartInputMethod_doesNotCallHost() =
        runTest(UnconfinedTestDispatcher()) {
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    manager.textInputSession { awaitCancellation() }
                }

            assertThat(host.startCalls).isEqualTo(0)

            job.cancel()
        }

    @Test
    fun session_suppressesFocusedLegacyEditor_andRestoresOnExit() =
        runTest(UnconfinedTestDispatcher()) {
            manager.textInputService.startInput(TextFieldValue(), ImeOptions.Default, {}, {})
            val legacyRequest = host.activeRequests.single()

            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    manager.textInputSession { awaitCancellation() }
                }

            // Even before startInputMethod is called, an active session hides the legacy editor.
            assertThat(host.activeRequests).isEmpty()

            job.cancel()

            // When the session exits without stopping legacy input, legacy is restored.
            assertThat(host.activeRequests).containsExactly(legacyRequest)
        }

    @Test
    fun legacyEditorFocusWhileSessionActive_isSuppressed() =
        runTest(UnconfinedTestDispatcher()) {
            val request = PlatformTextInputMethodRequest { BaseInputConnection(view, false) }
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    manager.textInputSession { startInputMethod(request) }
                }
            val sessionRequest = host.activeRequests.single()

            // Focusing a legacy editor mid-session does not add a legacy host call.
            manager.textInputService.startInput(TextFieldValue(), ImeOptions.Default, {}, {})
            assertThat(host.activeRequests).containsExactly(sessionRequest)
            assertThat(host.startCalls).isEqualTo(1)

            job.cancel()
            assertThat(host.activeRequests).isEmpty()
        }

    @Test
    fun legacyEditorFocusWhileSessionWaiting_startsWhenSessionEnds() =
        runTest(UnconfinedTestDispatcher()) {
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    manager.textInputSession { awaitCancellation() }
                }

            // Focusing a legacy editor before startInputMethod stays suppressed.
            manager.textInputService.startInput(TextFieldValue(), ImeOptions.Default, {}, {})
            assertThat(host.activeRequests).isEmpty()
            assertThat(host.startCalls).isEqualTo(0)

            job.cancel()

            // Once the session ends without stopInput, the focused legacy editor becomes active.
            assertThat(host.activeRequests).hasSize(1)
            assertThat(host.startCalls).isEqualTo(1)
        }

    @Test
    fun replacingSessionWhileLegacyFocused_doesNotRestoreLegacyBetweenSessions() =
        runTest(UnconfinedTestDispatcher()) {
            manager.textInputService.startInput(TextFieldValue(), ImeOptions.Default, {}, {})
            assertThat(host.startCalls).isEqualTo(1)

            val job1 =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    manager.textInputSession { awaitCancellation() }
                }
            assertThat(host.activeRequests).isEmpty()

            val job2 =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    manager.textInputSession { awaitCancellation() }
                }
            assertThat(host.activeRequests).isEmpty()
            // Only the initial legacy start call occurred; no flicker between job1 and job2.
            assertThat(host.startCalls).isEqualTo(1)

            job1.cancel()
            job2.cancel()
            assertThat(host.activeRequests).hasSize(1)
            assertThat(host.startCalls).isEqualTo(2)
        }

    @Test
    fun sessionRequest_afterCancellation_returnsInertConnection() =
        runTest(UnconfinedTestDispatcher()) {
            var factoryCalls = 0
            val request = PlatformTextInputMethodRequest {
                factoryCalls++
                BaseInputConnection(view, true)
            }
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    manager.textInputSession { startInputMethod(request) }
                }
            val hostRequest = host.activeRequests.single()

            job.cancel()

            // The host must stop using the request on cancellation; if it doesn't, it gets an
            // inert BaseInputConnection without invoking the disposed session request.
            val connection = hostRequest.createInputConnection(EditorInfo())
            assertThat(connection).isInstanceOf(BaseInputConnection::class.java)
            assertThat(factoryCalls).isEqualTo(0)
        }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun sessionConnection_marksInputConnectionCreated_onInterceptedService() =
        runTest(UnconfinedTestDispatcher()) {
            assumeTrue(AndroidComposeUiFlags.isSkipRedundantRestartInputOnInitialFocusEnabled)
            val imm = FakeInputMethodManager()
            val syncHost = RecordingInputMethodHost(view)
            val syncManager =
                AndroidPlatformTextInputManager(
                    host = syncHost,
                    matrixPositionCalculator = NoOpMatrixPositionCalculator,
                    afterFrameCommandExecutor = Executor { it.run() },
                    nextFrameCommandExecutor = Executor { it.run() },
                    coroutineContext = { Dispatchers.Unconfined },
                )
            platformTextInputServiceInterceptor = { original ->
                val origAndroid = original as TextInputServiceAndroid
                TextInputServiceAndroid(
                    view = origAndroid.view,
                    rootPositionCalculator = NoOpMatrixPositionCalculator,
                    inputMethodManager = imm,
                    afterFrameCommandExecutor = origAndroid.afterFrameCommandExecutor,
                    nextFrameCommandExecutor = origAndroid.nextFrameCommandExecutor,
                )
            }
            // Simulate InputMethodManager.isActive() querying View.onCreateInputConnection() on
            // initial focus.
            imm.onIsActive = {
                syncHost.activeRequests.single().createInputConnection(EditorInfo())
            }
            val request = PlatformTextInputMethodRequest { BaseInputConnection(view, false) }

            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    syncManager.textInputSession { startInputMethod(request) }
                }

            assertThat(imm.isActiveCalls).isEqualTo(1)
            assertThat(imm.restartCalls).isEqualTo(0)
            job.cancel()
        }

    @Test
    fun sessionWithStartInputMethod_doesNotRestoreLegacyOnExit() =
        runTest(UnconfinedTestDispatcher()) {
            manager.textInputService.startInput(TextFieldValue(), ImeOptions.Default, {}, {})
            assertThat(host.activeRequests).hasSize(1)

            val request = PlatformTextInputMethodRequest { BaseInputConnection(view, false) }
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    manager.textInputSession { startInputMethod(request) }
                }
            assertThat(host.activeRequests).hasSize(1)

            // Exiting a session that called startInputMethod calls TextInputService.stopInput(),
            // which clears legacy editor focus so legacy input is not restored.
            job.cancel()
            assertThat(host.activeRequests).isEmpty()
            assertThat(host.startCalls).isEqualTo(2)
        }

    @Test
    fun legacyHostCall_isIsolatedFromParentJob() {
        val parentJob = Job()
        val caught = mutableListOf<Throwable>()
        val handler = CoroutineExceptionHandler { _, throwable -> caught += throwable }
        val isolatedManager =
            AndroidPlatformTextInputManager(
                host = host,
                matrixPositionCalculator = NoOpMatrixPositionCalculator,
                afterFrameCommandExecutor = Executor {},
                nextFrameCommandExecutor = Executor {},
                coroutineContext = { Dispatchers.Unconfined + parentJob + handler },
            )

        isolatedManager.textInputService.startInput(TextFieldValue(), ImeOptions.Default, {}, {})
        assertThat(host.activeRequests).hasSize(1)

        // Cancelling the parent (e.g., Recomposer effectJob swap) leaves the legacy host call
        // intact.
        parentJob.cancel()
        assertThat(host.activeRequests).hasSize(1)

        isolatedManager.textInputService.stopInput()
        assertThat(host.activeRequests).isEmpty()

        // An exception in host.startInputMethod does not cancel a fresh parentJob.
        val healthyParent = Job()
        val throwingHost =
            RecordingInputMethodHost(view).apply {
                onStart = { throw IllegalStateException("host failure") }
            }
        val throwingManager =
            AndroidPlatformTextInputManager(
                host = throwingHost,
                matrixPositionCalculator = NoOpMatrixPositionCalculator,
                afterFrameCommandExecutor = Executor {},
                nextFrameCommandExecutor = Executor {},
                coroutineContext = { Dispatchers.Unconfined + healthyParent + handler },
            )

        throwingManager.textInputService.startInput(TextFieldValue(), ImeOptions.Default, {}, {})
        assertThat(caught).hasSize(1)
        assertThat(healthyParent.isActive).isTrue()
        healthyParent.cancel()
    }

    @Test
    fun session_whenHostThrowsSynchronously_skipsStartInput() =
        runTest(UnconfinedTestDispatcher()) {
            val counts = recordStartStopCalls()
            host.onStart = { throw IllegalStateException("host failure") }
            val request = PlatformTextInputMethodRequest { BaseInputConnection(view, false) }

            val failure = runCatching {
                manager.textInputSession { startInputMethod(request) }
            }

            assertThat(failure.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
            assertThat(counts.startCalls).isEqualTo(0)
            assertThat(counts.stopCalls).isEqualTo(0)
        }

    @Test
    fun session_whenHostCancelsSynchronously_skipsStartInput() =
        runTest(UnconfinedTestDispatcher()) {
            val counts = recordStartStopCalls()
            host.onStart = { throw CancellationException("host detached") }
            val request = PlatformTextInputMethodRequest { BaseInputConnection(view, false) }

            val job = launch { manager.textInputSession { startInputMethod(request) } }

            assertThat(job.isCancelled).isTrue()
            assertThat(counts.startCalls).isEqualTo(0)
            assertThat(counts.stopCalls).isEqualTo(0)
        }

    @Test
    fun session_whenHostCancelsAsynchronously_stopsInputAndCancelsSession() =
        runTest(UnconfinedTestDispatcher()) {
            val counts = recordStartStopCalls()
            val request = PlatformTextInputMethodRequest { BaseInputConnection(view, false) }

            val job = launch { manager.textInputSession { startInputMethod(request) } }
            assertThat(job.isActive).isTrue()
            assertThat(counts.startCalls).isEqualTo(1)
            assertThat(counts.stopCalls).isEqualTo(0)

            host.cancelActiveRequest()

            assertThat(job.isCancelled).isTrue()
            assertThat(counts.stopCalls).isEqualTo(1)
            assertThat(host.activeRequests).isEmpty()
        }

    @Test
    fun innerSessionReplacement_exposesNewRequestSynchronouslyUnderDispatchingDispatcher() =
        runTest(StandardTestDispatcher()) {
            val created = mutableListOf<String>()
            val request1 = PlatformTextInputMethodRequest {
                created += "first"
                BaseInputConnection(view, false)
            }
            val request2 = PlatformTextInputMethodRequest {
                created += "second"
                BaseInputConnection(view, false)
            }

            val outerJob = launch {
                manager.textInputSession {
                    launch(start = CoroutineStart.UNDISPATCHED) { startInputMethod(request1) }
                    host.activeRequests.single().createInputConnection(EditorInfo())

                    // Replacing the inner session on a dispatching dispatcher must install the new
                    // request synchronously before queued cancellation unwinds.
                    launch(start = CoroutineStart.UNDISPATCHED) { startInputMethod(request2) }
                    host.activeRequests.single().createInputConnection(EditorInfo())

                    awaitCancellation()
                }
            }
            advanceUntilIdle()

            assertThat(created).containsExactly("first", "second").inOrder()
            assertThat(host.startCalls).isEqualTo(2)
            assertThat(host.activeRequests).hasSize(1)

            outerJob.cancel()
        }

    @Test
    fun legacyEditor_handoffAndOverwriteWithoutStop_keepsOneHostCall() {
        manager.textInputService.startInput(TextFieldValue("a"), ImeOptions.Default, {}, {})
        assertThat(host.activeRequests).hasSize(1)
        val firstRequest = host.activeRequests.single()

        // Overwrite without stopInput keeps the existing host call without creating a second one.
        manager.textInputService.startInput(TextFieldValue("b"), ImeOptions.Default, {}, {})
        assertThat(host.activeRequests).containsExactly(firstRequest)
        assertThat(host.startCalls).isEqualTo(1)

        // Same-pass stopInput then startInput cycles cleanly to one active host call.
        manager.textInputService.stopInput()
        assertThat(host.activeRequests).isEmpty()
        manager.textInputService.startInput(TextFieldValue("c"), ImeOptions.Default, {}, {})
        assertThat(host.activeRequests).hasSize(1)
        assertThat(host.startCalls).isEqualTo(2)
    }

    @Test
    fun androidComposeView_startInputMethod_setsAndGuardsRequest() =
        runTest(UnconfinedTestDispatcher()) {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            val recomposer = Recomposer(Dispatchers.Unconfined)
            try {
                val activity = controller.get()
                val composeView =
                    AndroidComposeView(
                        activity,
                        ComposeViewContext(
                            view = activity.window.decorView,
                            compositionContext = recomposer,
                            lifecycleOwner = activity,
                            savedStateRegistryOwner = activity,
                        ),
                    )
                assertThat(composeView.onCheckIsTextEditor()).isFalse()
                assertThat(composeView.onCreateInputConnection(EditorInfo())).isNull()

                val conn1 = BaseInputConnection(composeView, false)
                val conn2 = BaseInputConnection(composeView, false)
                val job1 =
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        composeView.startInputMethod { conn1 }
                    }
                assertThat(composeView.onCheckIsTextEditor()).isTrue()
                assertThat(composeView.onCreateInputConnection(EditorInfo()))
                    .isSameInstanceAs(conn1)

                // If a second call starts before the first finishes cancelling, cancelling the
                // first does not clear the newer request.
                val job2 =
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        composeView.startInputMethod { conn2 }
                    }
                job1.cancel()
                assertThat(composeView.onCheckIsTextEditor()).isTrue()
                assertThat(composeView.onCreateInputConnection(EditorInfo()))
                    .isSameInstanceAs(conn2)

                job2.cancel()
                assertThat(composeView.onCheckIsTextEditor()).isFalse()
                assertThat(composeView.onCreateInputConnection(EditorInfo())).isNull()
            } finally {
                recomposer.cancel()
                controller.destroy()
            }
        }

    private class StartStopCounts {
        var startCalls = 0
        var stopCalls = 0
    }

    private fun recordStartStopCalls(): StartStopCounts {
        val counts = StartStopCounts()
        platformTextInputServiceInterceptor = { delegate ->
            object : PlatformTextInputService by delegate {
                override fun startInput() {
                    counts.startCalls++
                }

                override fun stopInput() {
                    counts.stopCalls++
                }
            }
        }
        return counts
    }
}

@Suppress("DEPRECATION")
private class FakeInputMethodManager : InputMethodManager {
    var isActiveCalls = 0
    var restartCalls = 0
    var onIsActive: () -> Unit = {}

    override fun isActive(): Boolean {
        isActiveCalls++
        onIsActive()
        return true
    }

    override fun restartInput() {
        restartCalls++
    }

    override fun showSoftInput() {}

    override fun hideSoftInput() {}

    override fun updateExtractedText(token: Int, extractedText: ExtractedText) {}

    override fun updateSelection(
        selectionStart: Int,
        selectionEnd: Int,
        compositionStart: Int,
        compositionEnd: Int,
    ) {}

    override fun updateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo) {}
}

private object NoOpMatrixPositionCalculator : MatrixPositionCalculator {
    override fun localToScreen(localPosition: Offset): Offset = localPosition

    override fun localToScreen(localTransform: Matrix) {}

    override fun screenToLocal(positionOnScreen: Offset): Offset = positionOnScreen
}

/** A [ComposeUiHost] that records [startInputMethod] calls; other members are inert. */
@OptIn(ExperimentalComposeUiHostApi::class)
private class RecordingInputMethodHost(override val view: View) : ComposeUiHost {
    val activeRequests = mutableListOf<PlatformTextInputMethodRequest>()
    private var activeContinuation: CancellableContinuation<Nothing>? = null
    var startCalls = 0
    var onStart: () -> Unit = {}

    fun cancelActiveRequest() {
        activeContinuation?.cancel()
    }

    override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
        startCalls++
        onStart()
        suspendCancellableCoroutine<Nothing> { continuation ->
            activeRequests += request
            activeContinuation = continuation
            continuation.invokeOnCancellation {
                activeRequests -= request
                if (activeContinuation === continuation) activeContinuation = null
            }
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
