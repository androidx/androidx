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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.BoundingBox.Companion.fromMinMax
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfEntity
import com.google.common.truth.Truth
import java.util.concurrent.Executor
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Config.TARGET_SDK])
class BoundsComponentImplTest {
    private val boundsComponent = BoundsComponentImpl()
    private val gltfEntity = mock<GltfEntity>()
    private val entity = mock<Entity>()
    private val executor = Executor { it.run() }
    private val listener = mock<Consumer<BoundingBox>>()

    @Before
    fun setUp() {
        whenever(gltfEntity.gltfModelBoundingBox)
            .thenReturn(fromMinMax(Vector3(0f, 0f, 0f), Vector3(0f, 0f, 0f)))
    }

    @Test
    fun onAttach_succeedsForGltfEntity() {
        Truth.assertThat(boundsComponent.onAttach(gltfEntity)).isTrue()
    }

    @Test
    fun onAttach_failsForNonGltfEntity() {
        Truth.assertThat(boundsComponent.onAttach(entity)).isFalse()
    }

    @Test
    fun onAttach_failsIfAlreadyAttached() {
        boundsComponent.onAttach(gltfEntity)

        Truth.assertThat(boundsComponent.onAttach(gltfEntity)).isFalse()
    }

    @Test
    fun onDetach_clearsEntityAndRemovesListener() {
        boundsComponent.onAttach(gltfEntity)
        boundsComponent.addOnBoundsUpdateListener(executor, listener)
        boundsComponent.onDetach(gltfEntity)

        verify(gltfEntity).removeOnBoundsUpdateListener(any())
        Truth.assertThat(boundsComponent.onAttach(gltfEntity)).isTrue()
    }

    @Test
    fun addOnBoundsUpdateListener_addsListenerToGltfEntity() {
        boundsComponent.onAttach(gltfEntity)
        boundsComponent.addOnBoundsUpdateListener(executor, listener)

        verify(gltfEntity).addOnBoundsUpdateListener(any())
    }

    @Test
    fun removeOnBoundsUpdateListener_removesListenerFromGltfEntity() {
        boundsComponent.onAttach(gltfEntity)
        boundsComponent.addOnBoundsUpdateListener(executor, listener)
        boundsComponent.removeOnBoundsUpdateListener(listener)

        verify(gltfEntity).removeOnBoundsUpdateListener(any())
    }

    @Test
    fun frameListener_notifiesListeners() {
        boundsComponent.onAttach(gltfEntity)
        boundsComponent.addOnBoundsUpdateListener(executor, listener)

        val captor = argumentCaptor<Consumer<BoundingBox>>()
        verify(gltfEntity).addOnBoundsUpdateListener(captor.capture())

        val frameListener = captor.lastValue
        val boundingBox = fromMinMax(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        frameListener.accept(boundingBox)

        verify(listener, times(1)).accept(boundingBox)
    }

    @Test
    fun onAttach_withExistingListeners_addsListenerToGltfEntityAndNotifies() {
        boundsComponent.addOnBoundsUpdateListener(executor, listener)
        boundsComponent.onAttach(gltfEntity)

        verify(gltfEntity).addOnBoundsUpdateListener(any())

        verify(listener, times(1)).accept(any<BoundingBox>())
    }

    @Test
    fun onDetach_withExistingListeners_removesListenerFromGltfEntity() {
        boundsComponent.addOnBoundsUpdateListener(executor, listener)
        boundsComponent.onAttach(gltfEntity)
        boundsComponent.onDetach(gltfEntity)

        verify(gltfEntity).removeOnBoundsUpdateListener(any())
    }

    @Test
    fun onDetach_withoutListeners_removesListenerFromGltfEntity() {
        boundsComponent.onAttach(gltfEntity)
        boundsComponent.onDetach(gltfEntity)

        verify(gltfEntity).removeOnBoundsUpdateListener(any())
    }

    @Test
    fun addMultipleListeners_receivesUpdates() {
        boundsComponent.onAttach(gltfEntity)
        val listener1 = mock<Consumer<BoundingBox>>()
        val listener2 = mock<Consumer<BoundingBox>>()
        val captor = argumentCaptor<Consumer<BoundingBox>>()
        val initialBoundingBox = fromMinMax(Vector3(0f, 0f, 0f), Vector3(0f, 0f, 0f))
        whenever(gltfEntity.gltfModelBoundingBox).thenReturn(initialBoundingBox)

        // Add first listener and capture the frame listener
        boundsComponent.addOnBoundsUpdateListener(executor, listener1)
        verify(gltfEntity).addOnBoundsUpdateListener(captor.capture())
        val frameListener = captor.lastValue

        // Verify listener1 received the initial bounding box
        verify(listener1, times(1)).accept(initialBoundingBox)

        // Trigger first update
        val boundingBox1 = fromMinMax(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        frameListener.accept(boundingBox1)
        verify(listener1, times(1)).accept(boundingBox1)
        verify(listener2, never()).accept(any())

        // Add second listener
        boundsComponent.addOnBoundsUpdateListener(executor, listener2)
        verify(listener2, times(1)).accept(initialBoundingBox)

        // Trigger second update
        val boundingBox2 = fromMinMax(Vector3(0f, 0f, 0f), Vector3(2f, 2f, 2f))
        frameListener.accept(boundingBox2)
        verify(listener1, times(1)).accept(boundingBox2)
        verify(listener2, times(1)).accept(boundingBox2)
    }
}
