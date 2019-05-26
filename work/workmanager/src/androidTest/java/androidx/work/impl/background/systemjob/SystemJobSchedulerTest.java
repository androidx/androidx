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

package androidx.work.impl.background.systemjob;


import static android.app.job.JobScheduler.RESULT_SUCCESS;

import static androidx.work.impl.background.systemjob.SystemJobInfoConverter.EXTRA_WORK_SPEC_ID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.os.PersistableBundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.work.Configuration;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManagerTest;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.SystemIdInfoDao;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.worker.TestWorker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL)
public class SystemJobSchedulerTest extends WorkManagerTest {

    private static final String TEST_ID = "test";

    private WorkManagerImpl mWorkManager;
    private JobScheduler mJobScheduler;
    private SystemJobScheduler mSystemJobScheduler;
    private WorkSpecDao mMockWorkSpecDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        Configuration configuration = new Configuration.Builder().build();
        WorkDatabase workDatabase = mock(WorkDatabase.class);
        SystemIdInfoDao systemIdInfoDao = mock(SystemIdInfoDao.class);
        mMockWorkSpecDao = mock(WorkSpecDao.class);

        mWorkManager = mock(WorkManagerImpl.class);
        mJobScheduler = mock(JobScheduler.class);

        when(mWorkManager.getConfiguration()).thenReturn(configuration);
        when(workDatabase.systemIdInfoDao()).thenReturn(systemIdInfoDao);
        when(workDatabase.workSpecDao()).thenReturn(mMockWorkSpecDao);
        when(mWorkManager.getWorkDatabase()).thenReturn(workDatabase);

        doReturn(RESULT_SUCCESS).when(mJobScheduler).schedule(any(JobInfo.class));

        List<JobInfo> allJobInfos = new ArrayList<>(2);
        PersistableBundle extras = new PersistableBundle();
        extras.putString(EXTRA_WORK_SPEC_ID, TEST_ID);
        JobInfo mockJobInfo1 = mock(JobInfo.class);
        doReturn(extras).when(mockJobInfo1).getExtras();
        JobInfo mockJobInfo2 = mock(JobInfo.class);
        doReturn(extras).when(mockJobInfo2).getExtras();

        allJobInfos.add(mockJobInfo1);
        allJobInfos.add(mockJobInfo2);
        doReturn(allJobInfos).when(mJobScheduler).getAllPendingJobs();

        mSystemJobScheduler =
                spy(new SystemJobScheduler(
                        context,
                        mWorkManager,
                        mJobScheduler,
                        new SystemJobInfoConverter(context)));

        doNothing().when(mSystemJobScheduler).scheduleInternal(any(WorkSpec.class), anyInt());
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 23)
    public void testSystemJobScheduler_schedulesTwiceOnApi23() {
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        WorkSpec workSpec1 = getWorkSpec(work1);
        addToWorkSpecDao(workSpec1);

        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        WorkSpec workSpec2 = getWorkSpec(work2);
        addToWorkSpecDao(workSpec2);

        mSystemJobScheduler.schedule(workSpec1, workSpec2);

        verify(mSystemJobScheduler, times(2))
                .scheduleInternal(eq(workSpec1), anyInt());
        verify(mSystemJobScheduler, times(2))
                .scheduleInternal(eq(workSpec2), anyInt());
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    public void testSystemJobScheduler_schedulesOnceAtOrAboveApi24() {
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        WorkSpec workSpec1 = getWorkSpec(work1);
        addToWorkSpecDao(workSpec1);

        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        WorkSpec workSpec2 = getWorkSpec(work2);
        addToWorkSpecDao(workSpec2);

        mSystemJobScheduler.schedule(workSpec1, workSpec2);

        verify(mSystemJobScheduler, times(1))
                .scheduleInternal(eq(workSpec1), anyInt());
        verify(mSystemJobScheduler, times(1))
                .scheduleInternal(eq(workSpec2), anyInt());
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 23)
    public void testSystemJobScheduler_cancelsAllOnApi23() {
        mSystemJobScheduler.cancel(TEST_ID);
        verify(mJobScheduler, times(2)).cancel(anyInt());
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    public void testSystemJobScheduler_cancelsOnceAtOrAboveApi24() {
        mSystemJobScheduler.cancel(TEST_ID);
        verify(mJobScheduler, times(1)).cancel(anyInt());
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23)
    public void testSystemJobScheduler_ignoresUnfoundWork() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        WorkSpec workSpec = getWorkSpec(work);
        // Don't use addToWorkSpecDao and put it in the database mock.

        mSystemJobScheduler.schedule(workSpec);
        verify(mSystemJobScheduler, never()).scheduleInternal(eq(workSpec), anyInt());
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23)
    public void testSystemJobScheduler_ignoresUnenqueuedWork() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(WorkInfo.State.CANCELLED)
                .build();
        WorkSpec workSpec = getWorkSpec(work);
        // Don't use addToWorkSpecDao and put it in the database mock.

        mSystemJobScheduler.schedule(workSpec);
        verify(mSystemJobScheduler, never()).scheduleInternal(eq(workSpec), anyInt());
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 23)
    public void testSystemJobScheduler_avoidCrash() {
        doCallRealMethod().when(mSystemJobScheduler)
                .scheduleInternal(any(WorkSpec.class), anyInt());

        doThrow(new RuntimeException("Crash")).when(mJobScheduler).getAllPendingJobs();

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        WorkSpec workSpec = getWorkSpec(work);
        addToWorkSpecDao(workSpec);
        mSystemJobScheduler.schedule(workSpec);
        // JobScheduler#schedule() should be called once at the very least.
        verify(mJobScheduler, times(1)).schedule(any(JobInfo.class));
    }

    private void addToWorkSpecDao(WorkSpec workSpec) {
        when(mMockWorkSpecDao.getWorkSpec(workSpec.id)).thenReturn(workSpec);
    }
}
