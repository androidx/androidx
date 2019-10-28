/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.graphics;

import static androidx.core.util.Preconditions.checkNotNull;

import android.graphics.Path;
import android.graphics.PointF;

import androidx.annotation.NonNull;

/**
 * A line segment that represents an approximate fraction of a {@link Path} after
 * {@linkplain PathUtils#flatten(Path) flattening}.
 */
public final class PathSegment {
    private final PointF mStart;
    private final float mStartFraction;
    private final PointF mEnd;
    private final float mEndFraction;

    public PathSegment(@NonNull PointF start, float startFraction, @NonNull PointF end,
            float endFraction) {
        mStart = checkNotNull(start, "start == null");
        mStartFraction = startFraction;
        mEnd = checkNotNull(end, "end == null");
        mEndFraction = endFraction;
    }

    /** The start point of the line segment. */
    @NonNull
    public PointF getStart() {
        return mStart;
    }

    /**
     * Fraction along the length of the path that the {@linkplain #getStart() start point} resides.
     */
    public float getStartFraction() {
        return mStartFraction;
    }

    /** The end point of the line segment. */
    @NonNull
    public PointF getEnd() {
        return mEnd;
    }

    /**
     * Fraction along the length of the path that the {@linkplain #getEnd() end point} resides.
     */
    public float getEndFraction() {
        return mEndFraction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PathSegment)) return false;
        PathSegment that = (PathSegment) o;
        return Float.compare(mStartFraction, that.mStartFraction) == 0
                && Float.compare(mEndFraction, that.mEndFraction) == 0
                && mStart.equals(that.mStart)
                && mEnd.equals(that.mEnd);
    }

    @Override
    public int hashCode() {
        int result = mStart.hashCode();
        result = 31 * result + (mStartFraction != +0.0f ? Float.floatToIntBits(mStartFraction) : 0);
        result = 31 * result + mEnd.hashCode();
        result = 31 * result + (mEndFraction != +0.0f ? Float.floatToIntBits(mEndFraction) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PathSegment{"
                + "start=" + mStart
                + ", startFraction=" + mStartFraction
                + ", end=" + mEnd
                + ", endFraction=" + mEndFraction
                + '}';
    }
}
