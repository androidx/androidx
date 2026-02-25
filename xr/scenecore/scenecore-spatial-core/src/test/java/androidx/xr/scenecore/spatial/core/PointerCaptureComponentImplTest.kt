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
package androidx.xr.scenecore.spatial.core

import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.PointerCaptureComponent
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.node.InputEvent
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.ShadowInputEvent
import com.android.extensions.xr.node.ShadowNode
import com.android.extensions.xr.node.Vec3
import com.google.common.truth.Truth
import com.google.common.util.concurrent.MoreExecutors
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PointerCaptureComponentImplTest {
    private val stateListener = FakeStateListener()
    private val inputListener = FakeInputEventListener()
    private val xrExtensions = getXrExtensions()
    private val fakeScheduler = FakeScheduledExecutorService()
    private val node: Node = xrExtensions!!.createNode()
    private val shadowNode: ShadowNode = ShadowNode.extract(node)
    private val entity: Entity =
        object : AndroidXrEntity(null, node, xrExtensions!!, EntityManager(), fakeScheduler) {}

    private fun sendInputEvent(inputEvent: InputEvent?) {
        shadowNode.inputExecutor.execute { shadowNode.inputListener.accept(inputEvent) }
    }

    @Test
    fun onAttach_enablesPointerCapture() {
        val component =
            PointerCaptureComponentImpl(
                MoreExecutors.directExecutor(),
                stateListener,
                inputListener,
            )

        Truth.assertThat(component.onAttach(entity)).isTrue()

        Truth.assertThat(shadowNode.pointerCaptureStateCallback).isNotNull()
    }

    @Test
    fun onAttach_setsUpInputEventPropagation() {
        val component =
            PointerCaptureComponentImpl(
                MoreExecutors.directExecutor(),
                stateListener,
                inputListener,
            )
        Truth.assertThat(component.onAttach(entity)).isTrue()

        val fakeInput =
            ShadowInputEvent.create(
                InputEvent.SOURCE_UNKNOWN,
                InputEvent.POINTER_TYPE_DEFAULT,
                /* timestamp= */ 0,
                /* origin= */ Vec3(0f, 0f, 0f),
                /* direction= */ Vec3(1f, 1f, 1f),
                InputEvent.DISPATCH_FLAG_CAPTURED_POINTER,
                InputEvent.ACTION_MOVE,
            )
        sendInputEvent(fakeInput)
        fakeScheduler.runAll()

        Truth.assertThat(inputListener.lastEvent).isNotNull()
    }

    // This should really be a test on AndroidXrEntity, but that does not have tests so it is here
    // for
    // the meantime.
    @Test
    fun onAttach_onlyPropagatesCapturedEvents() {
        val component =
            PointerCaptureComponentImpl(
                MoreExecutors.directExecutor(),
                stateListener,
                inputListener,
            )
        Truth.assertThat(component.onAttach(entity)).isTrue()

        val fakeCapturedInput =
            ShadowInputEvent.create(
                InputEvent.SOURCE_UNKNOWN,
                InputEvent.POINTER_TYPE_DEFAULT,
                /* timestamp= */ 100,
                /* origin= */ Vec3(0f, 0f, 0f),
                /* direction= */ Vec3(1f, 1f, 1f),
                InputEvent.DISPATCH_FLAG_CAPTURED_POINTER,
                InputEvent.ACTION_MOVE,
            )

        val fakeInput =
            ShadowInputEvent.create(
                InputEvent.SOURCE_UNKNOWN,
                InputEvent.POINTER_TYPE_DEFAULT,
                /* timestamp= */ 200,
                /* origin= */ Vec3(0f, 0f, 0f),
                /* direction= */ Vec3(1f, 1f, 1f),
                InputEvent.DISPATCH_FLAG_NONE,
                InputEvent.ACTION_MOVE,
            )

        sendInputEvent(fakeCapturedInput)
        sendInputEvent(fakeInput)

        fakeScheduler.runAll()

        Truth.assertThat(inputListener.lastEvent).isNotNull()
        Truth.assertThat(inputListener.lastEvent!!.timestamp).isEqualTo(fakeCapturedInput.timestamp)
    }

    @Test
    fun onAttach_propagatesInputOnCorrectThread() {
        val propagationExecutor = FakeScheduledExecutorService()
        val component =
            PointerCaptureComponentImpl(propagationExecutor, stateListener, inputListener)
        Truth.assertThat(component.onAttach(entity)).isTrue()

        val fakeCapturedInput =
            ShadowInputEvent.create(
                InputEvent.SOURCE_UNKNOWN,
                InputEvent.POINTER_TYPE_DEFAULT,
                /* timestamp= */ 100,
                /* origin= */ Vec3(0f, 0f, 0f),
                /* direction= */ Vec3(1f, 1f, 1f),
                InputEvent.DISPATCH_FLAG_CAPTURED_POINTER,
                InputEvent.ACTION_MOVE,
            )

        sendInputEvent(fakeCapturedInput)

        Truth.assertThat(propagationExecutor.hasNext()).isFalse()
        // Run the scheduler associated with the Entity so that the component's executor has the
        // task
        // scheduled on it.
        fakeScheduler.runAll()

        Truth.assertThat(propagationExecutor.hasNext()).isTrue()
    }

    @Test
    fun onAttach_setsUpCorrectStatePropagation() {
        val component =
            PointerCaptureComponentImpl(
                MoreExecutors.directExecutor(),
                stateListener,
                inputListener,
            )
        Truth.assertThat(component.onAttach(entity)).isTrue()

        shadowNode.pointerCaptureStateCallback.accept(Node.POINTER_CAPTURE_STATE_PAUSED)
        Truth.assertThat(stateListener.lastState)
            .isEqualTo(PointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_PAUSED)

        shadowNode.pointerCaptureStateCallback.accept(Node.POINTER_CAPTURE_STATE_ACTIVE)
        Truth.assertThat(stateListener.lastState)
            .isEqualTo(PointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE)

        shadowNode.pointerCaptureStateCallback.accept(Node.POINTER_CAPTURE_STATE_STOPPED)
        Truth.assertThat(stateListener.lastState)
            .isEqualTo(PointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_STOPPED)
    }

    @Test
    fun onAttach_failsIfAlreadyAttached() {
        val component =
            PointerCaptureComponentImpl(
                MoreExecutors.directExecutor(),
                stateListener,
                inputListener,
            )
        Truth.assertThat(component.onAttach(entity)).isTrue()
        Truth.assertThat(component.onAttach(entity)).isFalse()
    }

    @Test
    fun onAttach_failsIfEntityAlreadyHasAnAttachedPointerCaptureComponent() {
        val component =
            PointerCaptureComponentImpl(
                MoreExecutors.directExecutor(),
                stateListener,
                inputListener,
            )
        Truth.assertThat(component.onAttach(entity)).isTrue()

        val component2 =
            PointerCaptureComponentImpl(
                MoreExecutors.directExecutor(),
                stateListener,
                inputListener,
            )
        Truth.assertThat(component2.onAttach(entity)).isFalse()
    }

    @Test
    fun onDetach_stopsPointerCapture() {
        val component =
            PointerCaptureComponentImpl(
                MoreExecutors.directExecutor(),
                stateListener,
                inputListener,
            )
        Truth.assertThat(component.onAttach(entity)).isTrue()

        component.onDetach(entity)

        Truth.assertThat(shadowNode.pointerCaptureStateCallback).isNull()
    }

    @Test
    fun onDetach_removesInputListener() {
        val component =
            PointerCaptureComponentImpl(
                MoreExecutors.directExecutor(),
                stateListener,
                inputListener,
            )
        Truth.assertThat(component.onAttach(entity)).isTrue()

        component.onDetach(entity)

        Truth.assertThat(shadowNode.inputListener).isNull()
    }

    // Static private implementation of fakes so that the last received state can be grabbed.
    private class FakeStateListener : PointerCaptureComponent.StateListener {
        var lastState: Int = -1

        override fun onStateChanged(newState: Int) {
            lastState = newState
        }
    }

    private class FakeInputEventListener : InputEventListener {
        var lastEvent: androidx.xr.scenecore.runtime.InputEvent? = null

        override fun onInputEvent(event: androidx.xr.scenecore.runtime.InputEvent) {
            lastEvent = event
        }
    }
}
