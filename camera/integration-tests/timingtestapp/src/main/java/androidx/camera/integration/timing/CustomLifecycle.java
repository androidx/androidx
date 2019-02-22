/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.integration.timing;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

/** A customized lifecycle owner which obeys the lifecycle transition rules. */
public final class CustomLifecycle implements LifecycleOwner {
    private final LifecycleRegistry mLifecycleRegistry;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public CustomLifecycle() {
        mLifecycleRegistry = new LifecycleRegistry(this);
        mLifecycleRegistry.setCurrentState(Lifecycle.State.INITIALIZED);
        mLifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    /**
     * Called when activity resumes.
     */
    public void doOnResume() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    CustomLifecycle.this.doOnResume();
                }
            });
            return;
        }
        mLifecycleRegistry.setCurrentState(Lifecycle.State.RESUMED);
    }

    /**
     * Called when activity is destroyed.
     */
    public void doDestroyed() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    CustomLifecycle.this.doDestroyed();
                }
            });
            return;
        }
        mLifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
    }
}
