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
package android.support.media.tv;

import android.content.ContentValues;
import android.content.Intent;
import android.database.MatrixCursor;
import android.media.tv.TvContentRating;
import android.net.Uri;
import android.os.Build;
import android.support.test.filters.SmallTest;
import android.support.v4.os.BuildCompat;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * Tests that programs can be created using the Builder pattern and correctly obtain
 * values from them.
 */
@SmallTest
public class ProgramTest extends TestCase {
    @Test
    public void testEmptyProgram() {
        Program emptyProgram = new Program.Builder()
                .build();
        ContentValues contentValues = emptyProgram.toContentValues();
        compareProgram(emptyProgram,
                Program.fromCursor(getProgramCursor(Program.PROJECTION, contentValues)));
    }

    @Test
    public void testSampleProgram() {
        Program sampleProgram = new Program.Builder()
                .setTitle("Program Title")
                .setDescription("This is a sample program")
                .setChannelId(3)
                .setEpisodeNumber(5)
                .setSeasonNumber("The Final Season", 7)
                .setThumbnailUri(Uri.parse("http://www.example.com/programs/poster.png"))
                .setStartTimeUtcMillis(0)
                .setEndTimeUtcMillis(1000)
                .build();
        ContentValues contentValues = sampleProgram.toContentValues();
        compareProgram(sampleProgram,
                Program.fromCursor(getProgramCursor(Program.PROJECTION, contentValues)));

        Program clonedSampleProgram = new Program.Builder(sampleProgram).build();
        compareProgram(sampleProgram, clonedSampleProgram);
    }

    @Test
    public void testFullyPopulatedProgram() {
        Program fullyPopulatedProgram = new Program.Builder()
                .setSearchable(false)
                .setChannelId(3)
                .setThumbnailUri(Uri.parse("http://example.com/thumbnail.png"))
                .setAudioLanguages(new String [] {"eng", "kor"})
                .setBroadcastGenres(new String[] {"Music", "Family"})
                .setCanonicalGenres(new String[] {TvContractCompat.Programs.Genres.MOVIES})
                .setContentRatings(new TvContentRating[] {
                        TvContentRating.createRating("com.android.tv", "US_TV", "US_TV_Y7")})
                .setDescription("This is a sample program")
                .setEndTimeUtcMillis(1000)
                .setEpisodeNumber("Pilot", 0)
                .setEpisodeTitle("Hello World")
                .setLongDescription("This is a longer description than the previous description")
                .setPosterArtUri(Uri.parse("http://example.com/poster.png"))
                .setRecordingProhibited(false)
                .setSeasonNumber("The Final Season", 7)
                .setSeasonTitle("The Final Season")
                .setStartTimeUtcMillis(0)
                .setTitle("Google")
                .setVideoHeight(1080)
                .setVideoWidth(1920)
                .setInternalProviderId("ID-4321")
                .setPreviewVideoUri(Uri.parse("http://example.com/preview-video.mpg"))
                .setLastPlaybackPositionMillis(0)
                .setDurationMillis(60 * 1000)
                .setAppLinkIntentUri(Uri.parse(new Intent(Intent.ACTION_VIEW).toUri(
                        Intent.URI_INTENT_SCHEME)))
                .setWeight(100)
                .setInternalProviderFlag1(0x4)
                .setInternalProviderFlag2(0x3)
                .setInternalProviderFlag3(0x2)
                .setInternalProviderFlag4(0x1)
                .setTransient(false)
                .setType(TvContractCompat.Programs.TYPE_MOVIE)
                .setWatchNextType(TvContractCompat.Programs.WATCH_NEXT_TYPE_NEW)
                .setPosterArtAspectRatio(TvContractCompat.Programs.ASPECT_RATIO_2_3)
                .setThumbnailAspectRatio(TvContractCompat.Programs.ASPECT_RATIO_16_9)
                .setLogoUri(Uri.parse("http://example.com/program-logo.mpg"))
                .setAvailability(TvContractCompat.Programs.AVAILABILITY_AVAILABLE)
                .setStartingPrice("12.99 USD")
                .setOfferPrice("4.99 USD")
                .setReleaseDate("1997")
                .setItemCount(3)
                .setLive(false)
                .setInteractionType(TvContractCompat.Programs.INTERACTION_TYPE_LIKES)
                .setInteractionCount(10200)
                .setAuthor("author_name")
                .setReviewRatingStyle(TvContractCompat.Programs.REVIEW_RATING_STYLE_STARS)
                .setReviewRating("4.5")
                .build();

        ContentValues contentValues = fullyPopulatedProgram.toContentValues();
        compareProgram(fullyPopulatedProgram,
                Program.fromCursor(getProgramCursor(Program.PROJECTION, contentValues)));

        Program clonedFullyPopulatedProgram = new Program.Builder(fullyPopulatedProgram).build();
        compareProgram(fullyPopulatedProgram, clonedFullyPopulatedProgram);
    }

