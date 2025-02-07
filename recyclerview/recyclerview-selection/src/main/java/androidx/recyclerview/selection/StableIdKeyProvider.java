/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.recyclerview.selection;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;
import static androidx.recyclerview.selection.Shared.DEBUG;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import androidx.collection.LongSparseArray;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener;
import androidx.recyclerview.widget.RecyclerView.RecyclerListener;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An {@link ItemKeyProvider} that provides item keys by way of native
 * {@link RecyclerView.Adapter} stable ids.
 *
 * <p>The corresponding RecyclerView.Adapter instance must:
 * <ol>
 *     <li> Enable stable ids using {@link RecyclerView.Adapter#setHasStableIds(boolean)}
 *     <li> Override {@link RecyclerView.Adapter#getItemId(int)} with a real implementation.
 * </ol>
 *
 * <p>
 * There are trade-offs with this implementation:
 * <ul>
 *     <li>It necessarily auto-boxes {@code long} stable id values into {@code Long} values for
 *     use as selection keys.
 *     <li>It deprives Chromebook users (actually, any device with an attached pointer) of support
 *     for band-selection.
 * </ul>
 *
 * <p>See com.example.android.supportv7.widget.selection.fancy.DemoAdapter.KeyProvider in the
 * SupportV7 Demos package for an example of how to implement a better ItemKeyProvider.
 */
public final class StableIdKeyProvider extends ItemKeyProvider<Long> {

    private static final String TAG = "StableIdKeyProvider";

    private final SparseArray<Long> mPositionToKey = new SparseArray<>();
    private final LongSparseArray<Integer> mKeyToPosition = new LongSparseArray<>();
    private final ViewHost mHost;

    StableIdKeyProvider(@NonNull ViewHost host) {
        // Provider is based on the stable ids provided by ViewHolders which
        // are only accessible when the holders are attached or yet-to-be-recycled.
        // For that reason we can only satisfy "CACHED" scope key access which
        // limits library features such as mouse-driven band selection.
        super(SCOPE_CACHED);

        checkNotNull(host);
        mHost = host;

        mHost.registerLifecycleListener(
                new ViewHost.LifecycleListener() {
                    @Override
                    public void onAttached(@NonNull View view) {
                        StableIdKeyProvider.this.onAttached(view);
                    }

                    @Override
                    public void onRecycled(@NonNull View view) {
                        StableIdKeyProvider.this.onRecycled(view);
                    }
                }
        );
    }

    /**
     * Creates a new key provider that uses cached {@code long} stable ids associated
     * with the RecyclerView items.
     *
     * @param recyclerView the owner RecyclerView
     */
    public StableIdKeyProvider(@NonNull RecyclerView recyclerView) {
        this(new DefaultViewHost(recyclerView));

        // Adapters used w/ StableIdKeyProvider MUST have StableIds enabled.
        checkArgument(recyclerView.getAdapter().hasStableIds(), "RecyclerView"
                + ".Adapter#hasStableIds must return true.");
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onAttached(@NonNull View view) {
        ViewHolder holder = mHost.findViewHolder(view);
        if (holder == null) {
            if (DEBUG) {
                Log.w(TAG, "Unable to find ViewHolder for View. Ignoring onAttached event.");
            }
            return;
        }
        int position = mHost.getPosition(holder);
        long id = holder.getItemId();
        if (position != RecyclerView.NO_POSITION && id != RecyclerView.NO_ID) {
            mPositionToKey.put(position, id);
            mKeyToPosition.put(id, position);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onRecycled(@NonNull View view) {
        ViewHolder holder = mHost.findViewHolder(view);
        if (holder == null) {
            if (DEBUG) {
                Log.w(TAG, "Unable to find ViewHolder for View. Ignoring onDetached event.");
            }
            return;
        }
        int position = mHost.getPosition(holder);
        long id = holder.getItemId();
        if (position != RecyclerView.NO_POSITION && id != RecyclerView.NO_ID) {
            mPositionToKey.delete(position);
            mKeyToPosition.remove(id);
        }
    }

    @Override
    public @Nullable Long getKey(int position) {
        // TODO: Consider using RecyclerView.NO_ID for consistency w/ getPosition impl.
        // Currently GridModel impl depends on null return values.
        return mPositionToKey.get(position, null);
    }

    @Override
    public int getPosition(@NonNull Long key) {
        return mKeyToPosition.get(key, RecyclerView.NO_POSITION);
    }

    /**
     * A wrapper interface for RecyclerView allowing for easy unit testing.
     */
    interface ViewHost {
        /** Registers View{Holder} lifecycle event listener. */
        void registerLifecycleListener(@NonNull LifecycleListener listener);

        /**
         * Returns the ViewHolder containing {@code View}.
         */
        @Nullable ViewHolder findViewHolder(@NonNull View view);

        /**
         * Returns the position of the ViewHolder, or RecyclerView.NO_POSITION
         * if unknown.
         *
         * This method supports testing of StableIdKeyProvider independent of
         * a real RecyclerView instance. The correct runtime implementation is
         * {@code return holder.getAbsoluteAdapterPosition}. This implementation
         * depends on a concrete RecyclerView instance, which isn't test friendly
         * given the testing approach in StableIdKeyProviderTest. Thus the
         * introduction of this interface method allowing a test double to
         * supply adapter position as needed to test.
         */
        int getPosition(@NonNull ViewHolder holder);

        /** A View{Holder} lifecycle listener interface. */
        interface LifecycleListener {

            /** Called when view is attached. */
            void onAttached(@NonNull View view);

            /** Called when view is recycled. */
            void onRecycled(@NonNull View view);
        }
    }

    /**
     * Implementation of ViewHost that wraps a RecyclerView instance.
     */
    private static class DefaultViewHost implements ViewHost {
        private final @NonNull RecyclerView mRecyclerView;

        DefaultViewHost(@NonNull RecyclerView recyclerView) {
            checkNotNull(recyclerView);
            mRecyclerView = recyclerView;
        }

        @Override
        public void registerLifecycleListener(@NonNull LifecycleListener listener) {

            mRecyclerView.addOnChildAttachStateChangeListener(
                    new OnChildAttachStateChangeListener() {
                        @Override
                        public void onChildViewAttachedToWindow(@NonNull View view) {
                            listener.onAttached(view);
                        }

                        @Override
                        public void onChildViewDetachedFromWindow(@NonNull View view) {
                            // Cached position <> key data is discarded only when
                            // a view is recycled. See b/145767095 for details.
                        }
                    }
            );

            mRecyclerView.addRecyclerListener(
                    new RecyclerListener() {
                        @Override
                        public void onViewRecycled(@NonNull ViewHolder holder) {
                            listener.onRecycled(holder.itemView);
                        }
                    }
            );
        }

        @Override
        public @Nullable ViewHolder findViewHolder(@NonNull View view) {
            return mRecyclerView.findContainingViewHolder(view);
        }

        @Override
        public int getPosition(@NonNull ViewHolder holder) {
            return holder.getAbsoluteAdapterPosition();
        }
    }
}
