/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.control.data;

import static android.app.ApplicationErrorReport.CrashInfo;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.StringBuilderPrinter;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Used for sending details of an exception over aidl.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@SuppressLint("BanParcelableUsage")
public class CrashInfoParcel implements Parcelable {
    @NonNull public final CrashInfo crashInfo;

    public CrashInfoParcel(@NonNull Throwable exception) {
        crashInfo = new CrashInfo(exception);
    }

    CrashInfoParcel(Parcel in) {
        crashInfo = new CrashInfo(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        crashInfo.writeToParcel(dest, flags);
    }

    public static final Parcelable.Creator<CrashInfoParcel> CREATOR =
            new Parcelable.Creator<CrashInfoParcel>() {
                @Override
                public CrashInfoParcel createFromParcel(Parcel source) {
                    return new CrashInfoParcel(source);
                }

                @Override
                public CrashInfoParcel[] newArray(int size) {
                    return new CrashInfoParcel[size];
                }
            };

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        StringBuilderPrinter pr = new StringBuilderPrinter(sb);
        crashInfo.dump(pr, "");
        return sb.toString();
    }
}
