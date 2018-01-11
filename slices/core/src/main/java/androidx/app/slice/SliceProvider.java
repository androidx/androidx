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
package androidx.app.slice;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ProviderInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v4.os.BuildCompat;

import java.util.List;

import androidx.app.slice.compat.ContentProviderWrapper;
import androidx.app.slice.compat.SliceProviderCompat;
import androidx.app.slice.compat.SliceProviderWrapperContainer;

/**
 * A SliceProvider allows an app to provide content to be displayed in system spaces. This content
 * is templated and can contain actions, and the behavior of how it is surfaced is specific to the
 * system surface.
 * <p>
 * Slices are not currently live content. They are bound once and shown to the user. If the content
 * changes due to a callback from user interaction, then
 * {@link ContentResolver#notifyChange(Uri, ContentObserver)} should be used to notify the system.
 * </p>
 * <p>
 * The provider needs to be declared in the manifest to provide the authority for the app. The
 * authority for most slices is expected to match the package of the application.
 * </p>
 *
 * <pre class="prettyprint">
 * {@literal
 * <provider
 *     android:name="com.android.mypkg.MySliceProvider"
 *     android:authorities="com.android.mypkg" />}
 * </pre>
 * <p>
 * Slices can be identified by a Uri or by an Intent. To link an Intent with a slice, the provider
 * must have an {@link IntentFilter} matching the slice intent. When a slice is being requested via
 * an intent, {@link #onMapIntentToUri(Intent)} can be called and is expected to return an
 * appropriate Uri representing the slice.
 *
 * <pre class="prettyprint">
 * {@literal
 * <provider
 *     android:name="com.android.mypkg.MySliceProvider"
 *     android:authorities="com.android.mypkg">
 *     <intent-filter>
 *         <action android:name="android.intent.action.MY_SLICE_INTENT" />
 *     </intent-filter>
 * </provider>}
 * </pre>
 *
 * @see android.app.slice.Slice
 */
public abstract class SliceProvider extends ContentProviderWrapper {

    private static List<SliceSpec> sSpecs;

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        ContentProvider impl;
        if (BuildCompat.isAtLeastP()) {
            impl = new SliceProviderWrapperContainer.SliceProviderWrapper(this);
        } else {
            impl = new SliceProviderCompat(this);
        }
        super.attachInfo(context, info, impl);
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
    public abstract boolean onCreateSliceProvider();

    /**
     * Implemented to create a slice. Will be called on the main thread.
     * <p>
     * onBindSlice should return as quickly as possible so that the UI tied
     * to this slice can be responsive. No network or other IO will be allowed
     * during onBindSlice. Any loading that needs to be done should happen
     * off the main thread with a call to {@link ContentResolver#notifyChange(Uri, ContentObserver)}
     * when the app is ready to provide the complete data in onBindSlice.
     * <p>
     *
     * @see {@link Slice}.
     * @see {@link android.app.slice.Slice#HINT_PARTIAL}
     */
    // TODO: Provide alternate notifyChange that takes in the slice (i.e. notifyChange(Uri, Slice)).
    public abstract Slice onBindSlice(Uri sliceUri);

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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onSlicePinned(Uri sliceUri) {
    }

    /**
     * Called to inform an app that a slices is no longer pinned.
     * <p>
     * This means that no other apps on the device care about updates to this
     * slice anymore and therefore it is not important to be updated. Any syncs
     * or jobs related to this slice should be cancelled.
     * @see #onSlicePinned(Uri)
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onSliceUnpinned(Uri sliceUri) {
    }

    /**
     * This method must be overridden if an {@link IntentFilter} is specified on the SliceProvider.
     * In that case, this method can be called and is expected to return a non-null Uri representing
     * a slice. Otherwise this will throw {@link UnsupportedOperationException}.
     *
     * @return Uri representing the slice associated with the provided intent.
     * @see {@link android.app.slice.Slice}
     */
    public @NonNull Uri onMapIntentToUri(Intent intent) {
        throw new UnsupportedOperationException(
                "This provider has not implemented intent to uri mapping");
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void setSpecs(List<SliceSpec> specs) {
        sSpecs = specs;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static List<SliceSpec> getCurrentSpecs() {
        return sSpecs;
    }
}
