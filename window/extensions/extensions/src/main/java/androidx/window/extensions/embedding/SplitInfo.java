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

import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.window.extensions.RequiresVendorApiLevel;
import androidx.window.extensions.embedding.SplitAttributes.SplitType;

import java.util.Objects;

/** Describes a split of two containers with activities. */
public class SplitInfo {

    @NonNull
    private final ActivityStack mPrimaryActivityStack;
    @NonNull
    private final ActivityStack mSecondaryActivityStack;
    @NonNull
    private final SplitAttributes mSplitAttributes;

    @NonNull
    private final IBinder mToken;

    /**
     * The {@code SplitInfo} constructor
     *
     * @param primaryActivityStack The primary {@link ActivityStack}
     * @param secondaryActivityStack The secondary {@link ActivityStack}
     * @param splitAttributes The current {@link SplitAttributes} of this split pair
     * @param token The token to identify this split pair
     */
    SplitInfo(@NonNull ActivityStack primaryActivityStack,
            @NonNull ActivityStack secondaryActivityStack,
            @NonNull SplitAttributes splitAttributes,
            @NonNull IBinder token) {
        Objects.requireNonNull(primaryActivityStack);
        Objects.requireNonNull(secondaryActivityStack);
        Objects.requireNonNull(splitAttributes);
        Objects.requireNonNull(token);
        mPrimaryActivityStack = primaryActivityStack;
        mSecondaryActivityStack = secondaryActivityStack;
        mSplitAttributes = splitAttributes;
        mToken = token;
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
     * @deprecated Use {@link #getSplitAttributes()} starting with vendor API level 2. Only used if
     * {@link #getSplitAttributes()} can't be called on vendor API level 1.
     */
    @RequiresVendorApiLevel(level = 1, deprecatedSince = 2)
    @Deprecated
    public float getSplitRatio() {
        final SplitType splitType = mSplitAttributes.getSplitType();
        if (splitType instanceof SplitType.RatioSplitType) {
            return ((SplitType.RatioSplitType) splitType).getRatio();
        } else { // Fallback to use 0.0 because the WM Jetpack may not support HingeSplitType.
            return 0.0f;
        }
    }

    /** Returns the {@link SplitAttributes} of this split. */
    @RequiresVendorApiLevel(level = 2)
    @NonNull
    public SplitAttributes getSplitAttributes() {
        return mSplitAttributes;
    }

    /** Returns a token uniquely identifying the container. */
    @RequiresVendorApiLevel(level = 3)
    @NonNull
    public IBinder getToken() {
        return mToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SplitInfo)) return false;
        SplitInfo that = (SplitInfo) o;
        return mSplitAttributes.equals(that.mSplitAttributes) && mPrimaryActivityStack.equals(
                that.mPrimaryActivityStack) && mSecondaryActivityStack.equals(
                that.mSecondaryActivityStack) && mToken.equals(that.mToken);
    }

    @Override
    public int hashCode() {
        int result = mPrimaryActivityStack.hashCode();
        result = result * 31 + mSecondaryActivityStack.hashCode();
        result = result * 31 + mSplitAttributes.hashCode();
        result = result * 31 + mToken.hashCode();
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "SplitInfo{"
                + "mPrimaryActivityStack=" + mPrimaryActivityStack
                + ", mSecondaryActivityStack=" + mSecondaryActivityStack
                + ", mSplitAttributes=" + mSplitAttributes
                + ", mToken=" + mToken
                + '}';
    }
}
