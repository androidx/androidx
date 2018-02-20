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

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.firebase.jobdispatcher.JobParameters;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.work.BaseWork;
import androidx.work.State;
import androidx.work.Work;
import androidx.work.WorkManagerTest;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.taskexecutor.InstantTaskExecutorRule;
import androidx.work.worker.FirebaseInfiniteTestWorker;

@RunWith(AndroidJUnit4.class)
public class FirebaseJobServiceTest extends WorkManagerTest {

    @Rule
    public InstantTaskExecutorRule mRule = new InstantTaskExecutorRule();

    private WorkDatabase mDatabase;
    private FirebaseJobService mFirebaseJobService;

    @Before
    public void setUp() {
        mDatabase = WorkManagerImpl.getInstance(InstrumentationRegistry.getTargetContext())
                .getWorkDatabase();
        mFirebaseJobService = new FirebaseJobService(); // Bleh.
        mFirebaseJobService.onCreate();
    }

    @After
    public void tearDown() {
        mFirebaseJobService.onDestroy();
    }

    @Test
    @SmallTest
    public void testOnStopJob_ResetsWorkStatus() throws InterruptedException {
        Work work = new Work.Builder(FirebaseInfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getId());
        mFirebaseJobService.onStartJob(mockParams);

        // TODO(sumir): Remove later.  Put here because WorkerWrapper sets state to RUNNING.
        Thread.sleep(1000L);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work.getId()), is(State.RUNNING));

        mFirebaseJobService.onStopJob(mockParams);
        assertThat(workSpecDao.getState(work.getId()), is(State.ENQUEUED));
    }

    @Test
    @SmallTest
    public void testOnStopJob_ReschedulesWhenNotCancelled() {
        Work work = new Work.Builder(FirebaseInfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getId());
        assertThat(mFirebaseJobService.onStartJob(mockParams), is(true));
        assertThat(mFirebaseJobService.onStopJob(mockParams), is(true));
    }

    @Test
    @SmallTest
    public void testOnStopJob_DoesNotRescheduleWhenCancelled() {
        Work work = new Work.Builder(FirebaseInfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getId());
        assertThat(mFirebaseJobService.onStartJob(mockParams), is(true));
        WorkManagerImpl.getInstance(InstrumentationRegistry.getTargetContext())
                .cancelWorkForId(work.getId());
        assertThat(mFirebaseJobService.onStopJob(mockParams), is(false));
    }

    private JobParameters createMockJobParameters(String id) {
        JobParameters jobParameters = mock(JobParameters.class);
        when(jobParameters.getTag()).thenReturn(id);
        return jobParameters;
    }

    private void insertWork(BaseWork work) {
        mDatabase.workSpecDao().insertWorkSpec(getWorkSpec(work));
    }
}
