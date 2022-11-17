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

package androidx.lifecycle;

import androidx.annotation.NonNull;

class DefaultLifecycleObserverAdapter implements LifecycleEventObserver {

    private final DefaultLifecycleObserver mDefaultLifecycleObserver;
    private final LifecycleEventObserver mLifecycleEventObserver;

    DefaultLifecycleObserverAdapter(DefaultLifecycleObserver defaultLifecycleObserver,
            LifecycleEventObserver lifecycleEventObserver) {
        mDefaultLifecycleObserver = defaultLifecycleObserver;
        mLifecycleEventObserver = lifecycleEventObserver;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                mDefaultLifecycleObserver.onCreate(source);
                break;
            case ON_START:
                mDefaultLifecycleObserver.onStart(source);
                break;
            case ON_RESUME:
                mDefaultLifecycleObserver.onResume(source);
                break;
            case ON_PAUSE:
                mDefaultLifecycleObserver.onPause(source);
                break;
            case ON_STOP:
                mDefaultLifecycleObserver.onStop(source);
                break;
            case ON_DESTROY:
                mDefaultLifecycleObserver.onDestroy(source);
                break;
            case ON_ANY:
                throw new IllegalArgumentException("ON_ANY must not been send by anybody");
        }
        if (mLifecycleEventObserver != null) {
            mLifecycleEventObserver.onStateChanged(source, event);
        }
    }
}
