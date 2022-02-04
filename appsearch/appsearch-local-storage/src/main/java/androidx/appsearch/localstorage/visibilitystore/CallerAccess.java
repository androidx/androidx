/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.localstorage.visibilitystore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

/**
 * Contains attributes of an API caller relevant to its access via visibility store.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CallerAccess {
    private final String mCallingPackageName;
    private final int mCallingUid;
    private final boolean mCallerHasSystemAccess;

    /**
     * Constructs a new {@link CallerAccess}.
     *
     * @param callingPackageName    The name of the package which wants to access data.
     * @param callingUid            The uid of the package which wants to access data.
     * @param callerHasSystemAccess Whether {@code callingPackageName} has access to schema types
     *                              marked visible to system via
     *       {@link androidx.appsearch.app.SetSchemaRequest.Builder#setSchemaTypeDisplayedBySystem}.
     */
    public CallerAccess(
            @NonNull String callingPackageName, int callingUid, boolean callerHasSystemAccess) {
        mCallingPackageName = Preconditions.checkNotNull(callingPackageName);
        mCallingUid = callingUid;
        mCallerHasSystemAccess = callerHasSystemAccess;
    }

    /** Returns the name of the package which wants to access data. */
    @NonNull
    public String getCallingPackageName() {
        return mCallingPackageName;
    }

    /** Returns the uid of the package which wants to access data. */
    public int getCallingUid() {
        return mCallingUid;
    }

    /**
     * Returns whether {@code callingPackageName} has access to schema types marked visible to
     * system via
     * {@link androidx.appsearch.app.SetSchemaRequest.Builder#setSchemaTypeDisplayedBySystem}.
     */
    public boolean doesCallerHaveSystemAccess() {
        return mCallerHasSystemAccess;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof CallerAccess)) return false;
        CallerAccess that = (CallerAccess) o;
        return mCallingUid == that.mCallingUid
                && mCallerHasSystemAccess == that.mCallerHasSystemAccess
                && mCallingPackageName.equals(that.mCallingPackageName);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mCallingPackageName, mCallingUid, mCallerHasSystemAccess);
    }
}
