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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.job.JobInfo;
import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.systemjob.SystemJobIdGenerator;
import android.arch.background.workmanager.systemjob.SystemJobInfoConverter;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SystemJobInfoConverterTest {
    private SystemJobIdGenerator mMockJobIdGenerator;
    private SystemJobInfoConverter mConverter;

    @Before
    public void setUp() {
        mMockJobIdGenerator = mock(SystemJobIdGenerator.class);
        mConverter = new SystemJobInfoConverter(
                InstrumentationRegistry.getTargetContext(),
                mMockJobIdGenerator);
    }

    @Test
    public void convert() {
        int expectedJobId = 101;
        when(mMockJobIdGenerator.nextId()).thenReturn(expectedJobId);

        String expectedWorkSpecId = "026e3422-9cd1-11e7-abc4-cec278b6b50a";
        WorkSpec workSpec = new WorkSpec(expectedWorkSpecId);
        JobInfo jobInfo = mConverter.convert(workSpec);
        String actualWorkSpecId = jobInfo.getExtras().getString(
                SystemJobInfoConverter.EXTRAS_WORK_SPEC_ID);
        assertThat(actualWorkSpecId, is(expectedWorkSpecId));
        assertThat(jobInfo.getId(), is(expectedJobId));
    }

    @Test
    public void convertWithConstraints() {
        @Constraints.NetworkType int workSpecNetworkType = Constraints.NETWORK_TYPE_UNMETERED;
        Constraints expectedConstraints = new Constraints.Builder()
                .setInitialDelay(12345)
                .setRequiredNetworkType(workSpecNetworkType)
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build();
        WorkSpec workSpec = new WorkSpec("id");
        workSpec.setConstraints(expectedConstraints);
        JobInfo jobInfo = mConverter.convert(workSpec);

        int expectedNetworkType = mConverter.convertNetworkType(workSpecNetworkType);
        assertThat(jobInfo.getNetworkType(), is(expectedNetworkType));
        assertThat(jobInfo.getMinLatencyMillis(), is(expectedConstraints.getInitialDelay()));
        assertThat(jobInfo.isRequireCharging(), is(expectedConstraints.requiresCharging()));
        assertThat(jobInfo.isRequireDeviceIdle(), is(expectedConstraints.requiresDeviceIdle()));
        assertThat(
                jobInfo.isRequireBatteryNotLow(), is(expectedConstraints.requiresBatteryNotLow()));
        assertThat(
                jobInfo.isRequireStorageNotLow(), is(expectedConstraints.requiresStorageNotLow()));
    }

    @Test
    public void convertNetworkTypeAny() {
        convertNetworkTypeHelper(Constraints.NETWORK_TYPE_NONE, JobInfo.NETWORK_TYPE_NONE);
    }

    @Test
    public void convertNetworkTypeConnected() {
        convertNetworkTypeHelper(Constraints.NETWORK_TYPE_CONNECTED, JobInfo.NETWORK_TYPE_ANY);
    }

    @Test
    public void convertNetworkTypeUnmetered() {
        convertNetworkTypeHelper(
                Constraints.NETWORK_TYPE_UNMETERED, JobInfo.NETWORK_TYPE_UNMETERED);
    }

    @Test
    public void convertNetworkTypeNotRoaming() {
        convertNetworkTypeHelper(
                Constraints.NETWORK_TYPE_NOT_ROAMING, JobInfo.NETWORK_TYPE_NOT_ROAMING);
    }

    @Test
    public void convertNetworkTypeMetered() {
        convertNetworkTypeHelper(Constraints.NETWORK_TYPE_METERED, JobInfo.NETWORK_TYPE_METERED);
    }

    private void convertNetworkTypeHelper(@Constraints.NetworkType int constraintNetworkType,
                                          int expectedJobInfoNetworkType) {
        int convertedNetworkType = mConverter.convertNetworkType(constraintNetworkType);
        assertThat(convertedNetworkType, is(expectedJobInfoNetworkType));
    }
}
