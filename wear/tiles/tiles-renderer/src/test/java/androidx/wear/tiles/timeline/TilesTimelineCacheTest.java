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

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.Nullable;
import androidx.wear.tiles.TilesTestRunner;

import com.google.common.truth.Expect;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.time.Duration;

@RunWith(TilesTestRunner.class)
@DoNotInstrument
public class TilesTimelineCacheTest {
    @Rule public Expect expect = Expect.create();

    @Test
    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    public void timelineCache_noValidityMakesDefaultTile() {
        // Purposefully not setting a validity period.
        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Hello World"))
                        .build();
        androidx.wear.tiles.TimelineBuilders.Timeline timeline =
                new androidx.wear.tiles.TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(entry)
                        .build();

        TilesTimelineCache timelineCache = new TilesTimelineCache(timeline);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(0L), entry);
    }

    @Test
    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    public void timelineCache_nonOverlappingTilesShownAtCorrectTime() {
        // Check for non-overlapping time slots (i.e. pure sequential), for example:
        //     +-------------------+------------------+
        //     |       E1          |        E2        |
        //     +-------------------+------------------+
        //
        // Expected:
        //     +-------------------+------------------+
        //     |       E1          |        E2        |
        //     +-------------------+------------------+
        final long cutoverMillis = Duration.ofMinutes(10).toMillis();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry1 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Tile1"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(0)
                                        .setEndMillis(cutoverMillis)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry2 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Tile2"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(cutoverMillis)
                                        .setEndMillis(Long.MAX_VALUE)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.Timeline timeline =
                new androidx.wear.tiles.TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(entry1)
                        .addTimelineEntry(entry2)
                        .build();

        TilesTimelineCache timelineCache = new TilesTimelineCache(timeline);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(0L), entry1);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry1, 0L))
                .isEqualTo(cutoverMillis);

        // 1m before cutover
        expectTimelineEntryEqual(
                timelineCache.findTimelineEntryForTime(
                        cutoverMillis - Duration.ofMinutes(1).toMillis()),
                entry1);

        // Cutover
        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(cutoverMillis), entry2);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry2, cutoverMillis))
                .isEqualTo(Long.MAX_VALUE);

        // 1m after
        expectTimelineEntryEqual(
                timelineCache.findTimelineEntryForTime(
                        cutoverMillis + Duration.ofMinutes(1).toMillis()),
                entry2);
    }

    @Test
    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    public void timelineCache_overlappingEntryWithDefault() {
        // Test that with a default, and an entry "on top", the entry is shown for its validity
        // period, and the default for all other times. As an example
        //              +---------------------+
        //              |         E1          |
        //  ...---------+---------------------+----------------...
        //                        default
        //  ...------------------------------------------------...
        //
        // Expected:
        // +------------+---------------------+------------------+
        // |  default   |          E1         |      default     |
        // +------------+---------------------+------------------+
        final long entry1StartMillis = Duration.ofMinutes(10).toMillis();
        final long entry1EndMillis = entry1StartMillis + Duration.ofMinutes(10).toMillis();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry defaultEntry =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("DefaultTile"))
                        .build();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry1 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Entry1"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(entry1StartMillis)
                                        .setEndMillis(entry1EndMillis)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.Timeline timeline =
                new androidx.wear.tiles.TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(entry1)
                        .addTimelineEntry(defaultEntry)
                        .build();

        TilesTimelineCache timelineCache = new TilesTimelineCache(timeline);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(0L), defaultEntry);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(defaultEntry, 0L))
                .isEqualTo(entry1StartMillis);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry1StartMillis), entry1);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry1, entry1StartMillis))
                .isEqualTo(entry1EndMillis);

        expectTimelineEntryEqual(
                timelineCache.findTimelineEntryForTime(entry1EndMillis), defaultEntry);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(defaultEntry, entry1EndMillis))
                .isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    public void timelineCache_testStackedEntries() {
        // Do a test with "perfectly stacked" entries, for example
        //            +-------+
        //            |   E3  |
        //       +----+-------+-------+
        //       |       E2           |
        //    +--+--------------------+--+
        //    |          E1              |
        //    +--------------------------+
        //
        // Expected:
        // +--+--+----+-------+-------+--+------+
        // |D |E1| E2 |  E3   |  E2   |E1|  Def |
        // +--+--+----+-------+-------+--+------+
        final long entry1StartMillis = Duration.ofMinutes(10).toMillis();
        final long entry1EndMillis =
                entry1StartMillis + Duration.ofMinutes(10).toMillis(); // Valid for 10 minutes

        final long entry2StartMillis = Duration.ofMinutes(12).toMillis();
        final long entry2EndMillis =
                entry2StartMillis + Duration.ofMinutes(6).toMillis(); // Valid for 6 minutes

        final long entry3StartMillis = Duration.ofMinutes(14).toMillis();
        final long entry3EndMillis =
                entry3StartMillis + Duration.ofMinutes(2).toMillis(); // Valid for 2 minutes

        androidx.wear.tiles.TimelineBuilders.TimelineEntry defaultEntry =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("DefaultTile"))
                        .build();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry1 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Entry1"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(entry1StartMillis)
                                        .setEndMillis(entry1EndMillis)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry2 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Entry2"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(entry2StartMillis)
                                        .setEndMillis(entry2EndMillis)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry3 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Entry3"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(entry3StartMillis)
                                        .setEndMillis(entry3EndMillis)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.Timeline timeline =
                new androidx.wear.tiles.TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(defaultEntry)
                        .addTimelineEntry(entry1)
                        .addTimelineEntry(entry2)
                        .addTimelineEntry(entry3)
                        .build();

        TilesTimelineCache timelineCache = new TilesTimelineCache(timeline);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(0L), defaultEntry);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(defaultEntry, 0L))
                .isEqualTo(entry1StartMillis);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry1StartMillis), entry1);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry1, entry1StartMillis))
                .isEqualTo(entry2StartMillis);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry2StartMillis), entry2);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry2, entry2StartMillis))
                .isEqualTo(entry3StartMillis);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry3StartMillis), entry3);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry3, entry3StartMillis))
                .isEqualTo(entry3EndMillis);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry3EndMillis), entry2);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry2, entry3EndMillis))
                .isEqualTo(entry2EndMillis);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry2EndMillis), entry1);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry1, entry2EndMillis))
                .isEqualTo(entry1EndMillis);

        expectTimelineEntryEqual(
                timelineCache.findTimelineEntryForTime(entry1EndMillis), defaultEntry);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(defaultEntry, entry1EndMillis))
                .isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    public void timelineCache_testStackedHangingEntries() {
        // Test with "hanging" entries, for example
        //                +--------------+
        //                |      E3      |
        //         +------+--------+-----+
        //         |       E2      |
        //    +----+---------------+-----------+
        //    |              E1                |
        //    +--------------------------------+
        //
        // Expected:
        // +--+----+------+--------------+-----+-------+
        // |D | E1 |  E2  |      E3      |  E1 |  Def  |
        // +--+----+------+--------------+-----+-------+
        final long entry1StartMillis = Duration.ofMinutes(10).toMillis();
        final long entry1EndMillis =
                entry1StartMillis + Duration.ofMinutes(10).toMillis(); // Valid for 10 minutes

        final long entry2StartMillis = Duration.ofMinutes(11).toMillis();
        final long entry2EndMillis =
                entry2StartMillis + Duration.ofMinutes(5).toMillis(); // Valid for 5 minutes

        final long entry3StartMillis = Duration.ofMinutes(14).toMillis();
        final long entry3EndMillis =
                entry3StartMillis + Duration.ofMinutes(4).toMillis(); // Valid for 4 minutes

        androidx.wear.tiles.TimelineBuilders.TimelineEntry defaultEntry =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("DefaultTile"))
                        .build();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry1 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Entry1"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(entry1StartMillis)
                                        .setEndMillis(entry1EndMillis)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry2 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Entry2"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(entry2StartMillis)
                                        .setEndMillis(entry2EndMillis)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry3 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Entry3"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(entry3StartMillis)
                                        .setEndMillis(entry3EndMillis)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.Timeline timeline =
                new androidx.wear.tiles.TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(defaultEntry)
                        .addTimelineEntry(entry1)
                        .addTimelineEntry(entry2)
                        .addTimelineEntry(entry3)
                        .build();

        TilesTimelineCache timelineCache = new TilesTimelineCache(timeline);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(0L), defaultEntry);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(defaultEntry, 0L))
                .isEqualTo(entry1StartMillis);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry1StartMillis), entry1);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry1, entry1StartMillis))
                .isEqualTo(entry2StartMillis);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry2StartMillis), entry2);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry2, entry2StartMillis))
                .isEqualTo(entry3StartMillis);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry3StartMillis), entry3);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry3, entry3StartMillis))
                .isEqualTo(entry3EndMillis);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry3EndMillis), entry1);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry1, entry3EndMillis))
                .isEqualTo(entry1EndMillis);

        expectTimelineEntryEqual(
                timelineCache.findTimelineEntryForTime(entry1EndMillis), defaultEntry);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(defaultEntry, entry1EndMillis))
                .isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    public void timelineCache_stackedEntriesShortestAlwaysWins() {
        // Test that if entries are stacked, the shortest entry always wins, not the "top". For
        // example:
        //             +----------------+
        //             |      E3        |
        //           +-+----+-----------+
        //           |  E2  |
        //     +-----+------+---------------------+
        //     |                E1                |
        //     +----------------------------------+
        // This one should go E1, then E2 for the whole period of E2, then E3, then E1
        //
        // Expected:
        // +---+-----+------+-----------+---------+-------+
        // | D | E1  |  E2  |    E3     |    E1   |  Def  |
        // +---+-----+------+-----------+---------+-------+
        final long entry1StartMillis = Duration.ofMinutes(10).toMillis();
        final long entry1EndMillis =
                entry1StartMillis + Duration.ofMinutes(10).toMillis(); // Valid for 10 minutes

        final long entry2StartMillis = Duration.ofMinutes(11).toMillis();
        final long entry2EndMillis =
                entry2StartMillis + Duration.ofMinutes(3).toMillis(); // Valid for 3 minutes

        final long entry3StartMillis = Duration.ofMinutes(12).toMillis();
        final long entry3EndMillis =
                entry3StartMillis + Duration.ofMinutes(6).toMillis(); // Valid for 6 minutes

        androidx.wear.tiles.TimelineBuilders.TimelineEntry defaultEntry =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("DefaultTile"))
                        .build();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry1 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Entry1"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(entry1StartMillis)
                                        .setEndMillis(entry1EndMillis)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry2 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Entry2"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(entry2StartMillis)
                                        .setEndMillis(entry2EndMillis)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry3 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Entry3"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(entry3StartMillis)
                                        .setEndMillis(entry3EndMillis)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.Timeline timeline =
                new androidx.wear.tiles.TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(defaultEntry)
                        .addTimelineEntry(entry1)
                        .addTimelineEntry(entry2)
                        .addTimelineEntry(entry3)
                        .build();

        TilesTimelineCache timelineCache = new TilesTimelineCache(timeline);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(0L), defaultEntry);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(defaultEntry, 0L))
                .isEqualTo(entry1StartMillis);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry1StartMillis), entry1);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry1, entry1StartMillis))
                .isEqualTo(entry2StartMillis);

        // Ending time of entry2 should be entry2End, as it's always the shortest
        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry2StartMillis), entry2);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry2, entry2StartMillis))
                .isEqualTo(entry2EndMillis);

        // At entry3start, entry2 is still the shortest valid one.
        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry3StartMillis), entry2);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry2, entry3StartMillis))
                .isEqualTo(entry2EndMillis);

        // Should now switch to entry3
        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry2EndMillis), entry3);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry3, entry2EndMillis))
                .isEqualTo(entry3EndMillis);

        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(entry3EndMillis), entry1);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry1, entry3EndMillis))
                .isEqualTo(entry1EndMillis);

        expectTimelineEntryEqual(
                timelineCache.findTimelineEntryForTime(entry1EndMillis), defaultEntry);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(defaultEntry, entry1EndMillis))
                .isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    public void timelineCache_noValidTilePicksClosest() {
        final long entry1StartMillis = Duration.ofMinutes(10).toMillis();
        final long entry1EndMillis =
                entry1StartMillis + Duration.ofMinutes(10).toMillis(); // 10 minutes
        final long entry2StartMillis = entry1EndMillis; // Immediate switchover
        final long entry2EndMillis =
                entry2StartMillis + Duration.ofMinutes(10).toMillis(); // 10 minutes

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry1 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Entry1"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(entry1StartMillis)
                                        .setEndMillis(entry1EndMillis)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.TimelineEntry entry2 =
                new androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(buildTextLayout("Entry2"))
                        .setValidity(
                                new androidx.wear.tiles.TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(entry2StartMillis)
                                        .setEndMillis(entry2EndMillis)
                                        .build())
                        .build();

        androidx.wear.tiles.TimelineBuilders.Timeline timeline =
                new androidx.wear.tiles.TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(entry1)
                        .addTimelineEntry(entry2)
                        .build();

        TilesTimelineCache timelineCache = new TilesTimelineCache(timeline);

        // This is really undefined behaviour at the moment, but, well, let's keep this as the
        // assumed behaviour for now. Should just pick entry1 in this case.
        expectTimelineEntryEqual(timelineCache.findTimelineEntryForTime(0L), null);
        expectTimelineEntryEqual(timelineCache.findClosestTimelineEntry(0L), entry1);
        expect.that(timelineCache.findCurrentTimelineEntryExpiry(entry1, 0L))
                .isEqualTo(entry1EndMillis);

        // And after the end, should pick entry2
        expectTimelineEntryEqual(
                timelineCache.findTimelineEntryForTime(
                        entry2EndMillis + Duration.ofMinutes(1).toMillis()),
                null);
        expectTimelineEntryEqual(
                timelineCache.findClosestTimelineEntry(
                        entry2EndMillis + Duration.ofMinutes(1).toMillis()),
                entry2);

        expect.that(
                        timelineCache.findCurrentTimelineEntryExpiry(
                                entry1, entry2EndMillis + Duration.ofMinutes(1).toMillis()))
                .isEqualTo(Long.MAX_VALUE);
    }

    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    private void expectTimelineEntryEqual(
            @Nullable androidx.wear.tiles.TimelineBuilders.TimelineEntry actual,
            @Nullable androidx.wear.tiles.TimelineBuilders.TimelineEntry expected) {
        if (expected == null) {
            expect.that(actual).isNull();
        } else {
            assertThat(actual).isNotNull();
            expect.that(actual.toProto()).isEqualTo(expected.toProto());
        }
    }

    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    private static androidx.wear.tiles.LayoutElementBuilders.Layout buildTextLayout(String text) {
        return new androidx.wear.tiles.LayoutElementBuilders.Layout.Builder()
                .setRoot(
                        new androidx.wear.tiles.LayoutElementBuilders.Text.Builder()
                                .setText(text)
                                .build())
                .build();
    }
}
