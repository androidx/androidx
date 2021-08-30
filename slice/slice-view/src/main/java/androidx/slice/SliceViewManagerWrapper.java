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

import static androidx.slice.SliceConvert.unwrap;
import static androidx.slice.widget.SliceLiveData.SUPPORTED_SPECS;

import android.annotation.SuppressLint;
import android.app.slice.SliceSpec;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;

import java.util.Collection;
import java.util.Set;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(api = 28)
class SliceViewManagerWrapper extends SliceViewManagerBase {
    private static final String TAG = "SliceViewManagerWrapper"; // exactly 23

    private final ArrayMap<String, Boolean> mCachedSuspendFlags = new ArrayMap<>();
    private final ArrayMap<String, String> mCachedAuthorities = new ArrayMap<>();
    private final android.app.slice.SliceManager mManager;
    private final Set<SliceSpec> mSpecs;

    SliceViewManagerWrapper(Context context) {
        this(context, context.getSystemService(android.app.slice.SliceManager.class));
    }

    SliceViewManagerWrapper(Context context, android.app.slice.SliceManager manager) {
        super(context);
        mManager = manager;
        mSpecs = unwrap(SUPPORTED_SPECS);
    }

    @Override
    public void pinSlice(@NonNull Uri uri) {
        // TODO: When this is fixed in framework, remove this try / catch (b/80118259)
        try {
            mManager.pinSlice(uri, mSpecs);
        } catch (RuntimeException e) {
            // Check if a provider exists for this uri
            ContentResolver resolver = mContext.getContentResolver();
            ContentProviderClient provider = resolver.acquireContentProviderClient(uri);
            if (provider == null) {
                throw new IllegalArgumentException("No provider found for " + uri);
            } else {
                provider.release();
                throw e;
            }
        }
    }

    @Override
    public void unpinSlice(@NonNull Uri uri) {
        try {
            mManager.unpinSlice(uri);
        } catch (IllegalStateException e) {
            // There is no pinned slice with given uri
        }
    }

    @Nullable
    @Override
    public androidx.slice.Slice bindSlice(@NonNull Uri uri) {
        if (isAuthoritySuspended(uri.getAuthority())) {
            return null;
        }
        return SliceConvert.wrap(mManager.bindSlice(uri, mSpecs), mContext);
    }

    @Nullable
    @Override
    public androidx.slice.Slice bindSlice(@NonNull Intent intent) {
        if (isPackageSuspended(intent)) {
            return null;
        }
        return SliceConvert.wrap(mManager.bindSlice(intent, mSpecs), mContext);
    }

    private boolean isPackageSuspended(Intent intent) {
        if (intent.getComponent() != null) {
            return isPackageSuspended(intent.getComponent().getPackageName());
        }
        if (intent.getPackage() != null) {
            return isPackageSuspended(intent.getPackage());
        }
        if (intent.getData() != null) {
            return isAuthoritySuspended(intent.getData().getAuthority());
        }
        return false;
    }

    private boolean isAuthoritySuspended(String authority) {
        String pkg = mCachedAuthorities.get(authority);
        if (pkg == null) {
            ProviderInfo providerInfo = mContext.getPackageManager()
                    .resolveContentProvider(authority, 0);
            if (providerInfo == null) {
                return false;
            }
            pkg = providerInfo.packageName;
            mCachedAuthorities.put(authority, pkg);
        }
        return isPackageSuspended(pkg);
    }

    private boolean isPackageSuspended(String pkg) {
        Boolean isSuspended = mCachedSuspendFlags.get(pkg);
        if (isSuspended == null) {
            try {
                isSuspended = (mContext.getPackageManager().getApplicationInfo(pkg, 0).flags
                        & ApplicationInfo.FLAG_SUSPENDED) != 0;
                mCachedSuspendFlags.put(pkg, isSuspended);
            } catch (NameNotFoundException e) {
                return false;
            }
        }
        return isSuspended;
    }

    @NonNull
    @Override
    @SuppressLint("WrongThread") // TODO https://issuetracker.google.com/issues/116776070
    public Collection<Uri> getSliceDescendants(@NonNull Uri uri) {
        // TODO: When this is fixed in framework, remove this try / catch (b/80118259)
        try {
            return mManager.getSliceDescendants(uri);
        } catch (RuntimeException e) {
            // Check if a provider exists for this uri
            ContentResolver resolver = mContext.getContentResolver();
            ContentProviderClient provider = resolver.acquireContentProviderClient(uri);
            if (provider == null) {
                throw new IllegalArgumentException("No provider found for " + uri);
            } else {
                provider.release();
                throw e;
            }
        }
    }

    @Nullable
    @Override
    public Uri mapIntentToUri(@NonNull Intent intent) {
        return mManager.mapIntentToUri(intent);
    }
}
