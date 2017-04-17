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

package android.arch.lifecycle.testapp;

import static android.arch.lifecycle.testapp.TestEvent.LIFECYCLE_EVENT;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.util.Pair;

import java.util.List;

class TestObserver implements LifecycleObserver {
    private final List<Pair<TestEvent, Integer>> mCollectedEvents;

    TestObserver(List<Pair<TestEvent, Integer>> collectedEvents) {
        mCollectedEvents = collectedEvents;
    }

    @OnLifecycleEvent(Lifecycle.ON_CREATE)
    public void create(LifecycleOwner pr, int event) {
        mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, event));
    }

    @OnLifecycleEvent(Lifecycle.ON_START)
    public void start(LifecycleOwner pr, int event) {
        mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, event));
    }

    @OnLifecycleEvent(Lifecycle.ON_RESUME)
    public void resume(LifecycleOwner pr, int event) {
        mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, event));
    }
    @OnLifecycleEvent(Lifecycle.ON_PAUSE)
    public void pause(LifecycleOwner pr, int event) {
        mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, event));
    }

    @OnLifecycleEvent(Lifecycle.ON_STOP)
    public void stop(LifecycleOwner pr, int event) {
        mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, event));
    }

    @OnLifecycleEvent(Lifecycle.ON_DESTROY)
    public void destroy(LifecycleOwner pr, int event) {
        mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, event));
    }
}
