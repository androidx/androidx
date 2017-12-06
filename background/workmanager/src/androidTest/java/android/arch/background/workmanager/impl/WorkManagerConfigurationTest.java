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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import android.arch.background.workmanager.Scheduler;
import android.arch.background.workmanager.background.systemalarm.SystemAlarmScheduler;
import android.arch.background.workmanager.background.systemjob.SystemJobScheduler;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WorkManagerConfigurationTest {

    private WorkManagerConfiguration mConfiguration;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        mConfiguration = new WorkManagerConfiguration(context, true);
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23)
    public void testGetScheduler_afterApi23_returnsSystemJobScheduler() {
        Scheduler scheduler = mConfiguration.getBackgroundScheduler();
        assertThat(scheduler, is(instanceOf(SystemJobScheduler.class)));
    }

    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = 22)
    public void testGetScheduler_beforeApi23_returnsSystemAlarmScheduler() {
        Scheduler scheduler = mConfiguration.getBackgroundScheduler();
        assertThat(scheduler, is(instanceOf(SystemAlarmScheduler.class)));
    }
}
