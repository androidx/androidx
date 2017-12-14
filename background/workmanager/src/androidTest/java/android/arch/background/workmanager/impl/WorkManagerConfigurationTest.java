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

package android.arch.background.workmanager.impl;

import static android.arch.background.workmanager.impl.WorkManagerConfiguration
        .FIREBASE_JOB_SERVICE_CLASSNAME;
import static android.arch.background.workmanager.impl.utils.PackageManagerHelper
        .isComponentExplicitlyEnabled;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import android.arch.background.workmanager.impl.background.systemalarm.SystemAlarmScheduler;
import android.arch.background.workmanager.impl.background.systemalarm.SystemAlarmService;
import android.arch.background.workmanager.impl.background.systemjob.SystemJobScheduler;
import android.arch.background.workmanager.impl.background.systemjob.SystemJobService;
import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class WorkManagerConfigurationTest {

    private Context mAppContext;
    private WorkManagerConfiguration mConfiguration;

    @Before
    public void setUp() {
        mAppContext = InstrumentationRegistry.getTargetContext();
        mConfiguration = new WorkManagerConfiguration(mAppContext);
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    public void testGetBackgroundScheduler_afterApi23() {
        Scheduler scheduler = mConfiguration.getBackgroundScheduler();
        assertThat(scheduler, is(instanceOf(SystemJobScheduler.class)));
        assertServicesEnabled(true, false, false);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 22)
    public void testGetBackgroundScheduler_beforeApi23() {
        Scheduler scheduler = mConfiguration.getBackgroundScheduler();
        assertThat(scheduler, is(instanceOf(SystemAlarmScheduler.class)));
        assertServicesEnabled(false, false, true);
    }

    // Only one service should really be enabled at one time.
    private void assertServicesEnabled(
            boolean systemJobEnabled, boolean firebaseJobEnabled, boolean systemAlarmEnabled) {
        if (Build.VERSION.SDK_INT >= 23) {
            assertThat(isComponentExplicitlyEnabled(mAppContext, SystemJobService.class),
                    is(systemJobEnabled));
        }
        assertThat(isComponentExplicitlyEnabled(mAppContext, FIREBASE_JOB_SERVICE_CLASSNAME),
                is(firebaseJobEnabled));
        assertThat(isComponentExplicitlyEnabled(mAppContext, SystemAlarmService.class),
                is(systemAlarmEnabled));
    }
}
