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

package androidx.lifecycle;

import static androidx.lifecycle.Lifecycle.Event.ON_START;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.annotation.Nullable;
import androidx.arch.core.executor.JunitTaskExecutorRule;
import androidx.arch.core.executor.TaskExecutor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(JUnit4.class)
public class ThreadedLiveDataTest {

    private static final int TIMEOUT_SECS = 3;

    @Rule
    public JunitTaskExecutorRule mTaskExecutorRule = new JunitTaskExecutorRule(1, false);

    private LiveData<String> mLiveData;
    private LifecycleOwner mLifecycleOwner;
    private LifecycleRegistry mRegistry;

    @Before
    public void init() {
        mLiveData = new MutableLiveData<>();
        mLifecycleOwner = mock(LifecycleOwner.class);
        mRegistry = new LifecycleRegistry(mLifecycleOwner);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mRegistry);
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
                mLiveData.observe(mLifecycleOwner, observer);
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
