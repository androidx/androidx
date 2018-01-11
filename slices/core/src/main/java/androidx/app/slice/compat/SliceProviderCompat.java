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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceProvider;
import androidx.app.slice.SliceSpec;

/**
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public class SliceProviderCompat extends ContentProvider {

    private static final String TAG = "SliceProvider";

    public static final String EXTRA_BIND_URI = "slice_uri";
    public static final String METHOD_SLICE = "bind_slice";
    public static final String METHOD_MAP_INTENT = "map_slice";
    public static final String METHOD_PIN = "pin_slice";
    public static final String METHOD_UNPIN = "unpin_slice";
    public static final String METHOD_GET_PINNED_SPECS = "get_specs";

    public static final String EXTRA_INTENT = "slice_intent";
    public static final String EXTRA_SLICE = "slice";
    public static final String EXTRA_SUPPORTED_SPECS = "specs";
    public static final String EXTRA_SUPPORTED_SPECS_REVS = "revs";
    private static final String EXTRA_PKG = "pkg";
    private static final String DATA_PREFIX = "slice_data_";

    private static final boolean DEBUG = false;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SliceProvider mSliceProvider;
    private CompatPinnedList mPinnedList;

    public SliceProviderCompat(SliceProvider provider) {
        mSliceProvider = provider;
    }

    @Override
    public boolean onCreate() {
        mPinnedList = new CompatPinnedList(getContext(),
                DATA_PREFIX + mSliceProvider.getClass().getName());
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
            List<SliceSpec> specs = getSpecs(extras);

            Slice s = handleBindSlice(uri, specs);
            Bundle b = new Bundle();
            b.putParcelable(EXTRA_SLICE, s.toBundle());
            return b;
        } else if (method.equals(METHOD_MAP_INTENT)) {
            if (Binder.getCallingUid() != Process.myUid()) {
                getContext().enforceCallingPermission(permission.BIND_SLICE,
                        "Slice binding requires the permission BIND_SLICE");
            }
            Intent intent = extras.getParcelable(EXTRA_INTENT);
            Uri uri = mSliceProvider.onMapIntentToUri(intent);
            Bundle b = new Bundle();
            if (uri != null) {
                List<SliceSpec> specs = getSpecs(extras);
                Slice s = handleBindSlice(uri, specs);
                b.putParcelable(EXTRA_SLICE, s.toBundle());
            } else {
                b.putParcelable(EXTRA_SLICE, null);
            }
            return b;
        } else if (method.equals(METHOD_PIN)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            List<SliceSpec> specs = getSpecs(extras);
            String pkg = extras.getString(EXTRA_PKG);
            if (mPinnedList.addPin(uri, pkg, specs)) {
                handleSlicePinned(uri);
            }
            return null;
        } else if (method.equals(METHOD_UNPIN)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            String pkg = extras.getString(EXTRA_PKG);
            if (mPinnedList.removePin(uri, pkg)) {
                handleSliceUnpinned(uri);
            }
            return null;
        } else if (method.equals(METHOD_GET_PINNED_SPECS)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            Bundle b = new Bundle();
            addSpecs(b, mPinnedList.getSpecs(uri));
            return b;
        }
        return super.call(method, arg, extras);
    }

    private void handleSlicePinned(final Uri sliceUri) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mSliceProvider.onSlicePinned(sliceUri);
        } else {
            final CountDownLatch latch = new CountDownLatch(1);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mSliceProvider.onSlicePinned(sliceUri);
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleSliceUnpinned(final Uri sliceUri) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mSliceProvider.onSliceUnpinned(sliceUri);
        } else {
            final CountDownLatch latch = new CountDownLatch(1);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mSliceProvider.onSliceUnpinned(sliceUri);
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Slice handleBindSlice(final Uri sliceUri, final List<SliceSpec> specs) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return onBindSliceStrict(sliceUri, specs);
        } else {
            final CountDownLatch latch = new CountDownLatch(1);
            final Slice[] output = new Slice[1];
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    output[0] = onBindSliceStrict(sliceUri, specs);
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

    private Slice onBindSliceStrict(Uri sliceUri, List<SliceSpec> specs) {
        ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        try {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyDeath()
                    .build());
            SliceProvider.setSpecs(specs);
            try {
                return mSliceProvider.onBindSlice(sliceUri);
            } finally {
                SliceProvider.setSpecs(null);
            }
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    /**
     * Compat version of {@link Slice#bindSlice}.
     */
    public static Slice bindSlice(Context context, Uri uri,
            List<SliceSpec> supportedSpecs) {
        ContentProviderClient provider = context.getContentResolver()
                .acquireContentProviderClient(uri);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            addSpecs(extras, supportedSpecs);
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

    private static void addSpecs(Bundle extras, List<SliceSpec> supportedSpecs) {
        ArrayList<String> types = new ArrayList<>();
        ArrayList<Integer> revs = new ArrayList<>();
        for (SliceSpec spec : supportedSpecs) {
            types.add(spec.getType());
            revs.add(spec.getRevision());
        }
        extras.putStringArrayList(EXTRA_SUPPORTED_SPECS, types);
        extras.putIntegerArrayList(EXTRA_SUPPORTED_SPECS_REVS, revs);
    }

    private static List<SliceSpec> getSpecs(Bundle extras) {
        ArrayList<SliceSpec> specs = new ArrayList<>();
        ArrayList<String> types = extras.getStringArrayList(EXTRA_SUPPORTED_SPECS);
        ArrayList<Integer> revs = extras.getIntegerArrayList(EXTRA_SUPPORTED_SPECS_REVS);
        for (int i = 0; i < types.size(); i++) {
            specs.add(new SliceSpec(types.get(i), revs.get(i)));
        }
        return specs;
    }

    /**
     * Compat version of {@link Slice#bindSlice}.
     */
    public static Slice bindSlice(Context context, Intent intent,
            List<SliceSpec> supportedSpecs) {
        ContentResolver resolver = context.getContentResolver();

        // Check if the intent has data for the slice uri on it and use that
        final Uri intentData = intent.getData();
        if (intentData != null && SLICE_TYPE.equals(resolver.getType(intentData))) {
            return bindSlice(context, intentData, supportedSpecs);
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
            addSpecs(extras, supportedSpecs);
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

    /**
     * Compat version of {@link android.app.slice.SliceManager#pinSlice}.
     */
    public static void pinSlice(Context context, Uri uri,
            List<SliceSpec> supportedSpecs) {
        ContentProviderClient provider = context.getContentResolver()
                .acquireContentProviderClient(uri);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PKG, context.getPackageName());
            addSpecs(extras, supportedSpecs);
            provider.call(METHOD_PIN, null, extras);
        } catch (RemoteException e) {
            // Arbitrary and not worth documenting, as Activity
            // Manager will kill this process shortly anyway.
        } finally {
            provider.close();
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#unpinSlice}.
     */
    public static void unpinSlice(Context context, Uri uri,
            List<SliceSpec> supportedSpecs) {
        ContentProviderClient provider = context.getContentResolver()
                .acquireContentProviderClient(uri);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PKG, context.getPackageName());
            addSpecs(extras, supportedSpecs);
            provider.call(METHOD_UNPIN, null, extras);
        } catch (RemoteException e) {
            // Arbitrary and not worth documenting, as Activity
            // Manager will kill this process shortly anyway.
        } finally {
            provider.close();
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#getPinnedSpecs(Uri)}.
     */
    public static List<SliceSpec> getPinnedSpecs(Context context, Uri uri) {
        ContentProviderClient provider = context.getContentResolver()
                .acquireContentProviderClient(uri);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            final Bundle res = provider.call(METHOD_GET_PINNED_SPECS, null, extras);
            if (res == null) {
                return null;
            }
            return getSpecs(res);
        } catch (RemoteException e) {
            // Arbitrary and not worth documenting, as Activity
            // Manager will kill this process shortly anyway.
            return null;
        } finally {
            provider.close();
        }
    }
}
