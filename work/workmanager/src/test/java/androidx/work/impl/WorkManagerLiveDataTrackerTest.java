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

package androidx.work.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RunWith(JUnit4.class)
public class WorkManagerLiveDataTrackerTest {
    private WorkManagerLiveDataTracker mContainer = new WorkManagerLiveDataTracker();

    @Test
    public void add() {
        LiveData liveData = createLiveData();
        assertThat(mContainer.mLiveDataSet, is(setOf()));
        mContainer.onActive(liveData);
        assertThat(mContainer.mLiveDataSet, is(setOf(liveData)));
    }

    @Test
    public void add_twice() {
        LiveData liveData = createLiveData();
        mContainer.onActive(liveData);
        mContainer.onActive(liveData);
        assertThat(mContainer.mLiveDataSet, is(setOf(liveData)));
    }

    @Test
    public void remove() {
        LiveData liveData = createLiveData();
        mContainer.onActive(liveData);
        mContainer.onInactive(liveData);
        assertThat(mContainer.mLiveDataSet, is(setOf()));
    }

    @Test
    public void remove_twice() {
        LiveData liveData = createLiveData();
        mContainer.onActive(liveData);
        mContainer.onInactive(liveData);
        mContainer.onInactive(liveData);
        assertThat(mContainer.mLiveDataSet, is(setOf()));
    }

    @Test
    public void addRemoveMultiple() {
        LiveData ld1 = createLiveData();
        LiveData ld2 = createLiveData();
        assertThat(mContainer.mLiveDataSet, is(setOf()));
        mContainer.onActive(ld1);
        mContainer.onActive(ld2);
        assertThat(mContainer.mLiveDataSet, is(setOf(ld1, ld2)));
        mContainer.onInactive(ld1);
        assertThat(mContainer.mLiveDataSet, is(setOf(ld2)));
        mContainer.onInactive(ld1); // intentional
        assertThat(mContainer.mLiveDataSet, is(setOf(ld2)));
        mContainer.onActive(ld1);
        assertThat(mContainer.mLiveDataSet, is(setOf(ld1, ld2)));
        mContainer.onActive(ld1); // intentional
        assertThat(mContainer.mLiveDataSet, is(setOf(ld1, ld2)));
        mContainer.onInactive(ld2);
        assertThat(mContainer.mLiveDataSet, is(setOf(ld1)));
        mContainer.onInactive(ld1);
        assertThat(mContainer.mLiveDataSet, is(setOf()));
        mContainer.onActive(ld1);
        assertThat(mContainer.mLiveDataSet, is(setOf(ld1)));
        mContainer.onActive(ld2);
        assertThat(mContainer.mLiveDataSet, is(setOf(ld1, ld2)));
    }

    private LiveData<String> createLiveData() {
        return mContainer.track(new MutableLiveData<String>());
    }

    private static Set<LiveData> setOf(LiveData... items) {
        return new HashSet<>(Arrays.asList(items));
    }
}
