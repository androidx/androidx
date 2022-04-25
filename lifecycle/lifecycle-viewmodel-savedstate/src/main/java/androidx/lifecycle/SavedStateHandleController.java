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

package androidx.lifecycle;

import androidx.annotation.NonNull;
import androidx.savedstate.SavedStateRegistry;

final class SavedStateHandleController implements LifecycleEventObserver {
    private final String mKey;
    private boolean mIsAttached = false;
    private final SavedStateHandle mHandle;

    SavedStateHandleController(String key, SavedStateHandle handle) {
        mKey = key;
        mHandle = handle;
    }

    boolean isAttached() {
        return mIsAttached;
    }

    void attachToLifecycle(SavedStateRegistry registry, Lifecycle lifecycle) {
        if (mIsAttached) {
            throw new IllegalStateException("Already attached to lifecycleOwner");
        }
        mIsAttached = true;
        lifecycle.addObserver(this);
        registry.registerSavedStateProvider(mKey, mHandle.savedStateProvider());
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            mIsAttached = false;
            source.getLifecycle().removeObserver(this);
        }
    }

    SavedStateHandle getHandle() {
        return mHandle;
    }
}
