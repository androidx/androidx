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

package com.android.support.lifecycle.testapp;

import android.os.Bundle;
import android.util.Pair;

import com.android.support.lifecycle.Lifecycle;
import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.LifecycleObserver;
import com.android.support.lifecycle.LifecycleOwner;
import com.android.support.lifecycle.OnLifecycleEvent;
import com.android.support.lifecycle.ProcessLifecycleOwner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Activity for SimpleAppFullLifecycleTest
 */
public class SimpleAppLifecycleTestActivity extends LifecycleActivity {

    public enum TestEventType {
        PROCESS_EVENT,
        ACTIVITY_EVENT
    }

    private static final long TIMEOUT_SECS = 10; // secs

    static class TestObserver implements LifecycleObserver {

        private TestEventType mType;

        TestObserver(TestEventType type) {
            mType = type;
        }

        @SuppressWarnings("UnusedParameters")
        @OnLifecycleEvent(Lifecycle.ON_ANY)
        void onEvent(LifecycleOwner provider, @Lifecycle.Event int event) {
            sCollectedEvents.add(new Pair<>(mType, event));
            sLatch.countDown();
        }
    }

    static List<Pair<TestEventType, Integer>> sCollectedEvents = new ArrayList<>();
    static CountDownLatch sLatch = new CountDownLatch(11);

    /**
     * start process observer
     */
    public static void startProcessObserver() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(sProcessObserver);
    }

    /**
     * stop process observer
     */
    public static void stopProcessObserver() {
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(sProcessObserver);
    }

    private static TestObserver sProcessObserver = new TestObserver(TestEventType.PROCESS_EVENT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(new TestObserver(TestEventType.ACTIVITY_EVENT));
    }

    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }

    /**
     * returns collected events
     */
    public static List<Pair<TestEventType, Integer>> awaitForEvents()
            throws InterruptedException {
        boolean success = sLatch.await(TIMEOUT_SECS, TimeUnit.SECONDS);
        return success ? sCollectedEvents : null;
    }
}
