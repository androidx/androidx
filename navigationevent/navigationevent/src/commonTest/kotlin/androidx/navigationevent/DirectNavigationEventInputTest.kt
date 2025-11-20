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

import androidx.kruth.assertThat
import androidx.navigationevent.testing.TestNavigationEventHandler
import kotlin.test.Test

class DirectNavigationEventInputTest {
    @Test
    fun backStarted_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onBackStartedInvocations).isEqualTo(0)
        input.backStarted(NavigationEvent())
        assertThat(handler.onBackStartedInvocations).isEqualTo(1)
    }

    @Test
    fun backProgressed_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onBackProgressedInvocations).isEqualTo(0)
        input.backStarted(NavigationEvent())
        input.backProgressed(NavigationEvent())
        assertThat(handler.onBackProgressedInvocations).isEqualTo(1)
    }

    @Test
    fun backCancelled_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onBackCancelledInvocations).isEqualTo(0)
        input.backStarted(NavigationEvent())
        input.backCancelled()
        assertThat(handler.onBackCancelledInvocations).isEqualTo(1)
    }

    @Test
    fun backCompleted_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onBackCompletedInvocations).isEqualTo(0)
        input.backCompleted()
        assertThat(handler.onBackCompletedInvocations).isEqualTo(1)
    }

    @Test
    fun forwardStarted_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onForwardStartedInvocations).isEqualTo(0)
        input.forwardStarted(NavigationEvent())
        assertThat(handler.onForwardStartedInvocations).isEqualTo(1)
    }

    @Test
    fun forwardProgressed_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onForwardProgressedInvocations).isEqualTo(0)
        input.forwardStarted(NavigationEvent())
        input.forwardProgressed(NavigationEvent())
        assertThat(handler.onForwardProgressedInvocations).isEqualTo(1)
    }

    @Test
    fun forwardCancelled_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onForwardCancelledInvocations).isEqualTo(0)
        input.forwardStarted(NavigationEvent())
        input.forwardCancelled()
        assertThat(handler.onForwardCancelledInvocations).isEqualTo(1)
    }

    @Test
    fun forwardCompleted_afterConnected_shouldWork() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val handler = TestNavigationEventHandler()
        dispatcher.addHandler(handler)

        assertThat(handler.onForwardCompletedInvocations).isEqualTo(0)
        input.forwardCompleted()
        assertThat(handler.onForwardCompletedInvocations).isEqualTo(1)
    }
}
