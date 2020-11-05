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

import android.app.Activity;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;

/**
 * Subclass of {@link WindowBoundsHelper} used to override the results for testing.
 *
 * @see WindowBoundsHelper
 * @see WindowBoundsHelper#setForTesting(WindowBoundsHelper)
 */
class TestWindowBoundsHelper extends WindowBoundsHelper {
    private Rect mGlobalOverriddenBounds;
    private final HashMap<Activity, Rect> mOverriddenBounds = new HashMap<>();
    private final HashMap<Activity, Rect> mOverriddenMaximumBounds = new HashMap<>();

    /**
     * Overrides the bounds returned from this helper for the given context. Passing null {@code
     * bounds} has the effect of clearing the bounds override.
     * <p>
     * Note: A global override set as a result of {@link #setCurrentBounds(Rect)} takes precedence
     * over the value set with this method.
     */
    void setCurrentBoundsForActivity(@NonNull Activity activity, @Nullable Rect bounds) {
        mOverriddenBounds.put(activity, bounds);
    }

    /**
     * Overrides the max bounds returned from this helper for the given context. Passing {@code
     * null} {@code bounds} has the effect of clearing the bounds override.
     */
    void setMaximumBoundsForActivity(@NonNull Activity activity, @Nullable Rect bounds) {
        mOverriddenMaximumBounds.put(activity, bounds);
    }

    /**
     * Overrides the bounds returned from this helper for all supplied contexts. Passing null
     * {@code bounds} has the effect of clearing the global override.
     */
    void setCurrentBounds(@Nullable Rect bounds) {
        mGlobalOverriddenBounds = bounds;
    }

    @Override
    @NonNull
    Rect computeCurrentWindowBounds(Activity activity) {
        if (mGlobalOverriddenBounds != null) {
            return mGlobalOverriddenBounds;
        }

        Rect bounds = mOverriddenBounds.get(activity);
        if (bounds != null) {
            return bounds;
        }

        return super.computeCurrentWindowBounds(activity);
    }

    @NonNull
    @Override
    Rect computeMaximumWindowBounds(Activity activity) {
        Rect bounds = mOverriddenMaximumBounds.get(activity);
        if (bounds != null) {
            return bounds;
        }

        return super.computeMaximumWindowBounds(activity);
    }

    /**
     * Clears any overrides set with {@link #setCurrentBounds(Rect)} or
     * {@link #setCurrentBoundsForActivity(Activity, Rect)}.
     */
    void reset() {
        mGlobalOverriddenBounds = null;
        mOverriddenBounds.clear();
        mOverriddenMaximumBounds.clear();
    }
}
