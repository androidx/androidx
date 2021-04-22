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

package androidx.work.impl.workers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.testutils.RepeatRule;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.DatabaseTest;
import androidx.work.ForegroundUpdater;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.ProgressUpdater;
import androidx.work.WorkInfo;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.WorkerWrapper;
import androidx.work.impl.constraints.trackers.BatteryChargingTracker;
import androidx.work.impl.constraints.trackers.BatteryNotLowTracker;
import androidx.work.impl.constraints.trackers.NetworkStateTracker;
import androidx.work.impl.constraints.trackers.StorageNotLowTracker;
import androidx.work.impl.constraints.trackers.Trackers;
import androidx.work.impl.foreground.ForegroundProcessor;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.SynchronousExecutor;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.worker.EchoingWorker;
import androidx.work.worker.SleepTestWorker;
import androidx.work.worker.StopAwareForegroundWorker;
import androidx.work.worker.StopAwareWorker;
import androidx.work.worker.TestWorker;

import org.hamcrest.CoreMatchers;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ConstraintTrackingWorkerTest extends DatabaseTest {

    private static final long DELAY_IN_MS = 100;
    private static final long TEST_TIMEOUT_IN_MS = 6000L;
    private static final String TEST_ARGUMENT_NAME = "test";

    private Context mContext;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private WorkerFactory mWorkerFactory;

    private OneTimeWorkRequest mWork;
    private WorkerWrapper mWorkerWrapper;
    private ConstraintTrackingWorker mWorker;
    private WorkManagerImpl mWorkManagerImpl;
    private Configuration mConfiguration;
    private TaskExecutor mWorkTaskExecutor;
    private Scheduler mScheduler;
    private ProgressUpdater mProgressUpdater;
    private ForegroundUpdater mForegroundUpdater;
    private ForegroundProcessor mForegroundProcessor;
    private Trackers mTracker;
    private BatteryChargingTracker mBatteryChargingTracker;
    private BatteryNotLowTracker mBatteryNotLowTracker;
    private NetworkStateTracker mNetworkStateTracker;
    private StorageNotLowTracker mStorageNotLowTracker;

    @Rule
    public final RepeatRule mRepeatRule = new RepeatRule();

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext().getApplicationContext();
        mHandlerThread = new HandlerThread("ConstraintTrackingHandler");
        mHandlerThread.start();
        mHandler = HandlerCompat.createAsync(mHandlerThread.getLooper());
        mWorkerFactory = new SpyingWorkerFactory();
        mConfiguration = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .setWorkerFactory(mWorkerFactory)
                .build();
        mWorkTaskExecutor = new InstantWorkTaskExecutor();

        mWorkManagerImpl = mock(WorkManagerImpl.class);
        mScheduler = mock(Scheduler.class);
        mProgressUpdater = mock(ProgressUpdater.class);
        mForegroundUpdater = mock(ForegroundUpdater.class);
        mForegroundProcessor = mock(ForegroundProcessor.class);
        when(mWorkManagerImpl.getWorkDatabase()).thenReturn(mDatabase);
        when(mWorkManagerImpl.getWorkTaskExecutor()).thenReturn(mWorkTaskExecutor);
        when(mWorkManagerImpl.getConfiguration()).thenReturn(mConfiguration);

        mBatteryChargingTracker = spy(new BatteryChargingTracker(mContext, mWorkTaskExecutor));
        mBatteryNotLowTracker = spy(new BatteryNotLowTracker(mContext, mWorkTaskExecutor));
        // Requires API 24+ types.
        mNetworkStateTracker = mock(NetworkStateTracker.class);
        mStorageNotLowTracker = spy(new StorageNotLowTracker(mContext, mWorkTaskExecutor));
        mTracker = mock(Trackers.class);

        when(mTracker.getBatteryChargingTracker()).thenReturn(mBatteryChargingTracker);
        when(mTracker.getBatteryNotLowTracker()).thenReturn(mBatteryNotLowTracker);
        when(mTracker.getNetworkStateTracker()).thenReturn(mNetworkStateTracker);
        when(mTracker.getStorageNotLowTracker()).thenReturn(mStorageNotLowTracker);

        // Override Trackers being used by WorkConstraintsProxy
        Trackers.setInstance(mTracker);
    }

    @After
    public void tearDown() {
        mHandlerThread.quitSafely();
    }

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testConstraintTrackingWorker_onConstraintsMet() {
        when(mBatteryNotLowTracker.getInitialState()).thenReturn(true);
        setupDelegateForExecution(EchoingWorker.class.getName(), new SynchronousExecutor());

        WorkerWrapper.Builder builder = createWorkerWrapperBuilder();
        builder.withWorker(mWorker).withSchedulers(Collections.singletonList(mScheduler));

        mWorkerWrapper = builder.build();
        mWorkTaskExecutor.getBackgroundExecutor().execute(mWorkerWrapper);

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(mWork.getStringId());
        assertThat(workSpec.state, is(WorkInfo.State.SUCCEEDED));
        Data output = workSpec.output;
        assertThat(output.getBoolean(TEST_ARGUMENT_NAME, false), is(true));
    }

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testConstraintTrackingWorker_onConstraintsNotMet() {
        when(mBatteryNotLowTracker.getInitialState()).thenReturn(false);
        setupDelegateForExecution(TestWorker.class.getName(), new SynchronousExecutor());
        WorkerWrapper.Builder builder = createWorkerWrapperBuilder();
        builder.withWorker(mWorker).withSchedulers(Collections.singletonList(mScheduler));

        mWorkerWrapper = builder.build();
        mWorkTaskExecutor.getBackgroundExecutor().execute(mWorkerWrapper);

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(mWork.getStringId());
        assertThat(workSpec.state, is(WorkInfo.State.ENQUEUED));
    }

    @Ignore
    @Test
    @SdkSuppress(minSdkVersion = 24, maxSdkVersion = 25)
    public void testConstraintTrackingWorker_onConstraintsChanged() throws InterruptedException {
        // This test is flaky on API 23 for some reason.
        when(mBatteryNotLowTracker.getInitialState()).thenReturn(true);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        setupDelegateForExecution(SleepTestWorker.class.getName(), executorService);
        WorkerWrapper.Builder builder = createWorkerWrapperBuilder();
        builder.withWorker(mWorker).withSchedulers(Collections.singletonList(mScheduler));

        mWorkerWrapper = builder.build();
        executorService.execute(mWorkerWrapper);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBatteryNotLowTracker.setState(false);
            }
        }, DELAY_IN_MS);

        Thread.sleep(TEST_TIMEOUT_IN_MS);
        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(mWork.getStringId());
        assertThat(workSpec.state, is(WorkInfo.State.ENQUEUED));
        executorService.shutdown();
    }

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    @FlakyTest(bugId = 180654418, detail = "Passes locally all the time.")
    public void testConstraintTrackingWorker_onConstraintsChangedTwice()
            throws InterruptedException {
        when(mBatteryNotLowTracker.getInitialState()).thenReturn(true);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        setupDelegateForExecution(SleepTestWorker.class.getName(), executorService);

        WorkerWrapper.Builder builder = createWorkerWrapperBuilder();
        builder.withWorker(mWorker).withSchedulers(Collections.singletonList(mScheduler));

        mWorkerWrapper = builder.build();
        executorService.execute(mWorkerWrapper);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBatteryNotLowTracker.setState(false);
            }
        }, DELAY_IN_MS);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBatteryNotLowTracker.setState(true);
            }
        }, DELAY_IN_MS);

        Thread.sleep(TEST_TIMEOUT_IN_MS);
        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(mWork.getStringId());
        assertThat(workSpec.state, is(WorkInfo.State.ENQUEUED));
        executorService.shutdown();
    }

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testConstraintTrackingWorker_delegatesInterruption() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        setupDelegateForExecution(StopAwareWorker.class.getName(), executorService);

        WorkerWrapper.Builder builder = createWorkerWrapperBuilder();
        builder.withWorker(mWorker).withSchedulers(Collections.singletonList(mScheduler));

        mWorkerWrapper = builder.build();
        executorService.execute(mWorkerWrapper);

        Thread.sleep(TEST_TIMEOUT_IN_MS);
        executorService.shutdown();
        mWorkerWrapper.interrupt();
        assertThat(mWorker.isStopped(), is(true));
        assertThat(mWorker.getDelegate(), is(notNullValue()));
        assertThat(mWorker.getDelegate().isStopped(), is(true));
    }

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testConstraintTrackingWorker_delegatesInterruption_once()
            throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        setupDelegateForExecution(StopAwareWorker.class.getName(), executorService);

        WorkerWrapper.Builder builder = createWorkerWrapperBuilder();
        builder.withWorker(mWorker).withSchedulers(Collections.singletonList(mScheduler));

        mWorkerWrapper = builder.build();
        executorService.execute(mWorkerWrapper);

        Thread.sleep(TEST_TIMEOUT_IN_MS);
        executorService.shutdown();
        mWorkerWrapper.interrupt();
        mWorkerWrapper.interrupt();
        verify(mWorker.getDelegate(), times(1)).onStopped();
    }

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testConstraintTrackingWorker_delegatesIsRunInForeground()
            throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        setupDelegateForExecution(StopAwareForegroundWorker.class.getName(), executorService);
        WorkerWrapper.Builder builder = createWorkerWrapperBuilder();
        builder.withWorker(mWorker).withSchedulers(Collections.singletonList(mScheduler));

        mWorkerWrapper = builder.build();
        executorService.execute(mWorkerWrapper);
        Thread.sleep(TEST_TIMEOUT_IN_MS);

        mWorkerWrapper.interrupt();
        executorService.shutdown();
        assertThat(mWorker.isRunInForeground(), is(true));
        assertThat(mWorker.isStopped(), is(true));
        assertThat(mWorker.getDelegate(), is(notNullValue()));
        assertThat(mWorker.getDelegate().isStopped(), is(true));
    }

    private void setupDelegateForExecution(@NonNull String delegateName, Executor executor) {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

        Data input = new Data.Builder()
                .putString(ConstraintTrackingWorker.ARGUMENT_CLASS_NAME, delegateName)
                .putBoolean(TEST_ARGUMENT_NAME, true)
                .build();

        mWork = new OneTimeWorkRequest.Builder(ConstraintTrackingWorker.class)
                .setInputData(input)
                .setConstraints(constraints)
                .build();

        insertWork(mWork);

        WorkerFactory workerFactory = mConfiguration.getWorkerFactory();
        ListenableWorker worker = workerFactory.createWorkerWithDefaultFallback(
                mContext.getApplicationContext(),
                ConstraintTrackingWorker.class.getName(),
                new WorkerParameters(
                        mWork.getId(),
                        input,
                        Collections.<String>emptyList(),
                        new WorkerParameters.RuntimeExtras(),
                        1,
                        executor,
                        mWorkTaskExecutor,
                        workerFactory,
                        mProgressUpdater,
                        mForegroundUpdater));

        assertThat(worker, is(notNullValue()));
        assertThat(worker,
                is(CoreMatchers.<ListenableWorker>instanceOf(ConstraintTrackingWorker.class)));
        // mWorker is already a spy
        mWorker = (ConstraintTrackingWorker) worker;
        when(mWorker.getWorkDatabase()).thenReturn(mDatabase);
    }

    private WorkerWrapper.Builder createWorkerWrapperBuilder() {
        return new WorkerWrapper.Builder(
                mContext,
                mConfiguration,
                mWorkTaskExecutor,
                mForegroundProcessor,
                mDatabase,
                mWork.getStringId());
    }

    static class SpyingWorkerFactory extends WorkerFactory {
        private final WorkerFactory mDefaultFactory;

        SpyingWorkerFactory() {
            mDefaultFactory = WorkerFactory.getDefaultWorkerFactory();
        }

        @Nullable
        @Override
        public ListenableWorker createWorker(
                @NonNull @NotNull Context appContext,
                @NonNull @NotNull String workerClassName,
                @NonNull @NotNull WorkerParameters workerParameters) {

            ListenableWorker worker = mDefaultFactory.createWorkerWithDefaultFallback(
                    appContext,
                    workerClassName,
                    workerParameters);

            if (worker != null) {
                worker = spy(worker);
            }

            return worker;
        }
    }
}
