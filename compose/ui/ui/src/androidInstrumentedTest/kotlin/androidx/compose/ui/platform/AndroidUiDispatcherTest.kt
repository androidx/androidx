/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.view.Choreographer
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.test.setViewLayerTypeForApi28
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.test.fail
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AndroidUiDispatcherTest {
    @get:Rule val rule = activityScenarioRule<AppCompatActivity>()

    @Before
    fun setup() {
        setViewLayerTypeForApi28()
    }

    @Test
    fun currentThreadIsMainOnMainThread() =
        runBlocking(Dispatchers.Main) {
            assertSame(AndroidUiDispatcher.Main, AndroidUiDispatcher.CurrentThread)
        }

    @Test
    fun runsBeforeFrameCallback() =
        runBlocking(Dispatchers.Main) {
            rule.scenario.onActivity {
                // Force creation of decor view to ensure we have a frame scheduled
                it.window.decorView
            }

            var ranOnUiDispatcher = false
            launch(AndroidUiDispatcher.Main) { ranOnUiDispatcher = true }

            val choreographerResult = CompletableDeferred<Boolean>()
            Choreographer.getInstance().postFrameCallback {
                choreographerResult.complete(ranOnUiDispatcher)
            }

            assertTrue("UI dispatcher ran before choreographer frame", choreographerResult.await())
        }

    /**
     * Test that an AndroidUiDispatcher can be wrapped by another ContinuationInterceptor without
     * breaking the MonotonicFrameClock's ability to coordinate with its original dispatcher.
     *
     * Construct a situation where the Choreographer contains three frame callbacks:
     * 1) checkpoint 1
     * 2) the AndroidUiDispatcher awaiting-frame callback
     * 3) checkpoint 2 Confirm that a call to withFrameNanos made *after* these three frame
     *    callbacks are enqueued runs *before* checkpoint 2, indicating that it ran with the
     *    original dispatcher's awaiting-frame callback, even though we wrapped the dispatcher.
     */
    @Test
    fun wrappedDispatcherPostsToDispatcherFrameClock() =
        runBlocking(Dispatchers.Main) {
            val uiDispatcherContext = AndroidUiDispatcher.Main
            val uiDispatcher = uiDispatcherContext[ContinuationInterceptor] as CoroutineDispatcher
            val wrapperDispatcher =
                object : CoroutineDispatcher() {
                    override fun dispatch(context: CoroutineContext, block: Runnable) {
                        uiDispatcher.dispatch(context, block)
                    }
                }

            val choreographer = Choreographer.getInstance()

            val expectCount = AtomicInteger(1)
            fun expect(value: Int) {
                while (true) {
                    val old = expectCount.get()
                    if (old != value) fail("expected sequence $old but encountered $value")
                    if (expectCount.compareAndSet(value, value + 1)) break
                }
            }

            choreographer.postFrameCallback { expect(1) }

            launch(uiDispatcherContext, start = CoroutineStart.UNDISPATCHED) {
                withFrameNanos { expect(2) }
            }

            choreographer.postFrameCallback { expect(4) }

            withContext(uiDispatcherContext + wrapperDispatcher) {
                withFrameNanos { expect(3) }
                expect(5)
            }
        }
}
