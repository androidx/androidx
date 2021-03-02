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

package androidx.window;

import static androidx.window.FoldingFeature.STATE_FLAT;
import static androidx.window.FoldingFeature.STATE_HALF_OPENED;
import static androidx.window.FoldingFeature.TYPE_HINGE;

import android.app.Activity;
import android.graphics.Rect;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import java.util.Collections;

/**
 * An implementation of {@link ExtensionInterfaceCompat} that switches the state when a consumer
 * is unregistered. Useful for testing consumers when they go through a cycle of register then
 * unregister then register again.
 */
public final class SwitchOnUnregisterExtensionInterfaceCompat implements ExtensionInterfaceCompat {

    private final Object mLock = new Object();
    private final Rect mFoldBounds = new Rect(0, 100, 200, 100);
    @GuardedBy("mLock")
    private ExtensionCallbackInterface mCallback = new EmptyExtensionCallbackInterface();
    @GuardedBy("mLock")
    private int mState = STATE_FLAT;

    @Override
    public boolean validateExtensionInterface() {
        return true;
    }

    @Override
    public void setExtensionCallback(@NonNull ExtensionCallbackInterface extensionCallback) {
        synchronized (mLock) {
            mCallback = extensionCallback;
        }
    }

    @Override
    public void onWindowLayoutChangeListenerAdded(@NonNull Activity activity) {
        synchronized (mLock) {
            mCallback.onWindowLayoutChanged(activity, currentWindowLayoutInfo());
        }
    }

    @Override
    public void onWindowLayoutChangeListenerRemoved(@NonNull Activity activity) {
        synchronized (mLock) {
            mState = toggleState(mState);
        }
    }

    @Override
    public void onDeviceStateListenersChanged(boolean isEmpty) {

    }

    WindowLayoutInfo currentWindowLayoutInfo() {
        return new WindowLayoutInfo(Collections.singletonList(currentFoldingFeature()));
    }

    FoldingFeature currentFoldingFeature() {
        return new FoldingFeature(mFoldBounds, TYPE_HINGE, mState);
    }

    private static int toggleState(int currentState) {
        if (currentState == STATE_FLAT) {
            return STATE_HALF_OPENED;
        }
        return STATE_FLAT;
    }
}
