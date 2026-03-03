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

package androidx.navigationevent

import android.window.BackEvent
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import kotlin.test.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationEventTest {

    @Test
    @SdkSuppress(minSdkVersion = 34, maxSdkVersion = 35)
    fun toNavigationEvent_compileSdk34() {
        val fromEvent =
            BackEvent(
                /*touchX = */ 100f,
                /* touchY = */ 200f,
                /* progress = */ 0.5f,
                /* swipeEdge = */ BackEvent.EDGE_LEFT,
            )

        val toEvent = fromEvent.toNavigationEvent()

        assertThat(toEvent.touchX).isEqualTo(fromEvent.touchX)
        assertThat(toEvent.touchY).isEqualTo(fromEvent.touchY)
        assertThat(toEvent.progress).isEqualTo(fromEvent.progress)
        assertThat(toEvent.swipeEdge).isEqualTo(fromEvent.swipeEdge)

        // API 34<..36 doesn't have 'frameTimeMillis', defaults to 0.
        assertThat(toEvent.frameTimeMillis).isEqualTo(0L)
    }

    @Test
    @SdkSuppress(minSdkVersion = 36)
    fun toNavigationEvent_compileSdk36() {
        val fromEvent =
            BackEvent(
                /* touchX = */ 100f,
                /* touchY = */ 200f,
                /* progress = */ 0.5f,
                /* swipeEdge = */ BackEvent.EDGE_LEFT,
                /* frameTimeMillis = */ 9999L,
            )

        val toEvent = fromEvent.toNavigationEvent()

        assertThat(toEvent.touchX).isEqualTo(fromEvent.touchX)
        assertThat(toEvent.touchY).isEqualTo(fromEvent.touchY)
        assertThat(toEvent.progress).isEqualTo(fromEvent.progress)
        assertThat(toEvent.swipeEdge).isEqualTo(fromEvent.swipeEdge)

        // API >= 36 has 'frameTimeMillis'.
        assertThat(toEvent.frameTimeMillis).isEqualTo(fromEvent.frameTimeMillis)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34, maxSdkVersion = 35)
    fun toBackEvent_compileSdk34() {
        val fromEvent =
            NavigationEvent(
                touchX = 100f,
                touchY = 200f,
                progress = 0.5f,
                swipeEdge = NavigationEvent.EDGE_LEFT,
            )

        val toEvent = fromEvent.toBackEvent()

        assertThat(toEvent.touchX).isEqualTo(fromEvent.touchX)
        assertThat(toEvent.touchY).isEqualTo(fromEvent.touchY)
        assertThat(toEvent.progress).isEqualTo(fromEvent.progress)
        assertThat(toEvent.swipeEdge).isEqualTo(fromEvent.swipeEdge)

        // API 34<..36 doesn't have 'frameTimeMillis'.
    }

    @Test
    @SdkSuppress(minSdkVersion = 36)
    fun toBackEvent_compileSdk36() {
        val fromEvent =
            NavigationEvent(
                touchX = 100f,
                touchY = 200f,
                progress = 0.5f,
                swipeEdge = NavigationEvent.EDGE_LEFT,
                frameTimeMillis = 9999L,
            )

        val toEvent = fromEvent.toBackEvent()

        assertThat(toEvent.touchX).isEqualTo(fromEvent.touchX)
        assertThat(toEvent.touchY).isEqualTo(fromEvent.touchY)
        assertThat(toEvent.progress).isEqualTo(fromEvent.progress)
        assertThat(toEvent.swipeEdge).isEqualTo(fromEvent.swipeEdge)

        // API >= 36 has 'frameTimeMillis'.
        assertThat(toEvent.frameTimeMillis).isEqualTo(fromEvent.frameTimeMillis)
    }
}
