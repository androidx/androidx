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

package androidx.camera.camera2.pipe.internal

import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.StreamId
import com.google.common.truth.Truth.assertThat
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class ListenerStateTest {
    private val fakeListener =
        object : Frame.Listener {
            val frameStartedCalled = atomic(0)
            val frameInfoAvailableCalled = atomic(0)
            val imageAvailableCalled = atomic(0)
            val frameCompletedCalled = atomic(0)

            override fun onFrameStarted(frameNumber: FrameNumber, frameTimestamp: CameraTimestamp) {
                frameStartedCalled.incrementAndGet()
            }

            override fun onFrameInfoAvailable() {
                frameInfoAvailableCalled.incrementAndGet()
            }

            override fun onImageAvailable(streamId: StreamId) {
                // Do nothing. ListenerState doesn't care about onImageAvailable on stream level
                // currently.
            }

            override fun onImagesAvailable() {
                imageAvailableCalled.incrementAndGet()
            }

            override fun onFrameComplete() {
                frameCompletedCalled.incrementAndGet()
            }
        }

    private val testFrameNumber = FrameNumber(100)
    private val testTimestamp = CameraTimestamp(123456789)

    private lateinit var listenerState: ListenerState

    @Before
    fun setUp() {
        listenerState = ListenerState(fakeListener)
    }

    @Test
    fun invokeOnStarted_callMultipleTimes_callsOnFrameStartedOnce() {
        listenerState.invokeOnStarted(testFrameNumber, testTimestamp)
        listenerState.invokeOnStarted(testFrameNumber, testTimestamp)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
    }

    @Test
    fun invokeOnImagesAvailable_callsOnImagesAvailableAndOnStarted() {
        listenerState.invokeOnImagesAvailable(testFrameNumber, testTimestamp)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.imageAvailableCalled.value).isEqualTo(1)
    }

    @Test
    fun invokeOnImagesAvailable_onStartedAlreadyCalled_shouldNotTriggerOnStartedAgain() {
        listenerState.invokeOnStarted(testFrameNumber, testTimestamp)
        listenerState.invokeOnImagesAvailable(testFrameNumber, testTimestamp)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
    }

    @Test
    fun invokeOnFrameInfoAvailable_callsOnFrameInfoAvailableAndOnStarted() {
        listenerState.invokeOnFrameInfoAvailable(testFrameNumber, testTimestamp)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.imageAvailableCalled.value).isEqualTo(0)
    }

    @Test
    fun invokeOnFrameInfoAvailable_onStartedAlreadyCalled_doesNotInvokeOnStartedAgain() {
        listenerState.invokeOnStarted(testFrameNumber, testTimestamp)
        listenerState.invokeOnFrameInfoAvailable(testFrameNumber, testTimestamp)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
    }

    @Test
    fun invokeOnFrameComplete_callsAllPrecedingCallbacks() {
        listenerState.invokeOnFrameComplete(testFrameNumber, testTimestamp)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.imageAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameCompletedCalled.value).isEqualTo(1)
    }

    @Test
    fun invokeOnFrameComplete_onImagesAlreadyCalled_allCallbacksGetCalledOnce() {
        listenerState.invokeOnImagesAvailable(testFrameNumber, testTimestamp)
        listenerState.invokeOnFrameComplete(testFrameNumber, testTimestamp)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.imageAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameCompletedCalled.value).isEqualTo(1)
    }

    @Test
    fun invokeOnFrameComplete_onFrameInfoAlreadyCalled_allCallbacksGetCalledOnce() {
        listenerState.invokeOnFrameInfoAvailable(testFrameNumber, testTimestamp)
        listenerState.invokeOnFrameComplete(testFrameNumber, testTimestamp)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.imageAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameCompletedCalled.value).isEqualTo(1)
    }

    @Test
    fun concurrentInvokes_ensureCallbacksCalledOnce() = runBlocking {
        val numCoroutines = 100

        val jobs =
            List(numCoroutines) {
                launch(Dispatchers.Default) {
                    when (it % 4) {
                        0 -> listenerState.invokeOnStarted(testFrameNumber, testTimestamp)
                        1 -> listenerState.invokeOnImagesAvailable(testFrameNumber, testTimestamp)
                        2 ->
                            listenerState.invokeOnFrameInfoAvailable(testFrameNumber, testTimestamp)
                        3 -> listenerState.invokeOnFrameComplete(testFrameNumber, testTimestamp)
                    }
                }
            }
        jobs.joinAll()

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.imageAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameCompletedCalled.value).isEqualTo(1)
    }
}
