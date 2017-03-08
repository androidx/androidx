/*
 * Copyright 2017, The Android Open Source Project
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

package com.example.android.flatfoot.codelab.flatfootcodelab.step_databinding;

import android.util.Log;

import com.android.support.executors.AppToolkitTaskExecutor;
import com.android.support.lifecycle.Lifecycle;
import com.android.support.lifecycle.LifecycleObserver;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.OnLifecycleEvent;

import java.util.Timer;
import java.util.TimerTask;


public class LifecycleAwareTimer implements LifecycleObserver {

    private static final int ONE_SECOND = 1000;

    private LiveData<Long> liveElapsedTime = new LiveData<>();

    private long mElapsedTime;

    private Timer mTimer;

    public LiveData<Long> getLiveElapsedTime() {
        return liveElapsedTime;
    }

    @OnLifecycleEvent(Lifecycle.ON_CREATE)
    void setZero() {
        if (liveElapsedTime.getValue() == null) {
            liveElapsedTime.setValue(0l);
        }
    }

    @OnLifecycleEvent(Lifecycle.ON_RESUME)
    void resumeTimer() {
        Log.d("TimerViewModel", "Timer resumed");

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mElapsedTime++;
                AppToolkitTaskExecutor.getInstance().postToMainThread(new Runnable() {
                    @Override
                    public void run() {
                        liveElapsedTime.setValue(mElapsedTime);
                    }
                });
            }
        }, ONE_SECOND, ONE_SECOND);
    }

    @OnLifecycleEvent(Lifecycle.ON_PAUSE)
    void pauseTimer() {
        Log.d("TimerViewModel", "Timer paused");
        mTimer.cancel();
    }
}
