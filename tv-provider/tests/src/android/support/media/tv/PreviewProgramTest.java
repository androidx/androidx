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
import android.support.media.tv.TvContractCompat.PreviewPrograms;
import android.support.test.filters.SmallTest;
import android.support.v4.os.BuildCompat;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * Tests that preview programs can be created using the Builder pattern and correctly obtain
 * values from them.
 */
@SmallTest
public class PreviewProgramTest extends TestCase {
    @Test
    public void testEmptyPreviewProgram() {
        // TODO: Use @SdkSuppress once Build.VERSION_CODES.O has a right value.
        if (!BuildCompat.isAtLeastO()) {
            return;
        }
        PreviewProgram emptyProgram = new PreviewProgram.Builder().build();
        ContentValues contentValues = emptyProgram.toContentValues();
        compareProgram(emptyProgram,
                PreviewProgram.fromCursor(getProgramCursor(Program.PROJECTION, contentValues)));
    }

    @Test
    public void testSampleProgram() {
        if (!BuildCompat.isAtLeastO()) {
            return;
        }
        PreviewProgram sampleProgram = new PreviewProgram.Builder()
                .setTitle("Program Title")
                .setDescription("This is a sample program")
                .setEpisodeNumber(5)
                .setSeasonNumber("The Final Season", 7)
                .setThumbnailUri(Uri.parse("http://www.example.com/programs/poster.png"))
                .setChannelId(3)
                .setWeight(70)
                .build();
        ContentValues contentValues = sampleProgram.toContentValues();
        compareProgram(sampleProgram,
                PreviewProgram.fromCursor(
                        getProgramCursor(PreviewProgram.PROJECTION, contentValues)));

        PreviewProgram clonedSampleProgram = new PreviewProgram.Builder(sampleProgram).build();
        compareProgram(sampleProgram, clonedSampleProgram);
    }

    @Test
    public void testFullyPopulatedProgram() {
        if (!BuildCompat.isAtLeastO()) {
            return;
        }
        PreviewProgram fullyPopulatedProgram = new PreviewProgram.Builder()
                .setTitle("Google")
                .setInternalProviderId("ID-4321")
                .setChannelId(3)
                .setWeight(100)
                .setPreviewVideoUri(Uri.parse("http://example.com/preview-video.mpg"))
                .setLastPlaybackPositionMillis(0)
                .setDurationMillis(60 * 1000)
                .setAppLinkIntentUri(Uri.parse(new Intent(Intent.ACTION_VIEW).toUri(
                        Intent.URI_INTENT_SCHEME)))
                .setTransient(false)
                .setType(PreviewPrograms.TYPE_MOVIE)
                .setPosterArtAspectRatio(PreviewPrograms.ASPECT_RATIO_2_3)
                .setThumbnailAspectRatio(PreviewPrograms.ASPECT_RATIO_16_9)
                .setLogoUri(Uri.parse("http://example.com/program-logo.mpg"))
                .setAvailability(PreviewPrograms.AVAILABILITY_AVAILABLE)
                .setStartingPrice("12.99 USD")
                .setOfferPrice("4.99 USD")
                .setReleaseDate("1997")
                .setItemCount(3)
                .setLive(false)
                .setInteractionType(PreviewPrograms.INTERACTION_TYPE_LIKES)
                .setInteractionCount(10200)
                .setAuthor("author_name")
                .setReviewRatingStyle(PreviewPrograms.REVIEW_RATING_STYLE_STARS)
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
                .setContentId("CID-8642")
                .build();

        ContentValues contentValues = fullyPopulatedProgram.toContentValues();
        compareProgram(fullyPopulatedProgram,
                PreviewProgram.fromCursor(
                        getProgramCursor(PreviewProgram.PROJECTION, contentValues)));

        PreviewProgram clonedFullyPopulatedProgram =
                new PreviewProgram.Builder(fullyPopulatedProgram).build();
        compareProgram(fullyPopulatedProgram, clonedFullyPopulatedProgram);
    }

