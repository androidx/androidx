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
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Dimensions of a rectangular area: width and height.
 * Objects of this class are immutable.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("BanParcelableUsage")
public class Dimensions implements Parcelable {
    public static final Creator<Dimensions> CREATOR = new Creator<Dimensions>() {
        @Override
        public Dimensions createFromParcel(Parcel parcel) {
            return new Dimensions(parcel.readInt(), parcel.readInt());
        }

        @Override
        public Dimensions[] newArray(int size) {
            return new Dimensions[size];
        }
    };

    private final int mWidth;
    private final int mHeight;

    public Dimensions(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    public Dimensions(@NonNull Rect rect) {
        this.mWidth = rect.width();
        this.mHeight = rect.height();
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Dimensions) {
            Dimensions other = (Dimensions) o;
            return mWidth == other.mWidth && mHeight == other.mHeight;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * mWidth + mHeight;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("Dimensions (w:%d, h:%d)", mWidth, mHeight);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeInt(mWidth);
        parcel.writeInt(mHeight);
    }
}
