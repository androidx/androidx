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

import androidx.annotation.NonNull;
import androidx.window.extensions.ExperimentalWindowExtensionsApi;

/** Describes a split of two containers with activities. */
@ExperimentalWindowExtensionsApi
public class SplitInfo {
    @NonNull
    private final ActivityStack mPrimaryActivityStack;
    @NonNull
    private final ActivityStack mSecondaryActivityStack;
    private final float mSplitRatio;

    public SplitInfo(@NonNull ActivityStack primaryActivityStack,
            @NonNull ActivityStack secondaryActivityStack, float splitRatio) {
        mPrimaryActivityStack = primaryActivityStack;
        mSecondaryActivityStack = secondaryActivityStack;
        mSplitRatio = splitRatio;
    }

    @NonNull
    public ActivityStack getPrimaryActivityStack() {
        return mPrimaryActivityStack;
    }

    @NonNull
    public ActivityStack getSecondaryActivityStack() {
        return mSecondaryActivityStack;
    }

    public float getSplitRatio() {
        return mSplitRatio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SplitInfo)) return false;
        SplitInfo that = (SplitInfo) o;
        return Float.compare(that.mSplitRatio, mSplitRatio) == 0 && mPrimaryActivityStack.equals(
                that.mPrimaryActivityStack) && mSecondaryActivityStack.equals(
                that.mSecondaryActivityStack);
    }

    @Override
    public int hashCode() {
        int result = mPrimaryActivityStack.hashCode();
        result = result * 31 + mSecondaryActivityStack.hashCode();
        result = result * 31 + (int) (mSplitRatio * 17);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "SplitInfo{"
                + "mPrimaryActivityStack=" + mPrimaryActivityStack
                + ", mSecondaryActivityStack=" + mSecondaryActivityStack
                + ", mSplitRatio=" + mSplitRatio
                + '}';
    }
}
