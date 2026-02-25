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
import kotlinx.browser.localStorage
import kotlinx.browser.sessionStorage
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.w3c.dom.StorageEvent
import org.w3c.dom.StorageEventInit

class WebInterProcessCoordinatorTest {
    @Test
    fun coordinator_notifiesListenersInSameTab_afterAnUpdate_sessionStorage() = runTest {
        val storeName = "test-session-store"
        val versionKey = "datastore_SESSION_${storeName}_version"
        val coordinator =
            createWebProcessCoordinator(path = storeName, storageType = WebStorageType.SESSION)

        try {
            var notification = false
            val latch = CompletableDeferred<Unit>()
            val notificationReceived = CompletableDeferred<Unit>()

            // Launch a background coroutine to act as the "listener".
            val listenerJob = launch {
                // Have latch begin collecting
                latch.complete(Unit)
                // drop(1) to ignore the initial value from StateFlow
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
        } finally {
            sessionStorage.removeItem(versionKey)
        }
    }

    @Test
    fun coordinator_notifiesListenersInSameTab_afterAnUpdate_localStorage() = runTest {
        val storeName = "test-local-store"
        val versionKey = "datastore_LOCAL_${storeName}_version"
        val coordinator =
            createWebProcessCoordinator(path = storeName, storageType = WebStorageType.LOCAL)

        try {
            var notification = false
            val latch = CompletableDeferred<Unit>()
            val notificationReceived = CompletableDeferred<Unit>()

            // Launch a background coroutine to act as the "listener".
            val listenerJob = launch {
                // Have latch begin collecting
                latch.complete(Unit)
                // drop(1) to ignore the initial value from StateFlow
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
        } finally {
            // Clean up for next test
            localStorage.removeItem(versionKey)
        }
    }

    @Test
    fun coordinator_notifiesListenersInDifferentTabs_afterAnUpdate_localStorage() = runTest {
        val storeName = "test-local-store"
        val versionKey = "datastore_LOCAL_${storeName}_version"

        // Two processes accessing the same datastore - mimic different tabs
        val tabWithUpdate =
            createWebProcessCoordinator(path = storeName, storageType = WebStorageType.LOCAL)
        val tabReceivingUpdate =
            createWebProcessCoordinator(path = storeName, storageType = WebStorageType.LOCAL)

        try {
            var notification = false
            val latch = CompletableDeferred<Unit>()
            val notificationReceived = CompletableDeferred<Unit>()

            val listenerJob = launch {
                latch.complete(Unit)
                // drop(1) to ignore the initial value from StateFlow
                tabReceivingUpdate.updateNotifications.drop(1).collect {
                    notification = true
                    notificationReceived.complete(Unit)
                }
            }
            latch.await()
            assertThat(notification).isFalse()

            // Simulate an update from another tab by directly setting the localStorage item
            // and dispatching a storage event.
            val newVersion = "1"
            localStorage.setItem(versionKey, newVersion)
            window.dispatchEvent(
                StorageEvent(
                    "storage",
                    StorageEventInit().apply {
                        this.key = versionKey
                        this.newValue = newVersion
                        this.oldValue = "0"
                        this.storageArea = localStorage
                    },
                )
            )

            // We will time out if the `tabReceivingUpdate` did not successfully register
            // its internal `storageEventListener` on the window object during the `init` block of
            // the WebInterProcessCoordinator. Not timing out confirms the tab receiving the
            // notification correctly received it.
            notificationReceived.await()
            assertThat(notification).isTrue()

            listenerJob.cancel()
        } finally {
            localStorage.removeItem(versionKey)
        }
    }
}
