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

import android.app.Activity
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.InteractableComponent
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.node.InputEvent
import com.android.extensions.xr.node.ShadowInputEvent
import com.android.extensions.xr.node.ShadowNode
import com.android.extensions.xr.node.Vec3
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class InteractableComponentImplTest {
    private val activityController: ActivityController<Activity> =
        Robolectric.buildActivity(Activity::class.java)
    private val activity: Activity = activityController.create().start().get()
    private val fakeExecutor = FakeScheduledExecutorService()
    private val xrExtensions = getXrExtensions()
    private lateinit var fakeRuntime: SpatialSceneRuntime

    @Before
    fun setUp() {
        fakeRuntime =
            SpatialSceneRuntime.create(activity, fakeExecutor, xrExtensions!!, EntityManager())
    }

    @After
    fun tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        fakeRuntime.destroy()
    }

    private fun createTestEntity(): Entity {
        return fakeRuntime.createGroupEntity(Pose(), "test", fakeRuntime.activitySpace)
    }

    private fun sendInputEvent(node: ShadowNode, inputEvent: InputEvent?) {
        node.inputExecutor.execute { node.inputListener.accept(inputEvent) }
    }

    @Test
    fun addInteractableComponent_addsListenerToNode() {
        val entity = createTestEntity()
        val executor = MoreExecutors.directExecutor()
        val mockInputEventListener = mock<InputEventListener>()
        val interactableComponent: InteractableComponent =
            InteractableComponentImpl(executor, mockInputEventListener)

        assertThat(entity.addComponent(interactableComponent)).isTrue()

        val node = ShadowNode.extract((entity as AndroidXrEntity).getNode())

        assertThat(node.inputListener).isNotNull()
        assertThat(node.inputExecutor).isEqualTo(fakeExecutor)

        val inputEvent =
            ShadowInputEvent.create(
                /* origin= */ Vec3(0f, 0f, 0f),
                /* direction= */ Vec3(1f, 1f, 1f),
            )
        sendInputEvent(node, inputEvent)
        fakeExecutor.runAll()

        assertThat(entity.inputEventListenerMap).isNotEmpty()
        verify(mockInputEventListener).onInputEvent(any())
    }

    @Test
    fun removeInteractableComponent_removesListenerFromNode() {
        val entity = createTestEntity()
        val executor = MoreExecutors.directExecutor()
        val mockInputEventListener = mock<InputEventListener>()
        val interactableComponent: InteractableComponent =
            InteractableComponentImpl(executor, mockInputEventListener)

        assertThat(entity.addComponent(interactableComponent)).isTrue()

        val node = ShadowNode.extract((entity as AndroidXrEntity).getNode())

        assertThat(node.inputListener).isNotNull()
        assertThat(node.inputExecutor).isEqualTo(fakeExecutor)

        val inputEvent =
            ShadowInputEvent.create(
                /* origin= */ Vec3(0f, 0f, 0f),
                /* direction= */ Vec3(1f, 1f, 1f),
            )
        sendInputEvent(node, inputEvent)
        fakeExecutor.runAll()

        assertThat(entity.inputEventListenerMap).isNotEmpty()
        verify(mockInputEventListener).onInputEvent(any())

        entity.removeComponent(interactableComponent)

        assertThat(node.inputListener).isNull()
        assertThat(node.inputExecutor).isNull()
    }

    @Test
    fun interactableComponent_canAttachOnlyOnce() {
        val entity = createTestEntity()
        val entity2 = createTestEntity()
        val executor = MoreExecutors.directExecutor()
        val mockInputEventListener = mock<InputEventListener>()
        val interactableComponent: InteractableComponent =
            InteractableComponentImpl(executor, mockInputEventListener)

        assertThat(entity.addComponent(interactableComponent)).isTrue()
        assertThat(entity2.addComponent(interactableComponent)).isFalse()
    }

    @Test
    fun interactableComponent_canAttachAgainAfterDetach() {
        val entity = createTestEntity()
        val executor = MoreExecutors.directExecutor()
        val mockInputEventListener = mock<InputEventListener>()
        val interactableComponent: InteractableComponent =
            InteractableComponentImpl(executor, mockInputEventListener)

        assertThat(entity.addComponent(interactableComponent)).isTrue()

        entity.removeComponent(interactableComponent)

        assertThat(entity.addComponent(interactableComponent)).isTrue()
    }

    @Test
    fun interactableComponent_enablesColliderForGltfEntity() {
        val gltfEntity = mock<GltfEntity>()
        val executor = MoreExecutors.directExecutor()
        val mockInputEventListener = mock<InputEventListener>()
        val interactableComponent: InteractableComponent =
            InteractableComponentImpl(executor, mockInputEventListener)

        assertThat(interactableComponent.onAttach(gltfEntity)).isTrue()
        verify(gltfEntity).setColliderEnabled(true)
    }
}
