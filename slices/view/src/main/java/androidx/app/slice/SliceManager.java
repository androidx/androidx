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

package androidx.app.slice;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.os.BuildCompat;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Class to handle interactions with {@link Slice}s.
 * <p>
 * The SliceManager manages permissions and pinned state for slices.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class SliceManager {

    /**
     * Get a {@link SliceManager}.
     */
    @SuppressWarnings("NewApi")
    public static @NonNull SliceManager get(@NonNull Context context) {
        if (BuildCompat.isAtLeastP()) {
            return new SliceManagerWrapper(context);
        } else {
            return new SliceManagerCompat(context);
        }
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
     * @hide
     * @see SliceSpec
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract @NonNull List<SliceSpec> getPinnedSpecs(@NonNull Uri uri);

    /**
     * Turns a slice Uri into slice content.
     *
     * @param uri The URI to a slice provider
     * @return The Slice provided by the app or null if none is given.
     * @see Slice
     */
    public abstract @Nullable Slice bindSlice(@NonNull Uri uri);

    /**
     * Turns a slice intent into slice content. Expects an explicit intent. If there is no
     * {@link android.content.ContentProvider} associated with the given intent this will throw
     * {@link IllegalArgumentException}.
     *
     * @param intent The intent associated with a slice.
     * @return The Slice provided by the app or null if none is given.
     * @see Slice
     * @see androidx.app.slice.SliceProvider#onMapIntentToUri(Intent)
     * @see Intent
     */
    public abstract @Nullable Slice bindSlice(@NonNull Intent intent);

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
