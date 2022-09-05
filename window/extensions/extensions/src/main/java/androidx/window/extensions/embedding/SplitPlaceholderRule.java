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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.window.extensions.WindowExtensions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Predicate;

/**
 * Split configuration rules for split placeholders - activities used to occupy additional
 * available space on the side before the user selects content to show.
 */
public class SplitPlaceholderRule extends SplitRule {
    @NonNull
    private final Predicate<Activity> mActivityPredicate;
    @NonNull
    private final Predicate<Intent> mIntentPredicate;
    @NonNull
    private final Intent mPlaceholderIntent;
    private final boolean mIsSticky;

    /**
     * Determines what happens with the primary container when the placeholder activity is
     * finished in one of the containers in a split.
     */
    @IntDef({
            FINISH_ALWAYS,
            FINISH_ADJACENT
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface SplitPlaceholderFinishBehavior{}

    @SplitPlaceholderFinishBehavior
    private final int mFinishPrimaryWithPlaceholder;

    SplitPlaceholderRule(@NonNull Intent placeholderIntent,
            @NonNull SplitAttributes defaultSplitAttributes,
            boolean isSticky,
            @SplitPlaceholderFinishBehavior int finishPrimaryWithPlaceholder,
            @NonNull Predicate<Activity> activityPredicate,
            @NonNull Predicate<Intent> intentPredicate,
            @NonNull Predicate<WindowMetrics> parentWindowMetricsPredicate) {
        super(parentWindowMetricsPredicate, defaultSplitAttributes);
        mIsSticky = isSticky;
        mFinishPrimaryWithPlaceholder = finishPrimaryWithPlaceholder;
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
     * Determines whether the placeholder will show on top in a smaller window size after it first
     * appeared in a split with sufficient minimum width.
     */
    public boolean isSticky() {
        return mIsSticky;
    }

    /**
     * @deprecated Use {@link #getFinishPrimaryWithPlaceholder()} instead starting with
     * {@link WindowExtensions#VENDOR_API_LEVEL_2}. Only used if
     * {@link #getFinishPrimaryWithPlaceholder()} can't be called on
     * {@link WindowExtensions#VENDOR_API_LEVEL_1}.
     */
    @Deprecated
    @SplitPlaceholderFinishBehavior
    public int getFinishPrimaryWithSecondary() {
        return getFinishPrimaryWithPlaceholder();
    }

    /**
     * Determines what happens with the primary container when all activities are finished in the
     * associated secondary/placeholder container.
     *
     * @since {@link WindowExtensions#VENDOR_API_LEVEL_2}
     */
    // TODO(b/238905747): Add api guard for extensions.
    @SplitPlaceholderFinishBehavior
    public int getFinishPrimaryWithPlaceholder() {
        return mFinishPrimaryWithPlaceholder;
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
        // Keep for backward compatibility
        private float mSplitRatio;
        // Keep for backward compatibility
        @SplitAttributes.LayoutDir
        private int mLayoutDirection;
        private SplitAttributes mDefaultSplitAttributes;
        private boolean mIsSticky = false;
        @SplitPlaceholderFinishBehavior
        private int mFinishPrimaryWithPlaceholder = FINISH_ALWAYS;

        public Builder(@NonNull Intent placeholderIntent,
                @NonNull Predicate<Activity> activityPredicate,
                @NonNull Predicate<Intent> intentPredicate,
                @NonNull Predicate<WindowMetrics> parentWindowMetricsPredicate) {
            mActivityPredicate = activityPredicate;
            mIntentPredicate = intentPredicate;
            mPlaceholderIntent = placeholderIntent;
            mParentWindowMetricsPredicate = parentWindowMetricsPredicate;
        }

        /**
         * @deprecated Use {@link #setDefaultSplitAttributes(SplitAttributes)} starting with
         * {@link WindowExtensions#VENDOR_API_LEVEL_2}. Only used if
         * {@link #setDefaultSplitAttributes(SplitAttributes)} can't be called on
         * {@link WindowExtensions#VENDOR_API_LEVEL_1}.
         */
        @Deprecated
        @NonNull
        public Builder setSplitRatio(float splitRatio) {
            mSplitRatio = splitRatio;
            return this;
        }

        /**
         * @deprecated Use {@link #setDefaultSplitAttributes(SplitAttributes)} starting with
         * {@link WindowExtensions#VENDOR_API_LEVEL_2}. Only used if
         * {@link #setDefaultSplitAttributes(SplitAttributes)} can't be called on
         * {@link WindowExtensions#VENDOR_API_LEVEL_1}.
         */
        @Deprecated
        @NonNull
        public Builder setLayoutDirection(@SplitAttributes.LayoutDir int layoutDirection) {
            mLayoutDirection = layoutDirection;
            return this;
        }

        /**
         * See {@link SplitPlaceholderRule#getDefaultSplitAttributes()} for reference.
         * Overrides values if set in {@link #setSplitRatio(float)} and
         * {@link #setLayoutDirection(int)}
         *
         * @since {@link WindowExtensions#VENDOR_API_LEVEL_2}
         */
        @NonNull
        public Builder setDefaultSplitAttributes(@NonNull SplitAttributes attrs) {
            mDefaultSplitAttributes = attrs;
            return this;
        }

        /** @see SplitPlaceholderRule#isSticky() */
        @NonNull
        public Builder setSticky(boolean sticky) {
            mIsSticky = sticky;
            return this;
        }

        /**
         * @deprecated Use SplitPlaceholderRule#setFinishPrimaryWithPlaceholder(int)} starting with
         * {@link WindowExtensions#VENDOR_API_LEVEL_2}.
         */
        @Deprecated
        @NonNull
        public Builder setFinishPrimaryWithSecondary(
                @SplitPlaceholderFinishBehavior int finishBehavior) {
            if (finishBehavior == FINISH_NEVER) {
                finishBehavior = FINISH_ALWAYS;
            }
            return setFinishPrimaryWithPlaceholder(finishBehavior);
        }

        /**
         * @see SplitPlaceholderRule#getFinishPrimaryWithPlaceholder()
         * @since {@link WindowExtensions#VENDOR_API_LEVEL_2}
         */
        // TODO(b/238905747): Add api guard for extensions.
        @NonNull
        public Builder setFinishPrimaryWithPlaceholder(
                @SplitPlaceholderFinishBehavior int finishBehavior) {
            mFinishPrimaryWithPlaceholder = finishBehavior;
            return this;
        }

        /** Builds a new instance of {@link SplitPlaceholderRule}. */
        @NonNull
        public SplitPlaceholderRule build() {
            // To provide compatibility with prior version of WM Jetpack library, where
            // #setDefaultAttributes hasn't yet been supported and thus would not be set.
            mDefaultSplitAttributes = (mDefaultSplitAttributes != null)
                    ? mDefaultSplitAttributes
                    : new SplitAttributes.Builder()
                            .setSplitType(new SplitAttributes.SplitType.RatioSplitType(mSplitRatio))
                            .setLayoutDirection(mLayoutDirection)
                            .build();
            return new SplitPlaceholderRule(mPlaceholderIntent, mDefaultSplitAttributes, mIsSticky,
                    mFinishPrimaryWithPlaceholder, mActivityPredicate,
                    mIntentPredicate, mParentWindowMetricsPredicate);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SplitPlaceholderRule)) return false;
        if (!super.equals(o)) return false;

        SplitPlaceholderRule that = (SplitPlaceholderRule) o;

        if (mIsSticky != that.mIsSticky) return false;
        if (mFinishPrimaryWithPlaceholder != that.mFinishPrimaryWithPlaceholder) return false;
        if (!mActivityPredicate.equals(that.mActivityPredicate)) return false;
        if (!mIntentPredicate.equals(that.mIntentPredicate)) return false;
        return mPlaceholderIntent.equals(that.mPlaceholderIntent);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mActivityPredicate.hashCode();
        result = 31 * result + mIntentPredicate.hashCode();
        result = 31 * result + mPlaceholderIntent.hashCode();
        result = 31 * result + (mIsSticky ? 1 : 0);
        result = 31 * result + mFinishPrimaryWithPlaceholder;
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "SplitPlaceholderRule{"
                + "mDefaultSplitAttributes=" + getDefaultSplitAttributes()
                + ", mActivityPredicate=" + mActivityPredicate
                + ", mIsSticky=" + mIsSticky
                + ", mFinishPrimaryWithPlaceholder=" + mFinishPrimaryWithPlaceholder
                + '}';
    }
}
