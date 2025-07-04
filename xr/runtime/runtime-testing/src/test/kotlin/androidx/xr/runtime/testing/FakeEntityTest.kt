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

import androidx.xr.runtime.internal.InputEvent
import androidx.xr.runtime.internal.InputEventListener
import androidx.xr.runtime.internal.Space
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FakeEntityTest {
    private lateinit var underTest: FakeEntity

    @Before
    fun setUp() {
        underTest = FakeEntity()
    }

    @Test
    fun initial_State() {
        assertThat(underTest.contentDescription).isEqualTo("")
    }

    @Test
    fun addChild_confirmRelationshipBetweenEntities() {
        assertThat(underTest.parent).isNull()
        assertThat(underTest.children.count()).isEqualTo(0)

        val child = FakeEntity()
        assertThat(child.parent).isNull()
        underTest.addChild(child)
        assertThat(underTest.children).containsExactly(child)
        assertThat(child.parent).isEqualTo(underTest)

        child.parent = null
        assertThat(underTest.children).isEmpty()
        assertThat(child.parent).isNull()
    }

    @Test
    fun setHidden_checkWithAndWithoutIncludeParents() {
        val child = FakeEntity()
        underTest.addChild(child)

        // parent: false, child: false
        assertThat(underTest.isHidden(false)).isFalse()
        assertThat(underTest.isHidden(true)).isFalse()
        assertThat(child.isHidden(false)).isFalse()
        assertThat(child.isHidden(true)).isFalse()

        // parent: true, child: false
        underTest.setHidden(true)
        assertThat(underTest.isHidden(true)).isTrue()
        assertThat(underTest.isHidden(false)).isTrue()
        assertThat(child.isHidden(true)).isTrue()
        assertThat(child.isHidden(false)).isFalse()

        // parent: true, child: true
        child.setHidden(true)
        assertThat(underTest.isHidden(true)).isTrue()
        assertThat(underTest.isHidden(false)).isTrue()
        assertThat(child.isHidden(true)).isTrue()
        assertThat(child.isHidden(false)).isTrue()

        // parent: false, child: true
        underTest.setHidden(false)
        assertThat(underTest.isHidden(true)).isFalse()
        assertThat(underTest.isHidden(false)).isFalse()
        assertThat(child.isHidden(true)).isTrue()
        assertThat(child.isHidden(false)).isTrue()
    }

    @Test
    fun setPose_withDifferentSpaces() {
        assertThat(underTest.getPose(Space.PARENT)).isEqualTo(Pose.Identity)
        assertThat(underTest.getPose(Space.ACTIVITY)).isEqualTo(Pose.Identity)
        assertThat(underTest.getPose(Space.REAL_WORLD)).isEqualTo(Pose.Identity)

        val poseParent = Pose(Vector3.One, Quaternion.Identity)
        underTest.setPose(poseParent, Space.PARENT)
        assertPose(poseParent, underTest.getPose(Space.PARENT))

        val poseActivity = Pose(Vector3.Left, Quaternion.Identity)
        underTest.setPose(poseActivity, Space.ACTIVITY)
        assertPose(poseActivity, underTest.getPose(Space.ACTIVITY))

        val poseWorld = Pose(Vector3.Right, Quaternion.Identity)
        underTest.setPose(poseWorld, Space.REAL_WORLD)
        assertPose(poseWorld, underTest.getPose(Space.REAL_WORLD))
    }

    @Test
    fun setScale_withDifferentSpaces() {
        assertThat(underTest.getScale(Space.PARENT)).isEqualTo(Vector3.One)
        assertThat(underTest.getScale(Space.ACTIVITY)).isEqualTo(Vector3.One)
        assertThat(underTest.getScale(Space.REAL_WORLD)).isEqualTo(Vector3.One)

        val scaleParent = Vector3.One
        underTest.setScale(scaleParent, Space.PARENT)
        assertThat(underTest.getScale(Space.PARENT)).isEqualTo(scaleParent)

        val scaleActivity = Vector3(0.1f, 0.1f, 0.1f)
        underTest.setScale(scaleActivity, Space.ACTIVITY)
        assertThat(underTest.getScale(Space.ACTIVITY)).isEqualTo(scaleActivity)

        val scaleWorld = Vector3(0.2f, 0.2f, 0.2f)
        underTest.setScale(scaleWorld, Space.REAL_WORLD)
        assertThat(underTest.getScale(Space.REAL_WORLD)).isEqualTo(scaleWorld)

        val invalidScaleZeroX = Vector3(0f, 1f, 1f)
        val invalidScaleZeroY = Vector3(1f, 0f, 1f)
        val invalidScaleZeroZ = Vector3(1f, 1f, 0f)
        underTest.setScale(invalidScaleZeroX)
        // Last valid pose
        assertThat(underTest.getScale(Space.REAL_WORLD)).isEqualTo(scaleWorld)
        underTest.setScale(invalidScaleZeroY)
        // Last valid pose
        assertThat(underTest.getScale(Space.REAL_WORLD)).isEqualTo(scaleWorld)
        underTest.setScale(invalidScaleZeroZ)
        // Last valid pose
        assertThat(underTest.getScale(Space.REAL_WORLD)).isEqualTo(scaleWorld)
    }

    @Test
    fun setAlpha_withDifferentSpaces() {
        assertThat(underTest.getAlpha(Space.PARENT)).isEqualTo(1.0f)
        assertThat(underTest.getAlpha(Space.ACTIVITY)).isEqualTo(1.0f)
        assertThat(underTest.getAlpha(Space.REAL_WORLD)).isEqualTo(1.0f)

        val alphaParent = 0.1f
        underTest.setAlpha(alphaParent, Space.PARENT)
        assertThat(underTest.getAlpha(Space.PARENT)).isEqualTo(alphaParent)

        val alphaActivity = 0.2f
        underTest.setAlpha(alphaActivity, Space.ACTIVITY)
        assertThat(underTest.getAlpha(Space.ACTIVITY)).isEqualTo(alphaActivity)

        val alphaWorld = 0.3f
        underTest.setAlpha(alphaWorld, Space.REAL_WORLD)
        assertThat(underTest.getAlpha(Space.REAL_WORLD)).isEqualTo(alphaWorld)

        // Alpha value range = coerceIn(0f, 1f)
        val invalidAlphaNegative = -0.1f
        val invalidAlphaMoreThanOne = 1.1f
        underTest.setAlpha(invalidAlphaNegative, Space.REAL_WORLD)
        assertThat(underTest.getAlpha(Space.REAL_WORLD)).isEqualTo(0f)
        underTest.setAlpha(invalidAlphaMoreThanOne, Space.REAL_WORLD)
        assertThat(underTest.getAlpha(Space.REAL_WORLD)).isEqualTo(1f)
    }

    @Test
    fun addInputEventListener_checkInputEventListenerMap() {
        val executor1 = DirectExecutor()
        val listener1 = TestInputEventListener()
        underTest.addInputEventListener(executor1, listener1)
        assertThat(underTest.inputEventListenerMap.keys).containsExactly(listener1)

        underTest.removeInputEventListener(listener1)
        assertThat(underTest.inputEventListenerMap.keys).isEmpty()

        val executor2 = DirectExecutor()
        val listener2 = TestInputEventListener()
        underTest.addInputEventListener(executor1, listener1)
        underTest.addInputEventListener(executor2, listener2)
        assertThat(underTest.inputEventListenerMap.keys).containsExactly(listener1, listener2)

        underTest.dispose()
        assertThat(underTest.inputEventListenerMap.keys).isEmpty()
    }

    @Test
    fun addInputEventListener_callsListeners_getValidInputEvent() {
        val listener = TestInputEventListener()
        val executor = DirectExecutor()
        val inputEvent = InputEvent(0, 0, 0, Vector3.Zero, Vector3.Zero, 0, emptyList())
        underTest.addInputEventListener(executor, listener)
        for (entry in underTest.inputEventListenerMap) {
            entry.key.onInputEvent(inputEvent)
        }

        assertThat(listener.isInputEventCalled).isTrue()
        assertThat(listener.receivedEvent).isEqualTo(inputEvent)
    }

    @Test
    fun addComponent_checkComponentList() {
        assertThat(underTest.getComponents()).isEmpty()

        val component1 = FakeComponent()
        underTest.addComponent(component1)
        assertThat(underTest.getComponents().count()).isEqualTo(1)

        val component2 = FakeComponent()
        underTest.addComponent(component2)
        assertThat(underTest.getComponents().count()).isEqualTo(2)

        assertThat(underTest.getComponents()).containsExactly(component1, component2)
        assertThat(underTest.getComponentsOfType(FakeComponent::class.java))
            .containsExactly(component1, component2)

        underTest.removeComponent(component1)
        assertThat(underTest.getComponents().count()).isEqualTo(1)
        assertThat(underTest.getComponents()).containsExactly(component2)
        assertThat(underTest.getComponentsOfType(FakeComponent::class.java))
            .containsExactly(component2)

        underTest.removeAllComponents()
        assertThat(underTest.getComponents().count()).isEqualTo(0)
    }

    private class TestInputEventListener : InputEventListener {
        var isInputEventCalled = false
            private set

        var receivedEvent: InputEvent? = null
            private set

        override fun onInputEvent(event: InputEvent) {
            isInputEventCalled = true
            receivedEvent = event
        }
    }

    private class DirectExecutor : Executor {
        override fun execute(r: Runnable) {
            r.run()
        }
    }
}
