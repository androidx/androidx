/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.work.worker;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.Data;
import androidx.work.Result;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * A worker that passes its inputs as outputs.
 */
public class ChainedArgumentWorker extends Worker {

    public static final String ARGUMENT_KEY = "key";
    public static final String ARGUMENT_VALUE = "value";

    public ChainedArgumentWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public @NonNull Result doWork() {
        return Result.success(getChainedArguments());
    }

    public static Data getChainedArguments() {
        return new Data.Builder().putString(ARGUMENT_KEY, ARGUMENT_VALUE).build();
    }
}
