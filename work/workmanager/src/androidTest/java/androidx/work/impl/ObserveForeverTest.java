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


//package androidx.work.impl;
//
//import static org.hamcrest.CoreMatchers.is;
//import static org.hamcrest.CoreMatchers.notNullValue;
//import static org.hamcrest.MatcherAssert.assertThat;
//
//import androidx.annotation.Nullable;
//import androidx.lifecycle.Observer;
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//import androidx.test.filters.MediumTest;
//import androidx.test.platform.app.InstrumentationRegistry;
//import androidx.work.Configuration;
//import androidx.work.ExistingWorkPolicy;
//import androidx.work.OneTimeWorkRequest;
//import androidx.work.WorkContinuation;
//import androidx.work.WorkInfo;
//import androidx.work.impl.utils.SynchronousExecutor;
//import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
//import androidx.work.worker.TestWorker;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//
///**
// * TODO remove after moving to Room 2.1.x.
// * see: b/74477406 for details.
// *
// * This test suite is being @Ignored because observeForever() can no longer be called on a
// * background thread after the move to 2.x.
// */
//@RunWith(AndroidJUnit4.class)
//@MediumTest
//@Ignore
//public class ObserveForeverTest {
//    private WorkManagerImpl mWorkManagerImpl;
//    private final OneTimeWorkRequest mWork = new OneTimeWorkRequest.Builder(TestWorker.class)
//            .addTag("foo")
//            .setInitialDelay(1, TimeUnit.HOURS)
//            .build();
//
//    @Before
//    public void init() {
//        Configuration configuration = new Configuration.Builder()
//                .setExecutor(new SynchronousExecutor())
//                .build();
//        mWorkManagerImpl = new WorkManagerImpl(
//                InstrumentationRegistry.getInstrumentation().getTargetContext(),
//                configuration, new InstantWorkTaskExecutor());
//        WorkManagerImpl.setDelegate(mWorkManagerImpl);
//    }
//
//    @After
//    public void tearDown() {
//        WorkManagerImpl.setDelegate(null);
//    }
//
//    @Test
//    @Ignore
//    public void observeForever_byTags() throws InterruptedException {
//        LoggingObserver<List<WorkInfo>> observer = new LoggingObserver<>();
//        observer.expectValue();
//
//        mWorkManagerImpl
//                .getWorkInfosByTagLiveData("foo")
//                .observeForever(observer);
//        assertThat(observer.awaitNextValue(), is(Collections.<WorkInfo>emptyList()));
//
//        forceGc();
//        observer.expectValue();
//        mWorkManagerImpl.enqueue(mWork);
//
//        List<WorkInfo> received = observer.awaitNextValue();
//        assertThat(received.size(), is(1));
//        assertThat(received.get(0).getState(), is(WorkInfo.State.ENQUEUED));
//    }
//
//    @Test
//    @Ignore
//    public void observeForever_byId() throws InterruptedException, ExecutionException {
//        LoggingObserver<WorkInfo> observer = new LoggingObserver<>();
//        observer.expectValue();
//
//        mWorkManagerImpl
//                .getWorkInfoByIdLiveData(mWork.getId())
//                .observeForever(observer);
//
//        mWorkManagerImpl.enqueue(mWork);
//
//        WorkInfo received = observer.awaitNextValue();
//        assertThat(received, is(notNullValue()));
//        assertThat(received.getState(), is(WorkInfo.State.ENQUEUED));
//
//        observer.expectValue();
//        forceGc();
//        mWorkManagerImpl.cancelAllWork().getResult().get();
//
//        assertThat(observer.awaitNextValue().getState(), is(WorkInfo.State.CANCELLED));
//    }
//
//    @Test
//    @Ignore
//    public void observeForever_uniqueWork() throws InterruptedException {
//        LoggingObserver<List<WorkInfo>> observer = new LoggingObserver<>();
//        observer.expectValue();
//
//        mWorkManagerImpl
//                .getWorkInfosForUniqueWorkLiveData("custom-id")
//                .observeForever(observer);
//        assertThat(observer.awaitNextValue(), is(Collections.<WorkInfo>emptyList()));
//
//        forceGc();
//        observer.expectValue();
//        mWorkManagerImpl.beginUniqueWork("custom-id",
//                ExistingWorkPolicy.REPLACE,
//                mWork).enqueue();
//
//        List<WorkInfo> received = observer.awaitNextValue();
//        assertThat(received.size(), is(1));
//        assertThat(received.get(0).getState(), is(WorkInfo.State.ENQUEUED));
//    }
//
//    @Test
//    @Ignore
//    public void observeForever_workContinuation() throws InterruptedException {
//        LoggingObserver<List<WorkInfo>> observer = new LoggingObserver<>();
//        observer.expectValue();
//
//        WorkContinuation workContinuation = mWorkManagerImpl.beginWith(mWork);
//        workContinuation.getWorkInfosLiveData().observeForever(observer);
//
//        assertThat(observer.awaitNextValue(), is(Collections.<WorkInfo>emptyList()));
//
//        forceGc();
//
//        observer.expectValue();
//        workContinuation.enqueue();
//
//        List<WorkInfo> received = observer.awaitNextValue();
//        assertThat(received.size(), is(1));
//        assertThat(received.get(0).getState(), is(WorkInfo.State.ENQUEUED));
//    }
//
//    private void forceGc() {
//        Runtime.getRuntime().gc();
//        Runtime.getRuntime().runFinalization();
//        Runtime.getRuntime().gc();
//        Runtime.getRuntime().runFinalization();
//    }
//
//    static class LoggingObserver<T> implements Observer<T> {
//        CountDownLatch mLatch;
//        private T mValue;
//
//        void expectValue() {
//            if (mLatch != null) {
//                throw new IllegalStateException("You've not consumed previous value yet");
//            }
//            mLatch = new CountDownLatch(1);
//        }
//
//        T awaitNextValue() throws InterruptedException {
//            assertThat(mLatch.await(10, TimeUnit.SECONDS), is(true));
//            mLatch = null;
//            return mValue;
//        }
//
//        @Override
//        public void onChanged(@Nullable T t) {
//            if (mLatch == null) {
//                throw new IllegalStateException("Not expecting a value yet");
//            }
//            mValue = t;
//            mLatch.countDown();
//        }
//    }
//}

