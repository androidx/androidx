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

package android.arch.background.workmanager.impl.background.systemjob;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import android.app.job.JobParameters;
import android.arch.background.workmanager.DatabaseTest;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.background.workmanager.impl.utils.taskexecutor.InstantTaskExecutorRule;
import android.arch.background.workmanager.worker.InfiniteTestWorker;
import android.os.PersistableBundle;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 23)
public class SystemJobServiceTest extends DatabaseTest {

    @Rule
    public InstantTaskExecutorRule mRule = new InstantTaskExecutorRule();

    private SystemJobService mSystemJobService;

    @Before
    public void setUp() {
        mSystemJobService = new SystemJobService(); // Bleh.
        mSystemJobService.onCreate();
    }

    @After
    public void tearDown() {
        mSystemJobService.onDestroy();
    }

    @Test
    @SmallTest
    public void testOnStopJob_ReschedulesWhenNotCancelled() throws Exception {
        Work work = new Work.Builder(InfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getId());
        assertThat(mSystemJobService.onStartJob(mockParams), is(true));
        assertThat(mSystemJobService.onStopJob(mockParams), is(true));
    }

    @Test
    @SmallTest
    public void testOnStopJob_DoesNotRescheduleWhenCancelled() throws Exception {
        Work work = new Work.Builder(InfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getId());
        assertThat(mSystemJobService.onStartJob(mockParams), is(true));
        WorkManagerImpl.getInstance().cancelWorkForId(work.getId());
        assertThat(mSystemJobService.onStopJob(mockParams), is(false));
    }

    private JobParameters createMockJobParameters(String id) throws Exception {
        JobParameters jobParameters = mock(JobParameters.class);
        final PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putString(SystemJobInfoConverter.EXTRA_WORK_SPEC_ID, id);

        doAnswer(new Answer() {
            @Override
            public PersistableBundle answer(InvocationOnMock invocation) throws Throwable {
                return persistableBundle;
            }
        }).when(jobParameters).getExtras();
        return jobParameters;
    }
}
