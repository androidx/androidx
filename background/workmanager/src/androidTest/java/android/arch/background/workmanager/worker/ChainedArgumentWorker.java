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

package android.arch.background.workmanager.worker;

import android.arch.background.workmanager.Worker;
import android.arch.background.workmanager.model.Arguments;

/**
 * Created by sumir on 11/22/17.
 */

public class ChainedArgumentWorker extends Worker {

    public static final String ARGUMENT_KEY = "key";
    public static final String ARGUMENT_VALUE = "value";

    @Override
    public int doWork() {
        Arguments arguments =
                new Arguments.Builder().putString(ARGUMENT_KEY, ARGUMENT_VALUE).build();
        setOutput(arguments);
        return WORKER_RESULT_SUCCESS;
    }
}
