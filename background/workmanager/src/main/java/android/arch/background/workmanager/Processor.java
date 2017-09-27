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
package android.arch.background.workmanager;

import android.content.Context;

/**
 * A Processor can intelligently schedule and execute work on demand.
 */

public abstract class Processor implements ExecutionListener {

    protected Context mAppContext;
    protected WorkDatabase mWorkDatabase;

    public Processor(Context appContext, WorkDatabase workDatabase) {
        mAppContext = appContext;
        mWorkDatabase = workDatabase;
    }

    /**
     * @param id The work id to execute.
     */
    public abstract void process(String id);

    /**
     * @param id The work id to cancel.
     */
    public abstract void cancel(String id);
}
