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
package androidx.slice;

import static android.app.slice.Slice.HINT_PERMISSION_REQUEST;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.SliceProvider.SLICE_TYPE;

import static androidx.slice.compat.SliceProviderCompat.EXTRA_BIND_URI;
import static androidx.slice.compat.SliceProviderCompat.EXTRA_PKG;
import static androidx.slice.compat.SliceProviderCompat.EXTRA_PROVIDER_PKG;
import static androidx.slice.compat.SliceProviderCompat.PERMS_PREFIX;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Process;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.CoreComponentFactory;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.compat.CompatPermissionManager;
import androidx.slice.compat.SliceProviderCompat;
import androidx.slice.compat.SliceProviderWrapperContainer;
import androidx.slice.core.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A SliceProvider allows an app to provide {@link Slice}s to the Android OS. A slice is a piece of
 * app content and actions that can be displayed outside of the app in Android system surfaces
 * or within another app. Slices are identified by a Uri and a SliceProvider allows your app to
 * provide a slice based on a uri.
 * <p>
 * The primary method to implement in SliceProvider is {@link #onBindSlice(Uri)} which is called
 * whenever something wants to display a slice from your app. An app can have multiple slices all
 * served from the same slice provider, the Uri passed to onBindSlice will identify the specific
 * slice being requested.
 * </p>
 * <pre class="prettyprint">
 * public MySliceProvider extends SliceProvider {
 *
 *      public Slice onBindSlice(Uri sliceUri) {
 *          String path = sliceUri.getPath();
 *          switch (path) {
 *              case "/weather":
 *                  return createWeatherSlice(sliceUri);
 *              case "/traffic":
 *                  return createTrafficSlice(sliceUri);
 *          }
 *          return null;
 *      }
 * }
 * </pre>
 * <p>
 * Slices are constructed with {@link androidx.slice.builders.TemplateSliceBuilder}s.
 * </p>
 * <p>
 * Slices are not currently live content. They are bound once and shown to the user. If the content
 * in the slice changes due to user interaction or an update in the data being displayed, then
 * {@link ContentResolver#notifyChange(Uri, ContentObserver)} should be used to notify the system
 * to request the latest slice from the app.
 * </p>
 * <p>
 * The provider needs to be declared in the manifest to provide the authority for the app. The
 * authority for most slices is expected to match the package of the application.
 * </p>
 * <pre class="prettyprint">
 * {@literal
 * <provider
 *     android:name="com.android.mypkg.MySliceProvider"
 *     android:authorities="com.android.mypkg" />}
 * </pre>
 * <p>
 * Slices can also be identified by an intent. To link an intent with a slice, the slice provider
 * must have an {@link IntentFilter} matching the slice intent. When a slice is being requested via
 * an intent, {@link #onMapIntentToUri(Intent)} will be called and is expected to return an
 * appropriate Uri representing the slice.
 * </p>
 * <pre class="prettyprint">
 * {@literal
 * <provider
 *     android:name="com.android.mypkg.MySliceProvider"
 *     android:authorities="com.android.mypkg">
 *     <intent-filter>
 *         <action android:name="android.intent.action.MY_SLICE_INTENT" />
 *         <category android:name="android.app.slice.category.SLICE" />
 *     </intent-filter>
 * </provider>}
 * </pre>
 *
 * @see Slice
 */
public abstract class SliceProvider extends ContentProvider implements
        CoreComponentFactory.CompatWrapped {

    private static Set<SliceSpec> sSpecs;
    private static Clock sClock;

    private static final String TAG = "SliceProvider";

    private static final boolean DEBUG = false;
    private final String[] mAutoGrantPermissions;

    private Context mContext = null;
    private final Object mCompatLock = new Object();
    private SliceProviderCompat mCompat;

    private final Object mPinnedSliceUrisLock = new Object();
    private List<Uri> mPinnedSliceUris;

    private String mAuthority;
    private String[] mAuthorities;

    /**
     * A version of constructing a SliceProvider that allows autogranting slice permissions
     * to apps that hold specific platform permissions.
     * <p>
     * When an app tries to bind a slice from a provider that it does not have access to,
     * the provider will check if the caller holds permissions to any of the autoGrantPermissions
     * specified, if they do they will be granted persisted uri access to all slices of this
     * provider.
     *
     * @param autoGrantPermissions List of permissions that holders are auto-granted access
     *                             to slices.
     */
    public SliceProvider(@NonNull String... autoGrantPermissions) {
        mAutoGrantPermissions = autoGrantPermissions;
    }

    public SliceProvider() {
        mAutoGrantPermissions = new String[0];
    }

    /**
     * Implement this to initialize your slice provider on startup.
     * This method is called for all registered slice providers on the
     * application main thread at application launch time.  It must not perform
     * lengthy operations, or application startup will be delayed.
     *
     * <p>You should defer nontrivial initialization (such as opening,
     * upgrading, and scanning databases) until the slice provider is used
     * (via #onBindSlice, etc).  Deferred initialization
     * keeps application startup fast, avoids unnecessary work if the provider
     * turns out not to be needed, and stops database errors (such as a full
     * disk) from halting application launch.
     *
     * @return true if the provider was successfully loaded, false otherwise
     */
    @RequiresApi(19)
    public abstract boolean onCreateSliceProvider();

    /**
     * @hide
     */
    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    @RequiresApi(19)
    public Object getWrapper() {
        if (Build.VERSION.SDK_INT >= 28) {
            return new SliceProviderWrapperContainer.SliceProviderWrapper(this,
                    mAutoGrantPermissions);
        }
        return null;
    }

    @Override
    public final boolean onCreate() {
        if (Build.VERSION.SDK_INT < 19) return false;
        return onCreateSliceProvider();
    }

    @NonNull
    @RequiresApi(19)
    private SliceProviderCompat getSliceProviderCompat() {
        synchronized (mCompatLock) {
            if (mCompat == null) {
                mCompat = new SliceProviderCompat(this,
                        onCreatePermissionManager(mAutoGrantPermissions), getContext());
            }
        }
        return mCompat;
    }

    /**
     * @hide
     * @param autoGrantPermissions
     */
    @NonNull
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(19)
    protected CompatPermissionManager onCreatePermissionManager(
            @NonNull String[] autoGrantPermissions) {
        return new CompatPermissionManager(getContext(), PERMS_PREFIX + getClass().getName(),
                Process.myUid(), autoGrantPermissions);
    }

    @Nullable
    @Override
    public final String getType(@NonNull Uri uri) {
        if (Build.VERSION.SDK_INT < 19) return null;
        if (DEBUG) Log.d(TAG, "getFormat " + uri);
        return SLICE_TYPE;
    }

    @Override
    public void attachInfo(@Nullable Context context, @Nullable ProviderInfo info) {
        super.attachInfo(context, info);
        /*
         * Only allow it to be set once, so after the content service gives
         * this to us clients can't change it.
         */
        if (mContext == null) {
            mContext = context;
            if (info != null) {
                setAuthorities(info.authority);
            }
        }
    }

    /**
     * Handles the call to SliceProvider.
     *
     * <p>This function is unsupported for sdk < 19. For sdk 28 and above the call is handled by
     * {@link android.app.slice.SliceProvider}
     */
    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (Build.VERSION.SDK_INT < 19 || Build.VERSION.SDK_INT >= 28) return null;
        if (extras == null) return null;
        return getSliceProviderCompat().call(method, arg, extras);
    }

    /**
     * Change the authorities of the ContentProvider.
     * This is normally set for you from its manifest information when the provider is first
     * created.
     * @param authorities the semi-colon separated authorities of the ContentProvider.
     */
    private void setAuthorities(String authorities) {
        if (authorities != null) {
            if (authorities.indexOf(';') == -1) {
                mAuthority = authorities;
                mAuthorities = null;
            } else {
                mAuthority = null;
                mAuthorities = authorities.split(";");
            }
        }
    }

    private boolean matchesOurAuthorities(String authority) {
        if (mAuthority != null) {
            return mAuthority.equals(authority);
        }
        if (mAuthorities != null) {
            int length = mAuthorities.length;
            for (int i = 0; i < length; i++) {
                if (mAuthorities[i].equals(authority)) return true;
            }
        }
        return false;
    }

    /**
     * Called when an app requests a slice it does not have write permission
     * to the uri for.
     * <p>
     * The return value will be the action on a slice that prompts the user that
     * the calling app wants to show slices from this app. Returning null will use the default
     * implementation that launches a dialog that allows the user to grant access to this slice.
     * Apps that do not want to allow this user grant, can override this and instead
     * launch their own dialog with different behavior.
     *
     * @param sliceUri the Uri of the slice attempting to be bound.
     * @param callingPackage the packageName of the app requesting the slice
     */
    @Nullable
    public PendingIntent onCreatePermissionRequest(@NonNull Uri sliceUri,
            @NonNull String callingPackage) {
        return null;
    }

    /**
     * Generate a slice that contains a permission request.
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(19)
    public Slice createPermissionSlice(@NonNull Uri sliceUri,
            @NonNull String callingPackage) {
        Context context = getContext();
        PendingIntent action = onCreatePermissionRequest(sliceUri, callingPackage);
        if (action == null) {
            action = createPermissionIntent(context, sliceUri, callingPackage);
        }

        Slice.Builder parent = new Slice.Builder(sliceUri);
        Slice.Builder childAction = new Slice.Builder(parent)
                .addIcon(IconCompat.createWithResource(context,
                        R.drawable.abc_ic_permission), null)
                .addHints(Arrays.asList(HINT_TITLE, HINT_SHORTCUT))
                .addAction(action, new Slice.Builder(parent).build(), null);

        TypedValue tv = new TypedValue();
        new ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_Light)
                .getTheme().resolveAttribute(android.R.attr.colorAccent, tv, true);
        int deviceDefaultAccent = tv.data;

        parent.addSubSlice(new Slice.Builder(sliceUri.buildUpon().appendPath("permission").build())
                .addIcon(IconCompat.createWithResource(context,
                        R.drawable.abc_ic_arrow_forward), null)
                .addText(getPermissionString(context, callingPackage), null)
                .addInt(deviceDefaultAccent, SUBTYPE_COLOR)
                .addSubSlice(childAction.build(), null)
                .build(), null);
        return parent.addHints(Arrays.asList(HINT_PERMISSION_REQUEST)).build();
    }

    /**
     * Create a PendingIntent pointing at the permission dialog.
     */
    @RequiresApi(19)
    private static PendingIntent createPermissionIntent(Context context, Uri sliceUri,
            String callingPackage) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context.getPackageName(),
                "androidx.slice.compat.SlicePermissionActivity"));
        intent.putExtra(EXTRA_BIND_URI, sliceUri);
        intent.putExtra(EXTRA_PKG, callingPackage);
        intent.putExtra(EXTRA_PROVIDER_PKG, context.getPackageName());
        // Unique pending intent.
        intent.setData(sliceUri.buildUpon().appendQueryParameter("package", callingPackage)
                .build());

        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    /**
     * Get string describing permission request.
     */
    @RequiresApi(19)
    private static CharSequence getPermissionString(Context context, String callingPackage) {
        PackageManager pm = context.getPackageManager();
        try {
            return context.getString(R.string.abc_slices_permission_request,
                    pm.getApplicationInfo(callingPackage, 0).loadLabel(pm),
                    context.getApplicationInfo().loadLabel(pm));
        } catch (PackageManager.NameNotFoundException e) {
            // This shouldn't be possible since the caller is verified.
            throw new RuntimeException("Unknown calling app", e);
        }
    }

    /**
     * Implemented to create a slice.
     * <p>
     * onBindSlice should return as quickly as possible so that the UI tied
     * to this slice can be responsive. No network or other IO will be allowed
     * during onBindSlice. Any loading that needs to be done should happen
     * in the background with a call to {@link ContentResolver#notifyChange(Uri, ContentObserver)}
     * when the app is ready to provide the complete data in onBindSlice.
     * <p>
     *
     * @see Slice
     * @see android.app.slice.Slice#HINT_PARTIAL
     */
    // TODO: Provide alternate notifyChange that takes in the slice (i.e. notifyChange(Uri, Slice)).
    @Nullable
    @RequiresApi(19)
    public abstract Slice onBindSlice(@NonNull Uri sliceUri);

    /**
     * Called to inform an app that a slice has been pinned.
     * <p>
     * Pinning is a way that slice hosts use to notify apps of which slices
     * they care about updates for. When a slice is pinned the content is
     * expected to be relatively fresh and kept up to date.
     * <p>
     * Being pinned does not provide any escalated privileges for the slice
     * provider. So apps should do things such as turn on syncing or schedule
     * a job in response to a onSlicePinned.
     * <p>
     * Pinned state is not persisted through a reboot, and apps can expect a
     * new call to onSlicePinned for any slices that should remain pinned
     * after a reboot occurs.
     *
     * @param sliceUri The uri of the slice being unpinned.
     * @see #onSliceUnpinned(Uri)
     */
    @RequiresApi(19)
    public void onSlicePinned(@NonNull Uri sliceUri) {}

    /**
     * Called to inform an app that a slices is no longer pinned.
     * <p>
     * This means that no other apps on the device care about updates to this
     * slice anymore and therefore it is not important to be updated. Any syncs
     * or jobs related to this slice should be cancelled.
     * @see #onSlicePinned(Uri)
     */
    @RequiresApi(19)
    public void onSliceUnpinned(@NonNull Uri sliceUri) {}

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(19)
    public void handleSlicePinned(@NonNull Uri sliceUri) {
        List<Uri> pinnedSlices = getPinnedSlices();
        if (!pinnedSlices.contains(sliceUri)) {
            pinnedSlices.add(sliceUri);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(19)
    public void handleSliceUnpinned(@NonNull Uri sliceUri) {
        List<Uri> pinnedSlices = getPinnedSlices();
        if (pinnedSlices.contains(sliceUri)) {
            pinnedSlices.remove(sliceUri);
        }
    }

    /**
     * This method must be overridden if an {@link IntentFilter} is specified on the SliceProvider.
     * In that case, this method can be called and is expected to return a non-null Uri representing
     * a slice. Otherwise this will throw {@link UnsupportedOperationException}.
     *
     * @return Uri representing the slice associated with the provided intent.
     * @see android.app.slice.Slice
     */
    @NonNull
    @RequiresApi(19)
    public Uri onMapIntentToUri(@NonNull Intent intent) {
        throw new UnsupportedOperationException(
                "This provider has not implemented intent to uri mapping");
    }

    /**
     * Obtains a list of slices that are descendants of the specified Uri.
     * <p>
     * Implementing this is optional for a SliceProvider, but does provide a good
     * discovery mechanism for finding slice Uris.
     *
     * @param uri The uri to look for descendants under.
     * @return All slices within the space.
     * @see androidx.slice.SliceViewManager#getSliceDescendants(Uri)
     */
    @NonNull
    @RequiresApi(19)
    public Collection<Uri> onGetSliceDescendants(@NonNull Uri uri) {
        return Collections.emptyList();
    }

    /**
     * Returns a list of slice URIs that are currently pinned.
     *
     * @return All pinned slices.
     */
    @NonNull
    @RequiresApi(19)
    public List<Uri> getPinnedSlices() {
        synchronized (mPinnedSliceUrisLock) {
            if (mPinnedSliceUris == null) {
                mPinnedSliceUris = new ArrayList<>(SliceManager.getInstance(getContext())
                        .getPinnedSlices());
            }
        }
        return mPinnedSliceUris;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void validateIncomingAuthority(@Nullable String authority) throws SecurityException {
        if (!matchesOurAuthorities(getAuthorityWithoutUserId(authority))) {
            String message = "The authority " + authority + " does not match the one of the "
                    + "contentProvider: ";
            if (mAuthority != null) {
                message += mAuthority;
            } else {
                message += Arrays.toString(mAuthorities);
            }
            throw new SecurityException(message);
        }
    }

    /**
     * Removes userId part from authority string. Expects format:
     * userId@some.authority
     * If there is no userId in the authority, it symply returns the argument
     */
    private static String getAuthorityWithoutUserId(String auth) {
        if (auth == null) return null;
        int end = auth.lastIndexOf('@');
        return auth.substring(end + 1);
    }

    @Nullable
    @Override
    public final Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            @Nullable String selection, @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    @RequiresApi(28)
    public final Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            @Nullable Bundle queryArgs, @Nullable CancellationSignal cancellationSignal) {
        return null;
    }

    @Nullable
    @Override
    @RequiresApi(16)
    public final Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            @Nullable String selection, @Nullable String[] selectionArgs,
            @Nullable String sortOrder, @Nullable CancellationSignal cancellationSignal) {
        return null;
    }

    @Nullable
    @Override
    public final Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public final int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        return 0;
    }

    @Override
    public final int delete(@NonNull Uri uri, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public final int update(@NonNull Uri uri, @Nullable ContentValues values,
            @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    @RequiresApi(19)
    public final Uri canonicalize(@NonNull Uri url) {
        return null;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(19)
    public static void setSpecs(@Nullable Set<SliceSpec> specs) {
        sSpecs = specs;
    }

    /**
     * @hide
     */
    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @RequiresApi(19)
    public static Set<SliceSpec> getCurrentSpecs() {
        return sSpecs;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(19)
    public static void setClock(@Nullable Clock clock) {
        sClock = clock;
    }

    /**
     * @hide
     */
    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(19)
    public static Clock getClock() {
        return sClock;
    }
}
