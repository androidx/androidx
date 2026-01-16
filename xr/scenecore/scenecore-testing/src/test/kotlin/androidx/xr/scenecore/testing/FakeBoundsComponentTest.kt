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

package androidx.xr.scenecore.testing

import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FakeBoundsComponentTest {
    private lateinit var underTest: FakeBoundsComponent

    @Before
    fun setup() {
        underTest = FakeBoundsComponent()
    }

    @Test
    fun onAttach_GltfEntity_attachesSuccessfully() {
        val gltfEntity = FakeGltfEntity()

        assertThat(gltfEntity.addComponent(underTest)).isTrue()
    }

    @Test
    fun onAttach_NonGltfEntity_failedToAttach() {
        val anchorEntity = FakeAnchorEntity()
        val panelEntity = FakePanelEntity()
        val surfaceEntity = FakeSurfaceEntity()
        val subspaceNodeEntity = FakeSubspaceNodeEntity()

        assertThat(anchorEntity.addComponent(underTest)).isFalse()
        assertThat(panelEntity.addComponent(underTest)).isFalse()
        assertThat(surfaceEntity.addComponent(underTest)).isFalse()
        assertThat(subspaceNodeEntity.addComponent(underTest)).isFalse()
    }

    @Test
    fun onDetach_detachesSuccessfully() {
        check(underTest.entity == null)

        val gltfEntity = FakeGltfEntity()

        assertThat(gltfEntity.addComponent(underTest)).isTrue()
        assertThat(underTest.entity).isNotNull()

        gltfEntity.removeComponent(underTest)

        assertThat(underTest.entity).isNull()
    }

    @Test
    fun addOnBoundsUpdateListener_multipleListeners_addsSuccessfully() {
        assertThat(underTest.listeners).isEmpty()

        underTest.addOnBoundsUpdateListener(DirectExecutor) {}

        assertThat(underTest.listeners).hasSize(1)

        underTest.addOnBoundsUpdateListener(DirectExecutor) {}

        assertThat(underTest.listeners).hasSize(2)
    }

    @Test
    fun addOnBoundsUpdateListener_removeOnBoundsUpdateListener_removesSuccessfully() {
        check(underTest.listeners.isEmpty())

        val listener = Consumer<BoundingBox> {}
        underTest.addOnBoundsUpdateListener(DirectExecutor, listener)

        assertThat(underTest.listeners).hasSize(1)

        underTest.removeOnBoundsUpdateListener(listener)

        assertThat(underTest.listeners).isEmpty()
    }

    @Test
    fun addOnBoundsUpdateListener_callOnBoundingBoxUpdateEvent_correctListenerCallback() {
        // Arrange
        var receivedBoundingBox: BoundingBox? = null
        val listener = Consumer<BoundingBox> { boundingBox -> receivedBoundingBox = boundingBox }
        val expectedBoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)

        // Act
        underTest.addOnBoundsUpdateListener(DirectExecutor, listener)
        underTest.onBoundsUpdate(expectedBoundingBox)

        // Assert
        assertThat(receivedBoundingBox).isNotNull()
        assertThat(receivedBoundingBox).isEqualTo(expectedBoundingBox)
    }

    @Test
    fun addOnBoundsUpdateListener_multipleListeners_correctListenersCallback() {
        // Arrange
        var receivedBoundingBox1: BoundingBox? = null
        val listener1 = Consumer<BoundingBox> { boundingBox -> receivedBoundingBox1 = boundingBox }
        var receivedBoundingBox2: BoundingBox? = null
        val listener2 = Consumer<BoundingBox> { boundingBox -> receivedBoundingBox2 = boundingBox }
        val expectedBoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)

        // Act
        underTest.addOnBoundsUpdateListener(DirectExecutor, listener1)
        underTest.addOnBoundsUpdateListener(DirectExecutor, listener2)
        underTest.onBoundsUpdate(expectedBoundingBox)

        // Assert
        assertThat(receivedBoundingBox1).isNotNull()
        assertThat(receivedBoundingBox1).isEqualTo(expectedBoundingBox)
        assertThat(receivedBoundingBox2).isNotNull()
        assertThat(receivedBoundingBox2).isEqualTo(expectedBoundingBox)
    }

    @Test
    fun addOnBoundsUpdateListener_removeOnBoundsUpdateListener_correctListenersCallback() {
        // Arrange
        var receivedBoundingBox1: BoundingBox? = null
        val listener1 = Consumer<BoundingBox> { boundingBox -> receivedBoundingBox1 = boundingBox }
        var receivedBoundingBox2: BoundingBox? = null
        val listener2 = Consumer<BoundingBox> { boundingBox -> receivedBoundingBox2 = boundingBox }
        val expectedBoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)

        // Act
        underTest.addOnBoundsUpdateListener(DirectExecutor, listener1)
        underTest.addOnBoundsUpdateListener(DirectExecutor, listener2)
        underTest.removeOnBoundsUpdateListener(listener1)
        underTest.onBoundsUpdate(expectedBoundingBox)

        // Assert
        assertThat(receivedBoundingBox1).isNull()
        assertThat(receivedBoundingBox2).isNotNull()
        assertThat(receivedBoundingBox2).isEqualTo(expectedBoundingBox)
    }

    private object DirectExecutor : Executor {
        override fun execute(r: Runnable) {
            r.run()
        }
    }
}
