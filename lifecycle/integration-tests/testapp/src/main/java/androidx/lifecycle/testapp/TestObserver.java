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

import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;
import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;
import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;
import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;
import static androidx.lifecycle.testapp.TestEvent.LIFECYCLE_EVENT;

import androidx.core.util.Pair;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.List;

class TestObserver implements LifecycleObserver {
    private final List<Pair<TestEvent, Event>> mCollectedEvents;

    TestObserver(List<Pair<TestEvent, Event>> collectedEvents) {
        mCollectedEvents = collectedEvents;
    }

    @OnLifecycleEvent(ON_CREATE)
    public void create(LifecycleOwner pr) {
        mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, ON_CREATE));
    }

    @OnLifecycleEvent(ON_START)
    public void start(LifecycleOwner pr) {
        mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, ON_START));
    }

    @OnLifecycleEvent(ON_RESUME)
    public void resume(LifecycleOwner pr) {
        mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, ON_RESUME));
    }
    @OnLifecycleEvent(ON_PAUSE)
    public void pause(LifecycleOwner pr) {
        mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, ON_PAUSE));
    }

    @OnLifecycleEvent(ON_STOP)
    public void stop(LifecycleOwner pr) {
        mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, ON_STOP));
    }

    @OnLifecycleEvent(ON_DESTROY)
    public void destroy(LifecycleOwner pr) {
        mCollectedEvents.add(new Pair<>(LIFECYCLE_EVENT, ON_DESTROY));
    }
}