    @Test
    public void testPreviewProgram() {
        Program previewProgram = new Program.Builder()
                .setId(10)
                .setChannelId(3)
                .setTitle("Recommended Video 1")
                .setDescription("You should watch this!")
                .setPosterArtUri(Uri.parse("http://example.com/poster.png"))
                .setInternalProviderFlag2(0x0010010084108410L)
                .setInternalProviderId("ID-4321")
                .setPreviewVideoUri(Uri.parse("http://example.com/preview-video.mpg"))
                .setLastPlaybackPositionMillis(0)
                .setDurationMillis(60 * 1000)
                .setAppLinkIntentUri(Uri.parse(new Intent(Intent.ACTION_VIEW).toUri(
                        Intent.URI_INTENT_SCHEME)))
                .setWeight(100)
                .setTransient(false)
                .setType(TvContractCompat.Programs.TYPE_TV_EPISODE)
                .setWatchNextType(TvContractCompat.Programs.WATCH_NEXT_TYPE_CONTINUE)
                .setPosterArtAspectRatio(TvContractCompat.Programs.ASPECT_RATIO_3_2)
                .setThumbnailAspectRatio(TvContractCompat.Programs.ASPECT_RATIO_16_9)
                .setLogoUri(Uri.parse("http://example.com/program-logo.mpg"))
                .setAvailability(TvContractCompat.Programs.AVAILABILITY_FREE_WITH_SUBSCRIPTION)
                .setStartingPrice("9.99 USD")
                .setOfferPrice("3.99 USD")
                .setReleaseDate(new Date(97, 2, 8))
                .setLive(false)
                .setInteractionType(TvContractCompat.Programs.INTERACTION_TYPE_VIEWS)
                .setInteractionCount(99200)
                .setAuthor("author_name")
                .setReviewRatingStyle(TvContractCompat.Programs.REVIEW_RATING_STYLE_PERCENTAGE)
                .setReviewRating("83.9")
                .build();

        String[] partialProjection = {
                TvContractCompat.Programs._ID,
                TvContractCompat.Programs.COLUMN_CHANNEL_ID,
                TvContractCompat.Programs.COLUMN_TITLE,
                TvContractCompat.Programs.COLUMN_SHORT_DESCRIPTION,
                TvContractCompat.Programs.COLUMN_POSTER_ART_URI,
                TvContractCompat.Programs.COLUMN_INTERNAL_PROVIDER_FLAG2,
                TvContractCompat.Programs.COLUMN_INTERNAL_PROVIDER_ID,
                TvContractCompat.Programs.COLUMN_PREVIEW_VIDEO_URI,
                TvContractCompat.Programs.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
                TvContractCompat.Programs.COLUMN_DURATION_MILLIS,
                TvContractCompat.Programs.COLUMN_APP_LINK_INTENT_URI,
                TvContractCompat.Programs.COLUMN_WEIGHT,
                TvContractCompat.Programs.COLUMN_TRANSIENT,
                TvContractCompat.Programs.COLUMN_TYPE,
                TvContractCompat.Programs.COLUMN_WATCH_NEXT_TYPE,
                TvContractCompat.Programs.COLUMN_POSTER_ART_ASPECT_RATIO,
                TvContractCompat.Programs.COLUMN_THUMBNAIL_ASPECT_RATIO,
                TvContractCompat.Programs.COLUMN_LOGO_URI,
                TvContractCompat.Programs.COLUMN_AVAILABILITY,
                TvContractCompat.Programs.COLUMN_STARTING_PRICE,
                TvContractCompat.Programs.COLUMN_OFFER_PRICE,
                TvContractCompat.Programs.COLUMN_RELEASE_DATE,
                TvContractCompat.Programs.COLUMN_ITEM_COUNT,
                TvContractCompat.Programs.COLUMN_LIVE,
                TvContractCompat.Programs.COLUMN_INTERACTION_TYPE,
                TvContractCompat.Programs.COLUMN_INTERACTION_COUNT,
                TvContractCompat.Programs.COLUMN_AUTHOR,
                TvContractCompat.Programs.COLUMN_REVIEW_RATING_STYLE,
                TvContractCompat.Programs.COLUMN_REVIEW_RATING,
        };

        ContentValues contentValues = previewProgram.toContentValues();
        compareProgram(previewProgram,
                Program.fromCursor(getProgramCursor(partialProjection, contentValues)));

        Program clonedFullyPopulatedProgram = new Program.Builder(previewProgram).build();
        compareProgram(previewProgram, clonedFullyPopulatedProgram);
    }

