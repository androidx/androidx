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

package androidx.wear.tiles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.StateBuilders.State
import androidx.wear.protolayout.TimelineBuilders.Timeline.fromLayoutElement
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.protolayout.layout.basicText
import androidx.wear.protolayout.types.layoutString
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TileTest {
    @Test
    fun tile_requiredOnly_inflates() {
        val timeline = fromLayoutElement(TEST_ELEMENT1)

        val tile = tile(timeline = timeline)

        assertThat(tile.tileTimeline!!.toProto()).isEqualTo(timeline.toProto())
        assertThat(tile.state).isNull()
        assertThat(tile.freshnessIntervalMillis).isEqualTo(0)
        assertThat(tile.resourcesVersion).isEmpty()
    }

    @Test
    fun tile_allParams_inflates() {
        val timeline = fromLayoutElement(TEST_ELEMENT1)
        val freshness = 1234.milliseconds
        val state = State.Builder().build()
        val resVersion = "test"

        val tile =
            tile(
                timeline = timeline,
                freshness = freshness,
                state = state,
                resourcesVersion = resVersion,
            )

        assertThat(tile.tileTimeline!!.toProto()).isEqualTo(timeline.toProto())
        assertThat(tile.state!!.toProto()).isEqualTo(state.toProto())
        assertThat(tile.freshnessIntervalMillis)
            .isEqualTo(freshness.toLong(DurationUnit.MILLISECONDS))
        assertThat(tile.resourcesVersion).isEqualTo(resVersion)
    }

    @Test
    fun timeline_inflates() {
        val entry1 = TimelineEntry.fromLayoutElement(TEST_ELEMENT1)
        val entry2 = TimelineEntry.fromLayoutElement(TEST_ELEMENT2)

        val timeline = timeline(entry1, entry2)

        assertThat(timeline.timelineEntries).hasSize(2)
        assertThat(timeline.timelineEntries[0].toProto()).isEqualTo(entry1.toProto())
        assertThat(timeline.timelineEntries[1].toProto()).isEqualTo(entry2.toProto())
    }

    @Test
    fun timelineEntry_requiredOnly_inflates() {
        val start = 1.toDuration(DurationUnit.DAYS)
        val end = 2.toDuration(DurationUnit.DAYS)

        val interval = timeInterval(start, end)
        val entry = timelineEntry(TEST_ELEMENT1, interval)

        assertThat(entry.layout!!.root!!.toLayoutElementProto())
            .isEqualTo(TEST_ELEMENT1.toLayoutElementProto())
        assertThat(entry.validity!!.toProto()).isEqualTo(interval.toProto())
    }

    @Test
    fun timeInterval_requiredOnly_inflates() {
        val start = 1.toDuration(DurationUnit.DAYS)
        val end = 2.toDuration(DurationUnit.DAYS)

        val interval = timeInterval(start, end)

        assertThat(interval.startMillis).isEqualTo(start.toLong(DurationUnit.MILLISECONDS))
        assertThat(interval.endMillis).isEqualTo(end.toLong(DurationUnit.MILLISECONDS))
    }

    @Test
    fun timelineEntry_allParams_inflates() {
        val entry = timelineEntry(TEST_ELEMENT1)

        assertThat(entry.layout!!.root!!.toLayoutElementProto())
            .isEqualTo(TEST_ELEMENT1.toLayoutElementProto())
        assertThat(entry.validity).isNull()
    }

    @Test
    fun baseSingleEntry_fromLayoutElement_areSame() {
        assertThat(timeline(timelineEntry(TEST_ELEMENT1)).toProto())
            .isEqualTo(fromLayoutElement(TEST_ELEMENT1).toProto())
    }

    private companion object {
        val TEST_ELEMENT1 = basicText("Test 1".layoutString)
        val TEST_ELEMENT2 = basicText("Test 2".layoutString)
    }
}
