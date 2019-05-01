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

package androidx.ui.test.android

import android.app.Activity
import android.app.Instrumentation
import android.os.Looper
import android.view.Choreographer
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Density
import androidx.ui.core.SemanticsTreeNode
import androidx.ui.core.SemanticsTreeProvider
import androidx.ui.test.UiTestRunner
import com.google.r4a.Composable
import com.google.r4a.composeInto
import org.junit.Before
import org.junit.Rule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Allows to run ui tests on Android.
 *
 * Please not that you need to add the following activity
 * [androidx.ui.test.android.DefaultTestActivity] to you tests manifest file in order to use this.
 */
// TODO(pavlis): Move this to android specific directory
open class AndroidUiTestRunner : UiTestRunner {

    @get:Rule
    val activityTestRule = ActivityTestRule<DefaultTestActivity>(DefaultTestActivity::class.java)

    private lateinit var activity: DefaultTestActivity
    private lateinit var instrumentation: Instrumentation
    private lateinit var rootProvider: SemanticsTreeProvider

    val density: Density get() = Density(activity)

    @Before
    // TODO(pavlis): This is not great, if super forgets to call this (if redefining @before).
    fun setup() {
        activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        instrumentation = InstrumentationRegistry.getInstrumentation()
    }

    private fun runOnUiThread(runnable: () -> Unit) {
        activity.runOnUiThread {
            runnable.invoke()
        }
    }

    private fun onNextFrame(action: (time: Long) -> Unit) {
        Choreographer.getInstance().postFrameCallback {
            action.invoke(it)
        }
    }

    private fun runAfterCompose(action: (time: Long) -> Unit) {
        runOnUiThread {
            // TODO(catalintudor):  will change after public APIs for checking composition is done
            // will be added
            onNextFrame {
                onNextFrame {
                    action.invoke(it)
                }
            }
        }
    }

    private fun findCompositionRootProvider(): SemanticsTreeProvider {
        val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
        return findCompositionRootProvider(contentViewGroup)!!
    }

    private fun findCompositionRootProvider(parent: ViewGroup): SemanticsTreeProvider? {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is SemanticsTreeProvider) {
                return child
            } else if (child is ViewGroup) {
                val provider = findCompositionRootProvider(child)
                if (provider != null) {
                    return provider
                }
            }
        }
        return null
    }

    /**
     * Use this in your tests to setup the UI content to be tested. This should be called exactly
     * once per test.
     */
    @SuppressWarnings("SyntheticAccessor")
    fun setContent(composable: @Composable() () -> Unit) {
        val drawLatch = CountDownLatch(1)
        val runnable: Runnable = object : Runnable {
            override fun run() {
                activity.composeInto(composable)
            }
        }
        activityTestRule.runOnUiThread(runnable)
        drawLatch.await(1, TimeUnit.SECONDS)

        // TODO(pavlis): There are several things missing
        // 1) What about multiple roots?
        // 2) What about the composition changing so much that current provider is no longer valid?
        // => Maybe the providers can't be cached like this?
        rootProvider = findCompositionRootProvider()
    }

    override fun findSemantics(
        selector: (SemanticsTreeNode) -> Boolean
    ): List<SemanticsTreeNode> {
        return rootProvider.getAllSemanticNodes().filter { selector(it) }.toList()
    }

    override fun sendEvent(event: MotionEvent) {
        var isDone = false

        runOnUiThread {
            rootProvider.sendEvent(event)
        }

        runAfterCompose {
            synchronized(this) {
                isDone = true
            }
        }

        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw Exception("Test cannot be run on UI thread.")
        }

        doPollingCheck({
            synchronized(this) {
                isDone
            }
        })
    }
}

class DefaultTestActivity : Activity() {
    var hasFocusLatch = CountDownLatch(1)

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hasFocusLatch.countDown()
        }
    }
}

fun doPollingCheck(canProceed: () -> Boolean, timeoutPeriod: Long = 3000) {
    val timeSlice: Long = 50
    var timeout = timeoutPeriod

    if (canProceed()) {
        return
    }

    while (timeout > 0) {
        try {
            Thread.sleep(timeSlice)
        } catch (e: InterruptedException) {
            throw Exception("Unexpected InterruptedException")
        }

        if (canProceed()) {
            return
        }

        timeout -= timeSlice
    }

    throw Exception("Unexpected timeout")
}