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

@SuppressWarnings({"UnnecessaryInterfaceModifier", "WeakerAccess", "unused"})
public interface Lifecycle {
    /**
     * Adds a LifecycleObserver that will be notified when the owner LifecycleProvide changes state.
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
     *  * If the given observer has not yet received that event, it will not receive it.
     *  * If the given observer has more than 1 methods that observes the currently dispatched event
     *  and at least one of them received the event, all of them will receive the event and the
     *  removal will happen afterwards.
     *
     * @param observer The observer to be removed.
     */
    @MainThread
    void removeObserver(LifecycleObserver observer);

    int DESTROYED = 1;
    int INITIALIZED = DESTROYED << 1;
    int STOPPED = INITIALIZED << 1;
    int STARTED = STOPPED << 1;
    int RESUMED = STARTED << 1;
    @IntDef(value = {DESTROYED, INITIALIZED, STOPPED, STARTED, RESUMED})
    public @interface State {
    }

    /**
     * Returns the current state of the Lifecycle.
     *
     * @return The current state of the Lifecycle.
     */
    @MainThread @State
    public int getCurrentState();
    int ON_CREATE = RESUMED << 1;
    int ON_START = ON_CREATE << 2;
    int ON_RESUME = ON_START << 2;
    int ON_PAUSE = ON_RESUME << 2;
    int ON_STOP = ON_PAUSE << 2;
    int ON_DESTROY = ON_STOP << 2;
    int ANY = -1;

    @IntDef(value = {ON_CREATE, ON_START, ON_RESUME, ON_PAUSE, ON_STOP, ON_DESTROY, ANY},
            flag = true)
    public @interface Event {
    }
}
