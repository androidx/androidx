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
 *
 */

package com.android.support.lifecycle;

import android.support.annotation.IntDef;
import android.support.annotation.MainThread;

@SuppressWarnings({"UnnecessaryInterfaceModifier", "WeakerAccess", "unused"})
public interface Lifecycle {
    @MainThread
    void addObserver(LifecycleObserver callback);
    @MainThread
    void removeObserver(LifecycleObserver removeCallback);
    @MainThread
    @State
    public int getCurrentState();
    public static final int INITIALIZED = 1;
    public static final int CREATED = INITIALIZED << 2;
    public static final int STARTED = CREATED << 2;
    public static final int RESUMED = STARTED << 2;
    public static final int PAUSED = RESUMED << 2;
    public static final int STOPPED = PAUSED << 2;
    public static final int DESTROYED = STOPPED << 2;
    public static final int FINISHED = DESTROYED << 2;
    public static final int ANY = -1;

    @IntDef(value = {INITIALIZED, CREATED, STARTED, RESUMED, PAUSED, STOPPED, DESTROYED, ANY}, flag = true)
    public @interface State {
    }
}
