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

package androidx.glance.appwidget.testing.unit

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.unit.DpSize
import androidx.glance.Applier
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.LocalState
import androidx.glance.appwidget.LocalAppWidgetOptions
import androidx.glance.appwidget.RemoteViewsRoot
import androidx.glance.session.globalSnapshotMonitor
import androidx.glance.testing.GlanceNodeAssertion
import androidx.glance.testing.GlanceNodeAssertionCollection
import androidx.glance.testing.GlanceNodeMatcher
import androidx.glance.testing.TestContext
import androidx.glance.testing.matcherToSelector
import androidx.glance.testing.unit.GlanceMappedNode
import androidx.glance.testing.unit.MappedNode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

internal val DEFAULT_TIMEOUT = 2.seconds

/**
 * An implementation of [GlanceAppWidgetUnitTest] that provides APIs to run composition for
 * appwidget-specific Glance composable content.
 */
internal class GlanceAppWidgetUnitTestEnvironment(private val timeout: Duration) :
    GlanceAppWidgetUnitTest {
    private var testContext = TestContext<MappedNode, GlanceMappedNode>()
    private var testScope = TestScope()
    private var provideComposableJob: Job? = null

    // Data for composition locals
    private var context: Context? = null
    private val fakeGlanceID = GlanceAppWidgetUnitTestDefaults.glanceId()
    private var size: DpSize = GlanceAppWidgetUnitTestDefaults.size()
    private var state: Any? = null

    private val root = RemoteViewsRoot(10)

    private lateinit var recomposer: Recomposer
    private lateinit var composition: Composition

    @Suppress("UNUSED_EXPRESSION") // https://youtrack.jetbrains.com/issue/KT-21282
    // the UNUSED_EXPRESSION warning on block() call below is a false positive.
    fun runTest(block: GlanceAppWidgetUnitTest.() -> Unit) =
        testScope.runTest(timeout) {
            Log.d(TAG, "runTest start")
            var snapshotMonitor: Job? = null
            try {
                // GlobalSnapshotManager.ensureStarted() uses Dispatcher.Default, so using
                // globalSnapshotMonitor instead to be able to use test dispatcher instead.
                snapshotMonitor = launch { globalSnapshotMonitor() }
                val applier = Applier(root)
                recomposer = Recomposer(testScope.coroutineContext)
                composition = Composition(applier, recomposer)
                block()
            } finally {
                composition.dispose()
                snapshotMonitor?.cancel()
                recomposer.cancel()
                recomposer.join()
                provideComposableJob?.cancel()
                Log.d(TAG, "runTest complete")
            }
        }

    // Among the appWidgetOptions available, size related options shouldn't generally be necessary
    // for developers to look up - the LocalSize composition local should suffice. So, currently, we
    // only initialize host category.
    private val appWidgetOptions =
        Bundle().apply {
            putInt(
                AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                GlanceAppWidgetUnitTestDefaults.hostCategory()
            )
        }

    override fun provideComposable(composable: @Composable () -> Unit) {
        check(testContext.rootGlanceNode == null) { "provideComposable can only be called once" }

        provideComposableJob =
            testScope.launch {
                var compositionLocals =
                    arrayOf(
                        LocalGlanceId provides fakeGlanceID,
                        LocalState provides state,
                        LocalAppWidgetOptions provides appWidgetOptions,
                        LocalSize provides size
                    )
                context?.let {
                    compositionLocals = compositionLocals.plus(LocalContext provides it)
                }

                composition.setContent {
                    CompositionLocalProvider(
                        values = compositionLocals,
                        content = composable,
                    )
                }

                launch(currentCoroutineContext() + TestFrameClock()) {
                    recomposer.runRecomposeAndApplyChanges()
                }

                launch {
                    recomposer.currentState.collect { curState ->
                        Log.d(TAG, "Recomposer state: $curState")
                        when (curState) {
                            Recomposer.State.Idle -> {
                                testContext.rootGlanceNode =
                                    GlanceMappedNode(emittable = root.copy())
                            }
                            Recomposer.State.ShutDown -> {
                                cancel()
                            }
                            else -> {}
                        }
                    }
                }
            }
    }

    override fun awaitIdle() {
        testScope.testScheduler.advanceUntilIdle()
    }

    override fun onNode(
        matcher: GlanceNodeMatcher<MappedNode>
    ): GlanceNodeAssertion<MappedNode, GlanceMappedNode> {
        Log.d(TAG, "Letting all enqueued tasks finish before inspecting the tree")
        // Always let all the enqueued tasks finish before inspecting the tree.
        testScope.testScheduler.runCurrent()
        check(testContext.hasNodes()) {
            "No nodes found to perform the assertions. Provide the composable to be tested " +
                "using `provideComposable` function before performing assertions."
        }
        // Delegates matching to the next assertion.
        return GlanceNodeAssertion(testContext, matcher.matcherToSelector())
    }

    override fun onAllNodes(
        matcher: GlanceNodeMatcher<MappedNode>
    ): GlanceNodeAssertionCollection<MappedNode, GlanceMappedNode> {
        Log.d(TAG, "Letting all enqueued tasks finish before inspecting the tree")
        // Always let all the enqueued tasks finish before inspecting the tree.
        testScope.testScheduler.runCurrent()
        // Delegates matching to the next assertion.
        return GlanceNodeAssertionCollection(testContext, matcher.matcherToSelector())
    }

    override fun setAppWidgetSize(size: DpSize) {
        check(testContext.rootGlanceNode == null) {
            "setApWidgetSize should be called before calling provideComposable"
        }
        this.size = size
    }

    override fun <T> setState(state: T) {
        check(testContext.rootGlanceNode == null) {
            "setState should be called before calling provideComposable"
        }
        this.state = state
    }

    override fun setContext(context: Context) {
        check(testContext.rootGlanceNode == null) {
            "setContext should be called before calling provideComposable"
        }
        this.context = context
    }

    /** Test clock that sends all frames immediately. */
    // Same as TestUtils.TestFrameClock used in Glance unit tests.
    private class TestFrameClock : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R) =
            onFrame(System.currentTimeMillis())
    }

    companion object {
        const val TAG = "GlanceAppWidgetUnitTestEnvironment"
    }
}
