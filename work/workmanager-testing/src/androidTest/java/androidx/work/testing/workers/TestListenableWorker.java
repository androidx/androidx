/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.testing.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

public class TestListenableWorker extends ListenableWorker {
    private SettableFuture<Result> mResult;

    public TestListenableWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
        mResult = SettableFuture.create();
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        mResult.set(Result.success());
        return mResult;
    }
}
