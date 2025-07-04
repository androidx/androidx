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
import androidx.xr.runtime.internal.ResizeEvent
import androidx.xr.runtime.internal.ResizeEventListener
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeResizableComponentTest {
    private val initialSize = Dimensions(1.0f, 1.0f, 1.0f)
    private val initialMinSize = Dimensions(0.5f, 0.5f, 0.5f)
    private val initialMaxSize = Dimensions(2.0f, 2.0f, 2.0f)
    private val initialFixedAspectRatio = 1.0f
    private val initialAutoHideContent = false
    private val initialAutoUpdateSize = false
    private val initialForceShowResizeOverlay = false

    private lateinit var underTest: FakeResizableComponent

    @Before
    fun setUp() {
        underTest =
            FakeResizableComponent(
                size = initialSize,
                minimumSize = initialMinSize,
                maximumSize = initialMaxSize,
                fixedAspectRatio = initialFixedAspectRatio,
                autoHideContent = initialAutoHideContent,
                autoUpdateSize = initialAutoUpdateSize,
                forceShowResizeOverlay = initialForceShowResizeOverlay,
            )
    }

    @Test
    fun getDefaultValue_returnDefaultValue() {
        check(underTest.size == initialSize)
        check(underTest.minimumSize == initialMinSize)
        check(underTest.maximumSize == initialMaxSize)
        check(underTest.fixedAspectRatio == initialFixedAspectRatio)
        check(underTest.autoHideContent == initialAutoHideContent)
        check(underTest.autoUpdateSize == initialAutoUpdateSize)
        check(underTest.forceShowResizeOverlay == initialForceShowResizeOverlay)
    }

    @Test
    fun addListener_notifiesListener() {
        val listenerCalled = AtomicBoolean(false)
        val mockListener =
            object : ResizeEventListener {
                override fun onResizeEvent(event: ResizeEvent) {
                    listenerCalled.set(true)
                    assertThat(event.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_START)
                    assertThat(event.newSize).isEqualTo(Dimensions(2.0f, 2.0f, 2.0f))
                }
            }

        underTest.addResizeEventListener({ command -> command.run() }, mockListener)

        // For simplicity in the fake, we'll use some default values for fields
        // not directly provided by this simplified move signature.
        val event =
            ResizeEvent(ResizeEvent.ResizeState.RESIZE_STATE_START, Dimensions(2.0f, 2.0f, 2.0f))
        for ((listener, executor) in underTest.resizeEventListenersMap) {
            executor.execute { listener.onResizeEvent(event) }
        }

        assertThat(listenerCalled.get()).isTrue()
    }

    @Test
    fun removeListener_doesNotNotifyRemovedListener() {
        val listenerCalled = AtomicBoolean(false)
        val mockListener =
            object : ResizeEventListener {
                override fun onResizeEvent(event: ResizeEvent) {
                    listenerCalled.set(true)
                }
            }

        underTest.addResizeEventListener({ command -> command.run() }, mockListener)
        underTest.removeResizeEventListener(mockListener)

        // For simplicity in the fake, we'll use some default values for fields
        // not directly provided by this simplified move signature.
        val event =
            ResizeEvent(ResizeEvent.ResizeState.RESIZE_STATE_START, Dimensions(2.0f, 2.0f, 2.0f))

        for ((listener, executor) in underTest.resizeEventListenersMap) {
            executor.execute { listener.onResizeEvent(event) }
        }

        assertThat(listenerCalled.get()).isFalse()
    }
}
