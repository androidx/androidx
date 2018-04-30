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
package androidx.slice.compat;

import static android.app.slice.SliceManager.CATEGORY_SLICE;
import static android.app.slice.SliceManager.SLICE_METADATA_KEY;
import static android.app.slice.SliceProvider.SLICE_TYPE;

import static androidx.core.content.PermissionChecker.PERMISSION_DENIED;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.SliceSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public class SliceProviderCompat {
    public static final String PERMS_PREFIX = "slice_perms_";
    private static final String TAG = "SliceProviderCompat";
    private static final String DATA_PREFIX = "slice_data_";
    private static final String ALL_FILES = DATA_PREFIX + "all_slice_files";

    private static final long SLICE_BIND_ANR = 2000;

    public static final String METHOD_SLICE = "bind_slice";
    public static final String METHOD_MAP_INTENT = "map_slice";
    public static final String METHOD_PIN = "pin_slice";
    public static final String METHOD_UNPIN = "unpin_slice";
    public static final String METHOD_GET_PINNED_SPECS = "get_specs";
    public static final String METHOD_MAP_ONLY_INTENT = "map_only";
    public static final String METHOD_GET_DESCENDANTS = "get_descendants";
    public static final String METHOD_CHECK_PERMISSION = "check_perms";
    public static final String METHOD_GRANT_PERMISSION = "grant_perms";
    public static final String METHOD_REVOKE_PERMISSION = "revoke_perms";

    public static final String EXTRA_BIND_URI = "slice_uri";
    public static final String EXTRA_INTENT = "slice_intent";
    public static final String EXTRA_SLICE = "slice";
    public static final String EXTRA_SUPPORTED_SPECS = "specs";
    public static final String EXTRA_SUPPORTED_SPECS_REVS = "revs";
    public static final String EXTRA_PKG = "pkg";
    public static final String EXTRA_PROVIDER_PKG = "provider_pkg";
    public static final String EXTRA_SLICE_DESCENDANTS = "slice_descendants";
    public static final String EXTRA_UID = "uid";
    public static final String EXTRA_PID = "pid";
    public static final String EXTRA_RESULT = "result";

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Context mContext;

    private String mCallback;
    private final SliceProvider mProvider;
    private CompatPinnedList mPinnedList;
    private CompatPermissionManager mPermissionManager;

    public SliceProviderCompat(SliceProvider provider, CompatPermissionManager permissionManager,
            Context context) {
        mProvider = provider;
        mContext = context;
        String prefsFile = DATA_PREFIX + getClass().getName();
        SharedPreferences allFiles = mContext.getSharedPreferences(ALL_FILES, 0);
        Set<String> files = allFiles.getStringSet(ALL_FILES, Collections.<String>emptySet());
        if (!files.contains(prefsFile)) {
            // Make sure this is editable.
            files = new ArraySet<>(files);
            files.add(prefsFile);
            allFiles.edit()
                    .putStringSet(ALL_FILES, files)
                    .commit();
        }
        mPinnedList = new CompatPinnedList(mContext, prefsFile);
        mPermissionManager = permissionManager;
    }

    private Context getContext() {
        return mContext;
    }

    public String getCallingPackage() {
        return mProvider.getCallingPackage();
    }

    /**
     * Called by SliceProvider when compat is needed.
     */
    public Bundle call(String method, String arg, Bundle extras) {
        if (method.equals(METHOD_SLICE)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            Set<SliceSpec> specs = getSpecs(extras);

            Slice s = handleBindSlice(uri, specs, getCallingPackage());
            Bundle b = new Bundle();
            b.putParcelable(EXTRA_SLICE, s != null ? s.toBundle() : null);
            return b;
        } else if (method.equals(METHOD_MAP_INTENT)) {
            Intent intent = extras.getParcelable(EXTRA_INTENT);
            Uri uri = mProvider.onMapIntentToUri(intent);
            Bundle b = new Bundle();
            if (uri != null) {
                Set<SliceSpec> specs = getSpecs(extras);
                Slice s = handleBindSlice(uri, specs, getCallingPackage());
                b.putParcelable(EXTRA_SLICE, s != null ? s.toBundle() : null);
            } else {
                b.putParcelable(EXTRA_SLICE, null);
            }
            return b;
        } else if (method.equals(METHOD_MAP_ONLY_INTENT)) {
            Intent intent = extras.getParcelable(EXTRA_INTENT);
            Uri uri = mProvider.onMapIntentToUri(intent);
            Bundle b = new Bundle();
            b.putParcelable(EXTRA_SLICE, uri);
            return b;
        } else if (method.equals(METHOD_PIN)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            Set<SliceSpec> specs = getSpecs(extras);
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
        } else if (method.equals(METHOD_GET_DESCENDANTS)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            Bundle b = new Bundle();
            b.putParcelableArrayList(EXTRA_SLICE_DESCENDANTS,
                    new ArrayList<>(handleGetDescendants(uri)));
            return b;
        } else if (method.equals(METHOD_CHECK_PERMISSION)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            String pkg = extras.getString(EXTRA_PKG);
            int pid = extras.getInt(EXTRA_PID);
            int uid = extras.getInt(EXTRA_UID);
            Bundle b = new Bundle();
            b.putInt(EXTRA_RESULT, mPermissionManager.checkSlicePermission(uri, pid, uid));
            return b;
        } else if (method.equals(METHOD_GRANT_PERMISSION)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            String toPkg = extras.getString(EXTRA_PKG);
            if (Binder.getCallingUid() != Process.myUid()) {
                throw new SecurityException("Only the owning process can manage slice permissions");
            }
            mPermissionManager.grantSlicePermission(uri, toPkg);
        } else if (method.equals(METHOD_REVOKE_PERMISSION)) {
            Uri uri = extras.getParcelable(EXTRA_BIND_URI);
            String toPkg = extras.getString(EXTRA_PKG);
            if (Binder.getCallingUid() != Process.myUid()) {
                throw new SecurityException("Only the owning process can manage slice permissions");
            }
            mPermissionManager.revokeSlicePermission(uri, toPkg);
        }
        return null;
    }

    private Collection<Uri> handleGetDescendants(Uri uri) {
        mCallback = "onGetSliceDescendants";
        mHandler.postDelayed(mAnr, SLICE_BIND_ANR);
        try {
            return mProvider.onGetSliceDescendants(uri);
        } finally {
            mHandler.removeCallbacks(mAnr);
        }
    }

    private void handleSlicePinned(final Uri sliceUri) {
        mCallback = "onSlicePinned";
        mHandler.postDelayed(mAnr, SLICE_BIND_ANR);
        try {
            mProvider.onSlicePinned(sliceUri);
        } finally {
            mHandler.removeCallbacks(mAnr);
        }
    }

    private void handleSliceUnpinned(final Uri sliceUri) {
        mCallback = "onSliceUnpinned";
        mHandler.postDelayed(mAnr, SLICE_BIND_ANR);
        try {
            mProvider.onSliceUnpinned(sliceUri);
        } finally {
            mHandler.removeCallbacks(mAnr);
        }
    }

    private Slice handleBindSlice(final Uri sliceUri, final Set<SliceSpec> specs,
            final String callingPkg) {
        // This can be removed once Slice#bindSlice is removed and everyone is using
        // SliceManager#bindSlice.
        String pkg = callingPkg != null ? callingPkg
                : getContext().getPackageManager().getNameForUid(Binder.getCallingUid());
        if (mPermissionManager.checkSlicePermission(sliceUri, Binder.getCallingPid(),
                Binder.getCallingUid()) != PERMISSION_GRANTED) {
            return mProvider.createPermissionSlice(getContext(), sliceUri, pkg);
        }
        return onBindSliceStrict(sliceUri, specs);
    }

    private Slice onBindSliceStrict(Uri sliceUri, Set<SliceSpec> specs) {
        StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        mCallback = "onBindSlice";
        mHandler.postDelayed(mAnr, SLICE_BIND_ANR);
        try {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyDeath()
                    .build());
            SliceProvider.setSpecs(specs);
            try {
                return mProvider.onBindSlice(sliceUri);
            } finally {
                SliceProvider.setSpecs(null);
                mHandler.removeCallbacks(mAnr);
            }
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private final Runnable mAnr = new Runnable() {
        @Override
        public void run() {
            Process.sendSignal(Process.myPid(), Process.SIGNAL_QUIT);
            Log.wtf(TAG, "Timed out while handling slice callback " + mCallback);
        }
    };

    /**
     * Compat version of {@link Slice#bindSlice}.
     */
    public static Slice bindSlice(Context context, Uri uri,
            Set<SliceSpec> supportedSpecs) {
        ProviderHolder holder = acquireClient(context.getContentResolver(), uri);
        if (holder.mProvider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            addSpecs(extras, supportedSpecs);
            final Bundle res = holder.mProvider.call(METHOD_SLICE, null, extras);
            if (res == null) {
                return null;
            }
            Parcelable bundle = res.getParcelable(EXTRA_SLICE);
            if (!(bundle instanceof Bundle)) {
                return null;
            }
            return new Slice((Bundle) bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to bind slice", e);
            return null;
        } finally {
        }
    }

    /**
     * Compat way to push specs through the call.
     */
    public static void addSpecs(Bundle extras, Set<SliceSpec> supportedSpecs) {
        ArrayList<String> types = new ArrayList<>();
        ArrayList<Integer> revs = new ArrayList<>();
        for (SliceSpec spec : supportedSpecs) {
            types.add(spec.getType());
            revs.add(spec.getRevision());
        }
        extras.putStringArrayList(EXTRA_SUPPORTED_SPECS, types);
        extras.putIntegerArrayList(EXTRA_SUPPORTED_SPECS_REVS, revs);
    }

    /**
     * Compat way to push specs through the call.
     */
    public static Set<SliceSpec> getSpecs(Bundle extras) {
        ArraySet<SliceSpec> specs = new ArraySet<>();
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
            Set<SliceSpec> supportedSpecs) {
        Preconditions.checkNotNull(intent, "intent");
        Preconditions.checkArgument(intent.getComponent() != null || intent.getPackage() != null
                || intent.getData() != null,
                String.format("Slice intent must be explicit %s", intent));
        ContentResolver resolver = context.getContentResolver();

        // Check if the intent has data for the slice uri on it and use that
        final Uri intentData = intent.getData();
        if (intentData != null && SLICE_TYPE.equals(resolver.getType(intentData))) {
            return bindSlice(context, intentData, supportedSpecs);
        }
        // Otherwise ask the app
        Intent queryIntent = new Intent(intent);
        if (!queryIntent.hasCategory(CATEGORY_SLICE)) {
            queryIntent.addCategory(CATEGORY_SLICE);
        }
        List<ResolveInfo> providers =
                context.getPackageManager().queryIntentContentProviders(queryIntent, 0);
        if (providers == null || providers.isEmpty()) {
            // There are no providers, see if this activity has a direct link.
            ResolveInfo resolve = context.getPackageManager().resolveActivity(intent,
                    PackageManager.GET_META_DATA);
            if (resolve != null && resolve.activityInfo != null
                    && resolve.activityInfo.metaData != null
                    && resolve.activityInfo.metaData.containsKey(SLICE_METADATA_KEY)) {
                return bindSlice(context, Uri.parse(
                        resolve.activityInfo.metaData.getString(SLICE_METADATA_KEY)),
                        supportedSpecs);
            }
            return null;
        }
        String authority = providers.get(0).providerInfo.authority;
        Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority).build();
        ProviderHolder holder = acquireClient(resolver, uri);
        if (holder.mProvider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_INTENT, intent);
            addSpecs(extras, supportedSpecs);
            final Bundle res = holder.mProvider.call(METHOD_MAP_INTENT, null, extras);
            if (res == null) {
                return null;
            }
            Parcelable bundle = res.getParcelable(EXTRA_SLICE);
            if (!(bundle instanceof Bundle)) {
                return null;
            }
            return new Slice((Bundle) bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to bind slice", e);
            return null;
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#pinSlice}.
     */
    public static void pinSlice(Context context, Uri uri,
            Set<SliceSpec> supportedSpecs) {
        ProviderHolder holder = acquireClient(context.getContentResolver(), uri);
        if (holder.mProvider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PKG, context.getPackageName());
            addSpecs(extras, supportedSpecs);
            holder.mProvider.call(METHOD_PIN, null, extras);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to pin slice", e);
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#unpinSlice}.
     */
    public static void unpinSlice(Context context, Uri uri,
            Set<SliceSpec> supportedSpecs) {
        ProviderHolder holder = acquireClient(context.getContentResolver(), uri);
        if (holder.mProvider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PKG, context.getPackageName());
            addSpecs(extras, supportedSpecs);
            holder.mProvider.call(METHOD_UNPIN, null, extras);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to unpin slice", e);
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#getPinnedSpecs(Uri)}.
     */
    public static Set<SliceSpec> getPinnedSpecs(Context context, Uri uri) {
        ProviderHolder holder = acquireClient(context.getContentResolver(), uri);
        if (holder.mProvider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            final Bundle res = holder.mProvider.call(METHOD_GET_PINNED_SPECS, null, extras);
            if (res != null) {
                return getSpecs(res);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get pinned specs", e);
        }
        return null;
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#mapIntentToUri}.
     */
    public static Uri mapIntentToUri(Context context, Intent intent) {
        Preconditions.checkNotNull(intent, "intent");
        Preconditions.checkArgument(intent.getComponent() != null || intent.getPackage() != null
                || intent.getData() != null,
                String.format("Slice intent must be explicit %s", intent));
        ContentResolver resolver = context.getContentResolver();

        // Check if the intent has data for the slice uri on it and use that
        final Uri intentData = intent.getData();
        if (intentData != null && SLICE_TYPE.equals(resolver.getType(intentData))) {
            return intentData;
        }
        // Otherwise ask the app
        Intent queryIntent = new Intent(intent);
        if (!queryIntent.hasCategory(CATEGORY_SLICE)) {
            queryIntent.addCategory(CATEGORY_SLICE);
        }
        List<ResolveInfo> providers =
                context.getPackageManager().queryIntentContentProviders(queryIntent, 0);
        if (providers == null || providers.isEmpty()) {
            // There are no providers, see if this activity has a direct link.
            ResolveInfo resolve = context.getPackageManager().resolveActivity(intent,
                    PackageManager.GET_META_DATA);
            if (resolve != null && resolve.activityInfo != null
                    && resolve.activityInfo.metaData != null
                    && resolve.activityInfo.metaData.containsKey(SLICE_METADATA_KEY)) {
                return Uri.parse(
                        resolve.activityInfo.metaData.getString(SLICE_METADATA_KEY));
            }
            return null;
        }
        String authority = providers.get(0).providerInfo.authority;
        Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority).build();
        try (ProviderHolder holder = acquireClient(resolver, uri)) {
            if (holder.mProvider == null) {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_INTENT, intent);
            final Bundle res = holder.mProvider.call(METHOD_MAP_ONLY_INTENT, null, extras);
            if (res != null) {
                return res.getParcelable(EXTRA_SLICE);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to map slice", e);
        }
        return null;
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#getSliceDescendants(Uri)}
     */
    public static @NonNull Collection<Uri> getSliceDescendants(Context context, @NonNull Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        try (ProviderHolder holder = acquireClient(resolver, uri)) {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            final Bundle res = holder.mProvider.call(METHOD_GET_DESCENDANTS, null, extras);
            if (res != null) {
                return res.getParcelableArrayList(EXTRA_SLICE_DESCENDANTS);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get slice descendants", e);
        }
        return Collections.emptyList();
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#checkSlicePermission}.
     */
    public static int checkSlicePermission(Context context, String packageName, Uri uri, int pid,
            int uid) {
        ContentResolver resolver = context.getContentResolver();
        try (ProviderHolder holder = acquireClient(resolver, uri)) {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PKG, packageName);
            extras.putInt(EXTRA_PID, pid);
            extras.putInt(EXTRA_UID, uid);

            final Bundle res = holder.mProvider.call(METHOD_CHECK_PERMISSION, null, extras);
            if (res != null) {
                return res.getInt(EXTRA_RESULT);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to check slice permission", e);
        }
        return PERMISSION_DENIED;
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#grantSlicePermission}.
     */
    public static void grantSlicePermission(Context context, String packageName, String toPackage,
            Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        try (ProviderHolder holder = acquireClient(resolver, uri)) {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PROVIDER_PKG, packageName);
            extras.putString(EXTRA_PKG, toPackage);

            holder.mProvider.call(METHOD_GRANT_PERMISSION, null, extras);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get slice descendants", e);
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#revokeSlicePermission}.
     */
    public static void revokeSlicePermission(Context context, String packageName, String toPackage,
            Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        try (ProviderHolder holder = acquireClient(resolver, uri)) {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PROVIDER_PKG, packageName);
            extras.putString(EXTRA_PKG, toPackage);

            holder.mProvider.call(METHOD_REVOKE_PERMISSION, null, extras);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get slice descendants", e);
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#getPinnedSlices}.
     */
    public static List<Uri> getPinnedSlices(Context context) {
        ArrayList<Uri> pinnedSlices = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(ALL_FILES, 0);
        Set<String> prefSet = prefs.getStringSet(ALL_FILES, Collections.<String>emptySet());
        for (String pref : prefSet) {
            pinnedSlices.addAll(new CompatPinnedList(context, pref).getPinnedSlices());
        }
        return pinnedSlices;
    }

    private static ProviderHolder acquireClient(ContentResolver resolver, Uri uri) {
        ContentProviderClient provider = resolver.acquireContentProviderClient(uri);
        if (provider == null) {
            throw new IllegalArgumentException("No provider found for " + uri);
        }
        return new ProviderHolder(provider);
    }

    private static class ProviderHolder implements AutoCloseable {
        private final ContentProviderClient mProvider;

        ProviderHolder(ContentProviderClient provider) {
            this.mProvider = provider;
        }

        @Override
        public void close() {
            if (mProvider == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mProvider.close();
            } else {
                mProvider.release();
            }
        }
    }
}
