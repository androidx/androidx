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
@file:JvmName("TileKt")

package androidx.wear.tiles

import android.annotation.SuppressLint
import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.StateBuilders.State
import androidx.wear.protolayout.TimelineBuilders.TimeInterval
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.protolayout.expression.RequiresSchemaVersion
import androidx.wear.tiles.TileBuilders.Tile
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Creates a tile from the defined parameters and layout components defined in the [Timeline] that
 * can be rendered by a tile renderer.
 *
 * Example of usage to create a tile from the given [LayoutElement] for 1 entry: `tile(timeline =
 * timeline(timelineEntry(tileLayout())))`. For more Kotlin friendly support and automatic, more
 * performant resources handling, see [Material3TileService].
 *
 * @param timeline The [Timeline] containing the layouts for the tiles to show in the carousel,
 *   along with their validity periods.
 * @param freshness How many elapsed time (**not** wall clock time) this tile can be considered to
 *   be "fresh". The platform will attempt to refresh your tile at some point in the future after
 *   this interval has lapsed. A value of 0 here signifies that auto-refreshes should not be used
 *   (i.e. you will manually request updates via [TileService.getUpdater]. This mechanism should not
 *   be used to update your tile more frequently than once a minute, and the system may throttle
 *   your updates if you request updates faster than this interval. This interval is also inexact;
 *   the system will generally update your tile if it is on-screen, or about to be on-screen,
 *   although this is not guaranteed due to system-level optimizations. Negative values will be
 *   ignored and considered as 0.
 * @param state The [State] for this tile
 * @param resourcesVersion The resource version for tile's resources used for caching the resources
 *   in system. When using better resources handling via
 *   [androidx.wear.protolayout.ProtoLayoutScope] this can be omitted.
 */
@SuppressLint("ProtoLayoutMinSchema")
@Suppress(
    "MissingJvmstatic",
    "ValueClassUsageWithoutJvmName",
) // Kotlin-friendly version of already available Java Apis
public fun tile(
    timeline: Timeline,
    freshness: Duration? = null,
    @RequiresSchemaVersion(major = 1, minor = 200) state: State? = null,
    resourcesVersion: String? = null,
): Tile =
    Tile.Builder()
        .setTileTimeline(timeline)
        .apply {
            freshness?.let {
                setFreshnessIntervalMillis(it.toLong(unit = DurationUnit.MILLISECONDS))
            }
            state?.let { setState(it) }
            resourcesVersion?.let { setResourcesVersion(it) }
        }
        .build()

/**
 * Creates a [Timeline] as a collection of [TimelineEntry] items.
 *
 * [TimelineEntry] items can be used to update a tile layout on-screen at known times, without
 * having to explicitly update a layout. This allows for cases where, say, a calendar can be used to
 * show the next event, and automatically switch to showing the next event when one has passed.
 *
 * The active [TimelineEntry] is switched, at most, once a minute. In the case where the validity
 * periods of [TimelineEntry] items overlap, the item with the shortest* validity period will be
 * shown. This allows a layout provider to show a "default" layout, and override it at set points
 * without having to explicitly insert the default layout between the "override" layout.
 *
 * @param entries The list of one or more [TimelineEntry]. If your tile only has 1 [TimelineEntry],
 *   cleaner helper method to use instead of this one is [Timeline.fromLayoutElement] where
 *   [LayoutElement] can be passed in directly.
 */
public fun timeline(vararg entries: TimelineEntry): Timeline =
    Timeline.Builder().apply { entries.forEach { addTimelineEntry(it) } }.build()

/**
 * Creates one piece of renderable content along with the time that it is valid for.
 *
 * @param layout The contents of this timeline entry, root [LayoutElement] of tile
 * @param validity The validity period for this timeline entry. If not set, this entry doesn't
 *   expire.
 */
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
public fun timelineEntry(layout: LayoutElement, validity: TimeInterval? = null): TimelineEntry =
    TimelineEntry.Builder()
        .setLayout(Layout.fromLayoutElement(layout))
        .apply { validity?.let { setValidity(it) } }
        .build()

/**
 * Creates a time interval, typically used to describe the validity period of a [TimelineEntry].
 *
 * @param start The starting point of the time interval, since the Unix epoch
 * @param end The end point of the time interval, since the Unix epoch
 */
@Suppress("ValueClassUsageWithoutJvmName") // Kotlin-friendly version of already available Java Apis
public fun timeInterval(start: Duration, end: Duration): TimeInterval =
    TimeInterval.Builder()
        .setStartMillis(start.toLong(unit = DurationUnit.MILLISECONDS))
        .setEndMillis(end.toLong(unit = DurationUnit.MILLISECONDS))
        .build()
