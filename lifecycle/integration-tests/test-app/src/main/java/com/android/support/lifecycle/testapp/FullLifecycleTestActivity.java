/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.lifecycle.testapp;

import static com.android.support.lifecycle.testapp.FullLifecycleTestActivity.TestEvent
        .ACTIVITY_CALLBACK;
import static com.android.support.lifecycle.testapp.FullLifecycleTestActivity.TestEvent
        .LIFECYCLE_EVENT;

import android.os.Bundle;
import android.util.Pair;

import com.android.support.lifecycle.Lifecycle;
import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.LifecycleObserver;
import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.OnLifecycleEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Activity for testing full lifecycle
 */
public class FullLifecycleTestActivity extends LifecycleActivity {

    public static final long TIMEOUT = 5;

    class TestObserver implements LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.ON_CREATE)
        void create(LifecycleProvider pr, int event) {
            mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, event));
        }

        @OnLifecycleEvent(Lifecycle.ON_START)
        void start(LifecycleProvider pr, int event) {
            mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, event));
        }

        @OnLifecycleEvent(Lifecycle.ON_RESUME)
        void resume(LifecycleProvider pr, int event) {
            mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, event));
        }
        @OnLifecycleEvent(Lifecycle.ON_PAUSE)
        void pause(LifecycleProvider pr, int event) {
            mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, event));
        }

        @OnLifecycleEvent(Lifecycle.ON_STOP)
        void stop(LifecycleProvider pr, int event) {
            mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, event));
        }

        @OnLifecycleEvent(Lifecycle.ON_DESTROY)
        void destroy(LifecycleProvider pr, int event) {
            mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, event));
            mLatch.countDown();
        }
    }

    private TestObserver mTestObserver = new TestObserver();

    /**
     * Result events
     */
    public enum TestEvent {
        ACTIVITY_CALLBACK,
        LIFECYCLE_EVENT
    }

    private List<Pair<TestEvent, Integer>> mCollectedEvents = new ArrayList<>();
    private CountDownLatch mLatch = new CountDownLatch(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCollectedEvents.add(new Pair<>(ACTIVITY_CALLBACK, Lifecycle.ON_CREATE));
        getLifecycle().addObserver(mTestObserver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCollectedEvents.add(new Pair<>(ACTIVITY_CALLBACK, Lifecycle.ON_START));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCollectedEvents.add(new Pair<>(ACTIVITY_CALLBACK, Lifecycle.ON_RESUME));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCollectedEvents.add(new Pair<>(ACTIVITY_CALLBACK, Lifecycle.ON_DESTROY));
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCollectedEvents.add(new Pair<>(ACTIVITY_CALLBACK, Lifecycle.ON_STOP));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCollectedEvents.add(new Pair<>(ACTIVITY_CALLBACK, Lifecycle.ON_PAUSE));
    }

    /**
     * awaits for all events and returns them.
     * @return
     * @throws InterruptedException
     */
    public List<Pair<TestEvent, Integer>> waitForCollectedEvents() throws InterruptedException {
        mLatch.await(TIMEOUT, TimeUnit.SECONDS);
        return mCollectedEvents;
    }
}
