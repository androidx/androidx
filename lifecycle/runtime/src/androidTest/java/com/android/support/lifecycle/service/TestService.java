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

package com.android.support.lifecycle.service;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.android.support.lifecycle.Lifecycle;
import com.android.support.lifecycle.LifecycleObserver;
import com.android.support.lifecycle.LifecycleOwner;
import com.android.support.lifecycle.LifecycleService;
import com.android.support.lifecycle.OnLifecycleEvent;

public class TestService extends LifecycleService {

    public static final String ACTION_LOG_EVENT = "ACTION_LOG_EVENT";
    public static final String EXTRA_KEY_EVENT = "EXTRA_KEY_EVENT";

    private final IBinder mBinder = new Binder();

    public TestService() {
        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.ON_ANY)
            public void anyEvent(LifecycleOwner owner, @Lifecycle.Event int event) {
                Context context = (TestService) owner;
                Intent intent = new Intent(ACTION_LOG_EVENT);
                intent.putExtra(EXTRA_KEY_EVENT, event);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return mBinder;
    }
}
