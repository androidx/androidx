/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.background.workmanager.impl.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.arch.background.workmanager.TestLifecycleOwner;
import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class LiveDataUtilsTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void testDedupedLiveData() {
        MutableLiveData<String> originalLiveData = new MutableLiveData<>();
        LiveData<String> dedupedLiveData = LiveDataUtils.dedupedLiveDataFor(originalLiveData);
        assertThat(dedupedLiveData.getValue(), is(nullValue()));

        TestLifecycleOwner testLifecycleOwner = new TestLifecycleOwner();
        CountingObserver<String> observer = new CountingObserver<>();
        dedupedLiveData.observe(testLifecycleOwner, observer);
        assertThat(observer.mTimesUpdated, is(0));

        String value = "new value";
        originalLiveData.setValue(value);
        assertThat(dedupedLiveData.getValue(), is(value));
        assertThat(observer.mTimesUpdated, is(1));

        originalLiveData.setValue(value);
        assertThat(dedupedLiveData.getValue(), is(value));
        assertThat(observer.mTimesUpdated, is(1));

        String newerValue = "newer value";
        originalLiveData.setValue(newerValue);
        assertThat(dedupedLiveData.getValue(), is(newerValue));
        assertThat(observer.mTimesUpdated, is(2));
    }

    private static class CountingObserver<T> implements Observer<T> {

        int mTimesUpdated;

        @Override
        public void onChanged(@Nullable T t) {
            ++mTimesUpdated;
        }
    }
}
