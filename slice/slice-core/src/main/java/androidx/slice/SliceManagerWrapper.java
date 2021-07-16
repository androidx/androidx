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
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.content.PermissionChecker;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(api = 28)
class SliceManagerWrapper extends SliceManager {

    private final android.app.slice.SliceManager mManager;

    SliceManagerWrapper(Context context) {
        this(context.getSystemService(android.app.slice.SliceManager.class));
    }

    SliceManagerWrapper(android.app.slice.SliceManager manager) {
        mManager = manager;
    }

    @Override
    public @NonNull Set<androidx.slice.SliceSpec> getPinnedSpecs(@NonNull Uri uri) {
        if (Build.VERSION.SDK_INT == 28) {
            uri = maybeAddCurrentUserId(uri);
        }
        return SliceConvert.wrap(mManager.getPinnedSpecs(uri));
    }

    @Override
    @PermissionChecker.PermissionResult
    public int checkSlicePermission(@NonNull Uri uri, int pid, int uid) {
        return mManager.checkSlicePermission(uri, pid, uid);
    }

    @Override
    public void grantSlicePermission(@NonNull String toPackage, @NonNull Uri uri) {
        mManager.grantSlicePermission(toPackage, uri);
    }

    @Override
    public void revokeSlicePermission(@NonNull String toPackage, @NonNull Uri uri) {
        mManager.revokeSlicePermission(toPackage, uri);
    }

    @Override
    public List<Uri> getPinnedSlices() {
        return mManager.getPinnedSlices();
    }

    private Uri maybeAddCurrentUserId(Uri uri) {
        if (uri == null || uri.getAuthority().contains("@")) {
            return uri;
        }
        String auth = uri.getAuthority();
        return uri.buildUpon()
                .encodedAuthority(getCurrentUserId() + "@" + auth)
                .build();
    }

    private int getCurrentUserId() {
        UserHandle h = Process.myUserHandle();
        try {
            return (int) h.getClass().getDeclaredMethod("getIdentifier").invoke(h);
        } catch (IllegalAccessException e) {
            return 0;
        } catch (InvocationTargetException e) {
            return 0;
        } catch (NoSuchMethodException e) {
            return 0;
        }
    }
}
