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

import android.util.SparseArray;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener;

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link ItemKeyProvider} that provides stable ids by way of cached
 * {@link RecyclerView.Adapter} stable ids. Items enter the cache as they are laid out by
 * RecyclerView, and are removed from the cache as they are recycled.
 *
 * <p>
 * There are trade-offs with this implementation as it necessarily auto-boxes {@code long}
 * stable id values into {@code Long} values for use as selection keys. The core Selection API
 * uses a parameterized key type to permit other keys (such as Strings or URIs).
 */
public final class StableIdKeyProvider extends ItemKeyProvider<Long> {

    private final SparseArray<Long> mPositionToKey = new SparseArray<>();
    private final Map<Long, Integer> mKeyToPosition = new HashMap<Long, Integer>();
    private final RecyclerView mRecyclerView;

    /**
     * Creates a new key provider that uses cached {@code long} stable ids associated
     * with the RecyclerView items.
     *
     * @param recyclerView the owner RecyclerView
     */
    public StableIdKeyProvider(@NonNull RecyclerView recyclerView) {

        // Since this provide is based on stable ids based on whats laid out in the window
        // we can only satisfy "window" scope key access.
        super(SCOPE_CACHED);

        mRecyclerView = recyclerView;

        mRecyclerView.addOnChildAttachStateChangeListener(
                new OnChildAttachStateChangeListener() {
                    @Override
                    public void onChildViewAttachedToWindow(View view) {
                        onAttached(view);
                    }

                    @Override
                    public void onChildViewDetachedFromWindow(View view) {
                        onDetached(view);
                    }
                }
        );

    }

    private void onAttached(@NonNull View view) {
        RecyclerView.ViewHolder holder = mRecyclerView.findContainingViewHolder(view);
        int position = holder.getAdapterPosition();
        long id = holder.getItemId();
        if (position != RecyclerView.NO_POSITION && id != RecyclerView.NO_ID) {
            mPositionToKey.put(position, id);
            mKeyToPosition.put(id, position);
        }
    }

    private void onDetached(@NonNull View view) {
        RecyclerView.ViewHolder holder = mRecyclerView.findContainingViewHolder(view);
        int position = holder.getAdapterPosition();
        long id = holder.getItemId();
        if (position != RecyclerView.NO_POSITION && id != RecyclerView.NO_ID) {
            mPositionToKey.delete(position);
            mKeyToPosition.remove(id);
        }
    }

    @Override
    public @Nullable Long getKey(int position) {
        return mPositionToKey.get(position, null);
    }

    @Override
    public int getPosition(@NonNull Long key) {
        if (mKeyToPosition.containsKey(key)) {
            return mKeyToPosition.get(key);
        }
        return RecyclerView.NO_POSITION;
    }
}
