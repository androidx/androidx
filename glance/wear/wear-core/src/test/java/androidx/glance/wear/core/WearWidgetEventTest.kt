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

package androidx.glance.wear.core

import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class WearWidgetEventTest {
    @Test
    fun wearWidgetVisibleEvent_equals_sameInstance() {
        val event =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        assertThat(event).isEqualTo(event)
    }

    @Test
    fun wearWidgetVisibleEvent_equals_differentInstancesSameValues() {
        val event1 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        val event2 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        assertThat(event1).isEqualTo(event2)
    }

    @Test
    fun wearWidgetVisibleEvent_equals_differentInstanceId() {
        val event1 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        val event2 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 2),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        assertThat(event1).isNotEqualTo(event2)
    }

    @Test
    fun wearWidgetVisibleEvent_equals_differentTimestamp() {
        val event1 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        val event2 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(2000),
                duration = Duration.ofSeconds(2),
            )
        assertThat(event1).isNotEqualTo(event2)
    }

    @Test
    fun wearWidgetVisibleEvent_equals_differentDuration() {
        val event1 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        val event2 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(3),
            )
        assertThat(event1).isNotEqualTo(event2)
    }

    @Test
    fun wearWidgetVisibleEvent_equals_differentType() {
        val event =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        assertThat(event).isNotEqualTo(object : WearWidgetEvent {})
    }

    @Test
    fun wearWidgetVisibleEvent_equals_null() {
        val event =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        assertThat(event).isNotEqualTo(null)
    }

    @Test
    fun wearWidgetVisibleEvent_hashCode_sameForEqualObjects() {
        val event1 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        val event2 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
    }

    @Test
    fun wearWidgetVisibleEvent_hashCode_differentForDifferentInstanceId() {
        val event1 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        val event2 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 2),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        assertThat(event1.hashCode()).isNotEqualTo(event2.hashCode())
    }

    @Test
    fun wearWidgetVisibleEvent_hashCode_differentForDifferentTimestamp() {
        val event1 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        val event2 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(2000),
                duration = Duration.ofSeconds(2),
            )
        assertThat(event1.hashCode()).isNotEqualTo(event2.hashCode())
    }

    @Test
    fun wearWidgetVisibleEvent_hashCode_differentForDifferentDuration() {
        val event1 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(2),
            )
        val event2 =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId("ns", 1),
                startTime = Instant.ofEpochMilli(1000),
                duration = Duration.ofSeconds(3),
            )
        assertThat(event1.hashCode()).isNotEqualTo(event2.hashCode())
    }
}
