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
import static androidx.contentpager.content.TestContentProvider.UNPAGED_URI;

import android.app.Activity;
import android.database.Cursor;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.contentpager.content.ContentPager.ContentCallback;
import androidx.contentpager.content.ContentPager.QueryRunner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class LoaderQueryRunnerTest {

    @Rule
    public ActivityTestRule<Activity> mActivityRule = new ActivityTestRule(TestActivity.class);

    private Activity mActivity;
    private QueryRunner mRunner;
    private TestQueryCallback mCallback;

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mRunner = new LoaderQueryRunner(mActivity, mActivity.getLoaderManager());
        mCallback = new TestQueryCallback();
    }

    @Test
    public void testRunsQuery() throws Throwable {
        int offset = 0;
        int limit = 10;

        ContentCallback dummyContentCallback = new ContentCallback() {
            @Override
            public void onCursorReady(Query query, Cursor cursor) {
                // Nothing to see here. Move along.
            }
        };
        final Query query = new Query(
                UNPAGED_URI,
                null,
                createArgs(offset, limit),
                null,
                dummyContentCallback);

        mCallback.reset(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This calls through to LoaderManager.restartLoader
                // which must always be called on the main thread
                mRunner.query(query, mCallback);
            }
        });

        mCallback.waitFor(10);
        mCallback.assertQueried(query.getId());
        mCallback.assertReceivedContent(UNPAGED_URI, query.getId());
    }
}
