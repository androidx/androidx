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

package androidx.media2.widget;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.AttrRes;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

abstract class MediaViewGroup extends ViewGroup {
    private boolean mAggregatedIsVisible = false;
    MediaViewGroup(@NonNull Context context) {
        super(context);
    }

    MediaViewGroup(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    MediaViewGroup(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View view, int visibility) {
        if (Build.VERSION.SDK_INT < 24) {
            // onVisibilityAggregated() is introduced at API 24.
            // This is added to make the behavior compatible on < 24 devices.
            if (getWindowVisibility() == VISIBLE) {
                boolean newIsVisible = isShown();
                if (mAggregatedIsVisible != newIsVisible) {
                    onVisibilityAggregatedCompat(newIsVisible);
                }
            }
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (Build.VERSION.SDK_INT < 24) {
            // onVisibilityAggregated() is introduced at API 24.
            // This is added to make the behavior compatible on < 24 devices.
            if (isShown()) {
                onVisibilityAggregatedCompat(visibility == VISIBLE);
            }
        }
    }

    @Override
    @RequiresApi(24)
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        onVisibilityAggregatedCompat(isVisible);
    }

    @CallSuper
    void onVisibilityAggregatedCompat(boolean isVisible) {
        mAggregatedIsVisible = isVisible;
    }

    boolean isAggregatedVisible() {
        return mAggregatedIsVisible;
    }
}