    @Test
    public void testPreviewProgramWithPartialData() {
        if (!BuildCompat.isAtLeastO()) {
            return;
        }
        PreviewProgram previewProgram = new PreviewProgram.Builder()
                .setChannelId(3)
                .setWeight(100)
                .setInternalProviderId("ID-4321")
                .setPreviewVideoUri(Uri.parse("http://example.com/preview-video.mpg"))
                .setLastPlaybackPositionMillis(0)
                .setDurationMillis(60 * 1000)
                .setAppLinkIntentUri(Uri.parse(new Intent(Intent.ACTION_VIEW).toUri(
                        Intent.URI_INTENT_SCHEME)))
                .setTransient(false)
                .setType(PreviewPrograms.TYPE_TV_EPISODE)
                .setPosterArtAspectRatio(PreviewPrograms.ASPECT_RATIO_3_2)
                .setThumbnailAspectRatio(PreviewPrograms.ASPECT_RATIO_16_9)
                .setLogoUri(Uri.parse("http://example.com/program-logo.mpg"))
                .setAvailability(PreviewPrograms.AVAILABILITY_FREE_WITH_SUBSCRIPTION)
                .setStartingPrice("9.99 USD")
                .setOfferPrice("3.99 USD")
                .setReleaseDate(new Date(97, 2, 8))
                .setLive(false)
                .setInteractionType(PreviewPrograms.INTERACTION_TYPE_VIEWS)
                .setInteractionCount(99200)
                .setAuthor("author_name")
                .setReviewRatingStyle(PreviewPrograms.REVIEW_RATING_STYLE_PERCENTAGE)
                .setReviewRating("83.9")
                .setId(10)
                .setTitle("Recommended Video 1")
                .setDescription("You should watch this!")
                .setPosterArtUri(Uri.parse("http://example.com/poster.png"))
                .setInternalProviderFlag2(0x0010010084108410L)
                .build();

        String[] partialProjection = {
                PreviewPrograms._ID,
                PreviewPrograms.COLUMN_CHANNEL_ID,
                PreviewPrograms.COLUMN_TITLE,
                PreviewPrograms.COLUMN_SHORT_DESCRIPTION,
                PreviewPrograms.COLUMN_POSTER_ART_URI,
                PreviewPrograms.COLUMN_INTERNAL_PROVIDER_FLAG2,
                PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID,
                PreviewPrograms.COLUMN_PREVIEW_VIDEO_URI,
                PreviewPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
                PreviewPrograms.COLUMN_DURATION_MILLIS,
                PreviewPrograms.COLUMN_APP_LINK_INTENT_URI,
                PreviewPrograms.COLUMN_WEIGHT,
                PreviewPrograms.COLUMN_TRANSIENT,
                PreviewPrograms.COLUMN_TYPE,
                PreviewPrograms.COLUMN_POSTER_ART_ASPECT_RATIO,
                PreviewPrograms.COLUMN_THUMBNAIL_ASPECT_RATIO,
                PreviewPrograms.COLUMN_LOGO_URI,
                PreviewPrograms.COLUMN_AVAILABILITY,
                PreviewPrograms.COLUMN_STARTING_PRICE,
                PreviewPrograms.COLUMN_OFFER_PRICE,
                PreviewPrograms.COLUMN_RELEASE_DATE,
                PreviewPrograms.COLUMN_ITEM_COUNT,
                PreviewPrograms.COLUMN_LIVE,
                PreviewPrograms.COLUMN_INTERACTION_TYPE,
                PreviewPrograms.COLUMN_INTERACTION_COUNT,
                PreviewPrograms.COLUMN_AUTHOR,
                PreviewPrograms.COLUMN_REVIEW_RATING_STYLE,
                PreviewPrograms.COLUMN_REVIEW_RATING,
        };

        ContentValues contentValues = previewProgram.toContentValues();
        compareProgram(previewProgram,
                PreviewProgram.fromCursor(getProgramCursor(partialProjection, contentValues)));

        PreviewProgram clonedFullyPopulatedProgram =
                new PreviewProgram.Builder(previewProgram).build();
        compareProgram(previewProgram, clonedFullyPopulatedProgram);
    }

    private static void compareProgram(PreviewProgram programA, PreviewProgram programB) {
        assertTrue(Arrays.equals(programA.getAudioLanguages(), programB.getAudioLanguages()));
        assertTrue(Arrays.deepEquals(programA.getCanonicalGenres(), programB.getCanonicalGenres()));
        assertEquals(programA.getChannelId(), programB.getChannelId());
        assertTrue(Arrays.deepEquals(programA.getContentRatings(), programB.getContentRatings()));
        assertEquals(programA.getDescription(), programB.getDescription());
        assertEquals(programA.getEpisodeNumber(), programB.getEpisodeNumber());
        assertEquals(programA.getEpisodeTitle(), programB.getEpisodeTitle());
        assertEquals(programA.getLongDescription(), programB.getLongDescription());
        assertEquals(programA.getPosterArtUri(), programB.getPosterArtUri());
        assertEquals(programA.getId(), programB.getId());
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
        assertEquals(programA.getAppLinkIntentUri(), programB.getAppLinkIntentUri());
        assertEquals(programA.getWeight(), programB.getWeight());
        assertEquals(programA.isTransient(), programB.isTransient());
        assertEquals(programA.getType(), programB.getType());
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
        assertEquals(programA.isBrowsable(), programB.isBrowsable());
        assertEquals(programA.getContentId(), programB.getContentId());

        assertEquals(programA.toContentValues(), programB.toContentValues());
        assertEquals(programA.toString(), programB.toString());
        assertEquals(programA, programB);
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
