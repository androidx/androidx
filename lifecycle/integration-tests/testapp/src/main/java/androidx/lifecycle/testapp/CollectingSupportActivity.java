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

package androidx.lifecycle.testapp;

import static androidx.lifecycle.testapp.TestEvent.OWNER_CALLBACK;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * LifecycleRegistryOwner that extends FragmentActivity.
 */
public class CollectingSupportActivity extends FragmentActivity implements
        CollectingLifecycleOwner {

    private final List<Pair<TestEvent, Event>> mCollectedEvents = new ArrayList<>();
    private TestObserver mTestObserver = new TestObserver(mCollectedEvents);
    private CountDownLatch mSavedStateLatch = new CountDownLatch(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout layout = new FrameLayout(this);
        layout.setId(R.id.fragment_container);
        setContentView(layout);
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Event.ON_CREATE));
        getLifecycle().addObserver(mTestObserver);
    }

    /**
     * replaces the main content fragment w/ the given fragment.
     */
    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commitNow();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Event.ON_START));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Event.ON_RESUME));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Event.ON_DESTROY));
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Event.ON_STOP));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Event.ON_PAUSE));
        // helps with less flaky API 16 tests.
        overridePendingTransition(0, 0);
    }

    @Override
    public List<Pair<TestEvent, Event>> copyCollectedEvents() {
        return new ArrayList<>(mCollectedEvents);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSavedStateLatch.countDown();
    }

    /**
     * Waits for onSaveInstanceState to be called.
     */
    public boolean waitForStateSave(@SuppressWarnings("SameParameterValue") int seconds)
            throws InterruptedException {
        return mSavedStateLatch.await(seconds, TimeUnit.SECONDS);
    }
}
