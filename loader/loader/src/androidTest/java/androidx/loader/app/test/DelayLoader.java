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

package androidx.loader.app.test;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import java.util.concurrent.CountDownLatch;

public class DelayLoader extends AsyncTaskLoader<Boolean> {
    private final CountDownLatch mDeliverResultLatch;

    public DelayLoader(Context context, CountDownLatch deliverResultLatch) {
        super(context);
        mDeliverResultLatch = deliverResultLatch;
    }

    @Override
    public Boolean loadInBackground() {
        SystemClock.sleep(50);
        return true;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public void deliverResult(@Nullable Boolean data) {
        super.deliverResult(data);
        mDeliverResultLatch.countDown();
    }
}
