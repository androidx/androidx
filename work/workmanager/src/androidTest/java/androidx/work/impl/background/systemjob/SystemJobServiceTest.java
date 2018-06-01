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

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.job.JobParameters;
import android.arch.core.executor.ArchTaskExecutor;
import android.arch.core.executor.TaskExecutor;
import android.content.Context;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.Configuration;
import androidx.work.OneTimeWorkRequest;
import androidx.work.State;
import androidx.work.WorkManagerTest;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.taskexecutor.InstantTaskExecutorRule;
import androidx.work.worker.InfiniteTestWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL)
public class SystemJobServiceTest extends WorkManagerTest {

    @Rule
    public InstantTaskExecutorRule mRule = new InstantTaskExecutorRule();

    private WorkManagerImpl mWorkManagerImpl;
    private WorkDatabase mDatabase;
    private SystemJobService mSystemJobServiceSpy;

    @Before
    public void setUp() {
        // TODO: Remove after we figure out why these tests execute on API 17 emulators.
        if (Build.VERSION.SDK_INT < WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            return;
        }

        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override
            public void executeOnDiskIO(@NonNull Runnable runnable) {
                runnable.run();
            }

            @Override
            public void postToMainThread(@NonNull Runnable runnable) {
                runnable.run();
            }

            @Override
            public boolean isMainThread() {
                return true;
            }
        });

        Context context = InstrumentationRegistry.getTargetContext();
        Configuration configuration = new Configuration.Builder()
                .setExecutor(Executors.newSingleThreadExecutor())
                .build();
        mWorkManagerImpl = new WorkManagerImpl(context, configuration);
        WorkManagerImpl.setDelegate(mWorkManagerImpl);
        mDatabase = mWorkManagerImpl.getWorkDatabase();
        mSystemJobServiceSpy = spy(new SystemJobService());
        doNothing().when(mSystemJobServiceSpy).onExecuted(anyString(), anyBoolean(), anyBoolean());
        mSystemJobServiceSpy.onCreate();
    }

    @After
    public void tearDown() {
        // TODO: Remove after we figure out why these tests execute on API 17 emulators.
        if (Build.VERSION.SDK_INT < WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            return;
        }

        mSystemJobServiceSpy.onDestroy();
        WorkManagerImpl.setDelegate(null);
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    @LargeTest
    public void testOnStopJob_ResetsWorkStatus() throws InterruptedException {
        // TODO: Remove after we figure out why these tests execute on API 17 emulators.
        if (Build.VERSION.SDK_INT < WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            return;
        }

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getStringId());
        mSystemJobServiceSpy.onStartJob(mockParams);

        // TODO(sumir): Remove later.  Put here because WorkerWrapper sets state to RUNNING.
        Thread.sleep(5000L);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work.getStringId()), is(State.RUNNING));

        mSystemJobServiceSpy.onStopJob(mockParams);
        // TODO(rahulrav): Figure out why this test is flaky.
        Thread.sleep(5000L);
        assertThat(workSpecDao.getState(work.getStringId()), is(State.ENQUEUED));
    }

    @Test
    @SmallTest
    public void testOnStopJob_ReschedulesWhenNotCancelled() {
        // TODO: Remove after we figure out why these tests execute on API 17 emulators.
        if (Build.VERSION.SDK_INT < WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            return;
        }

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getStringId());
        assertThat(mSystemJobServiceSpy.onStartJob(mockParams), is(true));
        assertThat(mSystemJobServiceSpy.onStopJob(mockParams), is(true));
    }

    @Test
    @SmallTest
    public void testOnStopJob_DoesNotRescheduleWhenCancelled() {
        // TODO: Remove after we figure out why these tests execute on API 17 emulators.
        if (Build.VERSION.SDK_INT < WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            return;
        }

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getStringId());
        assertThat(mSystemJobServiceSpy.onStartJob(mockParams), is(true));
        WorkManagerImpl.getInstance().cancelWorkById(work.getId());
        assertThat(mSystemJobServiceSpy.onStopJob(mockParams), is(false));
    }

    @Test
    @SmallTest
    public void testStartJob_ReturnsFalseWithDuplicateJob() {
        // TODO: Remove after we figure out why these tests execute on API 17 emulators.
        if (Build.VERSION.SDK_INT < WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            return;
        }

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getStringId());
        assertThat(mSystemJobServiceSpy.onStartJob(mockParams), is(true));
        assertThat(mSystemJobServiceSpy.onStartJob(mockParams), is(false));
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = 24)
    public void testStartJob_PassesContentUriTriggers() throws InterruptedException {
        // TODO: Remove after we figure out why these tests execute on API 17 emulators.
        if (Build.VERSION.SDK_INT < WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            return;
        }

        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(ContentUriTriggerLoggingWorker.class).build();
        insertWork(work);

        final String[] testContentAuthorities = new String[] {
                work.getStringId(),
                "yet another " + work.getStringId()
        };

        final Uri[] testContentUris = new Uri[] {
                Uri.parse("http://www.android.com"),
                Uri.parse("http://www.google.com")
        };

        JobParameters mockParams = createMockJobParameters(work.getStringId());
        when(mockParams.getTriggeredContentAuthorities()).thenReturn(testContentAuthorities);
        when(mockParams.getTriggeredContentUris()).thenReturn(testContentUris);

        assertThat(ContentUriTriggerLoggingWorker.sTimesUpdated, is(0));
        assertThat(mSystemJobServiceSpy.onStartJob(mockParams), is(true));

        Thread.sleep(1000L);

        assertThat(ContentUriTriggerLoggingWorker.sTimesUpdated, is(1));
        assertThat(ContentUriTriggerLoggingWorker.sTriggeredContentAuthorities,
                is(testContentAuthorities));
        assertThat(ContentUriTriggerLoggingWorker.sTriggeredContentUris, is(testContentUris));
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = 28)
    public void testStartJob_passesNetwork() throws InterruptedException {
        WorkRequest work = new OneTimeWorkRequest.Builder(NetworkLoggingWorker.class).build();
        insertWork(work);

        Network mockNetwork = mock(Network.class);

        JobParameters mockParams = createMockJobParameters(work.getStringId());
        when(mockParams.getNetwork()).thenReturn(mockNetwork);

        assertThat(NetworkLoggingWorker.sTimesUpdated, is(0));
        assertThat(mSystemJobServiceSpy.onStartJob(mockParams), is(true));

        Thread.sleep(1000L);

        assertThat(NetworkLoggingWorker.sTimesUpdated, is(1));
        assertThat(NetworkLoggingWorker.sNetwork, is(mockNetwork));
    }

    private JobParameters createMockJobParameters(String id) {
        JobParameters jobParameters = mock(JobParameters.class);

        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putString(SystemJobInfoConverter.EXTRA_WORK_SPEC_ID, id);
        when(jobParameters.getExtras()).thenReturn(persistableBundle);

        return jobParameters;
    }

    private void insertWork(WorkRequest work) {
        mDatabase.workSpecDao().insertWorkSpec(getWorkSpec(work));
    }

    public static class ContentUriTriggerLoggingWorker extends Worker {

        static int sTimesUpdated = 0;
        static String[] sTriggeredContentAuthorities;
        static Uri[] sTriggeredContentUris;

        @Override
        public @NonNull Result doWork() {
            synchronized (ContentUriTriggerLoggingWorker.class) {
                ++sTimesUpdated;
                sTriggeredContentAuthorities = getTriggeredContentAuthorities();
                sTriggeredContentUris = getTriggeredContentUris();
            }
            return Result.SUCCESS;
        }
    }

    public static class NetworkLoggingWorker extends Worker {

        static int sTimesUpdated = 0;
        static Network sNetwork;

        @Override
        public @NonNull Result doWork() {
            synchronized (NetworkLoggingWorker.class) {
                ++sTimesUpdated;
                sNetwork = getNetwork();
            }
            return Result.SUCCESS;
        }
    }
}
