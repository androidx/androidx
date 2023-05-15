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

package androidx.wear.tiles.timeline;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.protolayout.proto.TimelineProto.TimelineEntry;
import androidx.wear.tiles.timeline.internal.TilesTimelineCacheInternal;

/**
 * Timeline cache for Wear Tiles. This will take in a full timeline, and return the appropriate
 * entry for the given time from {@code findTimelineEntryForTime}.
 */
public final class TilesTimelineCache {
    private final TilesTimelineCacheInternal mCache;

    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    public TilesTimelineCache(@NonNull androidx.wear.tiles.TimelineBuilders.Timeline timeline) {
        mCache = new TilesTimelineCacheInternal(timeline.toProto());
    }

    /**
     * Finds the entry which should be active at the given time. This will return the entry which
     * has the _shortest_ validity period at the current time, if validity periods overlap. Note
     * that an entry which has no validity period set will be considered a "default" and will be
     * used if no other entries are suitable.
     *
     * @param timeMillis The time to base the search on, in milliseconds.
     * @return The timeline entry which should be active at the given time. Returns {@code null} if
     *     none are valid.
     */
    @MainThread
    @Nullable
    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    public androidx.wear.tiles.TimelineBuilders.TimelineEntry findTimelineEntryForTime(
            long timeMillis) {
        TimelineEntry entry = mCache.findTimelineEntryForTime(timeMillis);

        if (entry == null) {
            return null;
        }

        return androidx.wear.tiles.TimelineBuilders.TimelineEntry.fromProto(entry);
    }

    /**
     * A (very) inexact version of {@link TilesTimelineCache#findTimelineEntryForTime(long)} which
     * finds the closest timeline entry to the current time, regardless of validity. This should
     * only used as a fallback if {@code findTimelineEntryForTime} fails, so it can attempt to at
     * least show something.
     *
     * <p>By this point, we're technically in an error state, so just show _something_. Note that
     * calling this if {@code findTimelineEntryForTime} returns a valid entry is invalid, and may
     * lead to incorrect results.
     *
     * @param timeMillis The time to search from, in milliseconds.
     * @return The timeline entry with validity period closest to {@code timeMillis}.
     */
    @MainThread
    @Nullable
    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    public androidx.wear.tiles.TimelineBuilders.TimelineEntry findClosestTimelineEntry(
            long timeMillis) {
        TimelineEntry entry = mCache.findClosestTimelineEntry(timeMillis);

        if (entry == null) {
            return null;
        }

        return androidx.wear.tiles.TimelineBuilders.TimelineEntry.fromProto(entry);
    }

    /**
     * Finds when the timeline entry {@code entry} should be considered "expired". This is either
     * when it is no longer valid (i.e. end_millis), or when another entry should be presented
     * instead.
     *
     * @param entry The entry to find the expiry time of.
     * @param fromTimeMillis The time to start searching from. The returned time will never be lower
     *     than the value passed here.
     * @return The time in millis that {@code entry} should be considered to be expired. This value
     *     will be {@link Long#MAX_VALUE} if {@code entry} does not expire.
     */
    @MainThread
    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    public long findCurrentTimelineEntryExpiry(
            @NonNull androidx.wear.tiles.TimelineBuilders.TimelineEntry entry,
            long fromTimeMillis) {
        return mCache.findCurrentTimelineEntryExpiry(entry.toProto(), fromTimeMillis);
    }
}
