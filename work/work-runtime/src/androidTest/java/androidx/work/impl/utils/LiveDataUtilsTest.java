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

package androidx.work.impl.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.annotation.Nullable;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.testing.TestLifecycleOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class LiveDataUtilsTest {

    private TaskExecutor mInstantWorkTaskExecutor = new InstantWorkTaskExecutor();

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void testDedupedMappedLiveData_dedupesValues() {
        Function<String, String> identityMapping = new Function<String, String>() {
            @Override
            public String apply(String input) {
                return input;
            }
        };

        MutableLiveData<String> originalLiveData = new MutableLiveData<>();
        LiveData<String> dedupedLiveData = LiveDataUtils.dedupedMappedLiveDataFor(
                originalLiveData,
                identityMapping,
                mInstantWorkTaskExecutor);
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

        dedupedLiveData.removeObservers(testLifecycleOwner);
    }

    @Test
    public void testDedupedMappedLiveData_mapsValues() {
        Function<Integer, String> intToStringMapping = new Function<Integer, String>() {
            @Override
            public String apply(Integer input) {
                return (input == null) ? "" : input.toString();
            }
        };

        MutableLiveData<Integer> originalLiveData = new MutableLiveData<>();
        LiveData<String> mappedLiveData = LiveDataUtils.dedupedMappedLiveDataFor(
                originalLiveData,
                intToStringMapping,
                mInstantWorkTaskExecutor);
        assertThat(mappedLiveData.getValue(), is(nullValue()));

        TestLifecycleOwner testLifecycleOwner = new TestLifecycleOwner();
        CountingObserver<String> observer = new CountingObserver<>();
        mappedLiveData.observe(testLifecycleOwner, observer);
        assertThat(observer.mTimesUpdated, is(0));

        Integer value = null;
        originalLiveData.setValue(value);
        assertThat(mappedLiveData.getValue(), is(""));

        value = 1337;
        originalLiveData.setValue(value);
        assertThat(mappedLiveData.getValue(), is(value.toString()));

        value = -0;
        originalLiveData.setValue(value);
        assertThat(mappedLiveData.getValue(), is(value.toString()));

        mappedLiveData.removeObservers(testLifecycleOwner);
    }

    private static class CountingObserver<T> implements Observer<T> {

        int mTimesUpdated;

        @Override
        public void onChanged(@Nullable T t) {
            ++mTimesUpdated;
        }
    }
}
