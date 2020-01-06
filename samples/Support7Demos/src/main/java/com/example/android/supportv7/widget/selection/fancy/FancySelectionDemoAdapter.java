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

package com.example.android.supportv7.widget.selection.fancy;

import static androidx.core.util.Preconditions.checkArgument;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.R;

import java.util.HashMap;
import java.util.Map;

final class FancySelectionDemoAdapter extends RecyclerView.Adapter<FancyHolder> {

    public static final int TYPE_HEADER = 1;
    public static final int TYPE_ITEM = 2;

    private final KeyProvider mKeyProvider;
    private final Context mContext;

    // This default implementation must be replaced
    // with a real implementation in #bindSelectionHelper.
    private SelectionTest mSelTest = new SelectionTest() {
        @Override
        public boolean isSelected(Uri id) {
            throw new IllegalStateException(
                    "Adapter must be initialized with SelectionTracker");
        }
    };

    FancySelectionDemoAdapter(Context context) {
        mContext = context;
        mKeyProvider = new KeyProvider("cheeses", Cheeses.sCheeseStrings);

        // In the fancy edition of selection support we supply access to stable
        // ids using content URI. Since we can map between position and selection key
        // at-will we get band selection and range support.
        setHasStableIds(false);
    }

    ItemKeyProvider<Uri> getItemKeyProvider() {
        return mKeyProvider;
    }

    // Glue together SelectionTracker and the adapter.
    public void bindSelectionHelper(final SelectionTracker<Uri> tracker) {
        checkArgument(tracker != null);
        mSelTest = new SelectionTest() {
            @Override
            public boolean isSelected(Uri id) {
                return tracker.isSelected(id);
            }
        };
    }

    void loadData() {
        onDataReady();
    }

    private void onDataReady() {
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mKeyProvider.getCount();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull FancyHolder holder, int position) {
        if (holder instanceof FancyHeaderHolder) {
            Uri uri = mKeyProvider.getKey(position);
            ((FancyHeaderHolder) holder).update(uri.getPathSegments().get(0));
        } else if (holder instanceof FancyItemHolder) {
            Uri uri = mKeyProvider.getKey(position);
            ((FancyItemHolder) holder).update(uri, uri.getPathSegments().get(1),
                    mSelTest.isSelected(uri));
        }
    }

    @Override
    public int getItemViewType(int position) {
        Uri key = mKeyProvider.getKey(position);
        if (key.getPathSegments().size() == 1) {
            return TYPE_HEADER;
        } else if (key.getPathSegments().size() == 2) {
            return TYPE_ITEM;
        }

        throw new RuntimeException("Unknown view type a position " + position);
    }

    @Override
    public FancyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_HEADER:
                return new FancyHeaderHolder(
                        inflateLayout(mContext, parent, R.layout.selection_demo_list_header));
            case TYPE_ITEM:
                return new FancyItemHolder(
                        inflateLayout(mContext, parent, R.layout.selection_demo_list_item));
        }
        throw new RuntimeException("Unsupported view type" + viewType);
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")  // Convenience to avoid clumsy cast.
    private static <V extends View> V inflateLayout(
            Context context, ViewGroup parent, int layout) {

        return (V) LayoutInflater.from(context).inflate(layout, parent, false);
    }

    private interface SelectionTest {
        boolean isSelected(Uri id);
    }

    private static final class KeyProvider extends ItemKeyProvider<Uri> {

        private final Uri[] mUris;
        private final Map<Uri, Integer> mPositions;

        KeyProvider(String authority, String[] values) {
            // Advise the world we can supply ids/position for entire copus
            // at any time.
            super(SCOPE_MAPPED);

            // For the convenience of this demo, we simply trust, based on
            // past understanding that Cheeses has at least one element
            // starting with each letter of the English alphabet :)
            mUris = new Uri[Cheeses.sCheeseStrings.length + 26];
            mPositions = new HashMap<>();

            char section = '-';  // anything value other than 'a' will do the trick here.
            int headerOffset = 0;

            for (int i = 0; i < Cheeses.sCheeseStrings.length; i++) {
                char leadingChar = Cheeses.sCheeseStrings[i].toLowerCase().charAt(0);
                // When we find a new leading character insert an artificial
                // cheese header
                if (leadingChar != section) {
                    section = leadingChar;
                    mUris[i + headerOffset] = new Uri.Builder()
                            .scheme("content")
                            .encodedAuthority(authority)
                            .appendPath(Character.toString(section))
                            .build();
                    mPositions.put(mUris[i + headerOffset], i + headerOffset);
                    headerOffset++;
                }
                mUris[i + headerOffset] = new Uri.Builder()
                        .scheme("content")
                        .encodedAuthority(authority)
                        .appendPath(Character.toString(section))
                        .appendPath(Cheeses.sCheeseStrings[i])
                        .build();
                mPositions.put(mUris[i + headerOffset], i + headerOffset);
            }
        }

        @Override
        public @Nullable Uri getKey(int position) {
            return mUris[position];
        }

        @Override
        public int getPosition(@NonNull Uri key) {
            return mPositions.get(key);
        }

        int getCount() {
            return mUris.length;
        }
    }
}
