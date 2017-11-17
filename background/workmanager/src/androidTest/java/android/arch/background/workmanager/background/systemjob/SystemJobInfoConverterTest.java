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

package android.arch.background.workmanager.background.systemjob;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.job.JobInfo;
import android.arch.background.workmanager.PeriodicWork;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.utils.IdGenerator;
import android.arch.background.workmanager.worker.TestWorker;
import android.net.Uri;
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
public class SystemJobInfoConverterTest {

    private static final long TEST_INTERVAL_DURATION =
            PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS + 1232L;
    private static final long TEST_FLEX_DURATION = PeriodicWork.MIN_PERIODIC_FLEX_MILLIS + 112L;

    private IdGenerator mMockIdGenerator;
    private SystemJobInfoConverter mConverter;

    @Before
    public void setUp() {
        mMockIdGenerator = mock(IdGenerator.class);
        mConverter = new SystemJobInfoConverter(
                InstrumentationRegistry.getTargetContext(),
                mMockIdGenerator);
    }

    @Test
    @SmallTest
    public void testConvert_ids() {
        final String expectedWorkSpecId = "026e3422-9cd1-11e7-abc4-cec278b6b50a";
        final int expectedJobId = 101;
        when(mMockIdGenerator.nextJobSchedulerId()).thenReturn(expectedJobId);
        WorkSpec workSpec = new WorkSpec(expectedWorkSpecId);
        JobInfo jobInfo = mConverter.convert(workSpec);
        String actualWorkSpecId = jobInfo.getExtras().getString(
                SystemJobInfoConverter.EXTRA_WORK_SPEC_ID);
        assertThat(actualWorkSpecId, is(expectedWorkSpecId));
        assertThat(jobInfo.getId(), is(expectedJobId));
    }

    @Test
    @SmallTest
    public void testConvert_setPersistedByDefault() {
        JobInfo jobInfo = mConverter.convert(new WorkSpec("id"));
        assertThat(jobInfo.isPersisted(), is(true));
    }

    /**
     * Due to b/6771687, calling {@link JobInfo.Builder#build} with no constraints throws an
     * {@link IllegalArgumentException}. This is testing that {@link SystemJobInfoConverter#convert}
     * sets some dummy constraint to toggle some internal boolean flags in {@link JobInfo.Builder}
     * to allow {@link Work} with no constraints to be converted without affecting its runtime,
     * e.g. calling builder.setMinLatencyMillis(0L).
     */
    @Test
    @SmallTest
    public void testConvert_noConstraints_doesNotThrowException() {
        mConverter.convert(new WorkSpec("id"));
    }

    @Test
    @SmallTest
    public void testConvert_retryPolicy() {
        long expectedBackoffDelayDuration = 50000;
        WorkSpec workSpec = new WorkSpec("id");
        workSpec.setBackoffDelayDuration(expectedBackoffDelayDuration);
        workSpec.setBackoffPolicy(Work.BACKOFF_POLICY_LINEAR);
        JobInfo jobInfo = mConverter.convert(workSpec);
        assertThat(jobInfo.getInitialBackoffMillis(), is(expectedBackoffDelayDuration));
        assertThat(jobInfo.getBackoffPolicy(), is(JobInfo.BACKOFF_POLICY_LINEAR));
    }

    @Test
    @SmallTest
    public void testConvert_initialDelay() {
        final long expectedInitialDelay = 12123L;
        WorkSpec workSpec = new WorkSpec("id");
        workSpec.setInitialDelay(expectedInitialDelay);
        JobInfo jobInfo = mConverter.convert(workSpec);
        assertThat(jobInfo.getMinLatencyMillis(), is(expectedInitialDelay));
    }

