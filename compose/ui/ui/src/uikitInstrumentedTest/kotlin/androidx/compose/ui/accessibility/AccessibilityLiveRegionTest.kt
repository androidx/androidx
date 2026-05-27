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

package androidx.compose.ui.accessibility

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.test.getAccessibilityTree
import androidx.compose.ui.test.runUIKitInstrumentedTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import platform.UIKit.UIAccessibilityAnnouncementNotification

class AccessibilityLiveRegionTest {

    @Test
    fun testLiveRegionPoliteAnnouncesOnContentChange() = runUIKitInstrumentedTest {
        var text by mutableStateOf("Initial")

        setContent {
            Text(text, modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
            })
        }

        getAccessibilityTree()
        waitForIdle()

        text = "Updated"
        waitForIdle()

        val last = lastAccessibilityNotification
        assertEquals(UIAccessibilityAnnouncementNotification, last?.notification)
        assertEquals("Updated", last?.message)
    }

    @Test
    fun testLiveRegionAssertiveAnnouncesOnContentChange() = runUIKitInstrumentedTest {
        var text by mutableStateOf("Initial")

        setContent {
            Text(text, modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Assertive
            })
        }

        getAccessibilityTree()
        waitForIdle()

        text = "Urgent update"
        waitForIdle()

        val last = lastAccessibilityNotification
        assertEquals(UIAccessibilityAnnouncementNotification, last?.notification)
        assertEquals("Urgent update", last?.message)
    }

    @Test
    fun testLiveRegionAnnouncesWhenNodeAppearsWithContent() = runUIKitInstrumentedTest {
        var showLiveRegion by mutableStateOf(false)

        setContent {
            Column {
                Text("Static text")
                if (showLiveRegion) {
                    Text("Live content appeared", modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    })
                }
            }
        }

        getAccessibilityTree()
        waitForIdle()

        showLiveRegion = true
        waitForIdle()

        val last = lastAccessibilityNotification
        assertEquals(UIAccessibilityAnnouncementNotification, last?.notification)
        assertEquals("Live content appeared", last?.message)
    }

    @Test
    fun testLiveRegionDoesNotAnnounceWhenContentDoesNotChange() = runUIKitInstrumentedTest {
        var unrelatedState by mutableStateOf(0)

        setContent {
            Column {
                Text("Live region text", modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                })
                Text("Counter: $unrelatedState")
            }
        }

        getAccessibilityTree()
        waitForIdle()

        accessibilityNotifications.clear()

        unrelatedState = 1
        waitForIdle()

        val hasAnnouncementNotification = accessibilityNotifications.any {
            it.notification == UIAccessibilityAnnouncementNotification
        }
        assertFalse(hasAnnouncementNotification)
        assertTrue(accessibilityNotifications.isNotEmpty())
    }

    @Test
    fun testLiveRegionAnnouncesMultipleUpdates() = runUIKitInstrumentedTest {
        var text by mutableStateOf("First")

        setContent {
            Text(text, modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
            })
        }

        getAccessibilityTree()
        waitForIdle()

        text = "Second"
        waitForIdle()

        var last = lastAccessibilityNotification
        assertEquals(UIAccessibilityAnnouncementNotification, last?.notification)
        assertEquals("Second", last?.message)

        text = "Third"
        waitForIdle()

        last = lastAccessibilityNotification
        assertEquals(UIAccessibilityAnnouncementNotification, last?.notification)
        assertEquals("Third", last?.message)
    }

    @Test
    fun testNoAnnouncementWithoutLiveRegion() = runUIKitInstrumentedTest {
        var text by mutableStateOf("Initial")

        setContent {
            Text(text)
        }

        getAccessibilityTree()
        waitForIdle()

        text = "Updated"
        waitForIdle()

        val last = lastAccessibilityNotification
        assertNotEquals(UIAccessibilityAnnouncementNotification, last?.notification)
    }

    @Test
    fun testLiveRegionNoAnnouncementWhenNodeDisappears() = runUIKitInstrumentedTest {
        var showLiveRegion by mutableStateOf(true)

        setContent {
            Column {
                Text("Static text")
                if (showLiveRegion) {
                    Text("Live content", modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    })
                }
            }
        }

        getAccessibilityTree()
        waitForIdle()

        showLiveRegion = false
        waitForIdle()

        val last = lastAccessibilityNotification
        assertNotEquals(UIAccessibilityAnnouncementNotification, last?.notification)
    }

    @Test
    fun testLiveRegionAnnouncesOnInitialTreeBuild() = runUIKitInstrumentedTest {
        setContent {
            Text("Live content", modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
            })
        }

        getAccessibilityTree()
        waitForIdle()

        val notification = accessibilityNotifications.first {
            it.notification == UIAccessibilityAnnouncementNotification
        }
        assertNotNull(notification)
        assertEquals("Live content", notification.message)
    }

    @Test
    fun testLiveRegionAnnouncementContainsLabelBeforeValue() = runUIKitInstrumentedTest {
        var label by mutableStateOf("Label")
        var value by mutableStateOf("Value")

        setContent {
            Text("placeholder", modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = label
                stateDescription = value
            })
        }

        getAccessibilityTree()
        waitForIdle()

        var last = lastAccessibilityNotification
        assertEquals(UIAccessibilityAnnouncementNotification, last?.notification)
        assertEquals("Label, Value", last?.message)

        label = "New Label"
        value = "New Value"
        waitForIdle()

        last = lastAccessibilityNotification
        assertEquals(UIAccessibilityAnnouncementNotification, last?.notification)
        assertEquals("New Label, New Value", last?.message)
    }
}
