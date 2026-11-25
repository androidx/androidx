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

package androidx.camera.effects.playservices.lowlight

import android.content.Context
import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Size
import androidx.camera.core.CameraEffect
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.effects.playservices.lowlight.LowLightBoostEffect.Companion.STATUS_ACTIVE
import androidx.camera.effects.playservices.lowlight.LowLightBoostEffect.Companion.STATUS_INACTIVE
import androidx.camera.effects.playservices.lowlight.LowLightBoostEffect.Companion.STATUS_OFF
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class LowLightBoostEffectTest {

    private lateinit var context: Context

    private val client = FakeLowLightBoostClient()

    private lateinit var sessionManager: LowLightBoostSessionManager

    private lateinit var processor: PlayServicesLowLightBoostSurfaceProcessor

    private lateinit var effect: LowLightBoostEffect

    // Coroutines launched by the test, which run on the main thread (the test thread).
    private val mainScope =
        CoroutineScope(SupervisorJob() + Handler(getMainLooper()).asCoroutineDispatcher())

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        PlayServicesLowLightBoostAvailability.reset()
        PlayServicesLowLightBoostAvailability.clientFactoryForTesting = { client }
        // Run the session work synchronously on the test thread.
        sessionManager =
            LowLightBoostSessionManager(
                CameraXExecutors.directExecutor(),
                Handler(getMainLooper()),
                rendererFactory = { FakeGlRenderer() },
            )
        processor = PlayServicesLowLightBoostSurfaceProcessor(context, { client }, sessionManager)
        effect = LowLightBoostEffect(CameraEffect.PREVIEW, processor)
    }

    @After
    fun tearDown() {
        mainScope.cancel()
        shadowOf(getMainLooper()).idle()
        PlayServicesLowLightBoostAvailability.reset()
    }

    @Test
    fun testLowLightBoostEffectInitialization() {
        val effect = LowLightBoostEffect(context, CameraEffect.PREVIEW)
        assertThat(effect.targets).isEqualTo(CameraEffect.PREVIEW)
        assertThat(effect.transformation).isEqualTo(CameraEffect.TRANSFORMATION_ARBITRARY)
        assertThat(effect.isLowLightBoostEnabled).isTrue()
        assertThat(effect.status).isEqualTo(STATUS_OFF)
        assertThat(effect.error).isNull()

        val effectWithAllTargets =
            LowLightBoostEffect(
                context,
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE,
            )
        assertThat(effectWithAllTargets.targets)
            .isEqualTo(
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE
            )

        assertThrows(IllegalArgumentException::class.java) {
            LowLightBoostEffect(context, CameraEffect.IMAGE_CAPTURE)
        }
    }

    @Test
    fun testIsLowLightBoostEnabledToggling() {
        // The processor callbacks run on the GL thread of the session manager.
        assertThat(effect.executor).isSameInstanceAs(sessionManager.glExecutor)
        assertThat(effect.isLowLightBoostEnabled).isTrue()

        effect.isLowLightBoostEnabled = false
        assertThat(effect.isLowLightBoostEnabled).isFalse()
        shadowOf(getMainLooper()).idle()
        assertThat(effect.status).isEqualTo(STATUS_OFF)
        assertThat(effect.error).isNull()

        // The effect isn't bound, so it doesn't process a camera stream.
        effect.isLowLightBoostEnabled = true
        assertThat(effect.isLowLightBoostEnabled).isTrue()
        shadowOf(getMainLooper()).idle()
        assertThat(effect.status).isEqualTo(STATUS_OFF)
        assertThat(effect.error).isNull()
    }

    @Test
    fun testIsSupported() = runBlocking {
        val camera0 = FakeCameraInfoInternal("0")
        client.cameraSupportedTask = { Tasks.forResult(false) }
        assertThat(LowLightBoostEffect.isSupported(context, camera0)).isFalse()

        // Once support is cached, isSupportedAsync completes immediately.
        PlayServicesLowLightBoostAvailability.setCameraSupported("0", true)
        val cachedSupportedFuture = LowLightBoostEffect.isSupportedAsync(context, camera0)
        assertThat(cachedSupportedFuture.isDone).isTrue()
        assertThat(cachedSupportedFuture.get()).isTrue()
        assertThat(LowLightBoostEffect.isSupported(context, camera0)).isTrue()
    }

    @Test
    fun testResolutionValidator() {
        val effect = LowLightBoostEffect(context, CameraEffect.PREVIEW)
        val validator = effect.resolutionValidator

        // Supported resolutions (width <= 2560, height <= 2560, area <= 4,194,304)
        assertThat(validator.test(Size(1920, 1080))).isTrue()
        assertThat(validator.test(Size(1920, 1440))).isTrue()
        assertThat(validator.test(Size(2560, 1440))).isTrue()

        // Unsupported resolutions (exceeds 2560 dimension or 4MP area)
        assertThat(validator.test(Size(3840, 2160))).isFalse()
        assertThat(validator.test(Size(2560, 1920))).isFalse()
    }

    @Test
    fun stateIsCommittedOnMainThread_notWhenPosted() {
        // Act.
        processor.postState(LowLightBoostStateSnapshot.ACTIVE)

        // Assert: the state is posted on the GL thread but only committed on the main thread.
        assertThat(processor.lastPostedState).isEqualTo(LowLightBoostStateSnapshot.ACTIVE)
        assertThat(effect.status).isEqualTo(STATUS_OFF)
        shadowOf(getMainLooper()).idle()
        assertThat(effect.status).isEqualTo(STATUS_ACTIVE)
        assertThat(effect.error).isNull()
    }

    @Test
    fun addListener_notifiesOnlyFutureTransitionsOnMainThread() {
        // Arrange: a state committed before the registration.
        commit(LowLightBoostStateSnapshot.INACTIVE)
        val listener = RecordingListener(effect)

        // Act.
        effect.addListener(listener)
        shadowOf(getMainLooper()).idle()
        commit(LowLightBoostStateSnapshot.ACTIVE)

        // Assert: the current state isn't emitted on registration, and the error didn't change.
        assertThat(listener.events)
            .containsExactly(Event.status(STATUS_ACTIVE, STATUS_ACTIVE, effectError = null))
        assertThat(listener.threads).containsExactly(getMainLooper().thread)
    }

    @Test
    fun addListener_errorEntryAndRecovery_readsConsistentStateInBothCallbacks() {
        verifyErrorEntryAndRecovery(register = { effect.addListener(it) })
    }

    @Test
    fun addListenerWithExecutor_errorEntryAndRecovery_readsConsistentStateInBothCallbacks() {
        verifyErrorEntryAndRecovery(register = { effect.addListener(Runnable::run, it) })
    }

    private fun verifyErrorEntryAndRecovery(register: (LowLightBoostEffect.Listener) -> Unit) {
        // Arrange.
        commit(LowLightBoostStateSnapshot.ACTIVE)
        val listener = RecordingListener(effect)
        register(listener)
        val error =
            LowLightBoostException(LowLightBoostException.ERROR_SESSION_FAILED, "Session died")

        // Act: (ACTIVE, null) -> (OFF, error) -> (INACTIVE, null).
        commit(LowLightBoostStateSnapshot.error(error))
        commit(LowLightBoostStateSnapshot.INACTIVE)

        // Assert: both callbacks of a transition read the new status and error.
        assertThat(listener.events)
            .containsExactly(
                Event.status(STATUS_OFF, STATUS_OFF, error),
                Event.error(error, STATUS_OFF, error),
                Event.status(STATUS_INACTIVE, STATUS_INACTIVE, effectError = null),
                Event.error(null, STATUS_INACTIVE, effectError = null),
            )
            .inOrder()
    }

    @Test
    fun addListenerWithExecutor_runsCallbacksOnExecutor() {
        // Arrange.
        val executor = QueueExecutor()
        val listener = RecordingListener(effect)
        effect.addListener(executor, listener)

        // Act.
        commit(LowLightBoostStateSnapshot.INACTIVE)

        // Assert: the callback is queued on the executor.
        assertThat(listener.events).isEmpty()
        executor.runAll()
        assertThat(listener.events)
            .containsExactly(Event.status(STATUS_INACTIVE, STATUS_INACTIVE, effectError = null))
    }

    @Test
    fun addListener_sameListenerTwice_isNoOp() {
        // Arrange: the second registration with another executor is ignored too.
        val executor = QueueExecutor()
        val listener = RecordingListener(effect)
        effect.addListener(listener)
        effect.addListener(listener)
        effect.addListener(executor, listener)

        // Act.
        commit(LowLightBoostStateSnapshot.INACTIVE)

        // Assert: the listener is notified once, on the main thread.
        assertThat(listener.events).hasSize(1)
        assertThat(executor.pendingCount).isEqualTo(0)
    }

    @Test
    fun removeListener_suppressesCallbacksQueuedOnExecutor() {
        // Arrange.
        val executor = QueueExecutor()
        val listener = RecordingListener(effect)
        effect.addListener(executor, listener)
        commit(LowLightBoostStateSnapshot.INACTIVE)
        commit(
            LowLightBoostStateSnapshot.error(
                LowLightBoostException(LowLightBoostException.ERROR_UNKNOWN, "Error")
            )
        )

        // Act.
        effect.removeListener(listener)
        executor.runAll()

        // Assert.
        assertThat(listener.events).isEmpty()
    }

    @Test
    fun removeListener_inStatusCallback_suppressesErrorCallbackOfSameTransition() {
        verifyRemovalInStatusCallback(register = { effect.addListener(it) })
    }

    @Test
    fun removeListenerWithExecutor_inStatusCallback_suppressesErrorCallbackOfSameTransition() {
        verifyRemovalInStatusCallback(register = { effect.addListener(Runnable::run, it) })
    }

    private fun verifyRemovalInStatusCallback(register: (LowLightBoostEffect.Listener) -> Unit) {
        // Arrange: a listener that removes itself when the status changes.
        commit(LowLightBoostStateSnapshot.ACTIVE)
        val events = mutableListOf<String>()
        val listener =
            object : LowLightBoostEffect.Listener {
                override fun onStatusChanged(status: Int) {
                    events.add("status")
                    effect.removeListener(this)
                }

                override fun onErrorChanged(error: LowLightBoostException?) {
                    events.add("error")
                }
            }
        register(listener)

        // Act: both the status and the error change.
        commit(
            LowLightBoostStateSnapshot.error(
                LowLightBoostException(LowLightBoostException.ERROR_UNKNOWN, "Error")
            )
        )
        commit(LowLightBoostStateSnapshot.INACTIVE)

        // Assert.
        assertThat(events).containsExactly("status")
    }

    @Test
    fun removeListener_inCallback_suppressesRemainingListenersOfTransition() {
        // Arrange: the first listener removes the second one.
        val second = RecordingListener(effect)
        val first =
            object : LowLightBoostEffect.Listener {
                override fun onStatusChanged(status: Int) {
                    effect.removeListener(second)
                }
            }
        effect.addListener(first)
        effect.addListener(second)

        // Act.
        commit(LowLightBoostStateSnapshot.INACTIVE)

        // Assert.
        assertThat(second.events).isEmpty()
    }

    @Test
    fun addListener_inCallback_isOnlyNotifiedOfLaterTransitions() {
        // Arrange: the first listener adds the second one.
        val second = RecordingListener(effect)
        val first =
            object : LowLightBoostEffect.Listener {
                override fun onStatusChanged(status: Int) {
                    effect.addListener(second)
                }
            }
        effect.addListener(first)

        // Act.
        commit(LowLightBoostStateSnapshot.INACTIVE)
        commit(LowLightBoostStateSnapshot.ACTIVE)

        // Assert.
        assertThat(second.events)
            .containsExactly(Event.status(STATUS_ACTIVE, STATUS_ACTIVE, effectError = null))
    }

    @Test
    fun addListener_afterRemoval_canUseAnotherExecutor() {
        // Arrange.
        val firstExecutor = QueueExecutor()
        val secondExecutor = QueueExecutor()
        val listener = RecordingListener(effect)
        effect.addListener(firstExecutor, listener)
        commit(LowLightBoostStateSnapshot.INACTIVE)

        // Act.
        effect.removeListener(listener)
        effect.addListener(secondExecutor, listener)
        commit(LowLightBoostStateSnapshot.ACTIVE)
        firstExecutor.runAll()
        secondExecutor.runAll()

        // Assert: the pending call on the first executor is dropped.
        assertThat(listener.events)
            .containsExactly(Event.status(STATUS_ACTIVE, STATUS_ACTIVE, effectError = null))
    }

    @Test
    fun addListenerWithThreadPool_deliversCallbacksSequentiallyInOrder() {
        // Arrange: a listener on an executor that runs tasks concurrently.
        val threadPool = Executors.newFixedThreadPool(4)
        val transitionCount = 100
        val statuses = Collections.synchronizedList(mutableListOf<Int>())
        val running = AtomicInteger(0)
        val maxRunning = AtomicInteger(0)
        val done = CountDownLatch(transitionCount)
        val listener =
            object : LowLightBoostEffect.Listener {
                override fun onStatusChanged(status: Int) {
                    maxRunning.accumulateAndGet(running.incrementAndGet()) { a, b -> maxOf(a, b) }
                    statuses.add(status)
                    running.decrementAndGet()
                    done.countDown()
                }
            }
        effect.addListener(threadPool, listener)

        try {
            // Act: alternate between INACTIVE and ACTIVE.
            val expected =
                List(transitionCount) { if (it % 2 == 0) STATUS_INACTIVE else STATUS_ACTIVE }
            expected.forEach {
                processor.postState(
                    if (it == STATUS_INACTIVE) LowLightBoostStateSnapshot.INACTIVE
                    else LowLightBoostStateSnapshot.ACTIVE
                )
            }
            shadowOf(getMainLooper()).idle()

            // Assert.
            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue()
            assertThat(statuses).containsExactlyElementsIn(expected).inOrder()
            assertThat(maxRunning.get()).isEqualTo(1)
        } finally {
            effect.removeListener(listener)
            threadPool.shutdown()
        }
    }

    @Test
    fun addAndRemoveListener_fromBackgroundThreads() {
        // Arrange.
        val listener = RecordingListener(effect)

        // Act.
        runOnBackgroundThread { effect.addListener(listener) }
        commit(LowLightBoostStateSnapshot.INACTIVE)
        runOnBackgroundThread { effect.removeListener(listener) }
        commit(LowLightBoostStateSnapshot.ACTIVE)

        // Assert.
        assertThat(listener.events)
            .containsExactly(Event.status(STATUS_INACTIVE, STATUS_INACTIVE, effectError = null))
        assertThat(listener.threads).containsExactly(getMainLooper().thread)
    }

    @Test
    fun distinctErrorsWithSameMessage_notifyErrorChangedOnly() {
        // Arrange.
        val firstError =
            LowLightBoostException(LowLightBoostException.ERROR_SESSION_FAILED, "Error")
        val secondError =
            LowLightBoostException(LowLightBoostException.ERROR_SESSION_FAILED, "Error")
        commit(LowLightBoostStateSnapshot.error(firstError))
        val listener = RecordingListener(effect)
        effect.addListener(listener)

        // Act.
        commit(LowLightBoostStateSnapshot.error(secondError))
        commit(LowLightBoostStateSnapshot.error(secondError))

        // Assert: a new failure is reported even though the status stays STATUS_OFF.
        assertThat(listener.events)
            .containsExactly(Event.error(secondError, STATUS_OFF, secondError))
    }

    @Test
    fun addListener_rejectedExecution_doesNotStopOtherListenersOrObserve() {
        // Arrange: a rejecting executor registered before a normal listener and an observe caller.
        val rejectingExecutor = Executor { throw RejectedExecutionException("Shut down") }
        val rejectedListenerCalls = AtomicInteger(0)
        val rejectedListener =
            object : LowLightBoostEffect.Listener {
                override fun onStatusChanged(status: Int) {
                    rejectedListenerCalls.incrementAndGet()
                }
            }
        val survivingListener = RecordingListener(effect)
        val observed = mutableListOf<Int>()
        effect.addListener(rejectingExecutor, rejectedListener)
        effect.addListener(survivingListener)
        launchOnMain { effect.observe { observed.add(status) } }

        // Act.
        commit(LowLightBoostStateSnapshot.ACTIVE)

        // Assert.
        assertThat(rejectedListenerCalls.get()).isEqualTo(0)
        assertThat(survivingListener.events)
            .containsExactly(Event.status(STATUS_ACTIVE, STATUS_ACTIVE, null))
        assertThat(observed).containsExactly(STATUS_OFF, STATUS_ACTIVE).inOrder()
    }

    @Test
    fun observe_emitsInitialStateAndOncePerTransition() {
        // Arrange.
        commit(LowLightBoostStateSnapshot.INACTIVE)
        val observed = mutableListOf<LowLightBoostStateSnapshot>()
        val failure =
            LowLightBoostException(LowLightBoostException.ERROR_SESSION_FAILED, "Session died")

        // Act: (INACTIVE, null) -> (ACTIVE, null) -> (OFF, failure) -> (INACTIVE, null).
        launchOnMain { effect.observe { observed.add(LowLightBoostStateSnapshot(status, error)) } }
        commit(LowLightBoostStateSnapshot.ACTIVE)
        commit(LowLightBoostStateSnapshot.error(failure))
        commit(LowLightBoostStateSnapshot.INACTIVE)

        // Assert: onChanged is called once per transition, even when both the status and the
        // error change.
        assertThat(observed)
            .containsExactly(
                LowLightBoostStateSnapshot.INACTIVE,
                LowLightBoostStateSnapshot.ACTIVE,
                LowLightBoostStateSnapshot.error(failure),
                LowLightBoostStateSnapshot.INACTIVE,
            )
            .inOrder()
    }

    @Test
    fun isLowLightBoostEnabled_sameValue_isNoOp() {
        val listener = RecordingListener(effect)
        val observed = mutableListOf<Boolean>()
        effect.addListener(listener)
        launchOnMain { effect.observe { observed.add(isLowLightBoostEnabled) } }
        observed.clear()

        // Already true.
        effect.isLowLightBoostEnabled = true
        shadowOf(getMainLooper()).idle()
        assertThat(listener.events).isEmpty()
        assertThat(observed).isEmpty()

        // Transition true -> false -> false (second false is a no-op).
        effect.isLowLightBoostEnabled = false
        effect.isLowLightBoostEnabled = false
        shadowOf(getMainLooper()).idle()
        assertThat(listener.events)
            .containsExactly(
                Event.enabled(
                    enabled = false,
                    effectEnabled = false,
                    effectStatus = STATUS_OFF,
                    effectError = null,
                )
            )
        assertThat(observed).containsExactly(false)
    }

    @Test
    fun isLowLightBoostEnabled_setFalseThenTrueSynchronously_deliversBothInOrder() {
        val mainListener = RecordingListener(effect)
        val executor = QueueExecutor()
        val executorListener = RecordingListener(effect)
        val observed = mutableListOf<Boolean>()
        effect.addListener(mainListener)
        effect.addListener(executor, executorListener)
        launchOnMain { effect.observe { observed.add(isLowLightBoostEnabled) } }

        // Back-to-back in the same frame on main thread.
        effect.isLowLightBoostEnabled = false
        effect.isLowLightBoostEnabled = true
        assertThat(effect.isLowLightBoostEnabled).isTrue()
        shadowOf(getMainLooper()).idle()
        executor.runAll()

        assertThat(mainListener.events)
            .containsExactly(
                Event.enabled(
                    enabled = false,
                    effectEnabled = true,
                    effectStatus = STATUS_OFF,
                    effectError = null,
                ),
                Event.enabled(
                    enabled = true,
                    effectEnabled = true,
                    effectStatus = STATUS_OFF,
                    effectError = null,
                ),
            )
            .inOrder()
        assertThat(mainListener.threads).containsExactly(getMainLooper().thread)
        assertThat(executorListener.events.map { it.value }).containsExactly(false, true).inOrder()
        // Initial (true) + one wakeup per enabled transition.
        assertThat(observed).containsExactly(true, true, true)
    }

    @Test
    fun isLowLightBoostEnabled_setFalseThenTrueFromBackgroundThread_notifiesOnMainInOrder() {
        val listener = RecordingListener(effect)
        val observed = mutableListOf<Boolean>()
        effect.addListener(listener)
        launchOnMain { effect.observe { observed.add(isLowLightBoostEnabled) } }

        var readImmediatelyAfterWrite: Boolean? = null
        runOnBackgroundThread {
            effect.isLowLightBoostEnabled = false
            effect.isLowLightBoostEnabled = true
            readImmediatelyAfterWrite = effect.isLowLightBoostEnabled
        }
        assertThat(readImmediatelyAfterWrite).isTrue()

        shadowOf(getMainLooper()).idle()
        assertThat(listener.events.map { it.value }).containsExactly(false, true).inOrder()
        assertThat(listener.threads).containsExactly(getMainLooper().thread)
        assertThat(observed).hasSize(3)
    }

    @Test
    fun addListener_afterEnabledChanged_doesNotEmitPreRegistrationChange() {
        val listener = RecordingListener(effect)

        effect.isLowLightBoostEnabled = false
        effect.addListener(listener)
        shadowOf(getMainLooper()).idle()

        assertThat(effect.isLowLightBoostEnabled).isFalse()
        assertThat(listener.events).isEmpty()
    }

    @Test
    fun removeListener_suppressesQueuedEnabledCallbacks() {
        val executor = QueueExecutor()
        val listener = RecordingListener(effect)
        effect.addListener(executor, listener)

        effect.isLowLightBoostEnabled = false
        shadowOf(getMainLooper()).idle()
        effect.removeListener(listener)
        executor.runAll()

        assertThat(listener.events).isEmpty()
    }

    @Test
    fun isLowLightBoostEnabled_toggledReentrantlyInsideCallback_deliversInOrder() {
        val events = mutableListOf<String>()
        val listener =
            object : LowLightBoostEffect.Listener {
                override fun onErrorChanged(error: LowLightBoostException?) {
                    events.add("error:${error?.message}")
                    if (error != null) {
                        effect.isLowLightBoostEnabled = false
                    }
                }

                override fun onLowLightBoostEnabledChanged(enabled: Boolean) {
                    events.add("enabled:$enabled")
                    if (!enabled) {
                        effect.isLowLightBoostEnabled = true
                    }
                }
            }
        effect.addListener(listener)

        commit(
            LowLightBoostStateSnapshot.error(
                LowLightBoostException(LowLightBoostException.ERROR_SESSION_FAILED, "boom")
            )
        )
        shadowOf(getMainLooper()).idle()

        assertThat(events)
            .containsExactly("error:boom", "enabled:false", "error:null", "enabled:true")
            .inOrder()
    }

    @Test
    fun onStatusChanged_precedesOnErrorChangedInSingleTask() {
        val callOrder = mutableListOf<String>()
        effect.addListener(
            object : LowLightBoostEffect.Listener {
                override fun onStatusChanged(status: Int) {
                    callOrder.add("status:$status:err=${effect.error?.message}")
                }

                override fun onErrorChanged(error: LowLightBoostException?) {
                    callOrder.add("error:${error?.message}:status=${effect.status}")
                }
            }
        )

        commit(LowLightBoostStateSnapshot.ACTIVE)
        commit(
            LowLightBoostStateSnapshot.error(
                LowLightBoostException(LowLightBoostException.ERROR_SESSION_FAILED, "boom")
            )
        )
        commit(LowLightBoostStateSnapshot.INACTIVE)

        assertThat(callOrder)
            .containsExactly(
                "status:$STATUS_ACTIVE:err=null",
                "status:$STATUS_OFF:err=boom",
                "error:boom:status=$STATUS_OFF",
                "status:$STATUS_INACTIVE:err=null",
                "error:null:status=$STATUS_INACTIVE",
            )
            .inOrder()
    }

    @Test
    fun observe_errorOnlyTransitions_callsOnChangedOncePerChange() {
        // Arrange.
        val firstError =
            LowLightBoostException(LowLightBoostException.ERROR_SESSION_FAILED, "Error")
        val secondError =
            LowLightBoostException(LowLightBoostException.ERROR_SESSION_FAILED, "Error")
        val observed = mutableListOf<LowLightBoostStateSnapshot>()

        // Act: (OFF, null) -> (OFF, firstError) -> (OFF, secondError) -> (OFF, null).
        launchOnMain { effect.observe { observed.add(LowLightBoostStateSnapshot(status, error)) } }
        commit(LowLightBoostStateSnapshot.error(firstError))
        commit(LowLightBoostStateSnapshot.error(secondError))
        commit(LowLightBoostStateSnapshot.OFF)

        // Assert.
        assertThat(observed)
            .containsExactly(
                LowLightBoostStateSnapshot.OFF,
                LowLightBoostStateSnapshot.error(firstError),
                LowLightBoostStateSnapshot.error(secondError),
                LowLightBoostStateSnapshot.OFF,
            )
            .inOrder()
    }

    @Test
    fun observe_fromBackgroundDispatcher_runsOnMainThread() {
        // Arrange: a background dispatcher whose single thread is idle once a task completes.
        val backgroundExecutor = Executors.newSingleThreadExecutor()
        val backgroundDispatcher = backgroundExecutor.asCoroutineDispatcher()
        val threads = Collections.synchronizedList(mutableListOf<Thread>())
        val scope = CoroutineScope(SupervisorJob() + backgroundDispatcher)

        try {
            // Act.
            scope.launch { effect.observe { threads.add(Thread.currentThread()) } }
            // Wait until the coroutine moved to the main thread, then run it there.
            backgroundExecutor.submit {}.get(5, TimeUnit.SECONDS)
            shadowOf(getMainLooper()).idle()
            commit(LowLightBoostStateSnapshot.ACTIVE)

            // Assert: the initial call and the transition both run on the main thread.
            assertThat(threads).containsExactly(getMainLooper().thread, getMainLooper().thread)
        } finally {
            scope.cancel()
            shadowOf(getMainLooper()).idle()
            backgroundDispatcher.close()
        }
    }

    @Test
    fun observe_cancelled_unregistersAndStopsCallingOnChanged() {
        // Arrange.
        val observed = mutableListOf<Int>()
        val job = launchOnMain { effect.observe { observed.add(status) } }
        assertThat(processor.transitionObserverCount).isEqualTo(1)

        // Act: cancel from a background thread before the main looper runs the cancellation task.
        runOnBackgroundThread { job.cancel() }

        // Assert: observer is removed immediately without waiting for main-looper dispatch.
        assertThat(processor.transitionObserverCount).isEqualTo(0)
        shadowOf(getMainLooper()).idle()
        commit(LowLightBoostStateSnapshot.ACTIVE)
        assertThat(job.isCancelled).isTrue()
        assertThat(observed).containsExactly(STATUS_OFF)
    }

    @Test
    fun observe_cancelledByInitialOnChanged_unregisters() {
        // Arrange.
        val observed = mutableListOf<Int>()
        lateinit var job: Job

        // Act: onChanged cancels the observing coroutine in its initial call.
        job =
            mainScope.launch(start = CoroutineStart.LAZY) {
                effect.observe {
                    observed.add(status)
                    job.cancel()
                }
            }
        job.start()
        shadowOf(getMainLooper()).idle()
        commit(LowLightBoostStateSnapshot.ACTIVE)

        // Assert.
        assertThat(job.isCancelled).isTrue()
        assertThat(processor.transitionObserverCount).isEqualTo(0)
        assertThat(observed).containsExactly(STATUS_OFF)
    }

    @Test
    fun observe_onChangedThrowsInInitialCall_propagatesAndUnregisters() {
        // Arrange.
        val failure = IllegalStateException("Initial failure")
        var thrown: Throwable? = null

        // Act.
        launchOnMain {
            try {
                effect.observe { throw failure }
            } catch (t: Throwable) {
                thrown = t
            }
        }

        // Assert.
        // Coroutines may recover the stack trace with a copy of the exception.
        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(thrown).hasMessageThat().isEqualTo(failure.message)
        assertThat(processor.transitionObserverCount).isEqualTo(0)
    }

    @Test
    fun observe_onChangedThrowsOnTransition_propagatesAndUnregisters() {
        // Arrange.
        val failure = IllegalStateException("Transition failure")
        val observed = mutableListOf<Int>()
        var thrown: Throwable? = null
        launchOnMain {
            try {
                effect.observe {
                    observed.add(status)
                    if (status == STATUS_ACTIVE) {
                        throw failure
                    }
                }
            } catch (t: Throwable) {
                thrown = t
            }
        }

        // Act.
        commit(LowLightBoostStateSnapshot.ACTIVE)
        commit(LowLightBoostStateSnapshot.INACTIVE)

        // Assert.
        // Coroutines may recover the stack trace with a copy of the exception.
        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(thrown).hasMessageThat().isEqualTo(failure.message)
        assertThat(processor.transitionObserverCount).isEqualTo(0)
        assertThat(observed).containsExactly(STATUS_OFF, STATUS_ACTIVE).inOrder()
    }

    /** Posts [snapshot] like the GL thread does and commits it on the main thread. */
    private fun commit(snapshot: LowLightBoostStateSnapshot) {
        processor.postState(snapshot)
        shadowOf(getMainLooper()).idle()
    }

    /** Starts [block] on the main thread right away and runs the main thread until it is idle. */
    private fun launchOnMain(block: suspend CoroutineScope.() -> Unit): Job =
        mainScope.launch(start = CoroutineStart.UNDISPATCHED, block = block).also {
            shadowOf(getMainLooper()).idle()
        }

    private fun runOnBackgroundThread(block: () -> Unit) {
        val thread = Thread(block)
        thread.start()
        thread.join(TimeUnit.SECONDS.toMillis(5))
        assertThat(thread.isAlive).isFalse()
    }

    /** A listener callback with the effect's state that the callback read. */
    private data class Event(
        val callback: String,
        val value: Any?,
        val effectEnabled: Boolean,
        val effectStatus: Int,
        val effectError: LowLightBoostException?,
    ) {
        companion object {
            fun enabled(
                enabled: Boolean,
                effectEnabled: Boolean,
                effectStatus: Int,
                effectError: LowLightBoostException?,
            ) =
                Event(
                    "onLowLightBoostEnabledChanged",
                    enabled,
                    effectEnabled,
                    effectStatus,
                    effectError,
                )

            fun status(
                status: Int,
                effectStatus: Int,
                effectError: LowLightBoostException?,
                effectEnabled: Boolean = true,
            ) = Event("onStatusChanged", status, effectEnabled, effectStatus, effectError)

            fun error(
                error: LowLightBoostException?,
                effectStatus: Int,
                effectError: LowLightBoostException?,
                effectEnabled: Boolean = true,
            ) = Event("onErrorChanged", error, effectEnabled, effectStatus, effectError)
        }
    }

    /** Records each callback with the effect's state read in the callback, and its thread. */
    private class RecordingListener(private val effect: LowLightBoostEffect) :
        LowLightBoostEffect.Listener {
        val events: MutableList<Event> = Collections.synchronizedList(mutableListOf())
        val threads: MutableSet<Thread> = Collections.synchronizedSet(mutableSetOf())

        override fun onLowLightBoostEnabledChanged(enabled: Boolean) {
            threads.add(Thread.currentThread())
            events.add(
                Event.enabled(enabled, effect.isLowLightBoostEnabled, effect.status, effect.error)
            )
        }

        override fun onStatusChanged(status: Int) {
            threads.add(Thread.currentThread())
            events.add(Event.status(status, effect.status, effect.error))
        }

        override fun onErrorChanged(error: LowLightBoostException?) {
            threads.add(Thread.currentThread())
            events.add(Event.error(error, effect.status, effect.error))
        }
    }

    /** An [Executor] that queues tasks until [runAll] is called. */
    private class QueueExecutor : Executor {
        private val tasks = ArrayDeque<Runnable>()

        val pendingCount: Int
            get() = tasks.size

        override fun execute(command: Runnable) {
            tasks.addLast(command)
        }

        fun runAll() {
            while (tasks.isNotEmpty()) {
                tasks.removeFirst().run()
            }
        }
    }
}
