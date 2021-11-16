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

package androidx.window.extensions.embedding;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.LayoutDirection;
import android.view.WindowMetrics;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.window.extensions.ExperimentalWindowExtensionsApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Predicate;

/**
 * Split configuration rules for activities that are launched to side in a split. Define when an
 * activity that was launched in a side container from another activity should be shown
 * side-by-side or on top of it, as well as the visual properties of the split. Can be applied to
 * new activities started from the same process automatically by the embedding implementation on
 * the device.
 */
@ExperimentalWindowExtensionsApi
public abstract class SplitRule extends EmbeddingRule {
    @NonNull
    private final Predicate<WindowMetrics> mParentWindowMetricsPredicate;
    private final float mSplitRatio;
    @LayoutDir
    private final int mLayoutDirection;

    @IntDef({
            LayoutDirection.LTR,
            LayoutDirection.RTL,
            LayoutDirection.LOCALE
    })
    @Retention(RetentionPolicy.SOURCE)
    // Not called LayoutDirection to avoid conflict with android.util.LayoutDirection
    @interface LayoutDir {}

    SplitRule(@NonNull Predicate<WindowMetrics> parentWindowMetricsPredicate, float splitRatio,
            @LayoutDir int layoutDirection) {
        mParentWindowMetricsPredicate = parentWindowMetricsPredicate;
        mSplitRatio = splitRatio;
        mLayoutDirection = layoutDirection;
    }

    /**
     * Verifies if the provided parent bounds allow to show the split containers side by side.
     */
    @SuppressLint("ClassVerificationFailure") // Only called by Extensions implementation on device.
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean checkParentMetrics(@NonNull WindowMetrics parentMetrics) {
        return mParentWindowMetricsPredicate.test(parentMetrics);
    }

    public float getSplitRatio() {
        return mSplitRatio;
    }

    @LayoutDir
    public int getLayoutDirection() {
        return mLayoutDirection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SplitRule)) return false;
        SplitRule that = (SplitRule) o;
        return Float.compare(that.mSplitRatio, mSplitRatio) == 0
                && mParentWindowMetricsPredicate.equals(that.mParentWindowMetricsPredicate)
                && mLayoutDirection == that.mLayoutDirection;
    }

    @Override
    public int hashCode() {
        int result = (int) (mSplitRatio * 17);
        result = 31 * result + mParentWindowMetricsPredicate.hashCode();
        result = 31 * result + mLayoutDirection;
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "SplitRule{"
                + "mSplitRatio=" + mSplitRatio
                + ", mLayoutDirection=" + mLayoutDirection
                + '}';
    }
}
