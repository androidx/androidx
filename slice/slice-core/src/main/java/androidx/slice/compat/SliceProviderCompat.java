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

import android.annotation.SuppressLint;
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

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.collection.ArraySet;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Preconditions;
import androidx.slice.Slice;
import androidx.slice.SliceItemHolder;
import androidx.slice.SliceProvider;
import androidx.slice.SliceSpec;
import androidx.versionedparcelable.ParcelUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 */
@RestrictTo(Scope.LIBRARY_GROUP)
@Deprecated
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

    public static final String ARG_SUPPORTS_VERSIONED_PARCELABLE = "supports_versioned_parcelable";

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Context mContext;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    String mCallback;
    private final SliceProvider mProvider;
    private final CompatPinnedList mPinnedList;
    private final CompatPermissionManager mPermissionManager;

    public SliceProviderCompat(@NonNull SliceProvider provider,
            @NonNull CompatPermissionManager permissionManager, @NonNull Context context) {
        mProvider = provider;
        mContext = context;
        String prefsFile = DATA_PREFIX + "androidx.slice.compat.SliceProviderCompat";
        SharedPreferences allFiles = mContext.getSharedPreferences(ALL_FILES, 0);
        Set<String> files = allFiles.getStringSet(ALL_FILES, Collections.emptySet());
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

    @Nullable
    public String getCallingPackage() {
        return mProvider.getCallingPackage();
    }

    /**
     * Called by SliceProvider when compat is needed.
     */
    @Nullable
    public Bundle call(@NonNull String method, @Nullable String arg, @NonNull Bundle extras) {
        switch (method) {
            case METHOD_SLICE: {
                Uri uri = extras.getParcelable(EXTRA_BIND_URI);
                mProvider.validateIncomingAuthority(uri.getAuthority());
                Set<SliceSpec> specs = getSpecs(extras);

                Slice s = handleBindSlice(uri, specs, getCallingPackage());
                Bundle b = new Bundle();
                if (ARG_SUPPORTS_VERSIONED_PARCELABLE.equals(arg)) {
                    synchronized (SliceItemHolder.sSerializeLock) {
                        b.putParcelable(EXTRA_SLICE,
                                s != null ? ParcelUtils.toParcelable(s) : null);
                    }
                } else {
                    b.putParcelable(EXTRA_SLICE, s != null ? s.toBundle() : null);
                }
                return b;
            }
            case METHOD_MAP_INTENT: {
                Intent intent = extras.getParcelable(EXTRA_INTENT);
                Uri uri = mProvider.onMapIntentToUri(intent);
                mProvider.validateIncomingAuthority(uri.getAuthority());
                Bundle b = new Bundle();
                Set<SliceSpec> specs = getSpecs(extras);
                Slice s = handleBindSlice(uri, specs, getCallingPackage());
                if (ARG_SUPPORTS_VERSIONED_PARCELABLE.equals(arg)) {
                    synchronized (SliceItemHolder.sSerializeLock) {
                        b.putParcelable(EXTRA_SLICE,
                                s != null ? ParcelUtils.toParcelable(s) : null);
                    }
                } else {
                    b.putParcelable(EXTRA_SLICE, s != null ? s.toBundle() : null);
                }
                return b;
            }
            case METHOD_MAP_ONLY_INTENT: {
                Intent intent = extras.getParcelable(EXTRA_INTENT);
                Uri uri = mProvider.onMapIntentToUri(intent);
                mProvider.validateIncomingAuthority(uri.getAuthority());
                Bundle b = new Bundle();
                b.putParcelable(EXTRA_SLICE, uri);
                return b;
            }
            case METHOD_PIN: {
                Uri uri = extras.getParcelable(EXTRA_BIND_URI);
                mProvider.validateIncomingAuthority(uri.getAuthority());
                Set<SliceSpec> specs = getSpecs(extras);
                String pkg = extras.getString(EXTRA_PKG);
                if (mPinnedList.addPin(uri, pkg, specs)) {
                    handleSlicePinned(uri);
                }
                return null;
            }
            case METHOD_UNPIN: {
                Uri uri = extras.getParcelable(EXTRA_BIND_URI);
                mProvider.validateIncomingAuthority(uri.getAuthority());
                String pkg = extras.getString(EXTRA_PKG);
                if (mPinnedList.removePin(uri, pkg)) {
                    handleSliceUnpinned(uri);
                }
                return null;
            }
            case METHOD_GET_PINNED_SPECS: {
                Uri uri = extras.getParcelable(EXTRA_BIND_URI);
                mProvider.validateIncomingAuthority(uri.getAuthority());
                Bundle b = new Bundle();
                ArraySet<SliceSpec> specs = mPinnedList.getSpecs(uri);
                if (specs.size() == 0) {
                    throw new IllegalStateException(uri + " is not pinned");
                }
                addSpecs(b, specs);
                return b;
            }
            case METHOD_GET_DESCENDANTS: {
                Uri uri = extras.getParcelable(EXTRA_BIND_URI);
                mProvider.validateIncomingAuthority(uri.getAuthority());
                Bundle b = new Bundle();
                b.putParcelableArrayList(EXTRA_SLICE_DESCENDANTS,
                        new ArrayList<>(handleGetDescendants(uri)));
                return b;
            }
            case METHOD_CHECK_PERMISSION: {
                Uri uri = extras.getParcelable(EXTRA_BIND_URI);
                mProvider.validateIncomingAuthority(uri.getAuthority());
                int pid = extras.getInt(EXTRA_PID);
                int uid = extras.getInt(EXTRA_UID);
                Bundle b = new Bundle();
                b.putInt(EXTRA_RESULT, mPermissionManager.checkSlicePermission(uri, pid, uid));
                return b;
            }
            case METHOD_GRANT_PERMISSION: {
                Uri uri = extras.getParcelable(EXTRA_BIND_URI);
                mProvider.validateIncomingAuthority(uri.getAuthority());
                String toPkg = extras.getString(EXTRA_PKG);
                if (Binder.getCallingUid() != Process.myUid()) {
                    throw new SecurityException(
                            "Only the owning process can manage slice permissions");
                }
                mPermissionManager.grantSlicePermission(uri, toPkg);
                break;
            }
            case METHOD_REVOKE_PERMISSION: {
                Uri uri = extras.getParcelable(EXTRA_BIND_URI);
                mProvider.validateIncomingAuthority(uri.getAuthority());
                String toPkg = extras.getString(EXTRA_PKG);
                if (Binder.getCallingUid() != Process.myUid()) {
                    throw new SecurityException(
                            "Only the owning process can manage slice permissions");
                }
                mPermissionManager.revokeSlicePermission(uri, toPkg);
                break;
            }
        }
        return null;
    }

    private Collection<Uri> handleGetDescendants(Uri uri) {
        mCallback = "onGetSliceDescendants";
        return mProvider.onGetSliceDescendants(uri);
    }

    private void handleSlicePinned(final Uri sliceUri) {
        mCallback = "onSlicePinned";
        mHandler.postDelayed(mAnr, SLICE_BIND_ANR);
        try {
            mProvider.onSlicePinned(sliceUri);
            mProvider.handleSlicePinned(sliceUri);
        } finally {
            mHandler.removeCallbacks(mAnr);
        }
    }

    private void handleSliceUnpinned(final Uri sliceUri) {
        mCallback = "onSliceUnpinned";
        mHandler.postDelayed(mAnr, SLICE_BIND_ANR);
        try {
            mProvider.onSliceUnpinned(sliceUri);
            mProvider.handleSliceUnpinned(sliceUri);
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
            return mProvider.createPermissionSlice(sliceUri, pkg);
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
            } catch (Exception e) {
                Log.wtf(TAG, "Slice with URI " + sliceUri.toString() + " is invalid.", e);
                return null;
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
    @Nullable
    public static Slice bindSlice(@NonNull Context context, @NonNull Uri uri,
            @NonNull Set<SliceSpec> supportedSpecs) {
        ProviderHolder holder = acquireClient(context.getContentResolver(), uri);
        if (holder.mProvider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            addSpecs(extras, supportedSpecs);
            final Bundle res = holder.mProvider.call(METHOD_SLICE,
                    ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
            return parseSlice(context, res);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to bind slice", e);
            return null;
        } finally {
            holder.close();
        }
    }

    /**
     * Compat way to push specs through the call.
     */
    public static void addSpecs(@NonNull Bundle extras, @NonNull Set<SliceSpec> supportedSpecs) {
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
    @NonNull
    public static Set<SliceSpec> getSpecs(@NonNull Bundle extras) {
        ArraySet<SliceSpec> specs = new ArraySet<>();
        ArrayList<String> types = extras.getStringArrayList(EXTRA_SUPPORTED_SPECS);
        ArrayList<Integer> revs = extras.getIntegerArrayList(EXTRA_SUPPORTED_SPECS_REVS);
        if (types != null && revs != null) {
            for (int i = 0; i < types.size(); i++) {
                specs.add(new SliceSpec(types.get(i), revs.get(i)));
            }
        }
        return specs;
    }

    /**
     * Compat version of {@link Slice#bindSlice}.
     */
    @Nullable
    public static Slice bindSlice(@NonNull Context context, @NonNull Intent intent,
            @NonNull Set<SliceSpec> supportedSpecs) {
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
            final Bundle res = holder.mProvider.call(METHOD_MAP_INTENT,
                    ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
            return parseSlice(context, res);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to bind slice", e);
            return null;
        } finally {
            holder.close();
        }
    }

    @SuppressLint("WrongConstant") // Needed for IconCompat.TYPE_RESOURCE lint failure
    private static Slice parseSlice(final Context context, Bundle res) {
        if (res == null) {
            return null;
        }
        synchronized (SliceItemHolder.sSerializeLock) {
            try {
                SliceItemHolder.sHandler = (holder, format) -> {
                    if (holder.mVersionedParcelable instanceof IconCompat) {
                        IconCompat icon = (IconCompat) holder.mVersionedParcelable;
                        icon.checkResource(context);
                        if (icon.getType() == IconCompat.TYPE_RESOURCE
                                && icon.getResId() == 0) {
                            holder.mVersionedParcelable = null;
                        }
                    }
                };
                res.setClassLoader(SliceProviderCompat.class.getClassLoader());
                Parcelable parcel = res.getParcelable(EXTRA_SLICE);
                if (parcel == null) {
                    return null;
                }
                if (parcel instanceof Bundle) {
                    return new Slice((Bundle) parcel);
                }
                return ParcelUtils.fromParcelable(parcel);
            } finally {
                SliceItemHolder.sHandler = null;
            }
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#pinSlice}.
     */
    public static void pinSlice(@NonNull Context context, @NonNull Uri uri,
            @NonNull Set<SliceSpec> supportedSpecs) {
        ProviderHolder holder = acquireClient(context.getContentResolver(), uri);
        if (holder.mProvider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PKG, context.getPackageName());
            addSpecs(extras, supportedSpecs);
            holder.mProvider.call(METHOD_PIN, ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to pin slice", e);
        } finally {
            holder.close();
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#unpinSlice}.
     */
    public static void unpinSlice(@NonNull Context context, @NonNull Uri uri,
            @NonNull Set<SliceSpec> supportedSpecs) {
        ProviderHolder holder = acquireClient(context.getContentResolver(), uri);
        if (holder.mProvider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PKG, context.getPackageName());
            addSpecs(extras, supportedSpecs);
            holder.mProvider.call(METHOD_UNPIN, ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to unpin slice", e);
        } finally {
            holder.close();
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#getPinnedSpecs(Uri)}.
     */
    @Nullable
    public static Set<SliceSpec> getPinnedSpecs(@NonNull Context context, @NonNull Uri uri) {
        ProviderHolder holder = acquireClient(context.getContentResolver(), uri);
        if (holder.mProvider == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            final Bundle res = holder.mProvider.call(METHOD_GET_PINNED_SPECS,
                    ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
            if (res != null) {
                return getSpecs(res);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get pinned specs", e);
        } finally {
            holder.close();
        }
        return null;
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#mapIntentToUri}.
     */
    @Nullable
    public static Uri mapIntentToUri(@NonNull Context context, @NonNull Intent intent) {
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
            final Bundle res = holder.mProvider.call(METHOD_MAP_ONLY_INTENT,
                    ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
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
    @NonNull
    @SuppressWarnings("MixedMutabilityReturnType")
    public static Collection<Uri> getSliceDescendants(@NonNull Context context, @NonNull Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        try (ProviderHolder holder = acquireClient(resolver, uri)) {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            final Bundle res = holder.mProvider.call(METHOD_GET_DESCENDANTS,
                    ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
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
    public static int checkSlicePermission(@NonNull Context context, @Nullable String packageName,
            @NonNull Uri uri, int pid, int uid) {
        ContentResolver resolver = context.getContentResolver();
        try (ProviderHolder holder = acquireClient(resolver, uri)) {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PKG, packageName);
            extras.putInt(EXTRA_PID, pid);
            extras.putInt(EXTRA_UID, uid);

            final Bundle res = holder.mProvider.call(METHOD_CHECK_PERMISSION,
                    ARG_SUPPORTS_VERSIONED_PARCELABLE, extras);
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
    public static void grantSlicePermission(@NonNull Context context, @Nullable String packageName,
            @Nullable String toPackage, @NonNull Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        try (ProviderHolder holder = acquireClient(resolver, uri)) {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PROVIDER_PKG, packageName);
            extras.putString(EXTRA_PKG, toPackage);

            holder.mProvider.call(METHOD_GRANT_PERMISSION, ARG_SUPPORTS_VERSIONED_PARCELABLE,
                    extras);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get slice descendants", e);
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#revokeSlicePermission}.
     */
    public static void revokeSlicePermission(@NonNull Context context, @Nullable String packageName,
            @Nullable String toPackage, @NonNull Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        try (ProviderHolder holder = acquireClient(resolver, uri)) {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            extras.putString(EXTRA_PROVIDER_PKG, packageName);
            extras.putString(EXTRA_PKG, toPackage);

            holder.mProvider.call(METHOD_REVOKE_PERMISSION, ARG_SUPPORTS_VERSIONED_PARCELABLE,
                    extras);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get slice descendants", e);
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#getPinnedSlices}.
     */
    @NonNull
    public static List<Uri> getPinnedSlices(@NonNull Context context) {
        ArrayList<Uri> pinnedSlices = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(ALL_FILES, 0);
        Set<String> prefSet = prefs.getStringSet(ALL_FILES, Collections.emptySet());
        for (String pref : prefSet) {
            pinnedSlices.addAll(new CompatPinnedList(context, pref).getPinnedSlices());
        }
        return pinnedSlices;
    }

    private static ProviderHolder acquireClient(ContentResolver resolver, Uri uri) {
        ContentProviderClient provider = resolver.acquireUnstableContentProviderClient(uri);
        if (provider == null) {
            throw new IllegalArgumentException("No provider found for " + uri);
        }
        return new ProviderHolder(provider);
    }

    private static class ProviderHolder implements AutoCloseable {
        final ContentProviderClient mProvider;

        ProviderHolder(ContentProviderClient provider) {
            this.mProvider = provider;
        }

        @Override
        public void close() {
            if (mProvider == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Api24Impl.close(mProvider);
            } else {
                mProvider.release();
            }
        }

        @RequiresApi(24)
        static class Api24Impl {
            private Api24Impl() {
                // This class is not instantiable.
            }

            @DoNotInline
            static void close(ContentProviderClient contentProviderClient) {
                contentProviderClient.close();
            }
        }
    }
}
