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

package android.support.v7.internal.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.View;
import android.widget.LinearLayout;

/**
 * @hide
 */
public class NativeActionModeAwareLayout extends LinearLayout {

    private OnActionModeForChildListener mActionModeForChildListener;

    public NativeActionModeAwareLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setActionModeForChildListener(OnActionModeForChildListener listener) {
        mActionModeForChildListener = listener;
    }

    @Override
    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback) {
        if (mActionModeForChildListener != null) {
            callback = mActionModeForChildListener.onActionModeForChild(callback);
        }
        return super.startActionModeForChild(originalView, callback);
    }

    /**
     * @hide
     */
    public interface OnActionModeForChildListener {
        ActionMode.Callback onActionModeForChild(ActionMode.Callback callback);
    }
}
