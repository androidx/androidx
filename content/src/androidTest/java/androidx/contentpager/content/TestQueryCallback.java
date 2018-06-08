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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class TestQueryCallback implements ContentPager.QueryRunner.Callback {

    private static final String URI_KEY = "testUri";
    private static final String URI_PAGE_ID = "testPageId";

    private CollectorLatch<Query> mQueryLatch;
    private CollectorLatch<Pair<Integer, Cursor>> mReplyLatch;

    @Override
    public @Nullable Cursor runQueryInBackground(Query query) {
        mQueryLatch.accept(query);
        Bundle extras = new Bundle();
        extras.putParcelable(URI_KEY, query.getUri());
        extras.putInt(URI_PAGE_ID, query.getId());
        MatrixCursor cursor = new MatrixCursor(new String[]{"id"}, 0);
        cursor.setExtras(extras);
        return cursor;
    }

    @Override
    public void onQueryFinished(Query query, Cursor cursor) {
        mReplyLatch.accept(new Pair<>(query.getId(), cursor));
    }

    public void reset(int expectedCount) throws InterruptedException {
        mQueryLatch = new CollectorLatch<>(expectedCount);
        mReplyLatch = new CollectorLatch<>(expectedCount);
    }

    public void waitFor(int seconds) throws InterruptedException {
        assertTrue(mQueryLatch.await(seconds, TimeUnit.SECONDS));
        assertTrue(mReplyLatch.await(seconds, TimeUnit.SECONDS));
    }

    public void assertQueried(final int expectedPageId) {
        mQueryLatch.assertHasItem(new Matcher<Query>() {
            @Override
            public boolean matches(Query query) {
                return expectedPageId == query.getId();
            }
        });
    }

    public void assertReceivedContent(Uri expectedUri, final int expectedPageId) {
        mReplyLatch.assertHasItem(new Matcher<Pair<Integer, Cursor>>() {
            @Override
            public boolean matches(Pair<Integer, Cursor> value) {
                return expectedPageId == value.first;
            }
        });
        List<Pair<Integer, Cursor>> collected = mReplyLatch.getCollected();
        Cursor cursor = null;

        for (Pair<Integer, Cursor> pair : collected) {
            if (expectedPageId == pair.first) {
                cursor = pair.second;
            }
        }

        assertEquals(0, cursor.getCount());  // we don't add any records to our test cursor.
        Bundle extras = cursor.getExtras();
        assertNotNull(extras);
        assertTrue(extras.containsKey(URI_KEY));
        assertEquals(extras.getParcelable(URI_KEY), expectedUri);
        assertTrue(extras.containsKey(URI_PAGE_ID));
        assertEquals(extras.getInt(URI_PAGE_ID), expectedPageId);
    }

    private static final class CollectorLatch<T> extends CountDownLatch {

        private final List<T> mCollected = new ArrayList<>();

        CollectorLatch(int count) {
            super(count);
        }

        void accept(@Nullable T value) {
            onReceived(value);
            super.countDown();
        }

        @Override
        public void countDown() {
            throw new UnsupportedOperationException("Count is incremented by calls to accept.");
        }

        void onReceived(@Nullable T value) {
            mCollected.add(value);
        }

        List<T> getCollected() {
            return mCollected;
        }

        public void assertHasItem(Matcher<T> matcher) {
            T item = null;
            for (T val : mCollected) {
                if (matcher.matches(val)) {
                    item = val;
                }
            }
            assertNotNull(item);
        }

        public @Nullable T get(int index) {
            return mCollected.get(index);
        }
    }

    interface Matcher<T> {
        boolean matches(T value);
    }
}
