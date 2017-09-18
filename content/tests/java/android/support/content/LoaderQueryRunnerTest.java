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

package android.support.content;

import static android.support.content.ContentPager.createArgs;
import static android.support.content.TestContentProvider.UNPAGED_URI;

import android.app.Activity;
import android.database.Cursor;
import android.os.Looper;
import android.support.content.ContentPager.ContentCallback;
import android.support.content.ContentPager.QueryRunner;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

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

        // Note: For some when running this test via tradefed (vs gradle) this
        // looper setup code doesn't work when run *in setUp*. Works fine in Gradle.
        // So this test fails when run on treehugger or run via tradefed.
        // To work around that issue we prepare the looper here.
        //
        // "Wait!" you say, why do you need to prepare a looper? We're using
        // a CursorLoader under the hoods which deep down creates an handler
        // to listen for content changes. That's not critical to our test
        // since we're waiting on results w/ latches, but we need to avoid the error.
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        ContentCallback dummyContentCallback = new ContentCallback() {
            @Override
            public void onCursorReady(Query query, Cursor cursor) {
                // Nothing to see here. Move along.
            }
        };
        Query query = new Query(
                UNPAGED_URI,
                null,
                createArgs(offset, limit),
                null,
                dummyContentCallback);

        mCallback.reset(1);
        mRunner.query(query, mCallback);

        mCallback.waitFor(10);
        mCallback.assertQueried(query.getId());
        mCallback.assertReceivedContent(UNPAGED_URI, query.getId());
    }
}
