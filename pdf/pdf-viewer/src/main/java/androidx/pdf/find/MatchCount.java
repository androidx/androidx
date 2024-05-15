/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.find;

import androidx.annotation.RestrictTo;

/**
 * Information about which match is selected and how many matches are found.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class MatchCount {
    public final int mSelectedIndex;  // Zero-based, -1 means no selected match.
    public final int mTotalMatches;
    public final boolean mIsAllPagesCounted;

    public MatchCount(int selectedIndex, int totalMatches, boolean isAllPagesCounted) {
        this.mSelectedIndex = selectedIndex;
        this.mTotalMatches = totalMatches;
        this.mIsAllPagesCounted = isAllPagesCounted;
    }

    @Override
    public int hashCode() {
        return (mSelectedIndex + 31 * mTotalMatches) * (mIsAllPagesCounted ? 1 : -1);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MatchCount)) {
            return false;
        }
        MatchCount that = (MatchCount) other;
        return this.mSelectedIndex == that.mSelectedIndex
                && this.mTotalMatches == that.mTotalMatches
                && this.mIsAllPagesCounted == that.mIsAllPagesCounted;
    }

    @Override
    public String toString() {
        return String.format("MatchCount(%d of %d, allPagesCounted=%s)",
                mSelectedIndex, mTotalMatches, mIsAllPagesCounted);
    }
}
