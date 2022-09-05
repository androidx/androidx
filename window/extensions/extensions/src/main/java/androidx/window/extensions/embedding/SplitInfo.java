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
import androidx.window.extensions.WindowExtensions;
import androidx.window.extensions.embedding.SplitAttributes.SplitType;

/** Describes a split of two containers with activities. */
public class SplitInfo {
    @NonNull
    private final ActivityStack mPrimaryActivityStack;
    @NonNull
    private final ActivityStack mSecondaryActivityStack;
    @NonNull
    private final SplitAttributes mSplitAttributes;

    /** @since {@link androidx.window.extensions.WindowExtensions#VENDOR_API_LEVEL_2} */
    public SplitInfo(@NonNull ActivityStack primaryActivityStack,
            @NonNull ActivityStack secondaryActivityStack,
            @NonNull SplitAttributes splitAttributes) {
        mPrimaryActivityStack = primaryActivityStack;
        mSecondaryActivityStack = secondaryActivityStack;
        mSplitAttributes = splitAttributes;
    }

    @NonNull
    public ActivityStack getPrimaryActivityStack() {
        return mPrimaryActivityStack;
    }

    @NonNull
    public ActivityStack getSecondaryActivityStack() {
        return mSecondaryActivityStack;
    }

    /**
     * @deprecated Use {@link #getSplitAttributes()} starting with
     * {@link WindowExtensions#VENDOR_API_LEVEL_2}. Only used if {@link #getSplitAttributes()}
     * can't be called on {@link WindowExtensions#VENDOR_API_LEVEL_1}.
     */
    @Deprecated
    public float getSplitRatio() {
        final SplitType splitType = mSplitAttributes.getSplitType();
        if (splitType instanceof SplitType.RatioSplitType) {
            return ((SplitType.RatioSplitType) splitType).getRatio();
        } else { // Fallback to use 0.0 because the WM Jetpack may not support HingeSplitType.
            return 0.0f;
        }
    }

    /**
     * Returns the {@link SplitAttributes} of this split.
     * @since {@link androidx.window.extensions.WindowExtensions#VENDOR_API_LEVEL_2}
     */
    @NonNull
    public SplitAttributes getSplitAttributes() {
        return mSplitAttributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SplitInfo)) return false;
        SplitInfo that = (SplitInfo) o;
        return mSplitAttributes.equals(that.mSplitAttributes) && mPrimaryActivityStack.equals(
                that.mPrimaryActivityStack) && mSecondaryActivityStack.equals(
                that.mSecondaryActivityStack);
    }

    @Override
    public int hashCode() {
        int result = mPrimaryActivityStack.hashCode();
        result = result * 31 + mSecondaryActivityStack.hashCode();
        result = result * 31 + mSplitAttributes.hashCode();
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "SplitInfo{"
                + "mPrimaryActivityStack=" + mPrimaryActivityStack
                + ", mSecondaryActivityStack=" + mSecondaryActivityStack
                + ", mSplitAttributes=" + mSplitAttributes
                + '}';
    }
}
