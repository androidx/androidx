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

package com.android.support.lifecycle;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;

import com.android.support.lifecycle.ReportInitializationFragment.ActivityInitializationListener;

/**
 * Class that provides lifecycle for the whole application process.
 * <p>

 * You can consider this LifecycleProvider as the composite of all of your Activities:
 * ProcessProvider will dispatch {@link Lifecycle#ON_CREATE}, {@link Lifecycle#ON_START},
 * {@link Lifecycle#ON_RESUME} events, as a first activity moves through these events.
 * {@link Lifecycle#ON_PAUSE}, {@link Lifecycle#ON_STOP},
 * {@link Lifecycle#ON_DESTROY} events will be dispatched with a <b>delay</b> after a last activity
 * passed through them. This delay is long enough to guarantee that ProcessProvider
 * won't send any events if activities are destroyed and recreated due to a
 * configuration change.
 *
 * <p>
 * It is useful for use cases where you would like to react on your app coming to the foreground or
 * going to the background and you don't need a milliseconds accuracy in receiving lifecycle events.
 */
@SuppressWarnings("WeakerAccess")
public class ProcessProvider implements LifecycleProvider {

    @VisibleForTesting
    static final long TIMEOUT_MS = 700; //mls

    // ground truth counters
    private int mCreatedCounter = 0;
    private int mStartedCounter = 0;
    private int mResumedCounter = 0;

    private boolean mPauseSent = true;
    private boolean mStopSent = true;
    private boolean mDestroySent = true;

    private Handler mHandler;
    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);

    private Runnable mDelayedPauseRunnable = new Runnable() {
        @Override
        public void run() {
            dispatchPauseIfNeeded();
            dispatchStopIfNeeded();
            dispatchDestroyIfNeeded();
        }
    };

    private ActivityInitializationListener mInitializationListener =
            new ActivityInitializationListener() {
                @Override
                public void onCreate() {
                    activityCreated();
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

    private static final ProcessProvider sInstance = new ProcessProvider();

    /**
     * The LifecycleProvider for the whole application process. Note that if your application
     * has multiple processes, this provider does not know about other processes.
     *
     * @return {@link LifecycleProvider} for the whole application.
     */
    public static LifecycleProvider get() {
        return sInstance;
    }

    static void init(Context context) {
        sInstance.attach(context);
    }

    void activityCreated() {
        mCreatedCounter++;
        if (mCreatedCounter == 1 && mDestroySent) {
            mRegistry.handleLifecycleEvent(Lifecycle.ON_CREATE);
            mDestroySent = false;
        }
    }

    void activityStarted() {
        mStartedCounter++;
        if (mStartedCounter == 1 && mStopSent) {
            mRegistry.handleLifecycleEvent(Lifecycle.ON_START);
            mStopSent = false;
        }
    }

    void activityResumed() {
        mResumedCounter++;
        if (mResumedCounter == 1) {
            if (mPauseSent) {
                mRegistry.handleLifecycleEvent(Lifecycle.ON_RESUME);
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
            mRegistry.handleLifecycleEvent(Lifecycle.ON_PAUSE);
        }
    }

    private void dispatchStopIfNeeded() {
        if (mStartedCounter == 0 && mPauseSent) {
            mRegistry.handleLifecycleEvent(Lifecycle.ON_STOP);
            mStopSent = true;
        }
    }

    private void dispatchDestroyIfNeeded() {
        if (mCreatedCounter == 0 && mStopSent) {
            mRegistry.handleLifecycleEvent(Lifecycle.ON_DESTROY);
            mDestroySent = true;
        }
    }

    void activityDestroyed() {
        mCreatedCounter--;
        dispatchDestroyIfNeeded();
    }

    private ProcessProvider() {
    }

    void attach(Context context) {
        mHandler = new Handler();
        Application app = (Application) context.getApplicationContext();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                ReportInitializationFragment.getOrCreateFor(activity)
                        .setProcessListener(mInitializationListener);
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
                activityPaused();
            }

            @Override
            public void onActivityStopped(Activity activity) {
                activityStopped();
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                activityDestroyed();
            }
        });
    }

    @Override
    public Lifecycle getLifecycle() {
        return mRegistry;
    }
}
