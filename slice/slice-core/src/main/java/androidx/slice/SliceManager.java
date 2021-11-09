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

package androidx.slice;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.content.PermissionChecker;

import java.util.List;
import java.util.Set;

/**
 * Class to handle interactions with {@link Slice}s.
 * <p>
 * The SliceViewManager manages permissions and pinned state for slices.
 */
@RequiresApi(19)
public abstract class SliceManager {

    /**
     * Get a {@link SliceManager}.
     */
    public static @NonNull SliceManager getInstance(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 28) {
            return new SliceManagerWrapper(context);
        } else {
            return new SliceManagerCompat(context);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    SliceManager() {
    }

    /**
     * Get the current set of specs for a pinned slice.
     * <p>
     * This is the set of specs supported for a specific pinned slice. It will take
     * into account all clients and returns only specs supported by all.
     * @see SliceSpec
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract @NonNull Set<SliceSpec> getPinnedSpecs(@NonNull Uri uri);

    /**
     * Determine whether a particular process and user ID has been granted
     * permission to access a specific slice URI.
     *
     * @param uri The uri that is being checked.
     * @param pid The process ID being checked against.  Must be &gt; 0.
     * @param uid The user ID being checked against.  A uid of 0 is the root
     * user, which will pass every permission check.
     *
     * @return {@link PackageManager#PERMISSION_GRANTED} if the given
     * pid/uid is allowed to access that uri, or
     * {@link PackageManager#PERMISSION_DENIED} if it is not.
     *
     * @see #grantSlicePermission(String, Uri)
     */
    @PermissionChecker.PermissionResult
    public abstract int checkSlicePermission(@NonNull Uri uri, int pid, int uid);

    /**
     * Grant permission to access a specific slice Uri to another package.
     *
     * @param toPackage The package you would like to allow to access the Uri.
     * @param uri The Uri you would like to grant access to.
     *
     * @see #revokeSlicePermission
     */
    public abstract void grantSlicePermission(@NonNull String toPackage, @NonNull Uri uri);

    /**
     * Remove permissions to access a particular content provider Uri
     * that were previously added with {@link #grantSlicePermission} for a specific target
     * package.  The given Uri will match all previously granted Uris that are the same or a
     * sub-path of the given Uri.  That is, revoking "content://foo/target" will
     * revoke both "content://foo/target" and "content://foo/target/sub", but not
     * "content://foo".  It will not remove any prefix grants that exist at a
     * higher level.
     *
     * @param toPackage The package you would like to allow to access the Uri.
     * @param uri The Uri you would like to revoke access to.
     *
     * @see #grantSlicePermission
     */
    public abstract void revokeSlicePermission(@NonNull String toPackage, @NonNull Uri uri);

    /**
     * Get the list of currently pinned slices for this app.
     * @see SliceProvider#onSlicePinned
     */
    public abstract @NonNull List<Uri> getPinnedSlices();
}
