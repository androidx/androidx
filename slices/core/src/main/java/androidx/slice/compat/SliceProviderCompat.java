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

import static android.app.slice.SliceProvider.SLICE_TYPE;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;
import androidx.slice.Slice;
import androidx.slice.SliceSpec;
import androidx.slice.core.SliceHints;

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
    private static final String TAG = "SliceProviderCompat";

    public static final String EXTRA_BIND_URI = "slice_uri";
    public static final String METHOD_SLICE = "bind_slice";
    public static final String METHOD_MAP_INTENT = "map_slice";
    public static final String METHOD_PIN = "pin_slice";
    public static final String METHOD_UNPIN = "unpin_slice";
    public static final String METHOD_GET_PINNED_SPECS = "get_specs";
    public static final String METHOD_MAP_ONLY_INTENT = "map_only";
    public static final String METHOD_GET_DESCENDANTS = "get_descendants";

    public static final String EXTRA_INTENT = "slice_intent";
    public static final String EXTRA_SLICE = "slice";
    public static final String EXTRA_SUPPORTED_SPECS = "specs";
    public static final String EXTRA_SUPPORTED_SPECS_REVS = "revs";
    public static final String EXTRA_PKG = "pkg";
    public static final String EXTRA_PROVIDER_PKG = "provider_pkg";
    public static final String EXTRA_SLICE_DESCENDANTS = "slice_descendants";

    /**
     * Compat version of {@link Slice#bindSlice}.
     */
    public static Slice bindSlice(Context context, Uri uri,
            Set<SliceSpec> supportedSpecs) {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                provider.close();
            } else {
                provider.release();
            }
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
            Set<SliceSpec> supportedSpecs) {
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
            Set<SliceSpec> supportedSpecs) {
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
    public static Set<SliceSpec> getPinnedSpecs(Context context, Uri uri) {
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

    /**
     * Compat version of {@link android.app.slice.SliceManager#mapIntentToUri}.
     */
    public static Uri mapIntentToUri(Context context, Intent intent) {
        Preconditions.checkNotNull(intent, "intent");
        Preconditions.checkArgument(intent.getComponent() != null || intent.getPackage() != null,
                String.format("Slice intent must be explicit %s", intent));
        ContentResolver resolver = context.getContentResolver();

        // Check if the intent has data for the slice uri on it and use that
        final Uri intentData = intent.getData();
        if (intentData != null && SLICE_TYPE.equals(resolver.getType(intentData))) {
            return intentData;
        }
        // Otherwise ask the app
        List<ResolveInfo> providers =
                context.getPackageManager().queryIntentContentProviders(intent, 0);
        if (providers == null || providers.isEmpty()) {
            // There are no providers, see if this activity has a direct link.
            ResolveInfo resolve = context.getPackageManager().resolveActivity(intent,
                    PackageManager.GET_META_DATA);
            if (resolve != null && resolve.activityInfo != null
                    && resolve.activityInfo.metaData != null
                    && resolve.activityInfo.metaData.containsKey(SliceHints.SLICE_METADATA_KEY)) {
                return Uri.parse(
                        resolve.activityInfo.metaData.getString(SliceHints.SLICE_METADATA_KEY));
            }
            return null;
        }
        String authority = providers.get(0).providerInfo.authority;
        Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority).build();
        try (ContentProviderClient provider = resolver.acquireContentProviderClient(uri)) {
            if (provider == null) {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_INTENT, intent);
            final Bundle res = provider.call(METHOD_MAP_ONLY_INTENT, null, extras);
            if (res == null) {
                return null;
            }
            return res.getParcelable(EXTRA_SLICE);
        } catch (RemoteException e) {
            // Arbitrary and not worth documenting, as Activity
            // Manager will kill this process shortly anyway.
            return null;
        }
    }

    /**
     * Compat version of {@link android.app.slice.SliceManager#getSliceDescendants(Uri)}
     */
    public static @NonNull Collection<Uri> getSliceDescendants(Context context, @NonNull Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        try (ContentProviderClient provider = resolver.acquireContentProviderClient(uri)) {
            Bundle extras = new Bundle();
            extras.putParcelable(EXTRA_BIND_URI, uri);
            final Bundle res = provider.call(METHOD_GET_DESCENDANTS, null, extras);
            return res.getParcelableArrayList(EXTRA_SLICE_DESCENDANTS);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get slice descendants", e);
        }
        return Collections.emptyList();
    }

    private SliceProviderCompat() {
    }
}
