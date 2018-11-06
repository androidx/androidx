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
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.DatabaseTest;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
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
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.SynchronousExecutor;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.worker.EchoingWorker;
import androidx.work.worker.SleepTestWorker;
import androidx.work.worker.StopAwareWorker;
import androidx.work.worker.TestWorker;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ConstraintTrackingWorkerTest extends DatabaseTest {

    private static final long DELAY_IN_MS = 100;
    private static final long TEST_TIMEOUT_IN_MS = 6000L;
    private static final String TEST_ARGUMENT_NAME = "test";

    private Context mContext;
    private Handler mHandler;

    private OneTimeWorkRequest mWork;
    private WorkerWrapper mWorkerWrapper;
    private ConstraintTrackingWorker mWorker;
    private WorkManagerImpl mWorkManagerImpl;
    private Configuration mConfiguration;
    private TaskExecutor mWorkTaskExecutor;
    private Scheduler mScheduler;
    private Trackers mTracker;
    private BatteryChargingTracker mBatteryChargingTracker;
    private BatteryNotLowTracker mBatteryNotLowTracker;
    private NetworkStateTracker mNetworkStateTracker;
    private StorageNotLowTracker mStorageNotLowTracker;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
        mHandler = new Handler(Looper.getMainLooper());
        mConfiguration = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .build();
        mWorkTaskExecutor = new InstantWorkTaskExecutor();

        mWorkManagerImpl = mock(WorkManagerImpl.class);
        mScheduler = mock(Scheduler.class);
        when(mWorkManagerImpl.getWorkDatabase()).thenReturn(mDatabase);
        when(mWorkManagerImpl.getConfiguration()).thenReturn(mConfiguration);

        mBatteryChargingTracker = spy(new BatteryChargingTracker(mContext));
        mBatteryNotLowTracker = spy(new BatteryNotLowTracker(mContext));
        // Requires API 24+ types.
        mNetworkStateTracker = mock(NetworkStateTracker.class);
        mStorageNotLowTracker = spy(new StorageNotLowTracker(mContext));
        mTracker = mock(Trackers.class);

        when(mTracker.getBatteryChargingTracker()).thenReturn(mBatteryChargingTracker);
        when(mTracker.getBatteryNotLowTracker()).thenReturn(mBatteryNotLowTracker);
        when(mTracker.getNetworkStateTracker()).thenReturn(mNetworkStateTracker);
        when(mTracker.getStorageNotLowTracker()).thenReturn(mStorageNotLowTracker);

        // Override Trackers being used by WorkConstraintsProxy
        Trackers.setInstance(mTracker);
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

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testConstraintTrackingWorker_onConstraintsChanged() throws InterruptedException {
        when(mBatteryNotLowTracker.getInitialState()).thenReturn(true);
        setupDelegateForExecution(SleepTestWorker.class.getName(),
                Executors.newSingleThreadExecutor());
        WorkerWrapper.Builder builder = createWorkerWrapperBuilder();
        builder.withWorker(mWorker).withSchedulers(Collections.singletonList(mScheduler));

        mWorkerWrapper = builder.build();
        mWorkTaskExecutor.getBackgroundExecutor().execute(mWorkerWrapper);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBatteryNotLowTracker.setState(false);
            }
        }, DELAY_IN_MS);

        Thread.sleep(TEST_TIMEOUT_IN_MS);
        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(mWork.getStringId());
        assertThat(workSpec.state, is(WorkInfo.State.ENQUEUED));
    }

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testConstraintTrackingWorker_onConstraintsChangedTwice()
            throws InterruptedException {
        when(mBatteryNotLowTracker.getInitialState()).thenReturn(true);
        setupDelegateForExecution(SleepTestWorker.class.getName(),
                Executors.newSingleThreadExecutor());

        WorkerWrapper.Builder builder = createWorkerWrapperBuilder();
        builder.withWorker(mWorker).withSchedulers(Collections.singletonList(mScheduler));

        mWorkerWrapper = builder.build();
        mWorkTaskExecutor.getBackgroundExecutor().execute(mWorkerWrapper);

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
    }

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testConstraintTrackingWorker_delegatesInterruption() throws InterruptedException {
        setupDelegateForExecution(StopAwareWorker.class.getName(),
                Executors.newSingleThreadExecutor());

        WorkerWrapper.Builder builder = createWorkerWrapperBuilder();
        builder.withWorker(mWorker).withSchedulers(Collections.singletonList(mScheduler));

        mWorkerWrapper = builder.build();
        mWorkTaskExecutor.getBackgroundExecutor().execute(mWorkerWrapper);

        Thread.sleep(TEST_TIMEOUT_IN_MS);

        mWorkerWrapper.interrupt(true);

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

        WorkerFactory workerFactory = WorkerFactory.getDefaultWorkerFactory();
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
                        workerFactory));

        assertThat(worker, is(notNullValue()));
        assertThat(worker,
                is(CoreMatchers.<ListenableWorker>instanceOf(ConstraintTrackingWorker.class)));
        mWorker = spy((ConstraintTrackingWorker) worker);
        when(mWorker.getWorkDatabase()).thenReturn(mDatabase);
    }

    private WorkerWrapper.Builder createWorkerWrapperBuilder() {
        return new WorkerWrapper.Builder(
                mContext,
                mConfiguration,
                mWorkTaskExecutor,
                mDatabase,
                mWork.getStringId());
    }
}
