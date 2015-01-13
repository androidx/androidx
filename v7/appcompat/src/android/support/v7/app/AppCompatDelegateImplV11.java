/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v7.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.v7.internal.view.SupportActionModeWrapper;
import android.support.v7.internal.widget.NativeActionModeAwareLayout;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.View;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class AppCompatDelegateImplV11 extends AppCompatDelegateImplV7
        implements NativeActionModeAwareLayout.OnActionModeForChildListener {

    private NativeActionModeAwareLayout mNativeActionModeAwareLayout;

    AppCompatDelegateImplV11(Activity activity, AppCompatActivityCallback callback) {
        super(activity, callback);
    }

    @Override
    void onSubDecorInstalled() {
        // NativeActionModeAwareLayout is used to notify us when a native Action Mode is started
        mNativeActionModeAwareLayout = (NativeActionModeAwareLayout) mActivity
                .findViewById(android.R.id.content);

        // Can be null when using FEATURE_ACTION_BAR_OVERLAY
        if (mNativeActionModeAwareLayout != null) {
            mNativeActionModeAwareLayout.setActionModeForChildListener(this);
        }
    }

    // From NativeActionModeAwareLayout.OnActionModeForChildListener
    @Override
    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback) {
        Context context = originalView.getContext();

        // Try and start a support action mode, wrapping the callback
        final android.support.v7.view.ActionMode supportActionMode = startSupportActionMode(
                new SupportActionModeWrapper.CallbackWrapper(context, callback));

        if (supportActionMode != null) {
            // If we received a support action mode, wrap and return it
            return new SupportActionModeWrapper(mActivity, supportActionMode);
        }
        return null;
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        // First, let the Activity's LayoutInflater.Factory2 method try...
        View result = mActivity.onCreateView(parent, name, context, attrs);
        if (result != null) {
            return result;
        }

        // Let the v7 delegate handle it
        return super.onCreateView(parent, name, context, attrs);
    }
}
