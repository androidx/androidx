/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * A dummy implementation for overriding a hidden framework class, ParceledListSlice.
 * When there are duplicated signatures between app and framework code, the framework code will be
 * run.
 * @hide
 */
public class ParceledListSlice<T extends Parcelable> implements Parcelable {
    public ParceledListSlice(List<T> list) {
    }

    @SuppressWarnings("unchecked")
    private ParceledListSlice(Parcel p, ClassLoader loader) {
    }

    private static void verifySameType(final Class<?> expected, final Class<?> actual) {
    }

    public List<T> getList() {
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    @SuppressWarnings("unchecked")
    public static final Parcelable.ClassLoaderCreator<ParceledListSlice> CREATOR =
            new Parcelable.ClassLoaderCreator<ParceledListSlice>() {
        public ParceledListSlice createFromParcel(Parcel in) {
            return null;
        }

        @Override
        public ParceledListSlice createFromParcel(Parcel in, ClassLoader loader) {
            return null;
        }

        public ParceledListSlice[] newArray(int size) {
            return null;
        }
    };
}
