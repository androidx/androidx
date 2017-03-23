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

package com.android.support.lifecycle;

import android.support.annotation.NonNull;

import com.android.support.apptoolkit.internal.SafeIterableMap;

import java.util.Map;

/**
 * An implementation of {@link Lifecycle} that can handle multiple observers.
 * <p>
 * It is used by Fragments and Support Library Activities. You can also directly use it if you have
 * a custom LifecycleProvider.
 */
@SuppressWarnings("WeakerAccess")
public class LifecycleRegistry implements Lifecycle {

    /**
     * Custom list that keeps observers and can handle removals / additions during traversal.
     */
    private SafeIterableMap<LifecycleObserver, ObserverWithState> mObserverSet =
            new SafeIterableMap<>();
    /**
     * Current state
     */
    @State
    private int mState;
    /**
     * Latest event that was provided via {@link #handleLifecycleEvent(int)}.
     */
    @Event
    private int mLastEvent;

    /**
     * The provider that owns this Lifecycle.
     */
    private final LifecycleProvider mLifecycleProvider;

    /**
     * Creates a new LifecycleRegistry for the given provider.
     * <p>
     * You should usually create this inside your LifecycleProvider class's constructor and hold
     * onto the same instance.
     *
     * @param provider The owner LifecycleProvider
     */
    public LifecycleRegistry(@NonNull LifecycleProvider provider) {
        mLifecycleProvider = provider;
        mState = INITIALIZED;
    }

    /**
     * Only marks the current state as the given value. It doesn't dispatch any event to its
     * listeners.
     * @param state new state
     */
    public void markState(@State int state) {
        mState = state;
    }

    /**
     * Sets the current state and notifies the observers.
     * <p>
     * Note that if the {@code currentState} is the same state as the last call to this method,
     * calling this method has no effect.
     *
     * @param event The event that was received
     */
    public void handleLifecycleEvent(@Event int event) {
        if (mLastEvent == event) {
            return;
        }
        mLastEvent = event;
        mState = getStateAfter(event);
        for (Map.Entry<LifecycleObserver, ObserverWithState> entry : mObserverSet) {
            entry.getValue().sync();
        }
    }

    @Override
    public void addObserver(LifecycleObserver observer) {
        ObserverWithState observerWithState = new ObserverWithState(observer);
        mObserverSet.putIfAbsent(observer, observerWithState);
        observerWithState.sync();
    }

    @Override
    public void removeObserver(LifecycleObserver observer) {
        // we consciously decided not to send destruction events here in opposition to addObserver.
        // Our reasons for that:
        // 1. These events haven't yet happened at all. In contrast to events in addObservers, that
        // actually occurred but earlier.
        // 2. There are cases when removeObserver happens as a consequence of some kind of fatal
        // event. If removeObserver method sends destruction events, then a clean up routine becomes
        // more cumbersome. More specific example of that is: your LifecycleObserver listens for
        // a web connection, in the usual routine in OnStop method you report to a server that a
        // session has just ended and you close the connection. Now let's assume now that you
        // lost an internet and as a result you removed this observer. If you get destruction
        // events in removeObserver, you should have a special case in your onStop method that
        // checks if your web connection died and you shouldn't try to report anything to a server.
        mObserverSet.remove(observer);
    }

    /**
     * The number of observers.
     *
     * @return The number of observers.
     */
    public int getObserverCount() {
        return mObserverSet.size();
    }

    @Override
    @State
    public int getCurrentState() {
        return mState;
    }

    @Lifecycle.State
    static int getStateAfter(@Event int event) {
        // TODO do some masking logic to return this fast.
        switch (event) {
            case ON_CREATE:
            case ON_STOP:
                return STOPPED;
            case ON_START:
            case ON_PAUSE:
                return STARTED;
            case ON_RESUME:
                return RESUMED;
            case ON_DESTROY:
                return DESTROYED;
            case Lifecycle.ON_ANY:
                break;
        }
        throw new IllegalArgumentException("Unexpected event value " + event);
    }

    @Event
    static int downEvent(@State int state) {
        switch (state) {
            case INITIALIZED:
                throw new IllegalArgumentException();
            case STOPPED:
                return ON_DESTROY;
            case STARTED:
                return ON_STOP;
            case RESUMED:
                return ON_PAUSE;
            case DESTROYED:
                throw new IllegalArgumentException();
        }
        throw new IllegalArgumentException("Unexpected state value " + state);
    }

    @Event
    static int upEvent(@State int state) {
        switch (state) {
            case INITIALIZED:
                return ON_CREATE;
            case STOPPED:
                return ON_START;
            case STARTED:
                return ON_RESUME;
            case RESUMED:
            case DESTROYED:
                throw new IllegalArgumentException();
        }
        throw new IllegalArgumentException("Unexpected state value " + state);
    }

    class ObserverWithState {
        @State
        private int mObserverCurrentState = INITIALIZED;
        private GenericLifecycleObserver mCallback;

        ObserverWithState(LifecycleObserver observer) {
            mCallback = Lifecycling.getCallback(observer);
        }

        void sync() {
            while (mObserverCurrentState != mState) {
                int event = mObserverCurrentState > mState ? downEvent(mObserverCurrentState)
                        : upEvent(mObserverCurrentState);
                mObserverCurrentState = getStateAfter(event);
                mCallback.onStateChanged(mLifecycleProvider, event);
            }
        }
    }
}
