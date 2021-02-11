/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity;

import static java.util.Objects.requireNonNull;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.activity.renderer.IRendererCallback;
import androidx.lifecycle.Lifecycle.Event;

/**
 * An activity lifecycle listener which dispatches the lifecycle events to a {@link
 * IRendererCallback}.
 */
final class ActivityLifecycleDelegate implements ActivityLifecycleCallbacks {
    public static final String TAG = "ActivityLifecycleListener";
    @Nullable
    private IRendererCallback mRendererCallback;
    @NonNull
    private Event mLastObservedEvent = Event.ON_ANY;

    /**
     * Registers a {@link IRendererCallback} that is notified of lifecycle method invocations.
     */
    void registerRendererCallback(@Nullable IRendererCallback rendererCallback) {
        mRendererCallback = rendererCallback;
        onActive();
    }

    private void onActive() {
        notifyEvent(mLastObservedEvent);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        requireNonNull(activity);
        notifyEvent(Event.ON_CREATE);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        requireNonNull(activity);
        notifyEvent(Event.ON_START);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        requireNonNull(activity);
        notifyEvent(Event.ON_RESUME);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        requireNonNull(activity);
        notifyEvent(Event.ON_PAUSE);
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        requireNonNull(activity);
        notifyEvent(Event.ON_STOP);
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        requireNonNull(activity);
        notifyEvent(Event.ON_DESTROY);
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        // No-op
    }

    private void notifyEvent(Event event) {
        mLastObservedEvent = event;

        if (mRendererCallback == null) {
            return;
        }

        try {
            switch (event) {
                case ON_CREATE:
                    mRendererCallback.onCreate();
                    break;
                case ON_START:
                    mRendererCallback.onStart();
                    break;
                case ON_RESUME:
                    mRendererCallback.onResume();
                    break;
                case ON_PAUSE:
                    mRendererCallback.onPause();
                    break;
                case ON_STOP:
                    mRendererCallback.onStop();
                    break;
                case ON_DESTROY:
                    mRendererCallback.onDestroyed();
                    break;
                case ON_ANY:
                    break;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
        }
    }
}

