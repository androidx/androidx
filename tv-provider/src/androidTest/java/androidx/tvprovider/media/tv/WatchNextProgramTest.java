/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.tvprovider.media.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.media.tv.TvContentRating;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.tvprovider.media.tv.TvContractCompat.WatchNextPrograms;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * Tests that watch next programs can be created using the Builder pattern and correctly obtain
 * values from them.
 */
@SmallTest
@SdkSuppress(minSdkVersion = 26)
@RunWith(AndroidJUnit4.class)
public class WatchNextProgramTest {

    @Before
    public void tearDown() {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        ContentResolver resolver = InstrumentationRegistry.getContext().getContentResolver();
        resolver.delete(WatchNextPrograms.CONTENT_URI, null, null);
    }

    @Test
    public void testEmptyPreviewProgram() {
        WatchNextProgram emptyProgram = new WatchNextProgram.Builder().build();
        ContentValues contentValues = emptyProgram.toContentValues(true);
        compareProgram(emptyProgram,
                WatchNextProgram.fromCursor(getProgramCursor(Program.PROJECTION, contentValues)),
                true);
    }

    @Test
    public void testSampleProgram() {
        WatchNextProgram sampleProgram = new WatchNextProgram.Builder()
                .setTitle("Program Title")
                .setDescription("This is a sample program")
                .setEpisodeNumber(5)
                .setSeasonNumber("The Final Season", 7)
                .setThumbnailUri(Uri.parse("http://www.example.com/programs/poster.png"))
                .build();
        ContentValues contentValues = sampleProgram.toContentValues(true);
        compareProgram(sampleProgram,
                WatchNextProgram.fromCursor(
                        getProgramCursor(WatchNextProgram.PROJECTION, contentValues)), true);

        WatchNextProgram clonedSampleProgram = new WatchNextProgram.Builder(sampleProgram).build();
        compareProgram(sampleProgram, clonedSampleProgram, true);
    }

    @Test
    public void testFullyPopulatedProgram() {
        WatchNextProgram fullyPopulatedProgram = createFullyPopulatedWatchNextProgram();
        ContentValues contentValues = fullyPopulatedProgram.toContentValues(true);
        compareProgram(fullyPopulatedProgram,
                WatchNextProgram.fromCursor(
                        getProgramCursor(WatchNextProgram.PROJECTION, contentValues)), true);

        WatchNextProgram clonedFullyPopulatedProgram =
                new WatchNextProgram.Builder(fullyPopulatedProgram).build();
        compareProgram(fullyPopulatedProgram, clonedFullyPopulatedProgram, true);
    }

    @Test
    public void testChannelWithSystemContentProvider() {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }
        WatchNextProgram fullyPopulatedProgram = createFullyPopulatedWatchNextProgram();
        ContentResolver resolver = InstrumentationRegistry.getContext().getContentResolver();
        Uri watchNextProgramUri = resolver.insert(WatchNextPrograms.CONTENT_URI,
                fullyPopulatedProgram.toContentValues());

