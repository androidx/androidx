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

package androidx.slice.widget;

import static androidx.slice.widget.SliceView.MODE_LARGE;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * Class containing configurable settings for SliceView that may impact interaction and contents
 * of the slice that are displayed.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(19)
public class SliceViewPolicy {

    /**
     * Implement this to find out about different view configuration changes.
     */
    public interface PolicyChangeListener {
        /**
         * Notified when scrolling policy changes.
         */
        void onScrollingChanged(boolean newScrolling);
        /**
         * Notified when available height changes.
         */
        void onMaxHeightChanged(int newNewHeight);
        /**
         * Notified when max small height changes.
         */
        void onMaxSmallChanged(int newMaxSmallHeight);
        /**
         * Notified when mode changes.
         */
        void onModeChanged(@SliceView.SliceMode int newMode);
    }

    private int mMaxHeight = 0;
    private int mMaxSmallHeight = 0;
    private boolean mScrollable = true;
    private @SliceView.SliceMode int mMode = MODE_LARGE;
    private PolicyChangeListener mListener;

    /**
     * @param listener the listener to notify for policy changes.
     */
    public void setListener(PolicyChangeListener listener) {
        mListener = listener;
    }

    /**
     * @return the maximum height ths slice has to be displayed in.
     */
    public int getMaxHeight() {
        return mMaxHeight;
    }

    /**
     * @return the maximum height a small slice should be presented in.
     */
    public int getMaxSmallHeight() {
        return mMaxSmallHeight;
    }

    /**
     * @return whether the slice is allowed to scroll or not.
     */
    public boolean isScrollable() {
        return mScrollable;
    }

    /**
     * @return the mode the slice is displayed in.
     */
    public @SliceView.SliceMode int getMode() {
        return mMode;
    }

    /**
     * Sets what the max height the slice can be presented in.
     */
    public void setMaxHeight(int max) {
        if (max != mMaxHeight) {
            mMaxHeight = max;
            if (mListener != null) {
                mListener.onMaxHeightChanged(max);
            }
        }
    }

    /**
     * Overrides the normal maximum height for a slice displayed in {@link SliceView#MODE_SMALL}.
     */
    public void setMaxSmallHeight(int maxSmallHeight) {
        if (mMaxSmallHeight != maxSmallHeight) {
            mMaxSmallHeight = maxSmallHeight;
            if (mListener != null) {
                mListener.onMaxSmallChanged(maxSmallHeight);
            }
        }
    }

    /**
     * Sets whether the slice should be presented as scrollable or not.
     */
    public void setScrollable(boolean scrollable) {
        if (scrollable != mScrollable) {
            mScrollable = scrollable;
            if (mListener != null) {
                mListener.onScrollingChanged(scrollable);
            }
        }
    }

    /**
     * Set the mode of the slice being presented.
     */
    public void setMode(@SliceView.SliceMode int mode) {
        if (mMode != mode) {
            mMode = mode;
            if (mListener != null) {
                mListener.onModeChanged(mode);
            }
        }
    }
}
