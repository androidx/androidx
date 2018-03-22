/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.slice;

import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_TITLE;

import static androidx.slice.SliceMetadata.LOADED_ALL;
import static androidx.slice.SliceMetadata.LOADED_NONE;
import static androidx.slice.SliceMetadata.LOADED_PARTIAL;
import static androidx.slice.core.SliceHints.HINT_KEY_WORDS;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.slice.core.SliceHints;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link SliceMetadata}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SliceMetadataTest {

    private final Context mContext = InstrumentationRegistry.getContext();

    @Test
    public void testKeywords() {
        Uri uri = Uri.parse("content://pkg/slice");
        Slice keywordSlice = new Slice.Builder(uri)
                .addHints(HINT_KEY_WORDS)
                .addText("keyword1", null)
                .addText("keyword2", null)
                .addText("keyword3", null).build();
        Slice slice = new Slice.Builder(uri)
                .addText("Some text", null, HINT_TITLE)
                .addText("Some other text", null)
                .addSubSlice(keywordSlice)
                .build();

        SliceMetadata sliceInfo1 = SliceMetadata.from(mContext, slice);
        List<String> sliceKeywords = sliceInfo1.getSliceKeywords();
        String[] expectedList = new String[] {"keyword1", "keyword2", "keyword3"};
        assertArrayEquals(expectedList, sliceKeywords.toArray());

        // Make sure it doesn't find keywords that aren't there
        Slice slice2 = new Slice.Builder(uri)
                .addText("Some text", null, HINT_TITLE)
                .addText("Some other text", null).build();

        SliceMetadata sliceInfo2 = SliceMetadata.from(mContext, slice2);
        List<String> slice2Keywords = sliceInfo2.getSliceKeywords();
        assertNull(slice2Keywords);

        // Make sure empty list if specified to have no keywords
        Slice noKeywordSlice = new Slice.Builder(uri).addHints(HINT_KEY_WORDS).build();
        Slice slice3 = new Slice.Builder(uri)
                .addText("Some text", null, HINT_TITLE)
                .addSubSlice(noKeywordSlice)
                .build();

        SliceMetadata sliceMetadata3 = SliceMetadata.from(mContext, slice3);
        List<String> slice3Keywords = sliceMetadata3.getSliceKeywords();
        assertTrue(slice3Keywords.isEmpty());
    }

    @Test
    public void testGetLoadingState() {
        Uri uri = Uri.parse("content://pkg/slice");
        Slice s1 = new Slice.Builder(uri).build();
        SliceMetadata sliceInfo1 = SliceMetadata.from(mContext, s1);
        int actualState1 = sliceInfo1.getLoadingState();
        assertEquals(LOADED_NONE, actualState1);

        Slice s2 = new Slice.Builder(uri).addText(null, null, HINT_PARTIAL).build();
        SliceMetadata sliceInfo2 = SliceMetadata.from(mContext, s2);
        int actualState2 = sliceInfo2.getLoadingState();
        assertEquals(LOADED_PARTIAL, actualState2);

        Slice s3 = new Slice.Builder(uri).addText("Text", null).build();
        SliceMetadata sliceInfo3 = SliceMetadata.from(mContext, s3);
        int actualState3 = sliceInfo3.getLoadingState();
        assertEquals(LOADED_ALL, actualState3);
    }

    @Test
    public void testGetExpiry() {
        Uri uri = Uri.parse("content://pkg/slice");
        long timestamp = System.currentTimeMillis();
        long ttl = TimeUnit.DAYS.toMillis(1);
        Slice ttlSlice = new Slice.Builder(uri)
                .addText("Some text", null)
                .addTimestamp(timestamp, null)
                .addTimestamp(timestamp, null, SliceHints.HINT_LAST_UPDATED)
                .addTimestamp(ttl, null, SliceHints.HINT_TTL)
                .build();

        SliceMetadata si1 = SliceMetadata.from(mContext, ttlSlice);
        long retrievedTtl = si1.getExpiry();
        assertEquals(ttl, retrievedTtl);

        Slice noTtlSlice = new Slice.Builder(uri)
                .addText("Some text", null)
                .addTimestamp(timestamp, null).build();
        SliceMetadata si2 = SliceMetadata.from(mContext, noTtlSlice);
        long retrievedTtl2 = si2.getExpiry();
        assertEquals(0, retrievedTtl2);
    }

    @Test
    public void testGetLastUpdated() {
        Uri uri = Uri.parse("content://pkg/slice");
        long timestamp = System.currentTimeMillis();
        long ttl = TimeUnit.DAYS.toMillis(1);
        Slice ttlSlice = new Slice.Builder(uri)
                .addText("Some text", null)
                .addTimestamp(timestamp - 20, null)
                .addTimestamp(timestamp, null, SliceHints.HINT_LAST_UPDATED)
                .addTimestamp(ttl, null, SliceHints.HINT_TTL)
                .build();

        SliceMetadata si1 = SliceMetadata.from(mContext, ttlSlice);
        long retrievedLastUpdated = si1.getLastUpdatedTime();
        assertEquals(timestamp, retrievedLastUpdated);

        Slice noTtlSlice = new Slice.Builder(uri)
                .addText("Some text", null)
                .addTimestamp(timestamp, null).build();

        SliceMetadata si2 = SliceMetadata.from(mContext, noTtlSlice);
        long retrievedLastUpdated2 = si2.getLastUpdatedTime();
        assertEquals(0, retrievedLastUpdated2);
    }
}
