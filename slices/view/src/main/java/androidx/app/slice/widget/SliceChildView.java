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

package androidx.app.slice.widget;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceItem;

/**
 * Base class for children views of {@link SliceView}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class SliceChildView extends FrameLayout {

    protected SliceView.SliceObserver mObserver;
    protected int mTintColor;

    public SliceChildView(@NonNull Context context) {
        super(context);
    }

    public SliceChildView(Context context, AttributeSet attributeSet) {
        this(context);
    }

    /**
     * @return the mode of the slice being presented.
     */
    public abstract int getMode();

    /**
     * @param slice the slice to show in this view.
     */
    public abstract void setSlice(Slice slice);

    /**
     * Called when the view should be reset.
     */
    public abstract void resetView();

    /**
     * @return the view.
     */
    public View getView() {
        return this;
    }

    /**
     * Sets a custom color to use for tinting elements like icons for this view.
     */
    public void setTint(@ColorInt int tintColor) {
        mTintColor = tintColor;
    }

    /**
     * Sets the observer to notify when an interaction events occur on the view.
     */
    public void setSliceObserver(SliceView.SliceObserver observer) {
        mObserver = observer;
    }

    /**
     * Populates style information for this view.
     */
    public void setStyle(AttributeSet attrs) {
        // TODO
    }

    /**
     * Called when the slice being displayed in this view is an element of a larger list.
     */
    public void setSliceItem(SliceItem slice, boolean isHeader, int rowIndex,
            SliceView.SliceObserver observer) {
        // Do nothing
    }
}
