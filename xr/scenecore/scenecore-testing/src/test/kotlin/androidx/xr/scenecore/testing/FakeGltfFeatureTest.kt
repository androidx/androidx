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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.NodeHolder
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeGltfFeatureTest {

    private lateinit var underTest: FakeGltfFeature

    private val nodeHolder: NodeHolder<*> =
        NodeHolder<FakeNode>(object : FakeNode {}, FakeNode::class.java)
    private val executor = FakeScheduledExecutorService()

    @Before
    fun setUp() {
        underTest = FakeGltfFeature(nodeHolder)
    }

    @Test
    fun getInitialState_returnsDefaultValues() {
        assertThat(underTest.nodes).isEmpty()
        assertThat(underTest.size).isEqualTo(FloatSize3d(1f, 1f, 1f))
    }

    @Test
    fun addNode_addsToExternalListAndDelegatesToInternal() {
        val newNode = FakeGltfModelNodeFeature("another_node")
        underTest.addNode(newNode)

        assertThat(underTest.nodes).contains(newNode)
    }

    @Test
    fun addAnimationAndGetAnimations_addsToExternalListAndDelegatesToInternal() {
        val expectedAnimation = FakeGltfAnimationFeature("test_animation")

        underTest.addAnimation(expectedAnimation)

        val animations = underTest.getAnimations(executor)
        assertThat(animations).containsExactly(expectedAnimation)
    }

    @Test
    fun getGltfModelBoundingBox_delegatesToInternal() {
        val expectedBox = BoundingBox.fromMinMax(Vector3(-1f, -1f, -1f), Vector3(1f, 1f, 1f))
        underTest.setGltfModelBoundingBox(expectedBox)

        assertThat(underTest.getGltfModelBoundingBox()).isEqualTo(expectedBox)
    }

    @Test
    fun setColliderEnabled_delegatesToInternal() {
        underTest.setColliderEnabled(true)
        assertThat(underTest.enableCollider).isTrue()

        underTest.setColliderEnabled(false)
        assertThat(underTest.enableCollider).isFalse()
    }

    @Test
    fun addAnimationStateListener_delegatesToInternal() {
        val listener = Consumer<Int> {}
        underTest.addAnimationStateListener(executor, listener)

        assertThat(underTest.animationStateListeners).containsKey(listener)
        assertThat(underTest.animationStateListeners[listener]).isEqualTo(executor)
    }

    @Test
    fun removeAnimationStateListener_delegatesToInternal() {
        val listener = Consumer<Int> {}
        underTest.addAnimationStateListener(executor, listener)
        underTest.removeAnimationStateListener(listener)

        assertThat(underTest.animationStateListeners).doesNotContainKey(listener)
    }

    @Test
    fun addOnBoundsUpdateListener_delegatesToInternal() {
        val listener = Consumer<BoundingBox> {}
        underTest.addOnBoundsUpdateListener(listener)

        assertThat(underTest.boundsUpdateListeners).contains(listener)
    }

    @Test
    fun removeOnBoundsUpdateListener_delegatesToInternal() {
        val listener = Consumer<BoundingBox> {}
        underTest.addOnBoundsUpdateListener(listener)
        underTest.removeOnBoundsUpdateListener(listener)

        assertThat(underTest.boundsUpdateListeners).doesNotContain(listener)
    }

    @Test
    fun setReformAffordanceEnabled_delegatesToInternal() {
        val fakeEntity = FakeGltfEntity()

        underTest.setReformAffordanceEnabled(
            entity = fakeEntity,
            enabled = true,
            executor = executor,
            systemMovable = true,
        )

        assertThat(underTest.reformAffordanceEnabled).isTrue()

        underTest.setReformAffordanceEnabled(
            entity = fakeEntity,
            enabled = false,
            executor = executor,
            systemMovable = true,
        )

        assertThat(underTest.reformAffordanceEnabled).isFalse()
    }

    @Test
    fun dispose_delegatesToInternal() {
        val boundsListener = Consumer<BoundingBox> {}
        underTest.addOnBoundsUpdateListener(boundsListener)

        val animationListener = Consumer<Int> {}
        underTest.addAnimationStateListener(executor, animationListener)

        underTest.dispose()

        assertThat(underTest.boundsUpdateListeners).isEmpty()
        assertThat(underTest.animationStateListeners).isEmpty()
    }
}
