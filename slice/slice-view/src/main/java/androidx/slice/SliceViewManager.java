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
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import java.util.Collection;
import java.util.concurrent.Executor;

/**
 * Class to handle interactions with {@link Slice}s.
 * <p>
 * The SliceViewManager manages permissions and pinned state for slices.
 */
@RequiresApi(19)
public abstract class SliceViewManager {

    /**
     * Get a {@link SliceViewManager}.
     */
    public static @NonNull SliceViewManager getInstance(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 28) {
            return new SliceViewManagerWrapper(context);
        } else {
            return new SliceViewManagerCompat(context);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    SliceViewManager() {
    }

    /**
     * Adds a callback to a specific slice uri.
     * <p>
     * This is a convenience method that performs a few slice actions at once. It will put
     * the slice in a pinned state since there is a callback attached. It will also
     * listen for content changes, when a content change is observed, the android system
     * will bind the new slice and provide it to all registered {@link SliceCallback}s.
     * <p>
     * This will not trigger a bindSlice immediately, it will only perform a bind and pass
     * it to the callback after a change occurs. To avoid race conditions and missing data,
     * callers should call bindSlice immediately after calling registerSliceCallback so that
     * it has the current slice.
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
     * Obtains a list of slices that are descendants of the specified Uri.
     * <p>
     * Not all slice providers will implement this functionality, in which case,
     * an empty collection will be returned.
     *
     * @param uri The uri to look for descendants under.
     * @return All slices within the space.
     * @see SliceProvider#onGetSliceDescendants(Uri)
     */
    @WorkerThread
    public abstract @NonNull Collection<Uri> getSliceDescendants(@NonNull Uri uri);

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
        void onSliceUpdated(@Nullable Slice s);
    }
}
