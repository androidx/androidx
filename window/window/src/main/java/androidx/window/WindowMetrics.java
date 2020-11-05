/*
 * Copyright 2020 The Android Open Source Project
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

import android.graphics.Point;
import android.graphics.Rect;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;

/**
 * Metrics about a {@link android.view.Window}, consisting of its bounds.
 * <p>
 * This is usually obtained from {@link WindowManager#getCurrentWindowMetrics()} or
 * {@link WindowManager#getMaximumWindowMetrics()}.
 *
 * @see WindowManager#getCurrentWindowMetrics()
 * @see WindowManager#getMaximumWindowMetrics()
 */
public final class WindowMetrics {
    @NonNull
    private final Rect mBounds;

    /**
     * Constructs a new {@link WindowMetrics} instance.
     *
     * @param bounds rect describing the bounds of the window, see {@link #getBounds}.
     */
    public WindowMetrics(@NonNull Rect bounds) {
        mBounds = new Rect(bounds);
    }

    /**
     * Returns a new {@link Rect} describing the bounds of the area the window occupies.
     * <p>
     * <b>Note that the size of the reported bounds can have different size than
     * {@link Display#getSize(Point)}.</b> This method reports the window size including all system
     * decorations, while {@link Display#getSize(Point)} reports the area excluding navigation bars
     * and display cutout areas.
     *
     * @return window bounds in pixels.
     */
    @NonNull
    public Rect getBounds() {
        return new Rect(mBounds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WindowMetrics that = (WindowMetrics) o;
        return mBounds.equals(that.mBounds);
    }

    @Override
    public int hashCode() {
        return mBounds.hashCode();
    }
}
