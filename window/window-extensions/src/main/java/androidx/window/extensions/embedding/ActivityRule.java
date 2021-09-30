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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.window.extensions.ExperimentalWindowExtensionsApi;

import java.util.function.Predicate;

/**
 * Split configuration rule for individual activities.
 */
@ExperimentalWindowExtensionsApi
public class ActivityRule extends EmbeddingRule {
    @NonNull
    private final Predicate<Activity> mActivityPredicate;
    @NonNull
    private final Predicate<Intent> mIntentPredicate;
    private final boolean mShouldAlwaysExpand;

    ActivityRule(@NonNull Predicate<Activity> activityPredicate,
            @NonNull Predicate<Intent> intentPredicate, boolean shouldAlwaysExpand) {
        mActivityPredicate = activityPredicate;
        mIntentPredicate = intentPredicate;
        mShouldAlwaysExpand = shouldAlwaysExpand;
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
     * Indicates whether the activity or activities that are covered by this rule should always be
     * launched in an expanded state and avoid the splits.
     */
    public boolean shouldAlwaysExpand() {
        return mShouldAlwaysExpand;
    }

    /**
     * Builder for {@link ActivityRule}.
     */
    public static final class Builder {
        @NonNull
        private final Predicate<Activity> mActivityPredicate;
        @NonNull
        private final Predicate<Intent> mIntentPredicate;
        private boolean mAlwaysExpand;

        public Builder(@NonNull Predicate<Activity> activityPredicate,
                @NonNull Predicate<Intent> intentPredicate) {
            mActivityPredicate = activityPredicate;
            mIntentPredicate = intentPredicate;
        }

        /** @see ActivityRule#shouldAlwaysExpand() */
        @NonNull
        public Builder setShouldAlwaysExpand(boolean alwaysExpand) {
            mAlwaysExpand = alwaysExpand;
            return this;
        }

        /** Builds a new instance of {@link ActivityRule}. */
        @NonNull
        public ActivityRule build() {
            return new ActivityRule(mActivityPredicate, mIntentPredicate, mAlwaysExpand);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityRule)) return false;
        ActivityRule that = (ActivityRule) o;
        return mShouldAlwaysExpand == that.mShouldAlwaysExpand
                && mActivityPredicate.equals(that.mActivityPredicate)
                && mIntentPredicate.equals(that.mIntentPredicate);
    }

    @Override
    public int hashCode() {
        int result = mActivityPredicate.hashCode();
        result = 31 * result + mIntentPredicate.hashCode();
        result = 31 * result + (mShouldAlwaysExpand ? 1 : 0);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "ActivityRule{" + "mShouldAlwaysExpand=" + mShouldAlwaysExpand + '}';
    }
}
