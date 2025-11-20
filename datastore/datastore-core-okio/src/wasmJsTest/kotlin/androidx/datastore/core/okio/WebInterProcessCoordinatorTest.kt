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

package androidx.datastore.core.okio

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class WebInterProcessCoordinatorTest {
    @Test
    fun coordinator_notifiesListenersInSameTab_afterAnUpdate() = runTest {
        val coordinator =
            createWebProcessCoordinator(
                path = "test-session-store",
                storageType = WebStorageType.SESSION,
            )
        var notification = false
        val latch = CompletableDeferred<Unit>()
        val notificationReceived = CompletableDeferred<Unit>()

        // Launch a background coroutine to act as the "listener".
        val listenerJob = launch {
            // Have latch begin collecting
            latch.complete(Unit)
            // drop(1) to ignore the initial value from StateFlow.
            coordinator.updateNotifications.drop(1).collect {
                notification = true
                notificationReceived.complete(Unit)
            }
        }

        // Wait until listener begins collecting before we trigger the update.
        latch.await()

        // Confirm no notifications have been sent yet. (Should be false as we dropped the
        // initial value.)
        assertThat(notification).isFalse()

        // Trigger an update.
        val newVersion = coordinator.incrementAndGetVersion()
        assertEquals(1, newVersion)

        // Wait for the collector to receive the new emission.
        notificationReceived.await()

        // Assert that a notification was received.
        assertThat(notification).isTrue()

        listenerJob.cancel()
    }
}
