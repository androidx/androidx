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
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.window.extensions.ExperimentalWindowExtensionsApi;

import java.util.function.Predicate;

/**
 * Split configuration rules for split placeholders - activities used to occupy additional
 * available space on the side before the user selects content to show.
 */
@ExperimentalWindowExtensionsApi
public class SplitPlaceholderRule extends SplitRule {
    @NonNull
    private final Predicate<Activity> mActivityPredicate;
    @NonNull
    private final Predicate<Intent> mIntentPredicate;
    @NonNull
    private final Intent mPlaceholderIntent;

    SplitPlaceholderRule(@NonNull Intent placeholderIntent,
            float splitRatio, @LayoutDir int layoutDirection,
            @NonNull Predicate<Activity> activityPredicate,
            @NonNull Predicate<Intent> intentPredicate,
            @NonNull Predicate<WindowMetrics> parentWindowMetricsPredicate) {
        super(parentWindowMetricsPredicate, splitRatio, layoutDirection);
        mActivityPredicate = activityPredicate;
        mIntentPredicate = intentPredicate;
        mPlaceholderIntent = placeholderIntent;
    }

    /**
     * Checks if the rule is applicable to the provided activity.
     */
    @SuppressLint("ClassVerificationFailure") // Only called by Extensions implementation on device.
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean matchesActivity(@NonNull Activity activity) {
        return mActivityPredicate.test(activity);
    }

    /**
     * Checks if the rule is applicable to the provided activity intent.
     */
    @SuppressLint("ClassVerificationFailure") // Only called by Extensions implementation on device.
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean matchesIntent(@NonNull Intent intent) {
        return mIntentPredicate.test(intent);
    }

    /**
     * An {@link Intent} used by Extensions Sidecar to launch the placeholder when the space allows.
     */
    @NonNull
    public Intent getPlaceholderIntent() {
        return mPlaceholderIntent;
    }

    /**
     * Builder for {@link SplitPlaceholderRule}.
     */
    public static final class Builder {
        @NonNull
        private final Predicate<Activity> mActivityPredicate;
        @NonNull
        private final Predicate<Intent> mIntentPredicate;
        @NonNull
        private final Predicate<WindowMetrics> mParentWindowMetricsPredicate;
        @NonNull
        private final Intent mPlaceholderIntent;
        private float mSplitRatio;
        @LayoutDir
        private int mLayoutDirection;

        public Builder(@NonNull Intent placeholderIntent,
                @NonNull Predicate<Activity> activityPredicate,
                @NonNull Predicate<Intent> intentPredicate,
                @NonNull Predicate<WindowMetrics> parentWindowMetricsPredicate) {
            mActivityPredicate = activityPredicate;
            mIntentPredicate = intentPredicate;
            mPlaceholderIntent = placeholderIntent;
            mParentWindowMetricsPredicate = parentWindowMetricsPredicate;
        }

        /** @see SplitRule#getSplitRatio() */
        @NonNull
        public Builder setSplitRatio(float splitRatio) {
            mSplitRatio = splitRatio;
            return this;
        }

        /** @see SplitRule#getLayoutDirection() */
        @NonNull
        public Builder setLayoutDirection(@LayoutDir int layoutDirection) {
            mLayoutDirection = layoutDirection;
            return this;
        }

        /** Builds a new instance of {@link SplitPlaceholderRule}. */
        @NonNull
        public SplitPlaceholderRule build() {
            return new SplitPlaceholderRule(mPlaceholderIntent, mSplitRatio,
                    mLayoutDirection, mActivityPredicate, mIntentPredicate,
                    mParentWindowMetricsPredicate);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SplitPlaceholderRule)) return false;
        SplitPlaceholderRule that = (SplitPlaceholderRule) o;
        return super.equals(o)
                && mActivityPredicate.equals(that.mActivityPredicate)
                && mIntentPredicate.equals(that.mIntentPredicate)
                && mPlaceholderIntent.equals(that.mPlaceholderIntent);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mActivityPredicate.hashCode();
        result = 31 * result + mIntentPredicate.hashCode();
        result = 31 * result + mPlaceholderIntent.hashCode();
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "SplitPlaceholderRule{" + "mPlaceholderIntent=" + mPlaceholderIntent + '}';
    }
}