        WatchNextProgram programFromSystemDb =
                loadWatchNextProgramFromContentProvider(resolver, watchNextProgramUri);
        compareProgram(fullyPopulatedProgram, programFromSystemDb, false);
    }

    @Test
    public void testWatchNextProgramUpdateWithContentProvider() {
        if (!Utils.hasTvInputFramework(InstrumentationRegistry.getContext())) {
            return;
        }

        WatchNextProgram fullyPopulatedProgram = createFullyPopulatedWatchNextProgram();
        ContentResolver resolver = InstrumentationRegistry.getContext().getContentResolver();
        Uri watchNextProgramUri = resolver.insert(WatchNextPrograms.CONTENT_URI,
                fullyPopulatedProgram.toContentValues());

        WatchNextProgram programFromSystemDb =
                loadWatchNextProgramFromContentProvider(resolver, watchNextProgramUri);
        compareProgram(fullyPopulatedProgram, programFromSystemDb, false);

        // Update a field from a fully loaded watch-next program.
        WatchNextProgram updatedProgram = new WatchNextProgram.Builder(programFromSystemDb)
                .setInteractionCount(programFromSystemDb.getInteractionCount() + 1).build();
        assertEquals(1, resolver.update(
                watchNextProgramUri, updatedProgram.toContentValues(), null, null));
        programFromSystemDb =
                loadWatchNextProgramFromContentProvider(resolver, watchNextProgramUri);
        compareProgram(updatedProgram, programFromSystemDb, false);

        // Update a field with null from a fully loaded watch-next program.
        updatedProgram = new WatchNextProgram.Builder(updatedProgram)
                .setPreviewVideoUri(null).build();
        assertEquals(1, resolver.update(
                watchNextProgramUri, updatedProgram.toContentValues(), null, null));
        programFromSystemDb = loadWatchNextProgramFromContentProvider(
                resolver, watchNextProgramUri);
        compareProgram(updatedProgram, programFromSystemDb, false);

        // Update a field without referencing fully watch-next program.
        ContentValues values = new PreviewProgram.Builder().setInteractionCount(1).build()
                .toContentValues();
        assertEquals(1, values.size());
        assertEquals(1, resolver.update(watchNextProgramUri, values, null, null));
        programFromSystemDb = loadWatchNextProgramFromContentProvider(
                resolver, watchNextProgramUri);
        WatchNextProgram expectedProgram = new WatchNextProgram.Builder(programFromSystemDb)
                .setInteractionCount(1).build();
        compareProgram(expectedProgram, programFromSystemDb, false);
    }

    @Test
    public void testWatchNextProgramEquals() {
        assertEquals(createFullyPopulatedWatchNextProgram(),
                createFullyPopulatedWatchNextProgram());
    }

    private static WatchNextProgram loadWatchNextProgramFromContentProvider(
            ContentResolver resolver, Uri watchNextProgramUri) {
        try (Cursor cursor = resolver.query(watchNextProgramUri, null, null, null, null)) {
            assertNotNull(cursor);
            assertEquals(1, cursor.getCount());
            cursor.moveToNext();
            return WatchNextProgram.fromCursor(cursor);
        }
    }

    @Test
    public void testWatchNextProgramWithPartialData() {
        WatchNextProgram previewProgram = new WatchNextProgram.Builder()
                .setInternalProviderId("ID-4321")
                .setPreviewVideoUri(Uri.parse("http://example.com/preview-video.mpg"))
                .setLastPlaybackPositionMillis(0)
                .setDurationMillis(60 * 1000)
                .setIntentUri(Uri.parse(new Intent(Intent.ACTION_VIEW).toUri(
                        Intent.URI_INTENT_SCHEME)))
                .setTransient(false)
                .setType(WatchNextPrograms.TYPE_TV_EPISODE)
                .setPosterArtAspectRatio(WatchNextPrograms.ASPECT_RATIO_3_2)
                .setThumbnailAspectRatio(WatchNextPrograms.ASPECT_RATIO_16_9)
                .setLogoUri(Uri.parse("http://example.com/program-logo.mpg"))
                .setAvailability(WatchNextPrograms.AVAILABILITY_FREE_WITH_SUBSCRIPTION)
                .setStartingPrice("9.99 USD")
                .setOfferPrice("3.99 USD")
                .setReleaseDate(new Date(97, 2, 8))
                .setLive(false)
                .setInteractionType(WatchNextPrograms.INTERACTION_TYPE_VIEWS)
                .setInteractionCount(99200)
                .setAuthor("author_name")
                .setReviewRatingStyle(WatchNextPrograms.REVIEW_RATING_STYLE_PERCENTAGE)
                .setReviewRating("83.9")
                .setId(10)
                .setTitle("Recommended Video 1")
                .setDescription("You should watch this!")
                .setPosterArtUri(Uri.parse("http://example.com/poster.png"))
                .setInternalProviderFlag2(0x0010010084108410L)
                .build();

        String[] partialProjection = {
                WatchNextPrograms._ID,
                WatchNextPrograms.COLUMN_TITLE,
                WatchNextPrograms.COLUMN_SHORT_DESCRIPTION,
                WatchNextPrograms.COLUMN_POSTER_ART_URI,
                WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_FLAG2,
                WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID,
                WatchNextPrograms.COLUMN_PREVIEW_VIDEO_URI,
                WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
                WatchNextPrograms.COLUMN_DURATION_MILLIS,
                WatchNextPrograms.COLUMN_INTENT_URI,
                WatchNextPrograms.COLUMN_TRANSIENT,
                WatchNextPrograms.COLUMN_TYPE,
                WatchNextPrograms.COLUMN_POSTER_ART_ASPECT_RATIO,
                WatchNextPrograms.COLUMN_THUMBNAIL_ASPECT_RATIO,
                WatchNextPrograms.COLUMN_LOGO_URI,
                WatchNextPrograms.COLUMN_AVAILABILITY,
                WatchNextPrograms.COLUMN_STARTING_PRICE,
                WatchNextPrograms.COLUMN_OFFER_PRICE,
                WatchNextPrograms.COLUMN_RELEASE_DATE,
                WatchNextPrograms.COLUMN_ITEM_COUNT,
                WatchNextPrograms.COLUMN_LIVE,
                WatchNextPrograms.COLUMN_INTERACTION_TYPE,
                WatchNextPrograms.COLUMN_INTERACTION_COUNT,
                WatchNextPrograms.COLUMN_AUTHOR,
                WatchNextPrograms.COLUMN_REVIEW_RATING_STYLE,
                WatchNextPrograms.COLUMN_REVIEW_RATING,
        };

        ContentValues contentValues = previewProgram.toContentValues(true);
        compareProgram(previewProgram,
                WatchNextProgram.fromCursor(getProgramCursor(partialProjection, contentValues)),
                true);

        WatchNextProgram clonedFullyPopulatedProgram =
                new WatchNextProgram.Builder(previewProgram).build();
        compareProgram(previewProgram, clonedFullyPopulatedProgram, true);
    }

    private static WatchNextProgram createFullyPopulatedWatchNextProgram() {
        return new WatchNextProgram.Builder()
                .setTitle("Google")
                .setInternalProviderId("ID-4321")
                .setPreviewVideoUri(Uri.parse("http://example.com/preview-video.mpg"))
                .setLastPlaybackPositionMillis(0)
                .setDurationMillis(60 * 1000)
                .setIntentUri(Uri.parse(new Intent(Intent.ACTION_VIEW).toUri(
                        Intent.URI_INTENT_SCHEME)))
                .setTransient(false)
                .setType(WatchNextPrograms.TYPE_MOVIE)
                .setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                .setPosterArtAspectRatio(WatchNextPrograms.ASPECT_RATIO_2_3)
                .setThumbnailAspectRatio(WatchNextPrograms.ASPECT_RATIO_16_9)
                .setLogoUri(Uri.parse("http://example.com/program-logo.mpg"))
                .setAvailability(WatchNextPrograms.AVAILABILITY_AVAILABLE)
                .setStartingPrice("12.99 USD")
                .setOfferPrice("4.99 USD")
                .setReleaseDate("1997")
                .setItemCount(3)
                .setLive(false)
                .setInteractionType(WatchNextPrograms.INTERACTION_TYPE_LIKES)
                .setInteractionCount(10200)
                .setAuthor("author_name")
                .setReviewRatingStyle(WatchNextPrograms.REVIEW_RATING_STYLE_STARS)
                .setReviewRating("4.5")
                .setSearchable(false)
                .setThumbnailUri(Uri.parse("http://example.com/thumbnail.png"))
                .setAudioLanguages(new String [] {"eng", "kor"})
                .setCanonicalGenres(new String[] {TvContractCompat.Programs.Genres.MOVIES})
                .setContentRatings(new TvContentRating[] {
                        TvContentRating.createRating("com.android.tv", "US_TV", "US_TV_Y7")})
                .setDescription("This is a sample program")
                .setEpisodeNumber("Pilot", 0)
                .setEpisodeTitle("Hello World")
                .setLongDescription("This is a longer description than the previous description")
                .setPosterArtUri(Uri.parse("http://example.com/poster.png"))
                .setSeasonNumber("The Final Season", 7)
                .setSeasonTitle("The Final Season")
                .setVideoHeight(1080)
                .setVideoWidth(1920)
                .setInternalProviderFlag1(0x4)
                .setInternalProviderFlag2(0x3)
                .setInternalProviderFlag3(0x2)
                .setInternalProviderFlag4(0x1)
                .setBrowsable(true)
                .setContentId("CID-8442")
                .build();
    }

    private static void compareProgram(WatchNextProgram programA, WatchNextProgram programB,
            boolean includeIdAndProtectedFields) {
        assertTrue(Arrays.equals(programA.getAudioLanguages(), programB.getAudioLanguages()));
        assertTrue(Arrays.deepEquals(programA.getCanonicalGenres(), programB.getCanonicalGenres()));
        assertTrue(Arrays.deepEquals(programA.getContentRatings(), programB.getContentRatings()));
        assertEquals(programA.getDescription(), programB.getDescription());
        assertEquals(programA.getEpisodeNumber(), programB.getEpisodeNumber());
        assertEquals(programA.getEpisodeTitle(), programB.getEpisodeTitle());
        assertEquals(programA.getLongDescription(), programB.getLongDescription());
        assertEquals(programA.getPosterArtUri(), programB.getPosterArtUri());
        assertEquals(programA.getSeasonNumber(), programB.getSeasonNumber());
        assertEquals(programA.getThumbnailUri(), programB.getThumbnailUri());
        assertEquals(programA.getTitle(), programB.getTitle());
        assertEquals(programA.getVideoHeight(), programB.getVideoHeight());
        assertEquals(programA.getVideoWidth(), programB.getVideoWidth());
        assertEquals(programA.isSearchable(), programB.isSearchable());
        assertEquals(programA.getInternalProviderFlag1(), programB.getInternalProviderFlag1());
        assertEquals(programA.getInternalProviderFlag2(), programB.getInternalProviderFlag2());
        assertEquals(programA.getInternalProviderFlag3(), programB.getInternalProviderFlag3());
        assertEquals(programA.getInternalProviderFlag4(), programB.getInternalProviderFlag4());
        assertTrue(Objects.equals(programA.getSeasonTitle(), programB.getSeasonTitle()));
        assertEquals(programA.getInternalProviderId(), programB.getInternalProviderId());
        assertEquals(programA.getPreviewVideoUri(), programB.getPreviewVideoUri());
        assertEquals(programA.getLastPlaybackPositionMillis(),
                programB.getLastPlaybackPositionMillis());
        assertEquals(programA.getDurationMillis(), programB.getDurationMillis());
        assertEquals(programA.getIntentUri(), programB.getIntentUri());
        assertEquals(programA.isTransient(), programB.isTransient());
        assertEquals(programA.getType(), programB.getType());
        assertEquals(programA.getWatchNextType(), programB.getWatchNextType());
        assertEquals(programA.getPosterArtAspectRatio(), programB.getPosterArtAspectRatio());
        assertEquals(programA.getThumbnailAspectRatio(), programB.getThumbnailAspectRatio());
        assertEquals(programA.getLogoUri(), programB.getLogoUri());
        assertEquals(programA.getAvailability(), programB.getAvailability());
        assertEquals(programA.getStartingPrice(), programB.getStartingPrice());
        assertEquals(programA.getOfferPrice(), programB.getOfferPrice());
        assertEquals(programA.getReleaseDate(), programB.getReleaseDate());
        assertEquals(programA.getItemCount(), programB.getItemCount());
        assertEquals(programA.isLive(), programB.isLive());
        assertEquals(programA.getInteractionType(), programB.getInteractionType());
        assertEquals(programA.getInteractionCount(), programB.getInteractionCount());
        assertEquals(programA.getAuthor(), programB.getAuthor());
        assertEquals(programA.getReviewRatingStyle(), programB.getReviewRatingStyle());
        assertEquals(programA.getReviewRating(), programB.getReviewRating());
        assertEquals(programA.getContentId(), programB.getContentId());
        if (includeIdAndProtectedFields) {
            // Skip row ID since the one from system DB has the valid ID while the other does not.
            assertEquals(programA.getId(), programB.getId());
            // When we insert a channel using toContentValues() to the system, we drop some
            // protected fields since they only can be modified by system apps.
            assertEquals(programA.isBrowsable(), programB.isBrowsable());
            assertEquals(programA.toContentValues(), programB.toContentValues());
            assertEquals(programA, programB);
        }
    }

    private static MatrixCursor getProgramCursor(String[] projection, ContentValues contentValues) {
        MatrixCursor cursor = new MatrixCursor(projection);
        MatrixCursor.RowBuilder builder = cursor.newRow();
        for (String col : projection) {
            if (col != null) {
                builder.add(col, contentValues.get(col));
            }
        }
        cursor.moveToFirst();
        return cursor;
    }
}
