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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnChildAttachStateChangeListener;
import android.util.SparseArray;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/**
 * ItemKeyProvider that provides stable ids by way of cached RecyclerView.Adapter stable ids.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class StableIdKeyProvider extends ItemKeyProvider<Long> {

    private final SparseArray<Long> mPositionToKey = new SparseArray<>();
    private final Map<Long, Integer> mKeyToPosition = new HashMap<Long, Integer>();
    private final RecyclerView mRecView;

    public StableIdKeyProvider(RecyclerView recView) {

        // Since this provide is based on stable ids based on whats laid out in the window
        // we can only satisfy "window" scope key access.
        super(SCOPE_CACHED);

        mRecView = recView;

        mRecView.addOnChildAttachStateChangeListener(
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

    private void onAttached(View view) {
        RecyclerView.ViewHolder holder = mRecView.findContainingViewHolder(view);
        int position = holder.getAdapterPosition();
        long id = holder.getItemId();
        if (position != RecyclerView.NO_POSITION && id != RecyclerView.NO_ID) {
            mPositionToKey.put(position, id);
            mKeyToPosition.put(id, position);
        }
    }

    private void onDetached(View view) {
        RecyclerView.ViewHolder holder = mRecView.findContainingViewHolder(view);
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
    public int getPosition(Long key) {
        if (mKeyToPosition.containsKey(key)) {
            return mKeyToPosition.get(key);
        }
        return RecyclerView.NO_POSITION;
    }
}
