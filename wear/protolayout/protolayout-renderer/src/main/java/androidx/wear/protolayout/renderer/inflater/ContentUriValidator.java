/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

class ContentUriValidator {
    @NonNull final Context mAppContext;
    @NonNull private final PackageManager mPackageManager;
    @NonNull private final String mAllowedPackageName;
    @NonNull private final UriPermissionValidator mUriPermissionValidator;

    public ContentUriValidator(@NonNull Context appContext, @NonNull String allowedPackageName) {
        this.mAppContext = appContext;
        this.mPackageManager = appContext.getPackageManager();
        this.mAllowedPackageName = allowedPackageName;
        this.mUriPermissionValidator = new UriPermissionValidator();
    }

    @VisibleForTesting
    ContentUriValidator(
            @NonNull Context appContext,
            @NonNull String allowedPackageName,
            @NonNull UriPermissionValidator uriPermissionValidator) {
        this.mAppContext = appContext;
        this.mPackageManager = appContext.getPackageManager();
        this.mAllowedPackageName = allowedPackageName;
        this.mUriPermissionValidator = uriPermissionValidator;
    }

    /**
     * Validate that a content URI can be used by the package name passed to this validator.
     *
     * <p>This will check multiple things: the content provider for the given authority must:
     *
     * <ul>
     *   <li>Exist and be resolvable.
     *   <li>Be exported.
     *   <li>Belong to the same app as the package name passed to this validator's constructor.
     *   <li>Have explicitly granted us read permission to its content URI (using {@link
     *       Context#grantUriPermission})
     * </ul>
     */
    @SuppressWarnings("deprecation")
    // PackageManager#resolveContentProvider(String,int) is deprecated. Though, we can't use the
    // replacement method as it was introduced in API level 33.
    public boolean validateUri(@NonNull Uri uri) {
        // First ensure that it's a content:// URI
        String scheme = uri.getScheme();
        String authority = uri.getAuthority();

        if (scheme == null || !scheme.equals("content")) {
            return false;
        }

        if (authority == null) {
            return false;
        }

        // Ensure that the authority belongs to the allowed package.
        ProviderInfo providerInfo =
                mPackageManager.resolveContentProvider(authority, /* flags= */ 0);

        if (providerInfo == null) {
            // Android does support authorities of the form <user_id>@authority. If so, try querying
            // for that one.
            int authorityIndex = authority.lastIndexOf("@");
            if (authorityIndex != -1) {
                String actualAuthority = authority.substring(authorityIndex + 1);
                providerInfo =
                        mPackageManager.resolveContentProvider(actualAuthority, /* flags= */ 0);
            }

            if (providerInfo == null) {
                return false;
            }
        }

        // Provider must be exported.
        if (!providerInfo.exported) {
            return false;
        }

        if (!mUriPermissionValidator.canAccessUri(uri)) {
            return false;
        }

        // Otherwise, only allow content from the same package that provided the tile.
        return providerInfo.packageName.equals(mAllowedPackageName);
    }

    /**
     * Utility class to check that this process has access to a given URI.
     *
     * <p>This only exists for testing; Robolectric doesn't provider a way to fake out
     * checkUriPermission, and stubbing out Context is generally frowned upon.
     */
    class UriPermissionValidator {
        public boolean canAccessUri(@NonNull Uri uri) {
            int pid = Process.myPid();
            int uid = Process.myUid();

            // Ensure that this process has been granted access to the content URI, *explicitly*.
            return mAppContext.checkUriPermission(
                            uri, pid, uid, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }
}