    private static void compareProgram(Program programA, Program programB) {
        assertTrue(Arrays.equals(programA.getAudioLanguages(), programB.getAudioLanguages()));
        assertTrue(Arrays.deepEquals(programA.getBroadcastGenres(), programB.getBroadcastGenres()));
        assertTrue(Arrays.deepEquals(programA.getCanonicalGenres(), programB.getCanonicalGenres()));
        assertEquals(programA.getChannelId(), programB.getChannelId());
        assertTrue(Arrays.deepEquals(programA.getContentRatings(), programB.getContentRatings()));
        assertEquals(programA.getDescription(), programB.getDescription());
        assertEquals(programA.getEndTimeUtcMillis(), programB.getEndTimeUtcMillis());
        assertEquals(programA.getEpisodeNumber(), programB.getEpisodeNumber());
        assertEquals(programA.getEpisodeTitle(), programB.getEpisodeTitle());
        assertEquals(programA.getLongDescription(), programB.getLongDescription());
        assertEquals(programA.getPosterArtUri(), programB.getPosterArtUri());
        assertEquals(programA.getId(), programB.getId());
        assertEquals(programA.getSeasonNumber(), programB.getSeasonNumber());
        assertEquals(programA.getStartTimeUtcMillis(), programB.getStartTimeUtcMillis());
        assertEquals(programA.getThumbnailUri(), programB.getThumbnailUri());
        assertEquals(programA.getTitle(), programB.getTitle());
        assertEquals(programA.getVideoHeight(), programB.getVideoHeight());
        assertEquals(programA.getVideoWidth(), programB.getVideoWidth());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertEquals(programA.isSearchable(), programB.isSearchable());
            assertEquals(programA.getInternalProviderFlag1(), programB.getInternalProviderFlag1());
            assertEquals(programA.getInternalProviderFlag2(), programB.getInternalProviderFlag2());
            assertEquals(programA.getInternalProviderFlag3(), programB.getInternalProviderFlag3());
            assertEquals(programA.getInternalProviderFlag4(), programB.getInternalProviderFlag4());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            assertTrue(Objects.equals(programA.getSeasonTitle(), programB.getSeasonTitle()));
            assertTrue(Objects.equals(programA.isRecordingProhibited(),
                    programB.isRecordingProhibited()));
        }
        if (BuildCompat.isAtLeastO()) {
            assertEquals(programA.getInternalProviderId(), programB.getInternalProviderId());
            assertEquals(programA.getPreviewVideoUri(), programB.getPreviewVideoUri());
            assertEquals(programA.getLastPlaybackPositionMillis(),
                    programB.getLastPlaybackPositionMillis());
            assertEquals(programA.getDurationMillis(), programB.getDurationMillis());
            assertEquals(programA.getAppLinkIntentUri(), programB.getAppLinkIntentUri());
            assertEquals(programA.getWeight(), programB.getWeight());
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
        }
        assertEquals(programA.toContentValues(), programB.toContentValues());
        assertEquals(programA.toString(), programB.toString());
        assertEquals(programA, programB);
    }

    private static MatrixCursor getProgramCursor(String[] projection, ContentValues contentValues) {
        MatrixCursor cursor = new MatrixCursor(projection);
        MatrixCursor.RowBuilder builder = cursor.newRow();
        for (String row: projection) {
            if (row != null) {
                builder.add(row, contentValues.get(row));
            }
        }
        cursor.moveToFirst();
        return cursor;
    }
}
