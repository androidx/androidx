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
package androidx.transition;

import androidx.annotation.NonNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This {@link Transition.TransitionListener} synchronously waits for the specified callback.
 */
class SyncTransitionListener implements Transition.TransitionListener {

    static final int EVENT_START = 1;
    static final int EVENT_END = 2;
    static final int EVENT_CANCEL = 3;
    static final int EVENT_PAUSE = 4;
    static final int EVENT_RESUME = 5;

    private final int mTargetEvent;
    private CountDownLatch mLatch = new CountDownLatch(1);

    SyncTransitionListener(int event) {
        mTargetEvent = event;
    }

    boolean await() {
        try {
            return mLatch.await(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    void reset() {
        mLatch = new CountDownLatch(1);
    }

    @Override
    public void onTransitionStart(@NonNull Transition transition) {
        if (mTargetEvent == EVENT_START) {
            mLatch.countDown();
        }
    }

    @Override
    public void onTransitionEnd(@NonNull Transition transition) {
        if (mTargetEvent == EVENT_END) {
            mLatch.countDown();
        }
    }

    @Override
    public void onTransitionCancel(@NonNull Transition transition) {
        if (mTargetEvent == EVENT_CANCEL) {
            mLatch.countDown();
        }
    }

    @Override
    public void onTransitionPause(@NonNull Transition transition) {
        if (mTargetEvent == EVENT_PAUSE) {
            mLatch.countDown();
        }
    }

    @Override
    public void onTransitionResume(@NonNull Transition transition) {
        if (mTargetEvent == EVENT_RESUME) {
            mLatch.countDown();
        }
    }
}
