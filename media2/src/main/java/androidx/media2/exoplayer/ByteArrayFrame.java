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

package androidx.media2.exoplayer;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.os.Parcel;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.media2.exoplayer.external.metadata.Metadata;
import androidx.media2.exoplayer.external.util.Util;

import java.util.Arrays;

/**
 * Metadata entry consisting of an ID3 frame as a byte array.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ final class ByteArrayFrame implements Metadata.Entry {

    public final long mTimestamp;
    public final byte[] mData;

    /** Creates a new byte array frame. */
    ByteArrayFrame(long timestamp, byte[] data) {
        mTimestamp = timestamp;
        mData = data;
    }

    /* package */ ByteArrayFrame(Parcel in) {
        mTimestamp = in.readLong();
        int length = in.readInt();
        byte[] data = new byte[length];
        in.readByteArray(data);
        mData = data;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ByteArrayFrame other = (ByteArrayFrame) obj;
        return Util.areEqual(mTimestamp, other.mTimestamp) && Arrays.equals(mData, other.mData);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (int) mTimestamp;
        result = 31 * result + Arrays.hashCode(mData);
        return result;
    }

    // Parcelable implementation.

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mTimestamp);
        dest.writeByteArray(mData);
    }

    public static final Creator<ByteArrayFrame> CREATOR =
            new Creator<ByteArrayFrame>() {

        @Override
        public ByteArrayFrame createFromParcel(Parcel in) {
            return new ByteArrayFrame(in);
        }

        @Override
        public ByteArrayFrame[] newArray(int size) {
            return new ByteArrayFrame[size];
        }

    };
}
