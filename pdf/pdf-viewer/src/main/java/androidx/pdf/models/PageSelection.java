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

import android.graphics.Rect;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.TextSelection;

import java.util.List;

/** Represents text selection on a particular page of a PDF. Immutable. */
// TODO: Use android.graphics.pdf.models.selection.PageSelection and remove this class
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("deprecation")
public class PageSelection extends TextSelection {

    @SuppressWarnings("hiding")
    public static final Creator<PageSelection> CREATOR = new Creator<PageSelection>() {
        @SuppressWarnings("unchecked")
        @Override
        public PageSelection createFromParcel(Parcel parcel) {
            return new PageSelection(parcel.readInt(), (SelectionBoundary) parcel.readParcelable(
                    SelectionBoundary.class.getClassLoader()),
                    (SelectionBoundary) parcel.readParcelable(
                            SelectionBoundary.class.getClassLoader()),
                    (List<Rect>) parcel.readArrayList(Rect.class.getClassLoader()),
                    parcel.readString());
        }

        @Override
        public PageSelection[] newArray(int size) {
            return new PageSelection[size];
        }
    };

    /** The page the selection is on. */
    private final int mPage;

    /** The bounding boxes of the highlighted text. */
    private final List<Rect> mRects;

    /** The highlighted text. */
    private final String mText;

    public PageSelection(int page, @NonNull SelectionBoundary start,
            @NonNull SelectionBoundary stop, @NonNull List<Rect> rects, @NonNull String text) {
        super(start, stop);
        this.mPage = page;
        this.mRects = rects;
        this.mText = text;
    }

    public int getPage() {
        return mPage;
    }

    @NonNull
    public List<Rect> getRects() {
        return mRects;
    }

    @NonNull
    public String getText() {
        return mText;
    }

    @Override
    public String toString() {
        return String.format("PageSelection(page=%d, start=%s, stop=%s, %d rects)", mPage,
                getStart(),
                getStop(), mRects.size());
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeInt(mPage);
        parcel.writeParcelable(getStart(), 0);
        parcel.writeParcelable(getStop(), 0);
        parcel.writeList(mRects);
        parcel.writeString(mText);
    }
}
