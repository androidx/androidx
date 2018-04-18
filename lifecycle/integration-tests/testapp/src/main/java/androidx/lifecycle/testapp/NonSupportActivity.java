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

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Activity which doesn't extend FragmentActivity, to test ProcessLifecycleOwner because it
 * should work anyway.
 */
public class NonSupportActivity extends Activity {

    private static final int TIMEOUT = 1; //secs
    private final Lock mLock = new ReentrantLock();
    private Condition mIsResumedCondition = mLock.newCondition();
    private boolean mIsResumed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLock.lock();
        try {
            mIsResumed = true;
            mIsResumedCondition.signalAll();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLock.lock();
        try {
            mIsResumed = false;
        } finally {
            mLock.unlock();
        }
    }

    /**
     *  awaits resumed state
     * @return
     * @throws InterruptedException
     */
    public boolean awaitResumedState() throws InterruptedException {
        mLock.lock();
        try {
            while (!mIsResumed) {
                if (!mIsResumedCondition.await(TIMEOUT, TimeUnit.SECONDS)) {
                    return false;
                }
            }
            return true;
        } finally {
            mLock.unlock();
        }
    }
}
