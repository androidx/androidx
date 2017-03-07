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

import android.support.annotation.IntDef;
import android.support.annotation.MainThread;

/**
 * Defines an object that has an Android Lifecycle. {@link android.support.v4.app.Fragment Fragment}
 * and {@link android.support.v4.app.FragmentActivity FragmentActivity} classes implement
 * {@link LifecycleProvider} interface which has the {@link LifecycleProvider#getLifecycle()
 * getLifecycle} method to access the Lifecycle. You can also implement {@link LifecycleProvider}
 * in your own classes.
 * <p>
 * {@link #ON_CREATE}, {@link #ON_START}, {@link #ON_RESUME} events in this class are dispatched
 * <b>after</b> the owner {@link LifecycleProvider}'s related method returns.
 * {@link #ON_PAUSE}, {@link #ON_STOP}, {@link #ON_DESTROY} events in this class are dispatched
 * <b>before</b> the owner {@link LifecycleProvider}'s related method is called.
 * For instance, {@link #ON_START} will be dispatched after
 * {@link android.app.Activity#onStart onStart} returns, {@link #ON_STOP} will be dispatched
 * before {@link android.app.Activity#onStop onStop} is called.
 * This gives you certain guarantees on which state the owner is in.
 * <p>
 * Lifecycle events are observed using annotations.
 * <pre>
 * interface TestObserver extends LifecycleObserver {
 *   {@literal @}OnLifecycleEvent(ON_STOP)
 *   void onStopped();
 * }
 * </pre>
 * <p>
 * A method can observe multiple events as well as multiple methods can observe the same event.
 * <pre>
 * interface TestObserver extends LifecycleObserver {
 *   {@literal @}OnLifecycleEvent(ON_STOP | ON_START)
 *   void onStoppedOrStarted();
 *   {@literal @}OnLifecycleEvent(ON_STOP)
 *   void onStopped();
 * }
 * </pre>
 * <p>
 * Observer methods can receive 0, 1 or 2 arguments.
 * If used, the first argument must be of type {@link LifecycleProvider} and the second argument
 * must be an integer with {@link Event} type annotation.
 * <pre>
 * interface TestObserver extends LifecycleObserver {
 *   {@literal @}OnLifecycleEvent(ON_CREATE)
 *   void onCreated(LifecycleProvider provider);
 *   {@literal @}OnLifecycleEvent(ON_STOP | ON_START)
 *   void onStoppedOrStarted(LifecycleProvider provider, {@literal @}Event int event);
 * }
 * </pre>
 * These additional parameters are provided to allow you to conveniently observe multiple providers
 * and events without tracking them manually.
 */
@SuppressWarnings({"UnnecessaryInterfaceModifier", "WeakerAccess", "unused"})
public interface Lifecycle {
    /**
     * Adds a LifecycleObserver that will be notified when the owner LifecycleProvider changes
     * state.
     * <p>
     * If this method is called while a state change is being dispatched, the given observer will
     * not receive that event.
     *
     * @param observer The observer to notify.
     */
    @MainThread
    void addObserver(LifecycleObserver observer);

    /**
     * Removes the given observer from the observers list.
     * <p>
     * If this method is called while a state change is being dispatched,
     * <ul>
     *  <li>If the given observer has not yet received that event, it will not receive it.
     *  <li>If the given observer has more than 1 method that observes the currently dispatched
     *  event and at least one of them received the event, all of them will receive the event and
     *  the removal will happen afterwards.
     * </ul>
     *
     * @param observer The observer to be removed.
     */
    @MainThread
    void removeObserver(LifecycleObserver observer);

    /**
     * Destroyed state for a LifecycleProvider. After this event, this Lifecycle will not dispatch
     * any more events. For instance, fo an {@link android.app.Activity}, this state is reached
     * <b>after</b> Activity's {@link android.app.Activity#onDestroy() onDestroy} returns.
     */
    int DESTROYED = 1;
    /**
     * Initialized state for a LifecycleProvider. For an {@link android.app.Activity}, this is
     * the state when it is constructed but has not received
     * {@link android.app.Activity#onCreate(android.os.Bundle) onCreate} yet.
     */
    int INITIALIZED = DESTROYED << 1;
    /**
     * Stopped state for a LifecycleProvider. For an {@link android.app.Activity}, this state
     * is reached after {@link android.app.Activity#onCreate(android.os.Bundle) onCreate} and
     * {@link android.app.Activity#onCreate(android.os.Bundle) onStop} calls.
     */
    int STOPPED = INITIALIZED << 1;
    /**
     * Started state for a LifecycleProvider. For an {@link android.app.Activity}, this state
     * is reached after {@link android.app.Activity#onStart() onStart} and
     * {@link android.app.Activity#onCreate(android.os.Bundle) onPause} calls.
     */
    int STARTED = STOPPED << 1;
    /**
     * Resumed state for a LifecycleProvider. For an {@link android.app.Activity}, this state
     * is reached after {@link android.app.Activity#onResume() onResume} is called.
     */
    int RESUMED = STARTED << 1;
    /**
     * IntDef for Lifecycle states. You can consider the states as the nodes in a graph and
     * {@link Event}s as the edges between these nodes.
     */
    @IntDef(value = {DESTROYED, INITIALIZED, STOPPED, STARTED, RESUMED})
    public @interface State {
    }

    /**
     * Returns the current state of the Lifecycle.
     *
     * @return The current state of the Lifecycle.
     */
    @MainThread @State
    int getCurrentState();

    /**
     * Constant for onCreate event of the {@link LifecycleProvider}.
     */
    int ON_CREATE = RESUMED << 1;
    /**
     * Constant for onStart event of the {@link LifecycleProvider}.
     */
    int ON_START = ON_CREATE << 2;
    /**
     * Constant for onResume event of the {@link LifecycleProvider}.
     */
    int ON_RESUME = ON_START << 2;
    /**
     * Constant for onPause event of the {@link LifecycleProvider}.
     */
    int ON_PAUSE = ON_RESUME << 2;
    /**
     * Constant for onStop event of the {@link LifecycleProvider}.
     */
    int ON_STOP = ON_PAUSE << 2;
    /**
     * Constant for onDestroy event of the {@link LifecycleProvider}.
     */
    int ON_DESTROY = ON_STOP << 2;
    /**
     * An {@link Event Event} constant that can be used to match all events.
     */
    int ANY = -1;

    /**
     * IntDef for Lifecycle events. You can consider the {@link State}s as the nodes in a graph and
     * Events as the edges between these nodes.
     */
    @IntDef(value = {ON_CREATE, ON_START, ON_RESUME, ON_PAUSE, ON_STOP, ON_DESTROY, ANY},
            flag = true)
    public @interface Event {
    }
}
