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

package androidx.activity.compose

import android.os.Build
import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PredictiveBackHandlerTestApi {
    @get:Rule
    val rule = createComposeRule()

    private fun OnBackPressedDispatcher.startGestureBack() =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            this.onBackPressed()
        } else {
            this.dispatchOnBackStarted(fakeBackEventCompat())
        }

    // send onBackPressed signal to trigger back completion but only for API 34+
    private fun OnBackPressedDispatcher.api34Complete() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            onBackPressed()
        }
    }

    @Test
    fun testHandleOnStart() {
        var onStart = false
        rule.setContent {
            PredictiveBackHandler { onStart = true }
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            Button(onClick = { dispatcher.startGestureBack() }) {
                Text(text = "backPress")
            }
        }

        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle {
            assertThat(onStart).isTrue()
        }
    }

    @Test
    fun testHandleOnComplete() {
        var counter = 0
        lateinit var dispatcher: OnBackPressedDispatcher

        rule.setContent {
            PredictiveBackHandler { progress ->
                progress.collect { }
                counter++
            }
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            Button(onClick = { dispatcher.startGestureBack() }) {
                Text(text = "backPress")
            }
        }

        rule.onNodeWithText("backPress").performClick()
        dispatcher.api34Complete()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun testDisabledBackHandler() {
        val result = mutableListOf<String>()
        var enabled by mutableStateOf(true)
        lateinit var dispatcher: OnBackPressedDispatcher

        rule.setContent {
            PredictiveBackHandler(enabled) { result += "onBack" }
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }

        dispatcher.startGestureBack()
        rule.runOnIdle {
            assertThat(result).isEqualTo(listOf("onBack"))
        }

        enabled = false
        rule.runOnIdle {
            dispatcher.startGestureBack()
            assertThat(result).isEqualTo(listOf("onBack"))
        }

        enabled = true
        rule.runOnIdle {
            dispatcher.startGestureBack()
            assertThat(result).isEqualTo(listOf("onBack", "onBack"))
        }
    }

    @Test
    fun testNoCollection() {
        val result = mutableListOf<String>()
        lateinit var dispatcher: OnBackPressedDispatcher

        rule.setContent {
            PredictiveBackHandler { _ ->
                result += "start"
                // simulate some extended work
                async {
                    delay(500)
                    result += "async"
                }
                result += "complete"
            }
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }

        dispatcher.startGestureBack()
        dispatcher.api34Complete()

        rule.waitUntil(1000) {
            result.size >= 3
        }
        rule.runOnIdle {
            assertThat(result).isEqualTo(listOf("start", "async", "complete"))
        }
    }

    @Test
    fun testRestartCancelsPreviousAsyncJob() {
        val result = mutableListOf<String>()
        var asyncStarted = false
        lateinit var dispatcher: OnBackPressedDispatcher

        rule.setContent {
            PredictiveBackHandler { _ ->
                result += "start"
                async {
                    asyncStarted = true
                    delay(300) // simulate some work
                    result += "async"
                }
            }
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }

        dispatcher.startGestureBack()
        rule.waitUntil { asyncStarted }
        dispatcher.startGestureBack()
        rule.waitUntil(1000) {
            result.size >= 3
        }

        rule.runOnIdle {
            // only second async work should complete
            assertThat(result).isEqualTo(listOf("start", "start", "async"))
        }
    }

    @Test
    fun testChildBackHandler() {
        val result = mutableListOf<String>()
        rule.setContent {
            PredictiveBackHandler { result += "parent" }
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            Button(onClick = { dispatcher.startGestureBack() }) {
                // only this second handler should be applied
                PredictiveBackHandler() { result += "child" }
                Text(text = "backPress")
            }
        }

        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle {
            assertThat(result).isEqualTo(listOf("child"))
        }
    }

    @Test
    fun testDisabledChildBackHandler() {
        val result = mutableListOf<String>()
        rule.setContent {
            // only first handler should be applied
            PredictiveBackHandler { result += "parent" }
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            Button(onClick = { dispatcher.startGestureBack() }) {
                PredictiveBackHandler(false) { result += "child" }
                Text(text = "backPress")
            }
        }

        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle {
            assertThat(result).isEqualTo(listOf("parent"))
        }
    }

    @Test
    fun testSiblingBackHandlers() {
        val result = mutableListOf<String>()
        rule.setContent {
            PredictiveBackHandler { result += "first" }
            // only this second handler should be applied
            PredictiveBackHandler { result += "second" }
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            Button(onClick = { dispatcher.startGestureBack() }) {
                Text(text = "backPress")
            }
        }

        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle {
            assertThat(result).isEqualTo(listOf("second"))
        }
    }

    @Test
    fun testDisabledSiblingBackHandlers() {
        val result = mutableListOf<String>()
        rule.setContent {
            PredictiveBackHandler { result += "first" }
            PredictiveBackHandler(false) { result += "second" }
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            Button(onClick = { dispatcher.startGestureBack() }) {
                // only this second handler should be applied
                Text(text = "backPress")
            }
        }

        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle {
            assertThat(result).isEqualTo(listOf("first"))
        }
    }

    @Test
    fun testBackHandlerOnBackChanged() {
        val results = mutableListOf<String>()
        var handler by mutableStateOf<(Flow<BackEventCompat>) -> Unit>(
            { results += "first" }
        )
        rule.setContent {
            PredictiveBackHandler(onBack = handler)
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            Button(onClick = { dispatcher.startGestureBack() }) {
                Text(text = "backPress")
            }
        }
        rule.onNodeWithText("backPress").performClick()
        rule.runOnIdle { handler = { results += "second" } }
        println("cfok perofmr second click")
        rule.onNodeWithText("backPress").performClick()

        rule.runOnIdle {
            assertThat(results).isEqualTo(listOf("first", "second"))
        }
    }

    @Test
    fun testLifecycleChange() {
        val lifecycleOwner = TestLifecycleOwner()
        var interceptedBack = false
        rule.setContent {
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            val dispatcherOwner =
                object : OnBackPressedDispatcherOwner, LifecycleOwner by lifecycleOwner {
                    override val onBackPressedDispatcher = dispatcher
                }
            dispatcher.addCallback(lifecycleOwner) { }
            CompositionLocalProvider(
                LocalOnBackPressedDispatcherOwner provides dispatcherOwner,
                LocalLifecycleOwner provides lifecycleOwner
            ) {
                PredictiveBackHandler { interceptedBack = true }
            }
            Button(onClick = { dispatcher.startGestureBack() }) {
                Text(text = "backPressed")
            }
        }

        lifecycleOwner.currentState = Lifecycle.State.CREATED
        lifecycleOwner.currentState = Lifecycle.State.RESUMED

        rule.onNodeWithText("backPressed").performClick()
        rule.runOnIdle {
            assertThat(interceptedBack).isEqualTo(true)
        }
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
@RequiresApi(34)
class PredictiveBackHandlerTestApi34 {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testHandleOnProgress() {
        val result = mutableListOf<Int>()
        var counter = 0
        lateinit var dispatcher: OnBackPressedDispatcher
        rule.setContent {
            PredictiveBackHandler { progress ->
                progress.collect { result += counter++ }
            }
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }

        dispatcher.dispatchOnBackStarted(fakeBackEventCompat())
        dispatcher.dispatchOnBackProgressed(fakeBackEventCompat())
        dispatcher.dispatchOnBackProgressed(fakeBackEventCompat())
        dispatcher.dispatchOnBackProgressed(fakeBackEventCompat())

        rule.waitForIdle()
        assertThat(result).isEqualTo(listOf(0, 1, 2))
    }

    @Test
    fun testHandleOnCancelled() {
        val result = mutableListOf<String>()
        lateinit var dispatcher: OnBackPressedDispatcher

        rule.setContent {
            PredictiveBackHandler { progress ->
                try {
                    result += "start"
                    progress.collect { result += "progress" }
                    result += "completed"
                } catch (e: CancellationException) {
                    result += e.message!!
                }
            }
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }

        dispatcher.dispatchOnBackStarted(fakeBackEventCompat())
        dispatcher.dispatchOnBackProgressed(fakeBackEventCompat())
        dispatcher.dispatchOnBackCancelled()

        rule.runOnIdle {
            assertThat(result).isEqualTo(listOf("start", "progress", "onBack cancelled"))
        }
    }

    @Test
    fun testHandleOnCancelledWithoutCatch() {
        val result = mutableListOf<String>()
        lateinit var dispatcher: OnBackPressedDispatcher

        rule.setContent {
            PredictiveBackHandler { progress ->
                result += "start"
                progress.collect { result += "progress" }
                result += "completed"
            }
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }

        dispatcher.dispatchOnBackStarted(fakeBackEventCompat())
        dispatcher.dispatchOnBackProgressed(fakeBackEventCompat())
        dispatcher.dispatchOnBackCancelled()

        rule.runOnIdle {
            assertThat(result).isEqualTo(listOf("start", "progress"))
        }
    }

    @Test
    fun testRestartCancelledBack() {
        val result = mutableListOf<String>()
        lateinit var dispatcher: OnBackPressedDispatcher

        rule.setContent {
            PredictiveBackHandler { progress ->
                try {
                    progress.collect { result += "progress" }
                    result += "complete"
                } catch (e: CancellationException) {
                    result += e.message!!
                }
            }
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }

        dispatcher.dispatchOnBackStarted(fakeBackEventCompat())
        dispatcher.dispatchOnBackProgressed(fakeBackEventCompat())
        dispatcher.dispatchOnBackCancelled()

        rule.runOnIdle {
            assertThat(result).isEqualTo(listOf("progress", "onBack cancelled"))
            result.clear()
        }

        // restart gesture and let to completion
        dispatcher.dispatchOnBackStarted(fakeBackEventCompat())
        dispatcher.dispatchOnBackProgressed(fakeBackEventCompat())
        dispatcher.dispatchOnBackProgressed(fakeBackEventCompat())
        dispatcher.onBackPressed()
        rule.runOnIdle {
            assertThat(result).isEqualTo(listOf("progress", "progress", "complete"))
        }
    }

    @Test
    fun testOnBackCancelledCancelsAsyncJob() {
        val result = mutableListOf<String>()
        var asyncStarted = false
        lateinit var dispatcher: OnBackPressedDispatcher

        rule.setContent {
            PredictiveBackHandler { _ ->
                result += "start"
                async {
                    asyncStarted = true
                    delay(300) // simulate some work
                    result += "async"
                }
            }
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }

        dispatcher.dispatchOnBackStarted(fakeBackEventCompat())
        rule.waitUntil { asyncStarted }
        dispatcher.dispatchOnBackCancelled()

        rule.runOnIdle {
            // time for async to complete if it would have completed
            runBlocking { delay(700) }
            // should only receive result of second async work
            assertThat(result).isEqualTo(listOf("start"))
        }
    }
}

private fun fakeBackEventCompat(progress: Float = 0f) =
    BackEventCompat(0.1F, 0.1F, progress, BackEvent.EDGE_LEFT)

private suspend fun async(work: suspend () -> Unit) = work()
