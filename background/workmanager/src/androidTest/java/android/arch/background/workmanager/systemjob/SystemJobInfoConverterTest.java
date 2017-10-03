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

package android.arch.background.workmanager.systemjob;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.job.JobInfo;
import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.WorkSpec;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 23)
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

    private WorkSpec createWorkSpecWithConstraints(Constraints constraints) {
        WorkSpec workSpec = new WorkSpec("id");
        workSpec.setConstraints(constraints);
        return workSpec;
    }

    @Test
    public void testConvert_ids() {
        final String expectedWorkSpecId = "026e3422-9cd1-11e7-abc4-cec278b6b50a";
        final int expectedJobId = 101;
        when(mMockJobIdGenerator.nextId()).thenReturn(expectedJobId);
        WorkSpec workSpec = new WorkSpec(expectedWorkSpecId);
        JobInfo jobInfo = mConverter.convert(workSpec);
        String actualWorkSpecId = jobInfo.getExtras().getString(
                SystemJobInfoConverter.EXTRA_WORK_SPEC_ID);
        assertThat(actualWorkSpecId, is(expectedWorkSpecId));
        assertThat(jobInfo.getId(), is(expectedJobId));
    }

    @Test
    public void testConvert_initialDelay() {
        final long expectedInitialDelay = 12123L;
        WorkSpec workSpec = createWorkSpecWithConstraints(
                new Constraints.Builder().setInitialDelay(expectedInitialDelay).build());
        JobInfo jobInfo = mConverter.convert(workSpec);
        assertThat(jobInfo.getMinLatencyMillis(), is(expectedInitialDelay));
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testConvert_requireCharging() {
        final boolean expectedRequireCharging = true;
        WorkSpec workSpec = createWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiresCharging(expectedRequireCharging).build());
        JobInfo jobInfo = mConverter.convert(workSpec);
        assertThat(jobInfo.isRequireCharging(), is(expectedRequireCharging));
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testConvert_requireDeviceIdle() {
        final boolean expectedRequireDeviceIdle = true;
        WorkSpec workSpec = createWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiresDeviceIdle(expectedRequireDeviceIdle).build());
        JobInfo jobInfo = mConverter.convert(workSpec);
        assertThat(jobInfo.isRequireDeviceIdle(), is(expectedRequireDeviceIdle));
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void testConvert_requireBatteryNotLow() {
        final boolean expectedRequireBatteryNotLow = true;
        WorkSpec workSpec = createWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiresBatteryNotLow(expectedRequireBatteryNotLow).build());
        JobInfo jobInfo = mConverter.convert(workSpec);
        assertThat(jobInfo.isRequireBatteryNotLow(), is(expectedRequireBatteryNotLow));
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void testConvert_requireStorageNotLow() {
        final boolean expectedRequireStorageNotLow = true;
        WorkSpec workSpec = createWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiresStorageNotLow(expectedRequireStorageNotLow).build());
        JobInfo jobInfo = mConverter.convert(workSpec);
        assertThat(jobInfo.isRequireStorageNotLow(), is(expectedRequireStorageNotLow));
    }

    @Test
    public void testConvert_networkTypeUnmeteredRequiresApi21() {
        convertWithRequiredNetworkType(
                Constraints.NETWORK_TYPE_UNMETERED, JobInfo.NETWORK_TYPE_UNMETERED, 21);
    }

    @Test
    public void testConvert_networkTypeNotRoamingRequiresApi24() {
        convertWithRequiredNetworkType(
                Constraints.NETWORK_TYPE_NOT_ROAMING, JobInfo.NETWORK_TYPE_NOT_ROAMING, 24);
    }

    @Test
    public void testConvert_networkTypeMeteredRequiresApi26() {
        convertWithRequiredNetworkType(
                Constraints.NETWORK_TYPE_METERED, JobInfo.NETWORK_TYPE_METERED, 26);
    }

    private void convertWithRequiredNetworkType(@Constraints.NetworkType int networkType,
                                                int jobInfoNetworkType,
                                                int minSdkVersion) {
        WorkSpec workSpec = createWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiredNetworkType(networkType).build());
        JobInfo jobInfo = mConverter.convert(workSpec);
        if (Build.VERSION.SDK_INT >= minSdkVersion) {
            assertThat(jobInfo.getNetworkType(), is(jobInfoNetworkType));
        } else {
            assertThat(jobInfo.getNetworkType(), is(JobInfo.NETWORK_TYPE_ANY));
        }
    }

    @Test
    public void testConvertNetworkType_none() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_NONE),
                is(JobInfo.NETWORK_TYPE_NONE));
    }

    @Test
    public void testConvertNetworkType_connected() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_CONNECTED),
                is(JobInfo.NETWORK_TYPE_ANY));
    }

    @Test
    public void testConvertNetworkType_unmetered() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_UNMETERED),
                is(JobInfo.NETWORK_TYPE_UNMETERED));
    }

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 23)
    public void testConvertNetworkType_notRoaming_returnAnyBeforeApi24() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_NOT_ROAMING),
                is(JobInfo.NETWORK_TYPE_ANY));
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testConvertNetworkType_notRoaming_returnsNotRoamingAfterApi24() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_NOT_ROAMING),
                is(JobInfo.NETWORK_TYPE_NOT_ROAMING));
    }

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testConvertNetworkType_metered_returnsAnyBeforeApi26() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_METERED),
                is(JobInfo.NETWORK_TYPE_ANY));
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void testConvertNetworkType_metered_returnsMeteredAfterApi26() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_METERED),
                is(JobInfo.NETWORK_TYPE_METERED));
    }
}
