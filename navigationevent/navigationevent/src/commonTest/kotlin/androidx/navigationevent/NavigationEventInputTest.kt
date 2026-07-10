/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigationevent

import kotlin.test.Test

class NavigationEventInputTest {
    @Test
    fun dispatchOnBackStarted_withoutDispatcher_shouldReturnSilently() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch(event: NavigationEvent) {
                    dispatchOnBackStarted(event)
                }
            }
        // Should not throw an exception.
        input.doDispatch(NavigationEvent())
    }

    @Test
    fun dispatchOnBackProgressed_withoutDispatcher_shouldReturnSilently() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch(event: NavigationEvent) {
                    dispatchOnBackProgressed(event)
                }
            }
        // Should not throw an exception.
        input.doDispatch(NavigationEvent())
    }

    @Test
    fun dispatchOnBackCancelled_withoutDispatcher_shouldReturnSilently() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch() {
                    dispatchOnBackCancelled()
                }
            }
        // Should not throw an exception.
        input.doDispatch()
    }

    @Test
    fun dispatchOnBackCompleted_withoutDispatcher_shouldReturnSilently() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch() {
                    dispatchOnBackCompleted()
                }
            }
        // Should not throw an exception.
        input.doDispatch()
    }

    @Test
    fun dispatchOnForwardStarted_withoutDispatcher_shouldReturnSilently() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch(event: NavigationEvent) {
                    dispatchOnForwardStarted(event)
                }
            }
        // Should not throw an exception.
        input.doDispatch(NavigationEvent())
    }

    @Test
    fun dispatchOnForwardProgressed_withoutDispatcher_shouldReturnSilently() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch(event: NavigationEvent) {
                    dispatchOnForwardProgressed(event)
                }
            }
        // Should not throw an exception.
        input.doDispatch(NavigationEvent())
    }

    @Test
    fun dispatchOnForwardCancelled_withoutDispatcher_shouldReturnSilently() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch() {
                    dispatchOnForwardCancelled()
                }
            }
        // Should not throw an exception.
        input.doDispatch()
    }

    @Test
    fun dispatchOnForwardCompleted_withoutDispatcher_shouldReturnSilently() {
        val input =
            object : NavigationEventInput() {
                fun doDispatch() {
                    dispatchOnForwardCompleted()
                }
            }
        // Should not throw an exception.
        input.doDispatch()
    }
}
