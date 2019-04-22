/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.integration.extensions.idlingresource;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.test.espresso.IdlingResource;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import java.util.Collection;

/**
 * Idling resource which wait for view with given resource id.
 */
public abstract class ViewIdlingResource implements IdlingResource {

    @IdRes
    private final int mViewId;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private ResourceCallback mResourceCallback;

    private boolean mIdle = false;

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Collection<Activity> resumedActivities =
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(
                            Stage.RESUMED);
            for (Activity activity : resumedActivities) {
                View view = activity.findViewById(mViewId);
                if (view != null && isViewIdle(view)) {
                    if (mResourceCallback != null) {
                        mResourceCallback.onTransitionToIdle();
                    }
                    mIdle = true;
                    return;
                }
            }
            mHandler.postDelayed(this, 1000);
        }
    };

    protected ViewIdlingResource(@IdRes int viewId) {
        mViewId = viewId;
    }

    protected abstract boolean isViewIdle(View view);

    @Override
    public String getName() {
        return "ViewIdlingResource";
    }

    @Override
    public boolean isIdleNow() {
        if (!mIdle && !mHandler.hasCallbacks(mRunnable)) {
            mHandler.post(mRunnable);
        }

        return mIdle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        mResourceCallback = callback;
    }
}
