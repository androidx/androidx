/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.checkers

import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.TilesTestRunner
import androidx.wear.tiles.TimelineBuilders
import com.google.common.truth.Truth.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TilesTestRunner::class)
class TimelineCheckerTest {
    @Test
    fun doCheck_callsAllCheckersOnSuccess() {
        val mockChecker1 = mock<TimelineEntryChecker> {
            on { name } doReturn "MockChecker1"
        }

        val mockChecker2 = mock<TimelineEntryChecker> {
            on { name } doReturn "MockChecker2"
        }

        val checker = TimelineChecker(listOf(mockChecker1, mockChecker2))
        val timeline = buildTimeline()
        checker.doCheck(timeline)

        argumentCaptor<TimelineBuilders.TimelineEntry>().apply {
            verify(mockChecker1, times(2)).check(capture())

            assertThat(firstValue.toProto()).isEqualTo(timeline.timelineEntries[0].toProto())
            assertThat(secondValue.toProto()).isEqualTo(timeline.timelineEntries[1].toProto())
        }

        argumentCaptor<TimelineBuilders.TimelineEntry>().apply {
            verify(mockChecker2, times(2)).check(capture())

            assertThat(firstValue.toProto()).isEqualTo(timeline.timelineEntries[0].toProto())
            assertThat(secondValue.toProto()).isEqualTo(timeline.timelineEntries[1].toProto())
        }
    }

    @Test
    fun doCheck_callsAllCheckersOnFailure() {
        val mockChecker1 = mock<TimelineEntryChecker> {
            on { name } doReturn "MockChecker1"
            on { check(any()) } doThrow CheckerException("Invalid...")
        }

        val mockChecker2 = mock<TimelineEntryChecker> {
            on { name } doReturn "MockChecker2"
            on { check(any()) } doThrow CheckerException("Invalid...")
        }

        val checker = TimelineChecker(listOf(mockChecker1, mockChecker2))
        val timeline = buildTimeline()
        checker.doCheck(timeline)

        // Even on failure, it should still work through everything...
        argumentCaptor<TimelineBuilders.TimelineEntry>().apply {
            verify(mockChecker1, times(2)).check(capture())

            assertThat(firstValue.toProto()).isEqualTo(timeline.timelineEntries[0].toProto())
            assertThat(secondValue.toProto()).isEqualTo(timeline.timelineEntries[1].toProto())
        }

        argumentCaptor<TimelineBuilders.TimelineEntry>().apply {
            verify(mockChecker2, times(2)).check(capture())

            assertThat(firstValue.toProto()).isEqualTo(timeline.timelineEntries[0].toProto())
            assertThat(secondValue.toProto()).isEqualTo(timeline.timelineEntries[1].toProto())
        }
    }

    private fun buildTimeline() =
        TimelineBuilders.Timeline.Builder().addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder().setLayout(
                LayoutElementBuilders.Layout.Builder().setRoot(
                    LayoutElementBuilders.Text.Builder().setText("Hello").build()
                ).build()
            ).build()
        ).addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder().setLayout(
                LayoutElementBuilders.Layout.Builder().setRoot(
                    LayoutElementBuilders.Text.Builder().setText("World").build()
                ).build()
            ).build()
        ).build()
}