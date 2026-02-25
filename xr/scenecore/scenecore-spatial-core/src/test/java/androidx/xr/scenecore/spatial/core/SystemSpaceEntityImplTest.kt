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

import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Matrix4.Companion.fromPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Quaternion.Companion.fromAxisAngle
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.runtime.testing.math.assertVector3
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.node.FakeCloseable
import com.android.extensions.xr.node.Mat4f
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.NodeTransform
import com.android.extensions.xr.node.ShadowNode
import com.android.extensions.xr.node.ShadowNodeTransform
import com.google.common.truth.Truth
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

/**
 * Abstract test class for [SystemSpaceEntityImpl] implementations.
 *
 * Concrete implementations of [SystemSpaceEntityImpl] should extend this class and provide
 * implementations for its abstract methods to ensure they comply with the abstract class.
 */
abstract class SystemSpaceEntityImplTest {
    /** Returns the [SystemSpaceEntityImpl] instance to test. */
    protected abstract val systemSpaceEntityImpl: SystemSpaceEntityImpl

    /** Returns the default fake executor used by the [SystemSpaceEntityImpl] constructor. */
    protected abstract val defaultFakeExecutor: FakeScheduledExecutorService

    /** Returns an arbitrary [AndroidXrEntity] instance which can set its parent. */
    protected abstract fun createChildAndroidXrEntity(): AndroidXrEntity

    /** Returns the [ActivitySpaceImpl] instance which is the root of the Activity Space. */
    protected abstract val activitySpaceEntity: ActivitySpaceImpl

