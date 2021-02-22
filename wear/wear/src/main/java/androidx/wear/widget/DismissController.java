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

package androidx.wear.widget;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;

/** @hide */
@RestrictTo(Scope.LIBRARY)
@UiThread
class DismissController {
    /**
     * Interface enabling listener to react to when the frame layout should be dismissed from the
     * UI.
     */
    @UiThread
    interface OnDismissListener {
        void onDismissStarted();
        void onDismissCanceled();
        void onDismissed();
    }

    @NonNull protected final Context mContext;
    @NonNull protected final DismissibleFrameLayout mLayout;
    @Nullable protected OnDismissListener mDismissListener;

    DismissController(@NonNull Context context, DismissibleFrameLayout layout) {
        mContext = context;
        mLayout = layout;
    }

    void setOnDismissListener(@Nullable OnDismissListener listener) {
        mDismissListener = listener;
    }
}
