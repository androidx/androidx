/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.testapp.pkg;

import android.os.Parcel;
import android.os.Parcelable;

public class MyPkgClass implements Parcelable {

    public MyPkgClass(Parcel parcel) {

    }

    @Override
    public void writeToParcel(Parcel parcel, int flahs) {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<MyPkgClass> CREATOR = new Parcelable.Creator<MyPkgClass>() {
        @Override
        public MyPkgClass createFromParcel(Parcel parcel) {
            return new MyPkgClass(parcel);
        }

        @Override
        public MyPkgClass[] newArray(int size) {
            return new MyPkgClass[size];
        }
    };
}