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

    @Test
    fun hasEnabledHandlers_callbacksTriggeredCorrectly() {
        val dispatcher = NavigationEventDispatcher()
        var hasAny = false
        var hasBack = false
        var hasForward = false
        val input =
            object : NavigationEventInput() {
                override fun onHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) {
                    hasAny = hasEnabledHandlers
                }

                override fun onHasEnabledBackHandlersChanged(hasEnabledBackHandlers: Boolean) {
                    hasBack = hasEnabledBackHandlers
                }

                override fun onHasEnabledForwardHandlersChanged(
                    hasEnabledForwardHandlers: Boolean
                ) {
                    hasForward = hasEnabledForwardHandlers
                }
            }
        dispatcher.addInput(input)

        // Initially all are false
        kotlin.test.assertEquals(false, hasAny)
        kotlin.test.assertEquals(false, hasBack)
        kotlin.test.assertEquals(false, hasForward)

        val handler =
            object :
                NavigationEventHandler<NavigationEventInfo>(
                    initialInfo = NavigationEventInfo.None,
                    isBackEnabled = false,
                    isForwardEnabled = true,
                ) {}
        dispatcher.addHandler(handler)

        // Forward handler enabled: Any=true, Back=false, Forward=true
        kotlin.test.assertEquals(true, hasAny)
        kotlin.test.assertEquals(false, hasBack)
        kotlin.test.assertEquals(true, hasForward)

        handler.isBackEnabled = true

        // Both enabled: Any=true, Back=true, Forward=true
        kotlin.test.assertEquals(true, hasAny)
        kotlin.test.assertEquals(true, hasBack)
        kotlin.test.assertEquals(true, hasForward)

        handler.isForwardEnabled = false

        // Forward disabled: Any=true, Back=true, Forward=false
        kotlin.test.assertEquals(true, hasAny)
        kotlin.test.assertEquals(true, hasBack)
        kotlin.test.assertEquals(false, hasForward)

        handler.isBackEnabled = false

        // All disabled: Any=false, Back=false, Forward=false
        kotlin.test.assertEquals(false, hasAny)
        kotlin.test.assertEquals(false, hasBack)
        kotlin.test.assertEquals(false, hasForward)
    }

    @Test
    fun hasEnabledHandlers_propertiesUpdatedCorrectly() {
        val dispatcher = NavigationEventDispatcher()
        val input = object : NavigationEventInput() {}
        dispatcher.addInput(input)

        // Initially all are false
        kotlin.test.assertEquals(false, input.hasEnabledHandlers)
        kotlin.test.assertEquals(false, input.hasEnabledBackHandlers)
        kotlin.test.assertEquals(false, input.hasEnabledForwardHandlers)

        val handler =
            object :
                NavigationEventHandler<NavigationEventInfo>(
                    initialInfo = NavigationEventInfo.None,
                    isBackEnabled = false,
                    isForwardEnabled = true,
                ) {}
        dispatcher.addHandler(handler)

        // Forward handler enabled: Any=true, Back=false, Forward=true
        kotlin.test.assertEquals(true, input.hasEnabledHandlers)
        kotlin.test.assertEquals(false, input.hasEnabledBackHandlers)
        kotlin.test.assertEquals(true, input.hasEnabledForwardHandlers)

        handler.isBackEnabled = true

        // Both enabled: Any=true, Back=true, Forward=true
        kotlin.test.assertEquals(true, input.hasEnabledHandlers)
        kotlin.test.assertEquals(true, input.hasEnabledBackHandlers)
        kotlin.test.assertEquals(true, input.hasEnabledForwardHandlers)

        handler.isForwardEnabled = false

        // Forward disabled: Any=true, Back=true, Forward=false
        kotlin.test.assertEquals(true, input.hasEnabledHandlers)
        kotlin.test.assertEquals(true, input.hasEnabledBackHandlers)
        kotlin.test.assertEquals(false, input.hasEnabledForwardHandlers)

        handler.isBackEnabled = false

        // All disabled: Any=false, Back=false, Forward=false
        kotlin.test.assertEquals(false, input.hasEnabledHandlers)
        kotlin.test.assertEquals(false, input.hasEnabledBackHandlers)
        kotlin.test.assertEquals(false, input.hasEnabledForwardHandlers)
    }
}
