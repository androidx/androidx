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

import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.ResizeEvent
import androidx.xr.scenecore.runtime.ResizeEventListener
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeResizableComponentTest {
    private val initialSize = Dimensions(1.0f, 1.0f, 1.0f)
    private val initialMinSize = Dimensions(0.5f, 0.5f, 0.5f)
    private val initialMaxSize = Dimensions(2.0f, 2.0f, 2.0f)
    private val initialFixedAspectRatio = true
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
                isFixedAspectRatioEnabled = initialFixedAspectRatio,
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
        check(underTest.isFixedAspectRatioEnabled == initialFixedAspectRatio)
        check(underTest.autoHideContent == initialAutoHideContent)
        check(underTest.autoUpdateSize == initialAutoUpdateSize)
        check(underTest.forceShowResizeOverlay == initialForceShowResizeOverlay)
    }

    @Test
    fun addListener_notifiesListener() {
        val expectedResizeState = ResizeEvent.RESIZE_STATE_START
        val expectedNewSize = Dimensions(2.0f, 2.0f, 2.0f)
        val listenerCalled = AtomicBoolean(false)
        val listener = ResizeEventListener { event ->
            listenerCalled.set(true)
            assertThat(event.resizeState).isEqualTo(expectedResizeState)
            assertThat(event.newSize).isEqualTo(expectedNewSize)
        }

        underTest.addResizeEventListener({ command -> command.run() }, listener)

        // For simplicity in the fake, we'll use some default values for fields
        // not directly provided by this simplified move signature.
        val event = ResizeEvent(expectedResizeState, expectedNewSize)
        underTest.onResizeEvent(event)

        assertThat(listenerCalled.get()).isTrue()
    }

    @Test
    fun removeListener_doesNotNotifyRemovedListener() {
        val listenerCalled = AtomicBoolean(false)
        val listener = ResizeEventListener { listenerCalled.set(true) }

        underTest.addResizeEventListener({ command -> command.run() }, listener)
        underTest.removeResizeEventListener(listener)

        // For simplicity in the fake, we'll use some default values for fields
        // not directly provided by this simplified move signature.
        val event =
            ResizeEvent(ResizeEvent.ResizeState.RESIZE_STATE_START, Dimensions(2.0f, 2.0f, 2.0f))

        underTest.onResizeEvent(event)

        assertThat(listenerCalled.get()).isFalse()
    }
}
