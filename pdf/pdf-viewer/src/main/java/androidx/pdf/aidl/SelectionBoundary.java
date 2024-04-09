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

package androidx.pdf.aidl;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;


/**
 * Represents one edge of the selected text. A boundary can be defined by
 * either an index into the text, a point on the page, or both.
 * Should be a nested class of {@link PageSelection}, but AIDL prevents that.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SelectionBoundary implements Parcelable {
    public static final SelectionBoundary PAGE_START = SelectionBoundary.atIndex(0);
    public static final SelectionBoundary PAGE_END = SelectionBoundary.atIndex(Integer.MAX_VALUE);

    public static final Creator<SelectionBoundary> CREATOR = new Creator<SelectionBoundary>() {
        @SuppressWarnings("unchecked")
        @Override
        public SelectionBoundary createFromParcel(Parcel parcel) {
            int[] state = new int[4];
            parcel.readIntArray(state);
            return new SelectionBoundary(state[0], state[1], state[2], state[3] > 0);
        }

        @Override
        public SelectionBoundary[] newArray(int size) {
            return new SelectionBoundary[size];
        }
    };

    private final int mIndex;

    private final int mX;

    private final int mY;

    private final boolean mIsRtl;

    public SelectionBoundary(int index, int x, int y, boolean isRtl) {
        this.mIndex = index;
        this.mX = x;
        this.mY = y;
        this.mIsRtl = isRtl;
    }

    public int getIndex() {
        return mIndex;
    }

    public int getX() {
        return mX;
    }

    public int getY() {
        return mY;
    }

    public boolean isRtl() {
        return mIsRtl;
    }

    /** Create a boundary that has a particular index, but the position is not known. */
    public static SelectionBoundary atIndex(int index) {
        return new SelectionBoundary(index, -1, -1, false);
    }

    /** Create a boundary at a particular point, but the index is not known. */
    public static SelectionBoundary atPoint(int x, int y) {
        return new SelectionBoundary(-1, x, y, false);
    }

    /** Create a boundary at a particular point, but the index is not known. */
    public static SelectionBoundary atPoint(Point p) {
        return new SelectionBoundary(-1, p.x, p.y, false);
    }

    @Override
    public String toString() {
        String indexStr = (mIndex == Integer.MAX_VALUE) ? "MAX" : Integer.toString(mIndex);
        return String.format("@%s (%d,%d)", indexStr, mX, mY);
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeIntArray(new int[]{mIndex, mX, mY, mIsRtl ? 1 : 0});
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SelectionBoundary)) {
            return false;
        }
        SelectionBoundary other = (SelectionBoundary) obj;
        return this.mX == other.mX && this.mY == other.mY && this.mIndex == other.mIndex
                && this.mIsRtl == other.mIsRtl;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + mX;
        result = 31 * result + mY;
        result = 31 * result + mIndex;
        result = 31 * result + (mIsRtl ? 1231 : 1237);
        return result;
    }
}
