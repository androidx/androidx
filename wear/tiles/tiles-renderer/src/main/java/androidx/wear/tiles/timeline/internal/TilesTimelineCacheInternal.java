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

package androidx.wear.tiles.timeline.internal;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.tiles.proto.TimelineProto.TimeInterval;
import androidx.wear.tiles.proto.TimelineProto.Timeline;
import androidx.wear.tiles.proto.TimelineProto.TimelineEntry;

/**
 * Timeline cache for Tiles. This will take in a full timeline, and return the appropriate entry for
 * the given time from {@code findTimelineEntryForTime}.
 */
public final class TilesTimelineCacheInternal {
    private final Timeline mTimeline;

    public TilesTimelineCacheInternal(@NonNull Timeline timeline) {
        this.mTimeline = timeline;
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
    public TimelineEntry findTimelineEntryForTime(long timeMillis) {
        TimelineEntry currentEntry = null;
        long currentEntryLength = Long.MAX_VALUE;

        // Iterate through, finding the _shortest_ valid timeline entry.
        for (TimelineEntry entry : mTimeline.getTimelineEntriesList()) {
            if (!entry.hasValidity()) {
                // Only override a default if there's no more specific entry found.
                if (currentEntryLength == Long.MAX_VALUE) {
                    // Let's treat an entry with no validity as being a "default", as long as we
                    // haven't found any other valid entries
                    currentEntry = entry;
                }
            } else {
                TimeInterval validity = entry.getValidity();

                long validityLength = validity.getEndMillis() - validity.getStartMillis();

                if (validityLength > currentEntryLength) {
                    continue;
                }

                if (validity.getStartMillis() <= timeMillis
                        && timeMillis < validity.getEndMillis()) {
                    currentEntry = entry;
                    currentEntryLength = validityLength;
                }
            }
        }

        return currentEntry;
    }

    /**
     * A (very) inexact version of {@link TilesTimelineCacheInternal#findTimelineEntryForTime(long)}
     * which finds the closest timeline entry to the current time, regardless of validity. This
     * should only used as a fallback if {@code findTimelineEntryForTime} fails, so it can attempt
     * to at least show something.
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
    public TimelineEntry findClosestTimelineEntry(long timeMillis) {
        long currentEntryError = Long.MAX_VALUE;
        TimelineEntry currentEntry = null;

        for (TimelineEntry entry : mTimeline.getTimelineEntriesList()) {
            if (!entry.hasValidity()) {
                // It's a default. This shouldn't happen if we've been called. Skip it.
                continue;
            }

            TimeInterval validity = entry.getValidity();

            if (!isTimeIntervalValid(validity)) {
                continue;
            }

            // It's valid in this time interval. Shouldn't happen. Skip anyway.
            if (validity.getStartMillis() <= timeMillis && timeMillis < validity.getEndMillis()) {
                continue;
            }

            long error;

            // It's in the future.
            if (validity.getStartMillis() > timeMillis) {
                error = validity.getStartMillis() - timeMillis;
            } else {
                // It's in the past.
                error = timeMillis - validity.getEndMillis();
            }

            if (error < currentEntryError) {
                currentEntry = entry;
                currentEntryError = error;
            }
        }

        return currentEntry;
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
    public long findCurrentTimelineEntryExpiry(@NonNull TimelineEntry entry, long fromTimeMillis) {
        long currentSmallestExpiry = Long.MAX_VALUE;
        long entryValidityLength = Long.MAX_VALUE;

        if (entry.hasValidity() && entry.getValidity().getEndMillis() > fromTimeMillis) {
            currentSmallestExpiry = entry.getValidity().getEndMillis();
            entryValidityLength =
                    entry.getValidity().getEndMillis() - entry.getValidity().getStartMillis();
        }

        // Search for the starting edge of an overlapping period (i.e. one with startTime between
        // entry.startTime and entry.endTime), with a validity period shorter than the one currently
        // being considered.
        for (TimelineEntry nextEntry : mTimeline.getTimelineEntriesList()) {
            // The entry can't invalidate itself
            if (nextEntry.equals(entry)) {
                continue;
            }

            // Discard if nextEntry doesn't have a validity period. In this case, it's a default (so
            // would potentially be used at entry.end_millis anyway).
            if (!nextEntry.hasValidity()) {
                continue;
            }

            TimeInterval nextEntryValidity = nextEntry.getValidity();

            // Discard if the validity period is flat out invalid.
            if (!isTimeIntervalValid(nextEntryValidity)) {
                continue;
            }

            // Discard if the start time of nextEntry doesn't fall in the current period (it can't
            // interrupt this entry, so this entry's expiry should be used).
            if (entry.hasValidity()) {
                if (nextEntryValidity.getStartMillis() > entry.getValidity().getEndMillis()
                        || nextEntryValidity.getStartMillis()
                                < entry.getValidity().getStartMillis()) {
                    continue;
                }
            }

            // Discard if its start time is greater than the current smallest one we've found. In
            // that case, the entry that gave us currentSmallestExpiry would be shown next.
            if (nextEntryValidity.getStartMillis() > currentSmallestExpiry) {
                continue;
            }

            // Discard if it's less than "fromTime". This prevents accidentally returning valid
            // times in
            // the past.
            if (nextEntryValidity.getStartMillis() < fromTimeMillis) {
                continue;
            }

            // Finally, consider whether the length of the validity period is shorter than the
            // current one. If this doesn't hold, the current entry would be shown instead (the
            // timeline entry with the shortest validity period is always shown if overlapping).
            //
            // We don't need to deal with the case of shortest validity between this entry, and an
            // already chosen candidate time, as if we've got here, the start time of nextEntry is
            // lower than the entry that is driving currentSmallestExpiry, so nextEntry would be
            // shown regardless.
            long nextEntryValidityLength =
                    nextEntryValidity.getEndMillis() - nextEntryValidity.getStartMillis();

            if (nextEntryValidityLength < entryValidityLength) {
                // It's valid!
                currentSmallestExpiry = nextEntryValidity.getStartMillis();
            }
        }

        return currentSmallestExpiry;
    }

    private static boolean isTimeIntervalValid(TimeInterval timeInterval) {
        // Zero-width (and "negative width") validity periods are not valid, and should never be
        // considered.
        return timeInterval.getEndMillis() > timeInterval.getStartMillis();
    }
}
