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

package androidx.navigationevent

import androidx.kruth.assertThrows
import kotlin.test.Test

class NavigationEventInputTest {
    @Test
    fun dispatchOnBackStarted_withoutDispatcher_shouldFail() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch(event: NavigationEvent) {
                    dispatchOnBackStarted(event)
                }
            }
        assertThrows(IllegalStateException::class) { input.doDispatch(NavigationEvent()) }
            .hasMessageThat()
            .contains("This input is not added to any dispatcher.")
    }

    @Test
    fun dispatchOnBackProgressed_withoutDispatcher_shouldFail() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch(event: NavigationEvent) {
                    dispatchOnBackProgressed(event)
                }
            }
        assertThrows(IllegalStateException::class) { input.doDispatch(NavigationEvent()) }
            .hasMessageThat()
            .contains("This input is not added to any dispatcher.")
    }

    @Test
    fun dispatchOnBackCancelled_withoutDispatcher_shouldFail() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch() {
                    dispatchOnBackCancelled()
                }
            }
        assertThrows(IllegalStateException::class) { input.doDispatch() }
            .hasMessageThat()
            .contains("This input is not added to any dispatcher.")
    }

    @Test
    fun dispatchOnBackCompleted_withoutDispatcher_shouldFail() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch() {
                    dispatchOnBackCompleted()
                }
            }
        assertThrows(IllegalStateException::class) { input.doDispatch() }
            .hasMessageThat()
            .contains("This input is not added to any dispatcher.")
    }

    @Test
    fun dispatchOnForwardStarted_withoutDispatcher_shouldFail() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch(event: NavigationEvent) {
                    dispatchOnForwardStarted(event)
                }
            }
        assertThrows(IllegalStateException::class) { input.doDispatch(NavigationEvent()) }
            .hasMessageThat()
            .contains("This input is not added to any dispatcher.")
    }

    @Test
    fun dispatchOnForwardProgressed_withoutDispatcher_shouldFail() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch(event: NavigationEvent) {
                    dispatchOnForwardProgressed(event)
                }
            }
        assertThrows(IllegalStateException::class) { input.doDispatch(NavigationEvent()) }
            .hasMessageThat()
            .contains("This input is not added to any dispatcher.")
    }

    @Test
    fun dispatchOnForwardCancelled_withoutDispatcher_shouldFail() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch() {
                    dispatchOnForwardCancelled()
                }
            }
        assertThrows(IllegalStateException::class) { input.doDispatch() }
            .hasMessageThat()
            .contains("This input is not added to any dispatcher.")
    }

    @Test
    fun dispatchOnForwardCompleted_withoutDispatcher_shouldFail() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch() {
                    dispatchOnForwardCompleted()
                }
            }
        assertThrows(IllegalStateException::class) { input.doDispatch() }
            .hasMessageThat()
            .contains("This input is not added to any dispatcher.")
    }
}
