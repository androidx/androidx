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

package androidx.contentpager.content;

import static org.junit.Assert.assertTrue;

import android.database.Cursor;
import android.net.Uri;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.contentpager.content.ContentPager.ContentCallback;

import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class QueryTest {

    private static final Uri URI_HAMMY = Uri.parse("content://hammy");
    private static final Uri URI_CHEESY = Uri.parse("content://cheesy");

    private static final ContentCallback sCallback = new ContentCallback() {
        @Override
        public void onCursorReady(Query query, Cursor cursor) {
            // nothing to see here. Move along.
        }
    };

    @Test
    public void testDistinctIdsForDifferentUris() throws Throwable {
        Query queryA = new Query(
                URI_HAMMY,
                null,
                ContentPager.createArgs(0, 10),
                null,
                sCallback);

        Query queryB = new Query(
                URI_CHEESY,
                null,
                ContentPager.createArgs(0, 10),
                null,
                sCallback);

        assertDistinctIds(queryA, queryB);
    }

    @Test
    public void testDistinctIdsForDifferentPagingArgs() throws Throwable {
        Query queryA = new Query(
                URI_HAMMY,
                null,
                ContentPager.createArgs(0, 10),
                null,
                sCallback);

        Query queryB = new Query(
                URI_HAMMY,
                null,
                ContentPager.createArgs(10, 10),
                null,
                sCallback);

        assertDistinctIds(queryA, queryB);
    }

    private void assertDistinctIds(Query a, Query b) {
        String msg = String.format(
                "id A (%d) and id B (%d) are equal, but should not be.",
                a.getId(),
                b.getId());
        assertTrue(msg, a.getId() != b.getId());
    }
}
