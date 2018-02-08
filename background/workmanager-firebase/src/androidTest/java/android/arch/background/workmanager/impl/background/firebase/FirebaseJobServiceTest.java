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

package android.arch.background.workmanager.impl.background.firebase;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import android.arch.background.workmanager.DatabaseTest;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.background.workmanager.impl.utils.taskexecutor.InstantTaskExecutorRule;
import android.arch.background.workmanager.worker.FirebaseInfiniteTestWorker;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.firebase.jobdispatcher.JobParameters;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(AndroidJUnit4.class)
public class FirebaseJobServiceTest extends DatabaseTest {

    @Rule
    public InstantTaskExecutorRule mRule = new InstantTaskExecutorRule();

    private FirebaseJobService mFirebaseJobService;

    @Before
    public void setUp() {
        mFirebaseJobService = new FirebaseJobService(); // Bleh.
        mFirebaseJobService.onCreate();
    }

    @After
    public void tearDown() {
        mFirebaseJobService.onDestroy();
    }

    @Test
    @SmallTest
    public void testOnStopJob_ReschedulesWhenNotCancelled() throws Exception {
        Work work = new Work.Builder(FirebaseInfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getId());
        assertThat(mFirebaseJobService.onStartJob(mockParams), is(true));
        assertThat(mFirebaseJobService.onStopJob(mockParams), is(true));
    }

    @Test
    @SmallTest
    public void testOnStopJob_DoesNotRescheduleWhenCancelled() throws Exception {
        Work work = new Work.Builder(FirebaseInfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getId());
        assertThat(mFirebaseJobService.onStartJob(mockParams), is(true));
        WorkManagerImpl.getInstance().cancelWorkForId(work.getId());
        assertThat(mFirebaseJobService.onStopJob(mockParams), is(false));
    }

    private JobParameters createMockJobParameters(final String id) throws Exception {
        JobParameters jobParameters = mock(JobParameters.class);

        doAnswer(new Answer() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return id;
            }
        }).when(jobParameters).getTag();
        return jobParameters;
    }
}
