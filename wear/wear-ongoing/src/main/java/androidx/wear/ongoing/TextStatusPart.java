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

package androidx.wear.ongoing;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * Implementation and internal representation of {@link Status.TextPart}.
 * <p>
 * Available since wear-ongoing:1.0.0
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@VersionedParcelize
class TextStatusPart extends StatusPart {
    @NonNull
    @ParcelField(value = 1, defaultValue = "")
    String mStr = "";

    // Required by VersionedParcelable
    TextStatusPart() {
    }

    TextStatusPart(@NonNull String str) {
        this.mStr = str;
    }

    /**
     * See {@link TimeDependentText#getText(Context, long)}
     */
    @NonNull
    @Override
    public CharSequence getText(@NonNull Context context, long timeNowMillis) {
        return mStr;
    }

    /**
     * See {@link TimeDependentText#getNextChangeTimeMillis(long)}
     */
    @Override
    public long getNextChangeTimeMillis(long fromTimeMillis) {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TextStatusPart
                && mStr.equals(((TextStatusPart) o).mStr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStr);
    }
}
