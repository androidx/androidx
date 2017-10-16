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

package android.arch.lifecycle;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.ReportFragment.ActivityInitializationListener;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;

/**
 * Class that provides lifecycle for the whole application process.
 * <p>
 * You can consider this LifecycleOwner as the composite of all of your Activities, except that
 * {@link Lifecycle.Event#ON_CREATE} will be dispatched once and {@link Lifecycle.Event#ON_DESTROY}
 * will never be dispatched. Other lifecycle events will be dispatched with following rules:
 * ProcessLifecycleOwner will dispatch {@link Lifecycle.Event#ON_START},
 * {@link Lifecycle.Event#ON_RESUME} events, as a first activity moves through these events.
 * {@link Lifecycle.Event#ON_PAUSE}, {@link Lifecycle.Event#ON_STOP}, events will be dispatched with
 * a <b>delay</b> after a last activity
 * passed through them. This delay is long enough to guarantee that ProcessLifecycleOwner
 * won't send any events if activities are destroyed and recreated due to a
 * configuration change.
 *
 * <p>
 * It is useful for use cases where you would like to react on your app coming to the foreground or
 * going to the background and you don't need a milliseconds accuracy in receiving lifecycle
 * events.
 */
@SuppressWarnings("WeakerAccess")
public class ProcessLifecycleOwner implements LifecycleOwner {

    @VisibleForTesting
    static final long TIMEOUT_MS = 700; //mls

    // ground truth counters
    private int mStartedCounter = 0;
    private int mResumedCounter = 0;

    private boolean mPauseSent = true;
    private boolean mStopSent = true;

    private Handler mHandler;
    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);

    private Runnable mDelayedPauseRunnable = new Runnable() {
        @Override
        public void run() {
            dispatchPauseIfNeeded();
            dispatchStopIfNeeded();
        }
    };

    private ActivityInitializationListener mInitializationListener =
            new ActivityInitializationListener() {
                @Override
                public void onCreate() {
                }

                @Override
                public void onStart() {
                    activityStarted();
                }

                @Override
                public void onResume() {
                    activityResumed();
                }
            };

    private static final ProcessLifecycleOwner sInstance = new ProcessLifecycleOwner();

    /**
     * The LifecycleOwner for the whole application process. Note that if your application
     * has multiple processes, this provider does not know about other processes.
     *
     * @return {@link LifecycleOwner} for the whole application.
     */
    public static LifecycleOwner get() {
        return sInstance;
    }

    static void init(Context context) {
        sInstance.attach(context);
    }

    void activityStarted() {
        mStartedCounter++;
        if (mStartedCounter == 1 && mStopSent) {
            mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
            mStopSent = false;
        }
    }

    void activityResumed() {
        mResumedCounter++;
        if (mResumedCounter == 1) {
            if (mPauseSent) {
                mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
                mPauseSent = false;
            } else {
                mHandler.removeCallbacks(mDelayedPauseRunnable);
            }
        }
    }

    void activityPaused() {
        mResumedCounter--;
        if (mResumedCounter == 0) {
            mHandler.postDelayed(mDelayedPauseRunnable, TIMEOUT_MS);
        }
    }

    void activityStopped() {
        mStartedCounter--;
        dispatchStopIfNeeded();
    }

    private void dispatchPauseIfNeeded() {
        if (mResumedCounter == 0) {
            mPauseSent = true;
            mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        }
    }

    private void dispatchStopIfNeeded() {
        if (mStartedCounter == 0 && mPauseSent) {
            mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
            mStopSent = true;
        }
    }

    private ProcessLifecycleOwner() {
    }

    void attach(Context context) {
        mHandler = new Handler();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        Application app = (Application) context.getApplicationContext();
        app.registerActivityLifecycleCallbacks(new EmptyActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                ReportFragment  .get(activity).setProcessListener(mInitializationListener);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                activityPaused();
            }

            @Override
            public void onActivityStopped(Activity activity) {
                activityStopped();
            }
        });
    }

    @Override
    public Lifecycle getLifecycle() {
        return mRegistry;
    }
}
