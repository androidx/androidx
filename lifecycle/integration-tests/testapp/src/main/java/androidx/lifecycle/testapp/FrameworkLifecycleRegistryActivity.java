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

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.LifecycleRegistryOwner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * LifecycleRegistryOwner that extends framework activity.
 */
@SuppressWarnings("deprecation")
public class FrameworkLifecycleRegistryActivity extends Activity implements
        LifecycleRegistryOwner, CollectingLifecycleOwner {
    private LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    @NonNull
    @Override
    public LifecycleRegistry getLifecycle() {
        return mLifecycleRegistry;
    }

    private List<Pair<TestEvent, Lifecycle.Event>> mCollectedEvents = new ArrayList<>();
    private TestObserver mTestObserver = new TestObserver(mCollectedEvents);
    private CountDownLatch mLatch = new CountDownLatch(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Lifecycle.Event.ON_CREATE));
        getLifecycle().addObserver(mTestObserver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Lifecycle.Event.ON_START));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Lifecycle.Event.ON_RESUME));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Lifecycle.Event.ON_DESTROY));
        mLatch.countDown();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Lifecycle.Event.ON_STOP));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCollectedEvents.add(new Pair<>(OWNER_CALLBACK, Lifecycle.Event.ON_PAUSE));
    }

    @Override
    public List<Pair<TestEvent, Lifecycle.Event>> copyCollectedEvents() {
        return new ArrayList<>(mCollectedEvents);
    }
}
