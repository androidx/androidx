/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.support.lifecycle;

import static com.android.support.lifecycle.Lifecycle.ON_START;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.support.annotation.Nullable;
import android.support.test.filters.SmallTest;

import com.android.support.apptoolkit.testing.JunitTaskExecutorRule;
import com.android.support.executors.TaskExecutor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SmallTest
public class ThreadedLiveDataTest {

    private static final int TIMEOUT_SECS = 3;

    @Rule
    public JunitTaskExecutorRule mTaskExecutorRule = new JunitTaskExecutorRule(1, false);

    private LiveData<String> mLiveData;
    private LifecycleProvider mProvider;
    private LifecycleRegistry mRegistry;

    @Before
    public void init() {
        mLiveData = new LiveData<>();
        mProvider = mock(LifecycleProvider.class);
        mRegistry = new LifecycleRegistry(mProvider);
        when(mProvider.getLifecycle()).thenReturn(mRegistry);
    }

    @Test
    public void testPostValue() throws InterruptedException {
        final TaskExecutor taskExecutor = mTaskExecutorRule.getTaskExecutor();
        final CountDownLatch finishTestLatch = new CountDownLatch(1);
        final Observer<String> observer = new Observer<String>() {
            @Override
            public void onChanged(@Nullable String newValue) {
                try {
                    assertThat(taskExecutor.isMainThread(), is(true));
                    assertThat(newValue, is("success"));
                } finally {
                    finishTestLatch.countDown();
                }
            }
        };
        taskExecutor.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                mRegistry.handleLifecycleEvent(ON_START);
                mLiveData.observe(mProvider, observer);
                final CountDownLatch latch = new CountDownLatch(1);
                taskExecutor.executeOnDiskIO(new Runnable() {
                    @Override
                    public void run() {
                        mLiveData.postValue("fail");
                        mLiveData.postValue("success");
                        latch.countDown();
                    }
                });
                try {
                    assertThat(latch.await(TIMEOUT_SECS, TimeUnit.SECONDS), is(true));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        assertThat(finishTestLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS), is(true));
    }
}
