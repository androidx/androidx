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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.content.PermissionChecker;
import androidx.core.os.BuildCompat;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Class to handle interactions with {@link Slice}s.
 * <p>
 * The SliceManager manages permissions and pinned state for slices.
 */
public abstract class SliceManager {

    /**
     * Get a {@link SliceManager}.
     */
    @SuppressWarnings("NewApi")
    public static @NonNull SliceManager getInstance(@NonNull Context context) {
        if (BuildCompat.isAtLeastP()) {
            return new SliceManagerWrapper(context);
        } else {
            return new SliceManagerCompat(context);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    SliceManager() {
    }

    /**
     * Adds a callback to a specific slice uri.
     * <p>
     * This is a convenience that performs a few slice actions at once. It will put
     * the slice in a pinned state since there is a callback attached. It will also
     * listen for content changes, when a content change observes, the android system
     * will bind the new slice and provide it to all registered {@link SliceCallback}s.
     *
     * @param uri The uri of the slice being listened to.
     * @param callback The listener that should receive the callbacks.
     * @see SliceProvider#onSlicePinned(Uri)
     */
    public abstract void registerSliceCallback(@NonNull Uri uri, @NonNull SliceCallback callback);

    /**
     * Adds a callback to a specific slice uri.
     * <p>
     * This is a convenience that performs a few slice actions at once. It will put
     * the slice in a pinned state since there is a callback attached. It will also
     * listen for content changes, when a content change observes, the android system
     * will bind the new slice and provide it to all registered {@link SliceCallback}s.
     *
     * @param uri The uri of the slice being listened to.
     * @param callback The listener that should receive the callbacks.
     * @see SliceProvider#onSlicePinned(Uri)
     */
    public abstract void registerSliceCallback(@NonNull Uri uri, @NonNull Executor executor,
            @NonNull SliceCallback callback);

    /**
     * Removes a callback for a specific slice uri.
     * <p>
     * Removes the app from the pinned state (if there are no other apps/callbacks pinning it)
     * in addition to removing the callback.
     *
     * @param uri The uri of the slice being listened to
     * @param callback The listener that should no longer receive callbacks.
     * @see #registerSliceCallback
     */
    public abstract void unregisterSliceCallback(@NonNull Uri uri, @NonNull SliceCallback callback);

    /**
     * Ensures that a slice is in a pinned state.
     * <p>
     * Pinned state is not persisted across reboots, so apps are expected to re-pin any slices
     * they still care about after a reboot.
     *
     * @param uri The uri of the slice being pinned.
     * @see SliceProvider#onSlicePinned(Uri)
     */
    public abstract void pinSlice(@NonNull Uri uri);

    /**
     * Remove a pin for a slice.
     * <p>
     * If the slice has no other pins/callbacks then the slice will be unpinned.
     *
     * @param uri The uri of the slice being unpinned.
     * @see #pinSlice
     * @see SliceProvider#onSliceUnpinned(Uri)
     */
    public abstract void unpinSlice(@NonNull Uri uri);

    /**
     * Get the current set of specs for a pinned slice.
     * <p>
     * This is the set of specs supported for a specific pinned slice. It will take
     * into account all clients and returns only specs supported by all.
     * @see SliceSpec
     */
    public abstract @NonNull Set<SliceSpec> getPinnedSpecs(@NonNull Uri uri);

    /**
     * Turns a slice Uri into slice content.
     *
     * @param uri The URI to a slice provider
     * @return The Slice provided by the app or null if none is given.
     * @see Slice
     */
    public abstract @Nullable Slice bindSlice(@NonNull Uri uri);

    /**
     * Turns a slice intent into slice content. Is a shortcut to perform the action
     * of both {@link #mapIntentToUri(Intent)} and {@link #bindSlice(Uri)} at once.
     *
     * @param intent The intent associated with a slice.
     * @return The Slice provided by the app or null if none is given.
     * @see Slice
     * @see androidx.slice.SliceProvider#onMapIntentToUri(Intent)
     * @see Intent
     */
    public abstract @Nullable Slice bindSlice(@NonNull Intent intent);

    /**
     * Turns a slice intent into a slice uri. Expects an explicit intent.
     * <p>
     * This goes through a several stage resolution process to determine if any slice
     * can represent this intent.
     * <ol>
     *  <li> If the intent contains data that {@link android.content.ContentResolver#getType} is
     *  {@link android.app.slice.SliceProvider#SLICE_TYPE} then the data will be returned.</li>
     *  <li>If the intent explicitly points at an activity, and that activity has
     *  meta-data for key {@link android.app.slice.SliceManager#SLICE_METADATA_KEY},
     *  then the Uri specified there will be returned.</li>
     *  <li>Lastly, if the intent with {@link android.app.slice.SliceManager#CATEGORY_SLICE} added
     *  resolves to a provider, then the provider will be asked to
     *  {@link SliceProvider#onMapIntentToUri} and that result will be returned.</li>
     *  <li>If no slice is found, then {@code null} is returned.</li>
     * </ol>
     * @param intent The intent associated with a slice.
     * @return The Slice Uri provided by the app or null if none exists.
     * @see Slice
     * @see SliceProvider#onMapIntentToUri(Intent)
     * @see Intent
     */
    public abstract @Nullable Uri mapIntentToUri(@NonNull Intent intent);

    /**
     * Determine whether a particular process and user ID has been granted
     * permission to access a specific slice URI.
     *
     * @param uri The uri that is being checked.
     * @param pid The process ID being checked against.  Must be &gt; 0.
     * @param uid The user ID being checked against.  A uid of 0 is the root
     * user, which will pass every permission check.
     *
     * @return {@link PackageManager#PERMISSION_GRANTED} if the given
     * pid/uid is allowed to access that uri, or
     * {@link PackageManager#PERMISSION_DENIED} if it is not.
     *
     * @see #grantSlicePermission(String, Uri)
     */
    @PermissionChecker.PermissionResult
    public abstract int checkSlicePermission(@NonNull Uri uri, int pid, int uid);

    /**
     * Grant permission to access a specific slice Uri to another package.
     *
     * @param toPackage The package you would like to allow to access the Uri.
     * @param uri The Uri you would like to grant access to.
     *
     * @see #revokeSlicePermission
     */
    public abstract void grantSlicePermission(@NonNull String toPackage, @NonNull Uri uri);

    /**
     * Remove permissions to access a particular content provider Uri
     * that were previously added with {@link #grantSlicePermission} for a specific target
     * package.  The given Uri will match all previously granted Uris that are the same or a
     * sub-path of the given Uri.  That is, revoking "content://foo/target" will
     * revoke both "content://foo/target" and "content://foo/target/sub", but not
     * "content://foo".  It will not remove any prefix grants that exist at a
     * higher level.
     *
     * @param toPackage The package you would like to allow to access the Uri.
     * @param uri The Uri you would like to revoke access to.
     *
     * @see #grantSlicePermission
     */
    public abstract void revokeSlicePermission(@NonNull String toPackage, @NonNull Uri uri);

    /**
     * Obtains a list of slices that are descendants of the specified Uri.
     * <p>
     * Not all slice providers will implement this functionality, in which case,
     * an empty collection will be returned.
     *
     * @param uri The uri to look for descendants under.
     * @return All slices within the space.
     * @see SliceProvider#onGetSliceDescendants(Uri)
     */
    public abstract @NonNull Collection<Uri> getSliceDescendants(@NonNull Uri uri);

    /**
     * Get the list of currently pinned slices for this app.
     * @see SliceProvider#onSlicePinned
     */
    public abstract @NonNull List<Uri> getPinnedSlices();

    /**
     * Class that listens to changes in {@link Slice}s.
     */
    public interface SliceCallback {

        /**
         * Called when slice is updated.
         *
         * @param s The updated slice.
         * @see #registerSliceCallback
         */
        void onSliceUpdated(@NonNull Slice s);
    }
}
