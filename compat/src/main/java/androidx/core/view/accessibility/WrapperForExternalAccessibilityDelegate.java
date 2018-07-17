/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.view.accessibility;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.view.AccessibilityDelegateCompat;

/**
 * Wrapper to allow support lib to modify accessibility behavior even on Views that already
 * have an accessibility delegate
 * @hide
 */
@RestrictTo(LIBRARY)
public class WrapperForExternalAccessibilityDelegate extends AccessibilityDelegateCompat {
    // The original delegate
    View.AccessibilityDelegate mOriginalDelegate;

    public WrapperForExternalAccessibilityDelegate(@NonNull View.AccessibilityDelegate delegate) {
        mOriginalDelegate = delegate;
    }

    @Override
    public void sendAccessibilityEvent(View host, int eventType) {
        mOriginalDelegate.sendAccessibilityEvent(host, eventType);
    }

    @Override
    public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
        mOriginalDelegate.sendAccessibilityEventUnchecked(host, event);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
        return mOriginalDelegate.dispatchPopulateAccessibilityEvent(host, event);
    }

    @Override
    public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
        mOriginalDelegate.onPopulateAccessibilityEvent(host, event);
    }

    @Override
    public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
        mOriginalDelegate.onInitializeAccessibilityEvent(host, event);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
        mOriginalDelegate.onInitializeAccessibilityNodeInfo(
                host, info.unwrap());
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
            AccessibilityEvent event) {
        return mOriginalDelegate.onRequestSendAccessibilityEvent(host, child, event);
    }

    @Override
    public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View host) {
        if (Build.VERSION.SDK_INT >= 16) {
            Object provider = mOriginalDelegate.getAccessibilityNodeProvider(host);
            if (provider != null) {
                return new AccessibilityNodeProviderCompat(provider);
            }
        }
        return null;
    }

    @Override
    public boolean performAccessibilityAction(View host, int action, Bundle args) {
        if (Build.VERSION.SDK_INT >= 16) {
            return mOriginalDelegate.performAccessibilityAction(host, action, args);
        }
        return false;
    }
}
