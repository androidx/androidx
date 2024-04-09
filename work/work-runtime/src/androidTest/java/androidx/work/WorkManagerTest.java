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

package androidx.work;

import android.app.job.JobScheduler;
import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.impl.WorkManagerImpl;

import org.junit.After;

public abstract class WorkManagerTest {

    @After
    public void clearJobs() {
        // Note: @SdkSuppress doesn't seem to work here.
        if (Build.VERSION.SDK_INT >= WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            JobScheduler jobScheduler = (JobScheduler) ApplicationProvider.getApplicationContext()
                    .getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (Build.VERSION.SDK_INT < 34) {
                jobScheduler.cancelAll();
            } else {
                jobScheduler.cancelInAllNamespaces();
            }
        }
    }
}
