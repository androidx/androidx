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

package androidx.pdf.models;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.models.PageMatchBounds;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ext.SdkExtensions;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.ListOfList;
import androidx.pdf.util.Preconditions;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the bounds of search matches as a {@code List<List<Rect>>}, where
 * the first {@code List<Rect>} is all of the rectangles needed to bound the
 * first match, and so on. Most matches will be surrounded with a single Rect.
 * <p>
 * Internally, data is stored as 1-dimensional Lists, to avoid the overhead of
 * a large amount of single-element lists.
 * <p>
 * Also contains data about the character index of each match - so {@link #get}
 * returns the rectangles that bound the match, and {@link #getCharIndex}
 * returns the character index that the match starts at.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("deprecation")
@SuppressLint("BanParcelableUsage")
public class MatchRects extends ListOfList<Rect> implements Parcelable {
    public static final MatchRects NO_MATCHES = new MatchRects(Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList());

    public static final Creator<MatchRects> CREATOR = new Creator<MatchRects>() {
        @SuppressWarnings("unchecked")
        @Override
        public MatchRects createFromParcel(Parcel parcel) {
            return new MatchRects(parcel.readArrayList(Rect.class.getClassLoader()),
                    parcel.readArrayList(Integer.class.getClassLoader()),
                    parcel.readArrayList(Integer.class.getClassLoader()));
        }

        @Override
        public MatchRects[] newArray(int size) {
            return new MatchRects[size];
        }
    };

    private final List<Rect> mRects;
    private final List<Integer> mMatchToRect;
    private final List<Integer> mCharIndexes;

    public MatchRects(@NonNull List<Rect> rects, @NonNull List<Integer> matchToRect,
            @NonNull List<Integer> charIndexes) {
        super(rects, matchToRect);
        this.mRects = Preconditions.checkNotNull(rects);
        this.mMatchToRect = Preconditions.checkNotNull(matchToRect);
        this.mCharIndexes = Preconditions.checkNotNull(charIndexes);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MatchRects)) {
            return false;
        }
        MatchRects that = (MatchRects) other;
        return this.mRects.equals(that.mRects)
                && this.mMatchToRect.equals(that.mMatchToRect)
                && this.mCharIndexes.equals(that.mCharIndexes);
    }

    @Override
    public int hashCode() {
        return mRects.hashCode() + 31 * mMatchToRect.hashCode() + 101 * mCharIndexes.hashCode();
    }

    /** Returns the character index corresponding to the given match. */
    public int getCharIndex(int match) {
        return mCharIndexes.get(match);
    }

    /** Returns the first rect for a given match. */
    @NonNull
    public Rect getFirstRect(int match) {
        return mRects.get(mMatchToRect.get(match));
    }

    /**
     * Returns the flattened, one-dimensional list of all rectangles that surround
     * all matches <strong>except</strong> for the given match.
     */
    @NonNull
    public List<Rect> flattenExcludingMatch(int match) {
        if (match < 0 || match >= mMatchToRect.size()) {
            throw new ArrayIndexOutOfBoundsException(match);
        }
        final int startExclude = indexToFirstValue(match);
        final int stopExclude = indexToFirstValue(match + 1);
        return new AbstractList<Rect>() {
            @Override
            public Rect get(int index) {
                return (index < startExclude) ? mRects.get(index)
                        : mRects.get(index - startExclude + stopExclude);
            }

            @Override
            public int size() {
                return mRects.size() - (stopExclude - startExclude);
            }
        };
    }

    /**
     * When the search term is updated, we automatically find the match that occurs
     * at the same character index (if it still matches), or next in the text (if
     * it no longer matches after the query changes).
     */
    public int getMatchNearestCharIndex(int oldCharIndex) {
        if (size() <= 1) {
            return size() - 1;
        }
        int searchResult = Collections.binarySearch(mCharIndexes, oldCharIndex);
        if (searchResult >= 0) {
            return searchResult;
        }
        return Math.min(size() - 1, -searchResult - 1);
    }

    @NonNull
    @Override
    public String toString() {
        return size() + " matches";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeList(mRects);
        parcel.writeList(mMatchToRect);
        parcel.writeList(mCharIndexes);
    }

    /**
     * Flattens the list of PageMatchBounds objects and converts it to a MatchRects objects.
     * <p>As an example, in case there are 2 matches on the page of the document with the 1st match
     * overflowing to the next line, {@code List<PageMatchBounds>} would have the following values -
     * <pre>
     * List(
     *      PageMatchBounds(
     *          bounds = [RectF(l1, t1, r1, b1), RectF(l2, t2, r2, b2)],
     *          mTextStartIndex = 1
     *      ),
     *      PageMatchBounds(
     *          bounds = [RectF(l3, t3, r3, b3)],
     *          mTextStartIndex = 3
     *      ),
     * )
     *
     * Using the method below, we can flatten the {@code List<PageMatchBounds>} to the following
     * representation -
     * MatchRects(
     *      mRects=[Rect(l1, t1, r1, b1), Rect(l2, t2, r2, b2), Rect(l3, t3, r3, b3)],
     *      mMatchToRect=[0,2],
     *      mCharIndexes=[1, 3]
     * )
     * </pre>
     */
    @NonNull
    public static MatchRects flattenList(@NonNull List<PageMatchBounds> pageMatchBoundsList) {
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            List<Rect> rects = new ArrayList<>();
            List<Integer> matchToRect = new ArrayList<>();
            List<Integer> charIndexes = new ArrayList<>();
            int numRects = 0;
            for (PageMatchBounds pageMatchBound : pageMatchBoundsList) {
                List<RectF> rectFBounds = pageMatchBound.getBounds();
                for (RectF rectF : rectFBounds) {
                    rects.add(new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right,
                            (int) rectF.bottom));
                }
                matchToRect.add(numRects);
                numRects += pageMatchBound.getBounds().size();
                charIndexes.add(pageMatchBound.getTextStartIndex());
            }
            return new MatchRects(rects, matchToRect, charIndexes);
        }
        throw new UnsupportedOperationException("Operation support above S");
    }

    @NonNull
    public List<Rect> getRects() {
        return mRects;
    }

    @NonNull
    public List<Integer> getMatchToRect() {
        return mMatchToRect;
    }

    @NonNull
    public List<Integer> getCharIndexes() {
        return mCharIndexes;
    }
}
