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

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.pdf.models.SelectionBoundary;

import java.util.Objects;

/** Represents the selection of part of a piece of text - a start and a stop. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TextSelection implements Parcelable {

    public static final TextSelection EMPTY_SELECTION = new TextSelection(
            SelectionBoundary.PAGE_START, SelectionBoundary.PAGE_START);

    public static final Creator<TextSelection> CREATOR = new Creator<TextSelection>() {
        @SuppressLint("ObsoleteSdkInt") //TODO: Remove after sdk extension 13 release
        @Override
        public TextSelection createFromParcel(Parcel parcel) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return Api33Impl.createFromParcel(parcel);
            } else {
                return ApiPre33Impl.createFromParcel(parcel);
            }
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

    public TextSelection(@NonNull SelectionBoundary start, @NonNull SelectionBoundary stop) {
        this.mStart = start;
        this.mStop = stop;
    }

    @NonNull
    public SelectionBoundary getStart() {
        return mStart;
    }

    @NonNull
    public SelectionBoundary getStop() {
        return mStop;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("TextSelection(start=%s, stop=%s)", mStart, mStop);
    }

    @SuppressLint("ObsoleteSdkInt") //TODO: Remove after sdk extension 13 release
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Api33Impl.writeToParcel(this, parcel, flags);
        } else {
            ApiPre33Impl.writeToParcel(this, parcel, flags);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @SuppressLint("ObsoleteSdkInt") //TODO: Remove after sdk extension 13 release
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU) // API 33
    private static final class Api33Impl {

        private Api33Impl() {
        } // Prevent instantiation

        public static TextSelection createFromParcel(Parcel parcel) {
            SelectionBoundary start = parcel.readParcelable(
                    SelectionBoundary.class.getClassLoader(), SelectionBoundary.class);
            SelectionBoundary stop = parcel.readParcelable(
                    SelectionBoundary.class.getClassLoader(), SelectionBoundary.class);

            Objects.requireNonNull(start, "Start SelectionBoundary cannot be null");
            Objects.requireNonNull(stop, "Stop SelectionBoundary cannot be null");

            return new TextSelection(start, stop);
        }

        public static void writeToParcel(
                TextSelection selection,
                @NonNull Parcel parcel,
                int flags
        ) {
            parcel.writeParcelable(selection.mStart, flags);
            parcel.writeParcelable(selection.mStop, flags);
        }
    }

    private static final class ApiPre33Impl {

        private ApiPre33Impl() {
        } // Prevent instantiation

        public static TextSelection createFromParcel(Parcel parcel) {
            SelectionBoundary start = SelectionBoundary.CREATOR.createFromParcel(parcel);
            SelectionBoundary stop = SelectionBoundary.CREATOR.createFromParcel(parcel);

            Objects.requireNonNull(start, "Start SelectionBoundary cannot be null");
            Objects.requireNonNull(stop, "Stop SelectionBoundary cannot be null");

            return new TextSelection(start, stop);
        }

        public static void writeToParcel(
                TextSelection selection,
                @NonNull Parcel parcel,
                int flags
        ) {
            selection.mStart.writeToParcel(parcel, flags);
            selection.mStop.writeToParcel(parcel, flags);
        }
    }
}
