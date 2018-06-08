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

import static androidx.work.impl.utils.PackageManagerHelper.isComponentExplicitlyEnabled;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.impl.background.systemalarm.SystemAlarmScheduler;
import androidx.work.impl.background.systemalarm.SystemAlarmService;
import androidx.work.impl.background.systemjob.SystemJobScheduler;
import androidx.work.impl.background.systemjob.SystemJobService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SchedulersTest {

    private Context mAppContext;
    private WorkManagerImpl mWorkManager;

    @Before
    public void setUp() {
        mWorkManager = mock(WorkManagerImpl.class);
        mAppContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    @SdkSuppress(minSdkVersion = WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL)
    public void testGetBackgroundScheduler_withJobSchedulerApiLevel() {
        Scheduler scheduler =
                Schedulers.createBestAvailableBackgroundScheduler(mAppContext, mWorkManager);
        assertThat(scheduler, is(instanceOf(SystemJobScheduler.class)));
        assertServicesEnabled(true, false, false);
    }

    @Test
    @SdkSuppress(maxSdkVersion = WorkManagerImpl.MAX_PRE_JOB_SCHEDULER_API_LEVEL)
    public void testGetBackgroundScheduler_beforeJobSchedulerApiLevel() {
        Scheduler scheduler =
                Schedulers.createBestAvailableBackgroundScheduler(mAppContext, mWorkManager);
        assertThat(scheduler, is(instanceOf(SystemAlarmScheduler.class)));
        assertServicesEnabled(false, false, true);
    }

    // Only one service should really be enabled at one time.
    private void assertServicesEnabled(
            boolean systemJobEnabled, boolean firebaseJobEnabled, boolean systemAlarmEnabled) {
        if (Build.VERSION.SDK_INT >= WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            assertThat(isComponentExplicitlyEnabled(mAppContext, SystemJobService.class),
                    is(systemJobEnabled));
        }
        assertThat(isComponentExplicitlyEnabled(
                mAppContext,
                Schedulers.FIREBASE_JOB_SERVICE_CLASSNAME),
                is(firebaseJobEnabled));
        assertThat(isComponentExplicitlyEnabled(mAppContext, SystemAlarmService.class),
                is(systemAlarmEnabled));
    }
}
