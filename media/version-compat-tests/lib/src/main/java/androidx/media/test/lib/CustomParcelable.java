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

package androidx.media.test.lib;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Custom Parcelable class to test sending/receiving user parcelables between processes.
 */
@SuppressWarnings("BanParcelableUsage")
@SuppressLint("BanParcelableUsage")
public class CustomParcelable implements Parcelable {

    public int mValue;

    public CustomParcelable(int value) {
        mValue = value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mValue);
    }

    public static final Parcelable.Creator<CustomParcelable> CREATOR =
            new Parcelable.Creator<CustomParcelable>() {
                @Override
                public CustomParcelable createFromParcel(Parcel in) {
                    int value = in.readInt();
                    return new CustomParcelable(value);
                }

                @Override
                public CustomParcelable[] newArray(int size) {
                    return new CustomParcelable[size];
                }
            };
}
