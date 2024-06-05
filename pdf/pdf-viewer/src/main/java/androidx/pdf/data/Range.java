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

package androidx.pdf.data;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.Iterator;

/**
 * A range of continuous numbers: [{@link #mFirst}, {@link #mLast}]. Both bounds are included.
 * Ranges are immutable.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class Range implements Iterable<Integer> {
    private final int mFirst;
    private final int mLast;

    public Range(int first, int last) {
        this.mFirst = first;
        this.mLast = last;
    }

    public Range() {
        this(0, -1);
    }

    public int getFirst() {
        return mFirst;
    }

    public int getLast() {
        return mLast;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Range)) {
            return false;
        }
        Range that = (Range) o;
        return this.mFirst == that.mFirst && this.mLast == that.mLast;
    }

    @Override
    public int hashCode() {
        return 991 * mFirst + mLast;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("Range [%d, %d]", mFirst, mLast);
    }

    public boolean isEmpty() {
        return mLast < mFirst;
    }

    /**
     *
     */
    public int length() {
        return 1 + mLast - mFirst;
    }

    /**
     *
     */
    @NonNull
    public Range union(int value) {
        if (isEmpty()) {
            return new Range(value, value);
        }
        if (value < mFirst) {
            return new Range(value, mLast);
        }
        if (value > mLast) {
            return new Range(mFirst, value);
        }
        return this;
    }

    /**
     *
     */
    @NonNull
    public Range union(@NonNull Range other) {
        if (contains(other)) {
            return this;
        } else if (other.contains(this)) {
            return other;
        } else {
            return new Range(Math.min(mFirst, other.mFirst), Math.max(mLast, other.mLast));
        }
    }

    /**
     *
     */
    @NonNull
    public Range intersect(@NonNull Range other) {
        if (contains(other)) {
            return this;
        } else if (other.contains(this)) {
            return other;
        } else {
            return new Range(Math.max(mFirst, other.mFirst), Math.min(mLast, other.mLast));
        }
    }

    /**
     *
     */
    @NonNull
    public Range[] minus(@NonNull Range other) {
        if (other.contains(this)) {
            return new Range[]{};
        }
        Range before = other.mFirst <= mFirst ? null : new Range(mFirst, other.mFirst - 1);
        Range after = other.mLast >= mLast ? null : new Range(other.mLast + 1, mLast);
        if (before != null) {
            if (after != null) {
                return new Range[]{before, after};
            } else {
                return new Range[]{before};
            }
        }
        return new Range[]{after};
    }

    /**
     *
     */
    public boolean contains(@NonNull Range other) {
        return other.isEmpty() || (mFirst <= other.mFirst && mLast >= other.mLast);
    }

    /**
     *
     */
    public boolean contains(int value) {
        return value >= mFirst && value <= mLast;
    }

    /** Create a new Range which is an expansion of this range both ways, up to the given bounds. */
    @NonNull
    public Range expand(int margin, @NonNull Range bounds) {
        return new Range(Math.max(mFirst - margin, bounds.mFirst),
                Math.min(mLast + margin, bounds.mLast));
    }

    @NonNull
    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {

            private int mCurrent = mFirst;

            @Override
            public boolean hasNext() {
                return mCurrent <= mLast;
            }

            @Override
            public Integer next() {
                return mCurrent++;
            }

            @Override
            public void remove() {
            }
        };
    }
}
