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

import static androidx.contentpager.content.ContentPager.createArgs;
import static androidx.contentpager.content.TestContentProvider.PAGED_URI;
import static androidx.contentpager.content.TestContentProvider.PAGED_WINDOWED_URI;
import static androidx.contentpager.content.TestContentProvider.UNPAGED_URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.Nullable;
import androidx.contentpager.content.ContentPager.ContentCallback;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ContentPagerTest {

    private ContentResolver mResolver;
    private TestQueryRunner mRunner;
    private TestContentCallback mCallback;
    private ContentPager mPager;

    @Rule
    public ActivityTestRule<Activity> mActivityRule = new ActivityTestRule(TestActivity.class);

    @Before
    public void setUp() {
        mRunner = new TestQueryRunner();
        mResolver = mActivityRule.getActivity().getContentResolver();
        mCallback = new TestContentCallback();
        mPager = new ContentPager(mResolver, mRunner);
    }

    @Test
    public void testRelaysProviderPagedResults() throws Throwable {
        int offset = 0;
        int limit = 10;

        // NOTE: Paging on Android O is accompolished by way of ContentResolver#query that
        // accepts a Bundle. That means on older platforms we either have to cook up
        // a special way of paging that doesn't use the bundle (that's what we do here)
        // or we simply skip testing how we deal with provider paged results.
        Uri uriWithTestPagingData = TestContentProvider.forcePagingSpec(PAGED_URI, offset, limit);

        Query query = mPager.query(
                uriWithTestPagingData,
                null,
                createArgs(offset, limit),
                null,
                mCallback);

        mCallback.assertNumPagesLoaded(1);
        mCallback.assertPageLoaded(query);
        Cursor cursor = mCallback.getCursor(query);
        Bundle extras = cursor.getExtras();

        assertExpectedRecords(cursor, query.getOffset());

        assertEquals(
                ContentPager.CURSOR_DISPOSITION_PAGED,
                extras.getInt(ContentPager.CURSOR_DISPOSITION, -1));

        assertEquals(
                TestContentProvider.DEFAULT_RECORD_COUNT,
                extras.getInt(ContentResolver.EXTRA_TOTAL_COUNT));

        assertHasHonoredArgs(
                extras,
                ContentResolver.QUERY_ARG_LIMIT,
                ContentResolver.QUERY_ARG_OFFSET);

        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_PROVIDER_PAGED));
        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_RESOLVED_QUERIES));
        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_TOTAL_QUERIES));
    }

    @Test
    public void testLimitsPagedResultsToWindowSize() throws Throwable {
        int offset = 0;
        int limit = 10;

        // NOTE: Paging on Android O is accompolished by way of ContentResolver#query that
        // accepts a Bundle. That means on older platforms we either have to cook up
        // a special way of paging that doesn't use the bundle (that's what we do here)
        // or we simply skip testing how we deal with provider paged results.
        Uri uriWithTestPagingData = TestContentProvider.forcePagingSpec(
                PAGED_WINDOWED_URI, offset, limit);

        Query query = mPager.query(
                uriWithTestPagingData,
                null,
                createArgs(offset, limit),
                null,
                mCallback);

        mCallback.assertNumPagesLoaded(1);
        mCallback.assertPageLoaded(query);
        Cursor cursor = mCallback.getCursor(query);
        Bundle extras = cursor.getExtras();

        assertExpectedRecords(cursor, query.getOffset());

        assertEquals(
                ContentPager.CURSOR_DISPOSITION_REPAGED,
                extras.getInt(ContentPager.CURSOR_DISPOSITION, -1));

        assertEquals(
                TestContentProvider.DEFAULT_RECORD_COUNT,
                extras.getInt(ContentResolver.EXTRA_TOTAL_COUNT));


        assertEquals(limit, extras.getInt(ContentPager.EXTRA_REQUESTED_LIMIT));

        assertEquals(7, extras.getInt(ContentPager.EXTRA_SUGGESTED_LIMIT));

        assertHasHonoredArgs(
                extras,
                ContentResolver.QUERY_ARG_LIMIT,
                ContentResolver.QUERY_ARG_OFFSET);

        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_PROVIDER_PAGED));
        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_RESOLVED_QUERIES));
        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_TOTAL_QUERIES));
    }

    @Test
    public void testAdaptsUnpagedToPaged() throws Throwable {
        Query query = mPager.query(
                UNPAGED_URI,
                null,
                createArgs(0, 10),
                null,
                mCallback);

        mCallback.assertNumPagesLoaded(1);
        mCallback.assertPageLoaded(query);
        Cursor cursor = mCallback.getCursor(query);
        Bundle extras = cursor.getExtras();

        assertExpectedRecords(cursor, query.getOffset());

        assertEquals(
                ContentPager.CURSOR_DISPOSITION_COPIED,
                extras.getInt(ContentPager.CURSOR_DISPOSITION));

        assertEquals(
                TestContentProvider.DEFAULT_RECORD_COUNT,
                extras.getInt(ContentResolver.EXTRA_TOTAL_COUNT));

        assertHasHonoredArgs(
                extras,
                ContentResolver.QUERY_ARG_LIMIT,
                ContentResolver.QUERY_ARG_OFFSET);

        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_COMPAT_PAGED));
        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_RESOLVED_QUERIES));
        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_TOTAL_QUERIES));
    }

    @Test
    public void testCachesUnpagedCursor() throws Throwable {
        mPager.query(
                UNPAGED_URI,
                null,
                createArgs(0, 10),
                null,
                mCallback);

        mPager.query(
                UNPAGED_URI,
                null,
                createArgs(10, 10),
                null,
                mCallback);

        // Rerun the same query as the first...extra exercise to ensure we can return
        // to previously loaded results.
        Query query = mPager.query(
                UNPAGED_URI,
                null,
                createArgs(0, 10),
                null,
                mCallback);

        mCallback.assertNumPagesLoaded(3);
        Cursor cursor = mCallback.getCursor(query);
        Bundle extras = cursor.getExtras();

        assertEquals(
                3,
                extras.getInt(ContentPager.Stats.EXTRA_COMPAT_PAGED));
        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_RESOLVED_QUERIES));
        assertEquals(
                3,
                extras.getInt(ContentPager.Stats.EXTRA_TOTAL_QUERIES));
    }

    @Test
    public void testWrapsCursorsThatJustHappenToFitInPageRange() throws Throwable {

        // NOTE: Paging on Android O is accompolished by way of ContentResolver#query that
        // accepts a Bundle. That means on older platforms we either have to cook up
        // a special way of paging that doesn't use the bundle (that's what we do here)
        // or we simply skip testing how we deal with provider paged results.
        Uri uri = TestContentProvider.forceRecordCount(UNPAGED_URI, 22);

        Query query = mPager.query(
                uri,
                null,
                createArgs(0, 44),
                null,
                mCallback);

        mCallback.assertNumPagesLoaded(1);
        // mCallback.assertPageLoaded(pageId);
        mCallback.assertPageLoaded(query);
        Cursor cursor = mCallback.getCursor(query);
        Bundle extras = cursor.getExtras();

        assertExpectedRecords(cursor, query.getOffset());

        assertEquals(
                ContentPager.CURSOR_DISPOSITION_WRAPPED,
                extras.getInt(ContentPager.CURSOR_DISPOSITION));

        assertEquals(
                22,
                extras.getInt(ContentResolver.EXTRA_TOTAL_COUNT));

        assertHasHonoredArgs(
                extras,
                ContentResolver.QUERY_ARG_LIMIT,
                ContentResolver.QUERY_ARG_OFFSET);

        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_COMPAT_PAGED));
        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_RESOLVED_QUERIES));
        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_TOTAL_QUERIES));
    }

    @Test
    public void testCorrectlyCopiesRecords_EndOfResults() throws Throwable {
        // finally, check the last page.
        int limit = 100;
        // This will be the size of the last page. Should be 67.
        int leftOvers = TestContentProvider.DEFAULT_RECORD_COUNT % limit;
        int offset = TestContentProvider.DEFAULT_RECORD_COUNT - leftOvers;

        Query query = mPager.query(
                UNPAGED_URI,
                null,
                createArgs(offset, limit),
                null,
                mCallback);

        mCallback.assertNumPagesLoaded(1);
        mCallback.assertPageLoaded(query);
        Cursor cursor = mCallback.getCursor(query);
        assertEquals(leftOvers, cursor.getCount());
        Bundle extras = cursor.getExtras();

        assertExpectedRecords(cursor, query.getOffset());

        assertEquals(
                ContentPager.CURSOR_DISPOSITION_COPIED,
                extras.getInt(ContentPager.CURSOR_DISPOSITION));

        assertHasHonoredArgs(
                extras,
                ContentResolver.QUERY_ARG_LIMIT,
                ContentResolver.QUERY_ARG_OFFSET);

        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_COMPAT_PAGED));
        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_RESOLVED_QUERIES));
        assertEquals(
                1,
                extras.getInt(ContentPager.Stats.EXTRA_TOTAL_QUERIES));
    }

    @Test
    public void testCancelsRunningQueriesOnReset() throws Throwable {
        mRunner.runQuery = false;
        Query query = mPager.query(
                UNPAGED_URI,
                null,
                createArgs(0, 10),
                null,
                mCallback);

        assertTrue(mRunner.isRunning(query));

        mPager.reset();

        assertFalse(mRunner.isRunning(query));
    }

    @Test
    public void testRelaysContentChangeNotificationsOnPagedCursors() throws Throwable {

        TestContentObserver observer = new TestContentObserver(
                new Handler(Looper.getMainLooper()));
        observer.expectNotifications(1);

        Query query = mPager.query(
                UNPAGED_URI,
                null,
                createArgs(10, 99),
                null,
                mCallback);

        Cursor cursor = mCallback.getCursor(query);
        cursor.registerContentObserver(observer);

        mResolver.notifyChange(UNPAGED_URI, null);

        assertTrue(observer.mNotifiedLatch.await(1000, TimeUnit.MILLISECONDS));
    }

    private void assertExpectedRecords(Cursor cursor, int offset) {
        for (int row = 0; row < cursor.getCount(); row++) {
            assertTrue(cursor.moveToPosition(row));
            int unpagedRow = offset + row;
            for (int column = 0; column < cursor.getColumnCount(); column++) {
                TestContentProvider.assertExpectedCellValue(cursor, unpagedRow, column);
            }
        }
    }

    private static void assertHasHonoredArgs(Bundle extras, String... expectedArgs) {
        List<String> honored = Arrays.asList(
                extras.getStringArray(ContentResolver.EXTRA_HONORED_ARGS));

        for (String arg : expectedArgs) {
            assertTrue(honored.contains(arg));
        }
    }

    private static final class TestContentCallback implements ContentCallback {

        private int mPagesLoaded;
        private Map<Query, Cursor> mCursors = new HashMap<>();

        @Override
        public void onCursorReady(Query query, Cursor cursor) {
            mPagesLoaded++;
            mCursors.put(query, cursor);
        }

        private void assertPageLoaded(Query query) {
            assertTrue(mCursors.containsKey(query));
            assertNotNull(mCursors.get(query));
        }

        private void assertNumPagesLoaded(int expected) {
            assertEquals(expected, mPagesLoaded);
        }

        private @Nullable Cursor getCursor(Query query) {
            return mCursors.get(query);
        }
    }
}
