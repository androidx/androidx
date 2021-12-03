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

package androidx.wear.watchface.complications.datasource

import androidx.wear.watchface.complications.data.ComplicationData
import java.time.Instant

/** The wire format for [ComplicationData]. */
internal typealias WireComplicationData = android.support.wearable.complications.ComplicationData

/**
 * A time interval, typically used to describe the validity period of a [TimelineEntry].
 *
 * @param start The [Instant] when this TimeInterval becomes valid
 * @param end The [Instant] when this TimeInterval becomes invalid, must be after [start]
 */
public class TimeInterval(
    public var start: Instant,
    public var end: Instant
) {
    init { require(start < end) { "start must be before end" } }
}

/**
 * One piece of renderable content along with the time that it is valid for.
 *
 * @param validity [TimeInterval] describing the validity period for this timeline entry.
 * @param complicationData The renderable [ComplicationData].
 */
public class TimelineEntry(
    public var validity: TimeInterval,
    public var complicationData: ComplicationData
)

/**
 * A collection of TimelineEntry items.
 *
 * This allows a sequence of [ComplicationData] to be delivered to the watch face which can be
 * cached and updated automatically. E.g. today's weather forecast at various times or multiple
 * upcoming calendar events.
 *
 * In the case where the validity periods of TimelineEntry items overlap, the item with the
 * *shortest* validity period will be shown. If none are valid then the [defaultComplicationData]
 * will be shown.  This allows a complication datasource to show a "default", and override it at set
 * points without having to explicitly insert the default [ComplicationData] between the each
 * "override".
 *
 * The complication to render from a timeline is selected each time the watch face is rendered,
 * however the presence of a timeline does not trigger any extra frames to be rendered. Most watch
 * faces render at least once per minute at the top of the minute so complication updates should be
 * timely.
 *
 * Note older watch faces only support [defaultComplicationData], and v1.1 of wear-watchface is
 * required to support [timelineEntries].
 *
 * @param defaultComplicationData The default [ComplicationData] to be displayed
 * @param timelineEntries A collection of "overrides" to be displayed at certain times
 */
public class ComplicationDataTimeline(
    public val defaultComplicationData: ComplicationData,
    public val timelineEntries: Collection<TimelineEntry>
) {
    init {
        for (entry in timelineEntries) {
            require(entry.complicationData.type == defaultComplicationData.type) {
                "TimelineEntry's complicationData must have the same type as the " +
                    "defaultComplicationData"
            }
        }
    }

    internal fun asWireComplicationData(): WireComplicationData {
        val wireTimelineEntries = timelineEntries.map { timelineEntry ->
            timelineEntry.complicationData.asWireComplicationData().apply {
                setTimelineStartInstant(timelineEntry.validity.start)
                setTimelineEndInstant(timelineEntry.validity.end)
            }
        }
        return defaultComplicationData.asWireComplicationData().apply {
            setTimelineEntryCollection(wireTimelineEntries)
        }
    }
}