    @Test
    @SmallTest
    public void testConvert_periodicWithNoFlex() {
        WorkSpec workSpec = new WorkSpec("id");
        workSpec.setPeriodic(TEST_INTERVAL_DURATION);
        JobInfo jobInfo = mConverter.convert(workSpec);
        assertThat(jobInfo.getIntervalMillis(), is(TEST_INTERVAL_DURATION));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    public void testConvert_periodicWithFlex() {
        WorkSpec workSpec = new WorkSpec("id");
        workSpec.setPeriodic(TEST_INTERVAL_DURATION, TEST_FLEX_DURATION);
        JobInfo jobInfo = mConverter.convert(workSpec);
        assertThat(jobInfo.getIntervalMillis(), is(TEST_INTERVAL_DURATION));
        assertThat(jobInfo.getFlexMillis(), is(TEST_FLEX_DURATION));
    }

    @Test
    @SmallTest
    public void testConvert_requireCharging() {
        final boolean expectedRequireCharging = true;
        WorkSpec workSpec = getTestWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiresCharging(expectedRequireCharging).build());
        JobInfo jobInfo = mConverter.convert(workSpec);
        assertThat(jobInfo.isRequireCharging(), is(expectedRequireCharging));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    public void testConvert_requireContentUriTrigger() {
        final Uri expectedUri = Uri.parse("TEST_URI");
        final JobInfo.TriggerContentUri expectedTriggerContentUri =
                new JobInfo.TriggerContentUri(
                        expectedUri, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS);
        WorkSpec workSpec = getTestWorkSpecWithConstraints(new Constraints.Builder()
                .addContentUriTrigger(expectedUri, true).build());
        JobInfo jobInfo = mConverter.convert(workSpec);

        JobInfo.TriggerContentUri[] triggerContentUris = jobInfo.getTriggerContentUris();
        assertThat(triggerContentUris, is(arrayContaining(expectedTriggerContentUri)));
    }

    @Test
    @SmallTest
    public void testConvert_requireDeviceIdle() {
        final boolean expectedRequireDeviceIdle = true;
        WorkSpec workSpec = getTestWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiresDeviceIdle(expectedRequireDeviceIdle).build());
        JobInfo jobInfo = mConverter.convert(workSpec);
        assertThat(jobInfo.isRequireDeviceIdle(), is(expectedRequireDeviceIdle));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    public void testConvert_requireBatteryNotLow() {
        final boolean expectedRequireBatteryNotLow = true;
        WorkSpec workSpec = getTestWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiresBatteryNotLow(expectedRequireBatteryNotLow).build());
        JobInfo jobInfo = mConverter.convert(workSpec);
        assertThat(jobInfo.isRequireBatteryNotLow(), is(expectedRequireBatteryNotLow));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    public void testConvert_requireStorageNotLow() {
        final boolean expectedRequireStorageNotLow = true;
        WorkSpec workSpec = getTestWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiresStorageNotLow(expectedRequireStorageNotLow).build());
        JobInfo jobInfo = mConverter.convert(workSpec);
        assertThat(jobInfo.isRequireStorageNotLow(), is(expectedRequireStorageNotLow));
    }

    @Test
    @SmallTest
    public void testConvert_networkTypeUnmeteredRequiresApi21() {
        convertWithRequiredNetworkType(
                Constraints.NETWORK_TYPE_UNMETERED, JobInfo.NETWORK_TYPE_UNMETERED, 21);
    }

    @Test
    @SmallTest
    public void testConvert_networkTypeNotRoamingRequiresApi24() {
        convertWithRequiredNetworkType(
                Constraints.NETWORK_TYPE_NOT_ROAMING, JobInfo.NETWORK_TYPE_NOT_ROAMING, 24);
    }

    @Test
    @SmallTest
    public void testConvert_networkTypeMeteredRequiresApi26() {
        convertWithRequiredNetworkType(
                Constraints.NETWORK_TYPE_METERED, JobInfo.NETWORK_TYPE_METERED, 26);
    }

    private void convertWithRequiredNetworkType(@Constraints.NetworkType int networkType,
                                                int jobInfoNetworkType,
                                                int minSdkVersion) {
        WorkSpec workSpec = getTestWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiredNetworkType(networkType).build());
        JobInfo jobInfo = mConverter.convert(workSpec);
        if (Build.VERSION.SDK_INT >= minSdkVersion) {
            assertThat(jobInfo.getNetworkType(), is(jobInfoNetworkType));
        } else {
            assertThat(jobInfo.getNetworkType(), is(JobInfo.NETWORK_TYPE_ANY));
        }
    }

    @Test
    @SmallTest
    public void testConvertNetworkType_none() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_NONE),
                is(JobInfo.NETWORK_TYPE_NONE));
    }

    @Test
    @SmallTest
    public void testConvertNetworkType_any() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_ANY),
                is(JobInfo.NETWORK_TYPE_ANY));
    }

    @Test
    @SmallTest
    public void testConvertNetworkType_unmetered() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_UNMETERED),
                is(JobInfo.NETWORK_TYPE_UNMETERED));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 23)
    public void testConvertNetworkType_notRoaming_returnAnyBeforeApi24() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_NOT_ROAMING),
                is(JobInfo.NETWORK_TYPE_ANY));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    public void testConvertNetworkType_notRoaming_returnsNotRoamingAfterApi24() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_NOT_ROAMING),
                is(JobInfo.NETWORK_TYPE_NOT_ROAMING));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testConvertNetworkType_metered_returnsAnyBeforeApi26() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_METERED),
                is(JobInfo.NETWORK_TYPE_ANY));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    public void testConvertNetworkType_metered_returnsMeteredAfterApi26() {
        assertThat(SystemJobInfoConverter.convertNetworkType(Constraints.NETWORK_TYPE_METERED),
                is(JobInfo.NETWORK_TYPE_METERED));
    }

    private WorkSpec getTestWorkSpecWithConstraints(Constraints constraints) {
        return new Work.Builder(TestWorker.class)
                .withConstraints(constraints)
                .build()
                .getWorkSpec();
    }
}
