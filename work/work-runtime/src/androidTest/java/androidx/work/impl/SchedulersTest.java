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

package androidx.work.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.MediumTest;
import androidx.work.Configuration;
import androidx.work.impl.background.systemjob.SystemJobScheduler;
import androidx.work.impl.background.systemjob.SystemJobService;

import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class SchedulersTest {

    private final Context mAppContext = ApplicationProvider.getApplicationContext();
    private final Configuration mConfiguration = new Configuration.Builder().build();
    private final WorkDatabase mWorkDatabase = WorkDatabase.create(mAppContext,
            mConfiguration.getExecutor(), mConfiguration.getClock(), false);

    @FlakyTest(bugId = 206647994)
    @Test
    public void testGetBackgroundScheduler_withJobSchedulerApiLevel() throws Exception {
        Scheduler scheduler = Schedulers.createBestAvailableBackgroundScheduler(mAppContext,
                mWorkDatabase, mConfiguration);
        assertThat(scheduler, is(instanceOf(SystemJobScheduler.class)));
        assertServicesEnabled();
    }
    // Only one service should really be enabled at one time.
    private void assertServicesEnabled() throws Exception {
        PackageManager pm = mAppContext.getPackageManager();
        ComponentName name = new ComponentName(mAppContext, SystemJobService.class);
        assertThat(pm.getServiceInfo(name, /* flags= */ 0).enabled, is(true));
    }
}
