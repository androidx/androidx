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
import android.database.MatrixCursor;
import android.media.tv.TvContentRating;
import android.os.Build;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.Arrays;
import java.util.Objects;

/**
 * Tests that programs can be created using the Builder pattern and correctly obtain
 * values from them.
 */
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
                .setThumbnailUri("http://www.example.com/programs/poster.png")
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
                .setThumbnailUri("http://example.com/thumbnail.png")
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
                .setPosterArtUri("http://example.com/poster.png")
                .setRecordingProhibited(false)
                .setSeasonNumber("The Final Season", 7)
                .setSeasonTitle("The Final Season")
                .setStartTimeUtcMillis(0)
                .setTitle("Google")
                .setVideoHeight(1080)
                .setVideoWidth(1920)
                .setInternalProviderId("ID-4321")
                .setPreviewVideoUri("http://example.com/preview-video.mpg")
                .setPreviewLastPlaybackPosition(0)
                .setPreviewDuration(60 * 1000)
                .setPreviewIntentUri("app_link_intent")
                .setPreviewWeight(100)
                .setInternalProviderFlag1(0x4)
                .setInternalProviderFlag2(0x3)
                .setInternalProviderFlag3(0x2)
                .setInternalProviderFlag4(0x1)
                .setTransient(false)
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
                .setPosterArtUri("http://example.com/poster.png")
                .setInternalProviderFlag2(0x0010010084108410L)
                .setInternalProviderId("ID-4321")
                .setPreviewVideoUri("http://example.com/preview-video.mpg")
                .setPreviewLastPlaybackPosition(0)
                .setPreviewDuration(60 * 1000)
                .setPreviewIntentUri("app_link_intent")
                .setPreviewWeight(100)
                .setTransient(false)
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
                TvContractCompat.Programs.COLUMN_PREVIEW_LAST_PLAYBACK_POSITION,
                TvContractCompat.Programs.COLUMN_PREVIEW_DURATION,
                TvContractCompat.Programs.COLUMN_PREVIEW_INTENT_URI,
                TvContractCompat.Programs.COLUMN_PREVIEW_WEIGHT,
                TvContractCompat.Programs.COLUMN_TRANSIENT,
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
