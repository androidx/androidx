/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.platform

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.JobSubject.Companion.assertThat
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.RenderingEntityFactory
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val INFINITE_BOUNDS =
    FloatSize3d(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)

private const val TIMEOUT = 50L

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ActivityExtKtTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    private val testRuntime: TestSceneRuntime
        get() =
            composeTestRule.session?.runtimes?.filterIsInstance<TestSceneRuntime>()?.firstOrNull()
                ?: error("Could not find test runtime.")

    @Test
    fun requestHomeSpace_completes_doesNotHangOrThrow() = runTest {
        withTimeout(TIMEOUT) { composeTestRule.activity.requestHomeSpace() }

        assertThat(composeTestRule.session?.scene?.activitySpace?.bounds)
            .isNotEqualTo(INFINITE_BOUNDS)
    }

    @Test
    fun requestFullSpace_completes_doesNotHangOrThrow() = runTest {
        withTimeout(TIMEOUT) { composeTestRule.activity.requestFullSpace() }

        assertThat(composeTestRule.session?.scene?.activitySpace?.bounds).isEqualTo(INFINITE_BOUNDS)
    }

    @Test
    fun requestSpace_sessionNotAvailable_returnsImmediately() = runTest {
        composeTestRule.activity.disableXr()

        val homeJob = launch { composeTestRule.activity.requestHomeSpace() }
        val fullJob = launch { composeTestRule.activity.requestFullSpace() }
        advanceUntilIdle()

        assertThat(homeJob.isCompleted).isTrue()
        assertThat(fullJob.isCompleted).isTrue()
    }

    @Test
    fun requestHomeSpace_coroutineCancelled_removesListener() = runTest {
        composeTestRule.configureFakeSession(
            sceneRuntime = { runtime ->
                object : TestSceneRuntime(runtime) {
                    override fun requestHomeSpaceMode() {}
                }
            }
        )
        val listeners = testRuntime.activitySpace.onBoundsChangedListeners
        assertThat(listeners).isEmpty()

        val job = launch { composeTestRule.activity.requestHomeSpace() }
        advanceUntilIdle() // wait for the coroutine to start and register the listeners
        assertThat(listeners).hasSize(1)
        job.cancelAndJoin()

        assertThat(listeners).isEmpty()
    }

    @Test
    fun requestFullSpace_coroutineCancelled_removesListener() = runTest {
        composeTestRule.configureFakeSession(
            sceneRuntime = { runtime ->
                object : TestSceneRuntime(runtime) {
                    override fun requestFullSpaceMode() {}
                }
            }
        )
        val listeners = testRuntime.activitySpace.onBoundsChangedListeners
        assertThat(listeners).isEmpty()

        composeTestRule.session?.scene?.requestHomeSpaceMode()

        val job = launch { composeTestRule.activity.requestFullSpace() }
        advanceUntilIdle() // wait for the coroutine to start and register the listeners
        assertThat(listeners).hasSize(1)
        job.cancelAndJoin()

        assertThat(listeners).isEmpty()
    }

    @Test
    fun requestHomeSpace_activityDestroyed_requestCancelled() = runTest {
        // composeTestRule's activity lifecycle cannot be directly controlled, so we use
        // ActivityScenario for this test
        val scenario = ActivityScenario.launch(SubspaceTestingActivity::class.java)
        var job: Job? = null

        scenario.onActivity { activity ->
            val session =
                activity.configureFakeSession(
                    sceneRuntime = { runtime ->
                        object : TestSceneRuntime(runtime) {
                            override fun requestHomeSpaceMode() {} // Do nothing, so it suspends
                        }
                    }
                )

            session.scene.requestFullSpaceMode() // start in full space
            assertThat(session.scene.activitySpace.bounds).isEqualTo(INFINITE_BOUNDS)

            job = launch { activity.requestHomeSpace() }
            advanceUntilIdle()
        }

        assertThat(job).isActive()

        // Destroy the activity
        scenario.moveToState(Lifecycle.State.DESTROYED)
        advanceUntilIdle()

        assertThat(job).isCancelled()
    }

    @Test
    fun requestFullSpace_activityDestroyed_requestCancelled() = runTest {
        // composeTestRule's activity lifecycle cannot be directly controlled, so we use
        // ActivityScenario for this test
        val scenario = ActivityScenario.launch(SubspaceTestingActivity::class.java)
        var job: Job? = null

        scenario.onActivity { activity ->
            val session =
                activity.configureFakeSession(
                    sceneRuntime = { runtime ->
                        object : TestSceneRuntime(runtime) {
                            override fun requestFullSpaceMode() {} // Do nothing, so it suspends
                        }
                    }
                )

            session.scene.requestHomeSpaceMode() // start in home space
            assertThat(session.scene.activitySpace.bounds).isNotEqualTo(INFINITE_BOUNDS)

            job = launch { activity.requestFullSpace() }
            advanceUntilIdle()
        }

        assertThat(job).isActive()

        // Destroy the activity
        scenario.moveToState(Lifecycle.State.DESTROYED)
        advanceUntilIdle()

        assertThat(job).isCancelled()
    }

    @Test
    fun multipleRapidRequests_previousRequestCancelled() = runTest {
        composeTestRule.configureFakeSession(
            sceneRuntime = { runtime ->
                object : TestSceneRuntime(runtime) {
                    override fun requestHomeSpaceMode() {}
                }
            }
        )

        assertThat(testRuntime.activitySpace.onBoundsChangedListeners).isEmpty()
        testRuntime.requestFullSpaceMode()
        val job = launch { composeTestRule.activity.requestHomeSpace() }

        advanceUntilIdle()
        assertThat(testRuntime.activitySpace.onBoundsChangedListeners).hasSize(1)
        assertThat(job).isActive()
        val job2 = launch { composeTestRule.activity.requestHomeSpace() }

        advanceUntilIdle()
        assertThat(testRuntime.activitySpace.onBoundsChangedListeners).hasSize(1)
        assertThat(job).isCancelled()
        assertThat(job2).isActive()

        job2.cancelAndJoin()

        assertThat(job2).isCancelled()
        assertThat(testRuntime.activitySpace.onBoundsChangedListeners).isEmpty()
    }

    @Test
    fun requestHomeSpace_alreadyInHomeSpace_completes() = runTest {
        // First, get into home space.
        withTimeout(TIMEOUT) { composeTestRule.activity.requestHomeSpace() }
        assertThat(composeTestRule.session?.scene?.activitySpace?.bounds)
            .isNotEqualTo(INFINITE_BOUNDS)

        // Now, request it again and ensure it completes without issues.
        withTimeout(TIMEOUT) { composeTestRule.activity.requestHomeSpace() }
        assertThat(composeTestRule.session?.scene?.activitySpace?.bounds)
            .isNotEqualTo(INFINITE_BOUNDS)
    }

    @Test
    fun requestFullSpace_alreadyInFullSpace_completes() = runTest {
        // First, get into full space.
        withTimeout(TIMEOUT) { composeTestRule.activity.requestFullSpace() }
        assertThat(composeTestRule.session?.scene?.activitySpace?.bounds).isEqualTo(INFINITE_BOUNDS)

        // Now, request it again and ensure it completes without issues.
        withTimeout(TIMEOUT) { composeTestRule.activity.requestFullSpace() }
        assertThat(composeTestRule.session?.scene?.activitySpace?.bounds).isEqualTo(INFINITE_BOUNDS)
    }

    @Test
    fun awaitSpaceUpdated_noUpdate_suspends() = runTest {
        composeTestRule.configureFakeSession(
            sceneRuntime = { runtime ->
                object : TestSceneRuntime(runtime) {
                    override fun requestHomeSpaceMode() {} // Never signals completion
                }
            }
        )

        val job = launch { composeTestRule.activity.requestHomeSpace() }
        advanceUntilIdle()
        assertThat(job).isActive()

        // Advance time and check it's still active
        advanceTimeBy(1000)
        assertThat(job).isActive()

        job.cancelAndJoin()
    }

    @Test
    fun requestSpace_concurrently_isThreadSafe() = runTest {
        composeTestRule.configureFakeSession(
            sceneRuntime = { runtime ->
                object : TestSceneRuntime(runtime) {
                    override fun requestHomeSpaceMode() {
                        launch {
                            delay(10L)
                            super.requestHomeSpaceMode()
                        }
                    }

                    override fun requestFullSpaceMode() {
                        launch {
                            delay(10L)
                            super.requestFullSpaceMode()
                        }
                    }
                }
            }
        )

        val jobs = mutableListOf<Job>()
        repeat(10) {
            jobs +=
                if (it % 2 == 0) {
                    launch { composeTestRule.activity.requestHomeSpace() }
                } else {
                    launch { composeTestRule.activity.requestFullSpace() }
                }
        }
        // Let coroutines run to completion. withTimeout will handle hangs.
        withTimeout(500) { jobs.joinAll() }
        // If it completes without timeout or crash, we consider it thread-safe for coroutines.
        // The main goal is to ensure no crashes from concurrent access within coroutines.
    }

    @Test
    fun requestSpace_invalidActivityState_noAction() = runTest {
        composeTestRule.activity.finish()
        composeTestRule.waitForIdle()

        // These should not throw and should complete quickly.
        val homeJob = launch { composeTestRule.activity.requestHomeSpace() }
        val fulljob = launch { composeTestRule.activity.requestFullSpace() }
        advanceUntilIdle()

        assertThat(homeJob.isCompleted).isTrue()
        assertThat(fulljob.isCompleted).isTrue()
    }

    @Test
    fun requestSpace_underlyingApiThrows_exceptionPropagated() = runTest {
        val exception = RuntimeException("Test Exception")
        composeTestRule.configureFakeSession(
            sceneRuntime = { runtime ->
                object : TestSceneRuntime(runtime) {
                    override fun requestHomeSpaceMode() {
                        throw exception
                    }

                    override fun requestFullSpaceMode() {
                        throw exception
                    }
                }
            }
        )

        val homeResult = runCatching { composeTestRule.activity.requestHomeSpace() }
        assertThat(homeResult.exceptionOrNull()?.cause).isSameInstanceAs(exception)

        val fullResult = runCatching { composeTestRule.activity.requestFullSpace() }
        assertThat(fullResult.exceptionOrNull()?.cause).isSameInstanceAs(exception)
    }
}

private open class TestSceneRuntime(private val base: SceneRuntime) :
    SceneRuntime by base, RenderingEntityFactory by (base as RenderingEntityFactory) {
    override val activitySpace = TestActivitySpace(base.activitySpace)
}

private class TestActivitySpace(private val base: ActivitySpace) : ActivitySpace by base {
    val onBoundsChangedListeners: MutableList<ActivitySpace.OnBoundsChangedListener> =
        mutableListOf()

    override fun addOnBoundsChangedListener(listener: ActivitySpace.OnBoundsChangedListener) {
        onBoundsChangedListeners.add(listener)
        base.addOnBoundsChangedListener(listener)
    }

    override fun removeOnBoundsChangedListener(listener: ActivitySpace.OnBoundsChangedListener) {
        onBoundsChangedListeners.remove(listener)
        base.removeOnBoundsChangedListener(listener)
    }
}