    @Test
    fun systemSpaceEntityImplConstructor_setsNodeTransformSubscription() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val fakeExecutor = this.defaultFakeExecutor
        val node = ShadowNode.extract(systemSpaceEntity.getNode())
        Truth.assertThat(node.transformListener).isNotNull()
        Truth.assertThat(node.transformExecutor).isEqualTo(fakeExecutor)
        Truth.assertThat(systemSpaceEntity.nodeTransformCloseable).isNotNull()
    }

    @Test
    fun dispose_closesNodeTransformSubscription() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val nodeTransformCloseable = systemSpaceEntity.nodeTransformCloseable as FakeCloseable
        Truth.assertThat(nodeTransformCloseable.isClosed).isFalse()

        systemSpaceEntity.dispose()
        Truth.assertThat(nodeTransformCloseable.isClosed).isTrue()
    }

    @Test
    fun getPoseInOpenXrReferenceSpace_defaultsToNull() {
        Truth.assertThat(this.systemSpaceEntityImpl.poseInOpenXrReferenceSpace).isNull()
    }

    @Test
    fun setOnOriginChangedListener_callListenersOnActivitySpace() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val listener1 = mock<Runnable>()
        val executor1 = FakeScheduledExecutorService()

        systemSpaceEntity.setOnOriginChangedListener(listener1, executor1)
        systemSpaceEntity.onOriginChanged()
        Truth.assertThat(executor1.hasNext()).isTrue()
        executor1.runAll()

        verify(listener1).run()
    }

    @Test
    fun setOnOriginChangedListener_multipleListeners_callLastListenersOnActivitySpace() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val listener1 = mock<Runnable>()
        val listener2 = mock<Runnable>()
        val executor1 = FakeScheduledExecutorService()
        val executor2 = FakeScheduledExecutorService()

        systemSpaceEntity.setOnOriginChangedListener(listener1, executor1)
        // This should override the previous listener.
        systemSpaceEntity.setOnOriginChangedListener(listener2, executor2)
        systemSpaceEntity.onOriginChanged()

        Truth.assertThat(executor1.hasNext()).isFalse()
        Truth.assertThat(executor2.hasNext()).isTrue()

        executor1.runAll()
        executor2.runAll()

        verify(listener1, never()).run()
        verify(listener2).run()
    }

    @Test
    fun setOnOriginChangedListener_withNullExecutor_usesInternalExecutor() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val fakeExecutor = this.defaultFakeExecutor
        val listener = mock<Runnable>()

        systemSpaceEntity.setOnOriginChangedListener(listener, null)
        systemSpaceEntity.onOriginChanged()

        Truth.assertThat(fakeExecutor.hasNext()).isTrue()
        fakeExecutor.runAll()
        verify(listener).run()
    }

    @Test
    fun setOnOriginChangedListener_withNullListener_noListenerCallOnActivitySpace() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val listener = mock<Runnable>()
        val executor = FakeScheduledExecutorService()
        systemSpaceEntity.setOnOriginChangedListener(listener, executor)
        systemSpaceEntity.setOnOriginChangedListener(null, executor)

        systemSpaceEntity.onOriginChanged()
        executor.runAll()

        verify(listener, never()).run()
    }

    @Test
    fun getPoseInOpenXrReferenceSpace_returnsPoseFromSubscribeToNodeTransform() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        // Column major, right-handed 4x4 Transformation Matrix with translation of (4, 8, 12) and
        // rotation 90 (@) around Z axis
        val mat4f =
            Mat4f(
                floatArrayOf(
                    0f,
                    1f,
                    0f,
                    0f, // --     cos(@),   sin(@), 0,  0
                    -1f,
                    0f,
                    0f,
                    0f, // --    -sin(@),  cos(@), 0,  0
                    0f,
                    0f,
                    1f,
                    0f, // --     0,        0,      1,  0
                    4f,
                    8f,
                    12f,
                    1f, // --    tx,       ty,     tz, 1
                )
            )
        val nodeTransformEvent = ShadowNodeTransform.create(mat4f)

        sendTransformEvent(systemSpaceEntity.getNode(), nodeTransformEvent)
        this.defaultFakeExecutor.runAll()

        val expectedPose = Pose(Vector3(4f, 8f, 12f), fromAxisAngle(Vector3(0f, 0f, 1f), 90f))

        assertPose(systemSpaceEntity.poseInOpenXrReferenceSpace!!, expectedPose)
    }

    private fun sendTransformEvent(node: Node?, nodeTransform: NodeTransform?) {
        val shadowNode = ShadowNode.extract(node)
        shadowNode.transformExecutor.execute { shadowNode.transformListener.accept(nodeTransform) }
    }

    @Test
    fun setOnOriginChangedListener_callsListenerOnNodeTransformEvent() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val mat4f = Mat4f(Matrix4.Identity.data)
        val nodeTransformEvent = ShadowNodeTransform.create(mat4f)

        val listener = mock<Runnable>()
        val executor = FakeScheduledExecutorService()
        systemSpaceEntity.setOnOriginChangedListener(listener, executor)

        sendTransformEvent(systemSpaceEntity.getNode(), nodeTransformEvent)
        this.defaultFakeExecutor.runAll()

        Truth.assertThat(executor.hasNext()).isTrue()
        executor.runAll()

        verify(listener).run()
    }

    @Test
    fun setOnOriginChangedListener_multipleListeners_callsLastListenerOnNodeTransformEvent() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val fakeExecutor = this.defaultFakeExecutor
        val mat4f = Mat4f(Matrix4.Identity.data)
        val nodeTransformEvent = ShadowNodeTransform.create(mat4f)

        val listener = mock<Runnable>()
        val listener2 = mock<Runnable>()
        val executor = FakeScheduledExecutorService()
        systemSpaceEntity.setOnOriginChangedListener(listener, executor)
        systemSpaceEntity.setOnOriginChangedListener(listener2, executor)

        sendTransformEvent(systemSpaceEntity.getNode(), nodeTransformEvent)
        fakeExecutor.runAll()
        Truth.assertThat(executor.hasNext()).isTrue()
        executor.runAll()

        verify(listener, never()).run()
        verify(listener2).run()
    }

    @Test
    fun setOnOriginChangedListener_nullExecutor_callsListenerOnNodeTransformEventExecutor() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val mat4f = Mat4f(Matrix4.Identity.data)
        val nodeTransformEvent = ShadowNodeTransform.create(mat4f)

        val listener = mock<Runnable>()
        systemSpaceEntity.setOnOriginChangedListener(listener, null)

        sendTransformEvent(systemSpaceEntity.getNode(), nodeTransformEvent)
        this.defaultFakeExecutor.runAll()

        verify(listener).run()
    }

    @Test
    fun setOnOriginChangedListener_withNullListener_noListenerCalledOnNodeTransformEvent() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val mat4f = Mat4f(Matrix4.Identity.data)
        val nodeTransformEvent = ShadowNodeTransform.create(mat4f)

        val listener = mock<Runnable>()
        systemSpaceEntity.setOnOriginChangedListener(listener, null)
        systemSpaceEntity.setOnOriginChangedListener(null, null)

        sendTransformEvent(systemSpaceEntity.getNode(), nodeTransformEvent)
        this.defaultFakeExecutor.runAll()

        verify(listener, never()).run()
    }

    @Test
    open fun zeroTransform_doesNotUpdatePoseOrScaleOrCallOnOriginChanged() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val listener = mock<Runnable>()
        val executor = FakeScheduledExecutorService()
        val expectedPose = Pose(Vector3.One, Quaternion.Identity)
        val expectedScale = Vector3(4f, 5f, 6f)

        systemSpaceEntity.openXrReferenceSpaceTransform.set(fromPose(expectedPose))
        systemSpaceEntity._worldSpaceScale = expectedScale
        systemSpaceEntity.setOnOriginChangedListener(listener, executor)
        systemSpaceEntity.setOpenXrReferenceSpaceTransform(Matrix4.Zero)
        executor.runAll()

        Truth.assertThat(systemSpaceEntity.poseInOpenXrReferenceSpace).isEqualTo(expectedPose)
        Truth.assertThat(systemSpaceEntity.worldSpaceScale).isEqualTo(expectedScale)
        verify(listener, never()).run()
    }

    @Test
    fun setPoseInOpenXrReferenceSpace_callsOnOriginChanged() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        val listener = mock<Runnable>()
        val executor = FakeScheduledExecutorService()

        systemSpaceEntity.setOnOriginChangedListener(listener, executor)
        systemSpaceEntity.setOpenXrReferenceSpaceTransform(Matrix4.Identity)
        executor.runAll()

        verify(listener).run()
    }

    @Test
    fun setPoseInOpenXrReferenceSpace_updatesPose() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        // Column major, right-handed 4x4 Transformation Matrix with translation of (4, 8, 12)
        // and rotation 90 (@) around Z axis
        val matrix =
            Matrix4(
                floatArrayOf(
                    0f,
                    1f,
                    0f,
                    0f, // --     cos(@),   sin(@), 0,  0
                    -1f,
                    0f,
                    0f,
                    0f, // --    -sin(@),  cos(@), 0,  0
                    0f,
                    0f,
                    1f,
                    0f, // --     0,        0,      1,  0
                    4f,
                    8f,
                    12f,
                    1f, // --    tx,       ty,     tz, 1
                )
            )
        val pose = Pose(Vector3(4f, 8f, 12f), fromAxisAngle(Vector3(0f, 0f, 1f), 90f))

        systemSpaceEntity.setOpenXrReferenceSpaceTransform(matrix)
        assertPose(systemSpaceEntity.poseInOpenXrReferenceSpace!!, pose)
    }

    @Test
    open fun setPoseInOpenXrReferenceSpace_updatesScale() {
        val systemSpaceEntity = this.systemSpaceEntityImpl
        // Column major, right-handed 4x4 Transformation Matrix with translation of (4, 8, 12) and
        // rotation 90 (@) around Z axis, and scale of 3.3.
        val matrix =
            Matrix4(
                floatArrayOf(
                    0f,
                    3.3f,
                    0f,
                    0f, // --     cos(@),   sin(@), 0,  0
                    -3.3f,
                    0f,
                    0f,
                    0f, // --    -sin(@),  cos(@), 0,  0
                    0f,
                    0f,
                    3.3f,
                    0f, // --     0,        0,      1,  0
                    4f,
                    8f,
                    12f,
                    1f, // --      tx,       ty,     tz, 1
                )
            )
        val scale = Vector3(3.3f, 3.3f, 3.3f)

        systemSpaceEntity.setOpenXrReferenceSpaceTransform(matrix)
        // Updated to expect scale, so AnchorEntityImpl passes. ActivitySpaceImpl overrides this.
        assertVector3(systemSpaceEntity.activitySpaceScale, scale)
        assertVector3(systemSpaceEntity.worldSpaceScale, scale)
        assertVector3(systemSpaceEntity.getScale(Space.ACTIVITY), scale)
    }
}
