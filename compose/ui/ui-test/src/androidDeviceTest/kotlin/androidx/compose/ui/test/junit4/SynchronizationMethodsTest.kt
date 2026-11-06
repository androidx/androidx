/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.test.junit4

import android.app.Dialog
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.EmptyActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.AndroidComposeUiTestEnvironment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class SynchronizationMethodsTest {

    private val environment =
        AndroidComposeUiTestEnvironment<ComponentActivity> {
            throw NotImplementedError("This test shouldn't use the Activity")
        }
    private val composeRootRegistry = environment.composeRootRegistry
    private val test = environment.test

    @get:Rule val testName = TestName()

    @get:Rule
    val registryRule: TestRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                composeRootRegistry.withRegistry { base.evaluate() }
            }
        }
    }

    @Before
    fun addResumedComposeRootMock() {
        composeRootRegistry.registerComposeRoot(mockResumedComposeRoot())
    }

    @Test
    fun runOnUiThread() {
        val result = test.runOnUiThread { "Hello" }
        assertThat(result).isEqualTo("Hello")
    }

    @Test
    fun runOnUiThread_void() {
        var called = false
        test.runOnUiThread { called = true }
        assertThat(called).isTrue()
    }

    @Test
    fun runOnUiThread_nullable() {
        val result: String? = test.runOnUiThread { null }
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun runOnUiThread_exception_rethrownWithCallersStackTrace() {
        var errorThrown = false
        val expectedError = CustomException("test")
        try {
            test.runOnUiThread<Unit> { throw expectedError }
        } catch (t: Throwable) {
            errorThrown = true
            assertWithMessage("caught error should be the original error")
                .that(t)
                .isSameInstanceAs(expectedError)
            assertWithMessage("test thread's stacktrace should be added as a suppressed exception")
                .that(
                    t.suppressedExceptions.first().stackTrace.any {
                        testName.methodName in it.methodName
                    }
                )
                .isTrue()
        }
        assertWithMessage("error on the UI thread was not propagated by runOnUiThread")
            .that(errorThrown)
            .isEqualTo(true)
    }

    @Test
    fun runOnIdle() {
        val result = test.runOnIdle { "Hello" }
        assertThat(result).isEqualTo("Hello")
    }

    @Test
    fun runOnIdle_void() {
        var called = false
        test.runOnIdle { called = true }
        assertThat(called).isTrue()
    }

    @Test
    fun runOnIdle_nullable() {
        val result: String? = test.runOnIdle { null }
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun runOnIdle_waitForIdle() {
        test.runOnIdle { test.waitForIdle() }
    }

    @Test
    fun runOnIdle_runOnIdle() {
        test.runOnIdle { test.runOnIdle {} }
    }

    @Test
    fun waitForIdle_dialogDetachedBeforeFrameIsScheduled() {
        withDialogRoot(onSelected = { dialog, _ -> dialog.dismiss() }) { view ->
            test.waitForIdle()
            assertThat(view.isAttachedToWindow).isFalse()
        }
    }

    @Test
    fun waitForIdle_dialogDetachedDuringFrame() {
        withDialogRoot(
            onSelected = { dialog, _ ->
                Choreographer.getInstance().postFrameCallback { dialog.dismiss() }
            }
        ) { view ->
            test.waitForIdle()
            assertThat(view.isAttachedToWindow).isFalse()
        }
    }

    @Test
    fun awaitIdle_dialogDetachedDuringFrame() {
        withDialogRoot(
            onSelected = { dialog, _ ->
                Choreographer.getInstance().postFrameCallback { dialog.dismiss() }
            }
        ) { view ->
            runBlocking { test.awaitIdle() }
            assertThat(view.isAttachedToWindow).isFalse()
        }
    }

    @Test
    fun waitForIdle_waitsForDrawAfterAnimationCallback() {
        val drawsAtSelection = AtomicInteger()
        withDialogRoot(
            onSelected = { _, view ->
                drawsAtSelection.set(view.draws.get())
                view.invalidate()
            }
        ) { view ->
            test.waitForIdle()
            assertThat(view.draws.get()).isGreaterThan(drawsAtSelection.get())
        }
    }

    /** Controls the detach race using a real window and real View callback queues. */
    private fun withDialogRoot(
        onSelected: (Dialog, FrameWaitView) -> Unit,
        block: (FrameWaitView) -> Unit,
    ) {
        ActivityScenario.launch(EmptyActivity::class.java).use { scenario ->
            lateinit var dialog: Dialog
            lateinit var view: FrameWaitView
            val selected = AtomicBoolean(false)
            val recovered = AtomicBoolean(false)
            val handler = Handler(Looper.getMainLooper())
            // Reattach to release callbacks stranded in View's run queue, so a regression
            // fails an assertion instead of hanging the instrumentation process.
            val recover = Runnable {
                recovered.set(true)
                dialog.show()
            }
            scenario.onActivity { activity ->
                dialog = Dialog(activity)
                view = FrameWaitView(activity)
                dialog.setContentView(view)
                dialog.show()
            }
            val root = mock<ViewRootForTest>()
            doReturn(view).whenever(root).view
            doReturn(true).whenever(root).isLifecycleInResumedState
            try {
                assertThat(view.attached.await(5, TimeUnit.SECONDS)).isTrue()
                composeRootRegistry.registerComposeRoot(root)
                view.onSelected = {
                    selected.set(true)
                    test.runOnUiThread {
                        handler.postDelayed(recover, 3_000)
                        onSelected(dialog, view)
                    }
                }
                block(view)
                assertThat(selected.get()).isTrue()
                assertWithMessage("Frame synchronization must not require reattaching the dialog")
                    .that(recovered.get())
                    .isFalse()
            } finally {
                composeRootRegistry.unregisterComposeRoot(root)
                test.runOnUiThread {
                    handler.removeCallbacks(recover)
                    dialog.dismiss()
                }
            }
        }
    }

    private class FrameWaitView(activity: ComponentActivity) : View(activity) {
        val attached = CountDownLatch(1)
        val draws = AtomicInteger()
        var onSelected: (() -> Unit)? = null

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            attached.countDown()
        }

        override fun onDraw(canvas: Canvas) {
            draws.incrementAndGet()
        }

        override fun getRootView(): View = this

        override fun isAttachedToWindow(): Boolean {
            val attached = super.isAttachedToWindow()
            if (Looper.myLooper() != Looper.getMainLooper()) {
                // Model detachment after the caller observed an attached root. Android's
                // attachment check and callback scheduling are not atomic with dismissal.
                val action = onSelected
                onSelected = null
                action?.invoke()
            }
            return attached
        }
    }

    private fun mockResumedComposeRoot(): ViewRootForTest {
        val composeRoot = mock<ViewRootForTest>()
        doReturn(true).whenever(composeRoot).isLifecycleInResumedState
        doReturn(mockRootView()).whenever(composeRoot).view
        return composeRoot
    }

    private fun mockRootView(): View {
        val rootView = mock<View>()
        doReturn(false).whenever(rootView).isAttachedToWindow
        doReturn(rootView).whenever(rootView).rootView
        return rootView
    }
}

private class CustomException(message: String?) : Exception(message)
