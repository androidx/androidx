/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.ongoingactivity;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * {@link OngoingActivityStatus} representing a plain, static text.
 */
@VersionedParcelize
public class TextOngoingActivityStatus extends OngoingActivityStatus {
    @NonNull
    @ParcelField(value = 1, defaultValue = "")
    private String mStr = "";

    public TextOngoingActivityStatus(@NonNull String str) {
        this.mStr = str;
    }

    /**
     * See {@link OngoingActivityStatus#getText(Context, long)}
     */
    @NonNull
    @Override
    public CharSequence getText(@NonNull Context context, long timeNowMillis) {
        return mStr;
    }

    /**
     * See {@link OngoingActivityStatus#getNextChangeTimeMillis(long)}
     */
    @Override
    public long getNextChangeTimeMillis(long fromTimeMillis) {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof TextOngoingActivityStatus
                && mStr.equals(((TextOngoingActivityStatus) o).mStr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStr);
    }
}
