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

package androidx.work.test;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.Configuration;
import androidx.work.WorkManager;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkManagerImpl;

import java.util.List;

/**
 * A concrete implementation of {@link WorkManager} which can be used for testing.
 * This implementation makes it easy to swap Schedulers.
 */
public abstract class TestWorkManagerImpl extends WorkManagerImpl implements TestDriver {
    public TestWorkManagerImpl(
            @NonNull Context context,
            @NonNull Configuration configuration) {

        // Note: This implies that the call to ForceStopRunnable() actually does nothing.
        // This is okay when testing.
        super(context, configuration, true);
    }

    /**
     * @return The list of Schedulers used when testing.
     */
    @Override
    @NonNull
    public abstract List<Scheduler> getSchedulers();
}
