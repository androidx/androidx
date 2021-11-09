/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ads.identifier.benchmark;

import static androidx.ads.identifier.AdvertisingIdUtils.GET_AD_ID_ACTION;
import static androidx.ads.identifier.benchmark.SampleAdvertisingIdProvider.DUMMY_AD_ID;
import static androidx.ads.identifier.testing.MockPackageManagerHelper.createServiceResolveInfo;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.ads.identifier.provider.internal.AdvertisingIdService;
import androidx.ads.identifier.testing.MockPackageManagerHelper;
import androidx.annotation.NonNull;
import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public class AdvertisingIdBenchmark {

    private static final int CONCURRENCY_NUM = 10;
    private static final String SERVICE_NAME = AdvertisingIdService.class.getName();

    @Rule
    public BenchmarkRule mBenchmarkRule = new BenchmarkRule();

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        Context applicationContext = ApplicationProvider.getApplicationContext();

        mContext = mockContext(applicationContext);
    }

    private static Context mockContext(Context context) throws Exception {
        MockPackageManagerHelper mockPackageManagerHelper = new MockPackageManagerHelper();
        mockPackageManagerHelper.mockQueryGetAdIdServices(Lists.newArrayList(
                createServiceResolveInfo(context.getPackageName(), SERVICE_NAME)));

        return new ContextWrapper(context) {
            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public PackageManager getPackageManager() {
                return mockPackageManagerHelper.getMockPackageManager();
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        clearAdvertisingIdConnection();
        stopAdvertisingIdService();
    }

    private void clearAdvertisingIdConnection() throws Exception {
        Method method = androidx.ads.identifier.AdvertisingIdClient.class.getDeclaredMethod(
                "clearConnectionClient");
        method.setAccessible(true);
        method.invoke(null);
    }

    private void stopAdvertisingIdService() {
        Intent serviceIntent = new Intent(GET_AD_ID_ACTION);
        serviceIntent.setClassName(mContext.getPackageName(), SERVICE_NAME);
        mContext.stopService(serviceIntent);
    }

    @Test
    public void getAdvertisingIdInfo() throws Exception {
        final BenchmarkState state = mBenchmarkRule.getState();
        while (state.keepRunning()) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            getAdvertisingIdInfoListenableFuture(countDownLatch);
            countDownLatch.await();
        }
    }

    private void getAdvertisingIdInfoListenableFuture(CountDownLatch countDownLatch) {
        ListenableFuture<androidx.ads.identifier.AdvertisingIdInfo>
                advertisingIdInfoListenableFuture =
                androidx.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(mContext);
        Futures.addCallback(advertisingIdInfoListenableFuture,
                new FutureCallback<androidx.ads.identifier.AdvertisingIdInfo>() {
                    @Override
                    public void onSuccess(
                            androidx.ads.identifier.AdvertisingIdInfo advertisingIdInfo) {
                        assertThat(advertisingIdInfo.getId()).isEqualTo(DUMMY_AD_ID);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                }, MoreExecutors.directExecutor());
    }

    @Test
    public void getAdvertisingIdInfo_worker() throws Exception {
        WorkManager workManager = WorkManager.getInstance(mContext);
        workManager.cancelAllWork();
        final BenchmarkState state = mBenchmarkRule.getState();
        while (state.keepRunning()) {
            workManager.enqueue(OneTimeWorkRequest.from(GetAdInfoWorker.class)).getResult().get();
        }
    }

    /** Get the Advertising ID on a worker thread. */
    public static class GetAdInfoWorker extends Worker {
        public GetAdInfoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            try {
                Context context = mockContext(getApplicationContext());
                androidx.ads.identifier.AdvertisingIdInfo advertisingIdInfo =
                        androidx.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(
                                context).get();
                assertThat(advertisingIdInfo.getId()).isEqualTo(DUMMY_AD_ID);
            } catch (Exception e) {
                return Result.failure();
            }
            return Result.success();
        }
    }

    @Test
    @SuppressWarnings("deprecation") /* AsyncTask */
    public void getAdvertisingIdInfo_asyncTask() throws Exception {
        final BenchmarkState state = mBenchmarkRule.getState();
        while (state.keepRunning()) {
            androidx.ads.identifier.AdvertisingIdInfo advertisingIdInfo =
                    new android.os.AsyncTask<Void, Void,
                            androidx.ads.identifier.AdvertisingIdInfo>() {
                        @Override
                        protected androidx.ads.identifier.AdvertisingIdInfo doInBackground(
                                Void... voids) {
                            try {
                                return androidx.ads.identifier.AdvertisingIdClient
                                        .getAdvertisingIdInfo(mContext).get();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }.execute().get();
            assertThat(advertisingIdInfo.getId()).isEqualTo(DUMMY_AD_ID);
        }
    }

    @Test
    public void getAdvertisingIdInfo_thread() throws Exception {
        final BenchmarkState state = mBenchmarkRule.getState();
        while (state.keepRunning()) {
            Thread thread = new Thread(() -> {
                try {
                    androidx.ads.identifier.AdvertisingIdInfo advertisingIdInfo =
                            androidx.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(
                                    mContext).get();
                    assertThat(advertisingIdInfo.getId()).isEqualTo(DUMMY_AD_ID);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
            thread.join();
        }
    }

    @Test
    public void getAdvertisingIdInfo_concurrency() throws Exception {
        getAdvertisingIdInfo_concurrencyWithDelay(0);
    }

    @Test
    public void getAdvertisingIdInfo_concurrencyWithDelay1Millis() throws Exception {
        getAdvertisingIdInfo_concurrencyWithDelay(1);
    }

    @Test
    public void getAdvertisingIdInfo_concurrencyWithDelay10Millis() throws Exception {
        getAdvertisingIdInfo_concurrencyWithDelay(10);
    }

    private void getAdvertisingIdInfo_concurrencyWithDelay(long millis) throws Exception {
        final BenchmarkState state = mBenchmarkRule.getState();
        while (state.keepRunning()) {
            CountDownLatch countDownLatch = new CountDownLatch(CONCURRENCY_NUM);
            for (int i = 0; i < CONCURRENCY_NUM; i++) {
                if (millis != 0) {
                    Thread.sleep(millis);
                }

                getAdvertisingIdInfoListenableFuture(countDownLatch);
            }
            countDownLatch.await();
        }
    }
}
