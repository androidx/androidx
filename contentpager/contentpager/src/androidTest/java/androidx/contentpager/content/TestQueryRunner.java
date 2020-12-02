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

import java.util.HashSet;
import java.util.Set;

/**
 * Test friendly synchronous QueryRunner. Not suitable for use
 * in production code.
 */
public final class TestQueryRunner implements ContentPager.QueryRunner {

    // if false, we'll skip calling through to the mCallback when query is called
    // this simulates async processing, and allows tests to check that cancel
    // is handled correctly.
    public boolean runQuery = true;

    private final Set<Query> mRunning = new HashSet<>();

    @Override
    public void query(Query query, Callback callback) {
        if (runQuery) {
            callback.onQueryFinished(query, callback.runQueryInBackground(query));
        } else {
            mRunning.add(query);
        }
    }

    @Override
    public boolean isRunning(Query query) {
        return mRunning.contains(query);
    }

    @Override
    public void cancel(Query query) {
        mRunning.remove(query);
    }
}
