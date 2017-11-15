/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.app.slice.compat;

import static android.app.slice.SliceProvider.SLICE_TYPE;

import android.Manifest.permission;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceProvider;

/**
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public class SliceProviderCompat extends ContentProvider {

    private static final String TAG = "SliceProvider";

    public static final String EXTRA_BIND_URI = "slice_uri";
    public static final String METHOD_SLICE = "bind_slice";
    public static final String METHOD_MAP_INTENT = "map_slice";
    public static final String EXTRA_INTENT = "slice_intent";
    public static final String EXTRA_SLICE = "slice";

    private static final boolean DEBUG = false;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SliceProvider mSliceProvider;

    public SliceProviderCompat(SliceProvider provider) {
        mSliceProvider = provider;
    }

    @Override
    public boolean onCreate() {
        return mSliceProvider.onCreateSliceProvider();
    }

    @Override
    public final int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        if (DEBUG) Log.d(TAG, "update " + uri);
        return 0;
    }

    @Override
    public final int delete(Uri uri, String selection, String[] selectionArgs) {
        if (DEBUG) Log.d(TAG, "delete " + uri);
        return 0;
    }

    @Override
    public final Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        if (DEBUG) Log.d(TAG, "query " + uri);
        return null;
    }

    @Override
    public final Cursor query(Uri uri, String[] projection, String selection, String[]
            selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        if (DEBUG) Log.d(TAG, "query " + uri);
        return null;
    }

    @Override
    public final Cursor query(Uri uri, String[] projection, Bundle queryArgs,
            CancellationSignal cancellationSignal) {
        if (DEBUG) Log.d(TAG, "query " + uri);
        return null;
    }

    @Override
    public final Uri insert(Uri uri, ContentValues values) {
        if (DEBUG) Log.d(TAG, "insert " + uri);
        return null;
    }

    @Override
    public final String getType(Uri uri) {
        if (DEBUG) Log.d(TAG, "getFormat " + uri);
        return SLICE_TYPE;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (method.equals(METHOD_SLICE)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            if (Binder.getCallingUid() != Process.myUid()) {
                getContext().enforceUriPermission(uri, permission.BIND_SLICE,
                        permission.BIND_SLICE, Binder.getCallingPid(), Binder.getCallingUid(),
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        "Slice binding requires the permission BIND_SLICE");
            }

            Slice s = handleBindSlice(uri);
            Bundle b = new Bundle();
            b.putParcelable(EXTRA_SLICE, s.toBundle());
            return b;
        } else if (method.equals(METHOD_MAP_INTENT)) {
            getContext().enforceCallingPermission(permission.BIND_SLICE,
                    "Slice binding requires the permission BIND_SLICE");
            Intent intent = extras.getParcelable(EXTRA_INTENT);
            Uri uri = mSliceProvider.onMapIntentToUri(intent);
            Bundle b = new Bundle();
            if (uri != null) {
                Slice s = handleBindSlice(uri);
                b.putParcelable(EXTRA_SLICE, s.toBundle());
            } else {
                b.putParcelable(EXTRA_SLICE, null);
            }
            return b;
        }
        return super.call(method, arg, extras);
    }

    private Slice handleBindSlice(final Uri sliceUri) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return onBindSliceStrict(sliceUri);
        } else {
            final CountDownLatch latch = new CountDownLatch(1);
            final Slice[] output = new Slice[1];
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    output[0] = onBindSliceStrict(sliceUri);
                    latch.countDown();
                }
            });
            try {
                latch.await();
                return output[0];
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Slice onBindSliceStrict(Uri sliceUri) {
        ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        try {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyDeath()
                    .build());
            return mSliceProvider.onBindSlice(sliceUri);
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    /**
     * Compat version of {@link Slice#bindSlice(Context, Uri)}.
     */
    public static Slice bindSlice(Context context, Uri uri) {
        ContentProviderClient provider = context.getContentResolver()
                .acquireContentProviderClient(uri);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            final Bundle res = provider.call(METHOD_SLICE, null, extras);
            if (res == null) {
                return null;
            }
            Parcelable bundle = res.getParcelable(EXTRA_SLICE);
            if (!(bundle instanceof Bundle)) {
                return null;
            }
            return new Slice((Bundle) bundle);
        } catch (RemoteException e) {
            // Arbitrary and not worth documenting, as Activity
            // Manager will kill this process shortly anyway.
            return null;
        } finally {
            provider.close();
        }
    }

    /**
     * Compat version of {@link Slice#bindSlice(Context, Intent)}.
     */
    public static Slice bindSlice(Context context, Intent intent) {
        ContentResolver resolver = context.getContentResolver();

        // Check if the intent has data for the slice uri on it and use that
        final Uri intentData = intent.getData();
        if (intentData != null && SLICE_TYPE.equals(resolver.getType(intentData))) {
            return bindSlice(context, intentData);
        }
        // Otherwise ask the app
        List<ResolveInfo> providers =
                context.getPackageManager().queryIntentContentProviders(intent, 0);
        if (providers == null) {
            throw new IllegalArgumentException("Unable to resolve intent " + intent);
        }
        String authority = providers.get(0).providerInfo.authority;
        Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority).build();
        ContentProviderClient provider = resolver.acquireContentProviderClient(uri);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_INTENT, intent);
            final Bundle res = provider.call(METHOD_MAP_INTENT, null, extras);
            if (res == null) {
                return null;
            }
            Parcelable bundle = res.getParcelable(EXTRA_SLICE);
            if (!(bundle instanceof Bundle)) {
                return null;
            }
            return new Slice((Bundle) bundle);
        } catch (RemoteException e) {
            // Arbitrary and not worth documenting, as Activity
            // Manager will kill this process shortly anyway.
            return null;
        } finally {
            provider.close();
        }
    }
}
