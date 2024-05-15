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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;
import androidx.pdf.models.SelectionBoundary;

/** Represents the selection of part of a piece of text - a start and a stop. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TextSelection implements Parcelable {

    public static final TextSelection EMPTY_SELECTION = new TextSelection(
            SelectionBoundary.PAGE_START, SelectionBoundary.PAGE_START);

    @SuppressWarnings("deprecation")
    public static final Creator<TextSelection> CREATOR = new Creator<TextSelection>() {
        @SuppressWarnings("unchecked")
        @Override
        public TextSelection createFromParcel(Parcel parcel) {
            return new TextSelection((SelectionBoundary) parcel.readParcelable(
                    SelectionBoundary.class.getClassLoader()),
                    (SelectionBoundary) parcel.readParcelable(
                            SelectionBoundary.class.getClassLoader()));
        }

        @Override
        public TextSelection[] newArray(int size) {
            return new TextSelection[size];
        }
    };

    /** The start of the selection - index is inclusive. */
    private final SelectionBoundary mStart;

    /** The end of the selection - index is exclusive. */
    private final SelectionBoundary mStop;

    public TextSelection(SelectionBoundary start, SelectionBoundary stop) {
        this.mStart = start;
        this.mStop = stop;
    }

    public SelectionBoundary getStart() {
        return mStart;
    }

    public SelectionBoundary getStop() {
        return mStop;
    }

    @Override
    public String toString() {
        return String.format("TextSelection(start=%s, stop=%s)", mStart, mStop);
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(mStart, 0);
        parcel.writeParcelable(mStop, 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
