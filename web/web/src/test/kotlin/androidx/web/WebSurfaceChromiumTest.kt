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

package androidx.web

import com.google.common.truth.Truth.assertThat
import org.chromium.support_lib_boundary.web.WebSurfaceEvent
import org.chromium.support_lib_boundary.web.util.WebSurfaceEventPacker
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(InternalWebApi::class)
@RunWith(JUnit4::class)
class WebSurfaceChromiumTest {

    private class RecordingEventListener : WebSurfaceChromium.EventListener {
        var invalidateCount = 0
        val overScrollEvents = mutableListOf<OverScrollCall>()

        override fun onInvalidate() {
            invalidateCount++
        }

        override fun onOverScroll(
            deltaX: Float,
            deltaY: Float,
            velocityX: Float,
            velocityY: Float,
        ) {
            overScrollEvents.add(OverScrollCall(deltaX, deltaY, velocityX, velocityY))
        }
    }

    private data class OverScrollCall(
        val deltaX: Float,
        val deltaY: Float,
        val velocityX: Float,
        val velocityY: Float,
    )

    @Test
    fun testEventListenerAdapterMapsInvalidateEvent() {
        val listener = RecordingEventListener()
        val adapter = WebSurfaceChromium.EventListenerAdapter(listener)

        adapter.accept(WebSurfaceEvent.INVALIDATE, null)

        assertThat(listener.invalidateCount).isEqualTo(1)
        assertThat(listener.overScrollEvents).isEmpty()
    }

    @Test
    fun testEventListenerAdapterMapsOverScrollEventValuesCorrectly() {
        val listener = RecordingEventListener()
        val adapter = WebSurfaceChromium.EventListenerAdapter(listener)
        val payload =
            WebSurfaceEventPacker.packOverScroll(
                /* deltaX= */ 12.5f,
                /* deltaY= */ -34.25f,
                /* velocityX= */ 560.0f,
                /* velocityY= */ -780.5f,
            )

        adapter.accept(WebSurfaceEvent.OVER_SCROLL, payload)

        assertThat(listener.invalidateCount).isEqualTo(0)
        assertThat(listener.overScrollEvents)
            .containsExactly(
                OverScrollCall(
                    deltaX = 12.5f,
                    deltaY = -34.25f,
                    velocityX = 560.0f,
                    velocityY = -780.5f,
                )
            )
    }

    @Test
    fun testEventListenerAdapterMapsMultipleEventsInOrder() {
        val listener = RecordingEventListener()
        val adapter = WebSurfaceChromium.EventListenerAdapter(listener)

        adapter.accept(WebSurfaceEvent.INVALIDATE, null)
        adapter.accept(
            WebSurfaceEvent.OVER_SCROLL,
            WebSurfaceEventPacker.packOverScroll(1f, 2f, 0f, 0f),
        )
        adapter.accept(WebSurfaceEvent.INVALIDATE, null)
        adapter.accept(
            WebSurfaceEvent.OVER_SCROLL,
            WebSurfaceEventPacker.packOverScroll(0f, 0f, 100f, -200f),
        )

        assertThat(listener.invalidateCount).isEqualTo(2)
        assertThat(listener.overScrollEvents)
            .containsExactly(
                OverScrollCall(deltaX = 1f, deltaY = 2f, velocityX = 0f, velocityY = 0f),
                OverScrollCall(deltaX = 0f, deltaY = 0f, velocityX = 100f, velocityY = -200f),
            )
            .inOrder()
    }

    @Test
    fun testEventListenerAdapterIgnoresUnknownEvent() {
        val listener = RecordingEventListener()
        val adapter = WebSurfaceChromium.EventListenerAdapter(listener)

        adapter.accept(-1, null)

        assertThat(listener.invalidateCount).isEqualTo(0)
        assertThat(listener.overScrollEvents).isEmpty()
    }
}
