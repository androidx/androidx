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

package androidx.xr.runtime.testing

import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.internal.MovableComponent
import androidx.xr.runtime.internal.MoveEvent
import androidx.xr.runtime.internal.MoveEventListener
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FakeMovableComponentTest {
    lateinit var underTest: FakeMovableComponent
    lateinit var fakeExecutor: Executor
    private val tasks = ConcurrentLinkedQueue<Runnable>()

    @Before
    fun setUp() {
        underTest = FakeMovableComponent()
        fakeExecutor = Executor { command -> tasks.add(command) }
    }

    @Test
    fun getDefaultValues_returnsDefaultValues() {
        check(underTest.scaleWithDistanceMode == MovableComponent.ScaleWithDistanceMode.DEFAULT)
        check(underTest.size == Dimensions(2.0f, 1.0f, 0.0f))
    }

    @Test
    fun move_notifiesListener() {
        val listenerCalled = AtomicBoolean(false)
        val mockListener =
            object : MoveEventListener {
                override fun onMoveEvent(event: MoveEvent) {
                    listenerCalled.set(true)
                }
            }

        underTest.addMoveEventListener(fakeExecutor, mockListener)

        // For simplicity in the fake, we'll use some default values for fields
        // not directly provided by this simplified move signature.
        val dummyState = MoveEvent.MoveState.MOVE_STATE_START
        val initialRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val currentRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val previousPose = Pose(Vector3(0f, 0f, 0f), Quaternion(1f, 0f, 0f, 0f))
        val currentPose = Pose(Vector3(0f, 0f, 0f), Quaternion(1f, 0f, 0f, 0f))
        val previousScale = Vector3(1f, 1f, 1f)
        val currentScale = Vector3(1f, 1f, 1f)
        val initialParent = FakeEntity()
        val updatedParent = FakeEntity()
        val disposedEntity = null

        underTest.moveEventListenersMap.forEach { entry ->
            entry.key.onMoveEvent(
                MoveEvent(
                    dummyState,
                    initialRay,
                    currentRay,
                    previousPose,
                    currentPose,
                    previousScale,
                    currentScale,
                    initialParent,
                    updatedParent,
                    disposedEntity,
                )
            )
        }

        tasks.forEach { it.run() }
        assertThat(listenerCalled.get()).isTrue()
    }

    @Test
    fun move_doesNotNotifyWhenListenerRemoved() {
        val listenerCalled = AtomicBoolean(false)
        val mockListener =
            object : MoveEventListener {
                override fun onMoveEvent(event: MoveEvent) {
                    listenerCalled.set(true)
                }
            }

        underTest.addMoveEventListener(fakeExecutor, mockListener)
        underTest.removeMoveEventListener(mockListener)

        // For simplicity in the fake, we'll use some default values for fields
        // not directly provided by this simplified move signature.
        val dummyState = MoveEvent.MoveState.MOVE_STATE_START
        val initialRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val currentRay = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val previousPose = Pose(Vector3(0f, 0f, 0f), Quaternion(1f, 0f, 0f, 0f))
        val currentPose = Pose(Vector3(0f, 0f, 0f), Quaternion(1f, 0f, 0f, 0f))
        val previousScale = Vector3(1f, 1f, 1f)
        val currentScale = Vector3(1f, 1f, 1f)
        val initialParent = FakeEntity()
        val updatedParent = FakeEntity()
        val disposedEntity = null

        underTest.moveEventListenersMap.forEach { entry ->
            entry.key.onMoveEvent(
                MoveEvent(
                    dummyState,
                    initialRay,
                    currentRay,
                    previousPose,
                    currentPose,
                    previousScale,
                    currentScale,
                    initialParent,
                    updatedParent,
                    disposedEntity,
                )
            )
        }

        tasks.forEach { it.run() }
        assertThat(listenerCalled.get()).isFalse()
    }
}
