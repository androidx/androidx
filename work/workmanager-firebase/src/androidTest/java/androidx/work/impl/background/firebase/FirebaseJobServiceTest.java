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

package androidx.work.impl.background.firebase;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.support.test.filters.LargeTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.OneTimeWorkRequest;
import androidx.work.State;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.taskexecutor.InstantTaskExecutorRule;
import androidx.work.worker.FirebaseInfiniteTestWorker;

import com.firebase.jobdispatcher.JobParameters;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FirebaseJobServiceTest {

    @Rule
    public InstantTaskExecutorRule mRule = new InstantTaskExecutorRule();

    private WorkDatabase mDatabase;
    private FirebaseJobService mFirebaseJobService;

    @Before
    public void setUp() {
        mDatabase = WorkManagerImpl.getInstance().getWorkDatabase();
        mFirebaseJobService = new FirebaseJobService();
        mFirebaseJobService.onCreate();
    }

    @After
    public void tearDown() {
        mFirebaseJobService.onDestroy();
    }

    @Test
    @LargeTest
    public void testOnStopJob_ResetsWorkStatus() throws InterruptedException {
        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(FirebaseInfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getStringId());
        mFirebaseJobService.onStartJob(mockParams);

        // TODO(sumir): Remove later.  Put here because WorkerWrapper sets state to RUNNING.
        Thread.sleep(5000L);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work.getStringId()), is(State.RUNNING));

        mFirebaseJobService.onStopJob(mockParams);
        assertThat(workSpecDao.getState(work.getStringId()), is(State.ENQUEUED));
    }

    @Test
    @SmallTest
    public void testOnStopJob_ReschedulesWhenNotCancelled() {
        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(FirebaseInfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getStringId());
        assertThat(mFirebaseJobService.onStartJob(mockParams), is(true));
        assertThat(mFirebaseJobService.onStopJob(mockParams), is(true));
    }

    @Test
    @SmallTest
    public void testOnStopJob_DoesNotRescheduleWhenCancelled() {
        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(FirebaseInfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getStringId());
        assertThat(mFirebaseJobService.onStartJob(mockParams), is(true));
        WorkManagerImpl.getInstance().cancelWorkById(work.getId());
        assertThat(mFirebaseJobService.onStopJob(mockParams), is(false));
    }

    private JobParameters createMockJobParameters(String id) {
        JobParameters jobParameters = mock(JobParameters.class);
        when(jobParameters.getTag()).thenReturn(id);
        return jobParameters;
    }

    private void insertWork(WorkRequest work) {
        mDatabase.workSpecDao().insertWorkSpec(work.getWorkSpec());
    }
}
