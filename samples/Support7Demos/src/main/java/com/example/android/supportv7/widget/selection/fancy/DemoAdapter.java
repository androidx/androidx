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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DemoAdapter extends RecyclerView.Adapter<DemoHolder> {

    public static final int TYPE_HEADER = 1;
    public static final int TYPE_ITEM = 2;

    private final KeyProvider mKeyProvider;
    private final Context mContext;
    // Our list of thingies. Our DemoHolder subclasses extract display
    // values directly from the Uri, so we only need this simple list.
    // The list also contains entries for alphabetical section headers.
    private final List<Uri> mCheeses;

    // This default implementation must be replaced
    // with a real implementation in #bindSelectionHelper.
    private SelectionTest mSelTest = new SelectionTest() {
        @Override
        public boolean isSelected(Uri id) {
            throw new IllegalStateException(
                    "Adapter must be initialized with SelectionTracker");
        }
    };

    DemoAdapter(Context context) {
        mContext = context;
        mCheeses = createCheeseList("CheeseKindom");
        mKeyProvider = new KeyProvider(mCheeses);

        // In the fancy edition of selection support we supply access to stable
        // ids using content URI. Since we can map between position and selection key
        // at-will we get band selection and range support.
        setHasStableIds(false);
    }

    ItemKeyProvider<Uri> getItemKeyProvider() {
        return mKeyProvider;
    }

    // Glue together SelectionTracker and the adapter.
    public void bindSelectionTracker(final SelectionTracker<Uri> tracker) {
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
        return mCheeses.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull DemoHolder holder, int position) {
        if (holder instanceof DemoHeaderHolder) {
            Uri uri = mKeyProvider.getKey(position);
            ((DemoHeaderHolder) holder).update(uri.getPathSegments().get(0));
        } else if (holder instanceof DemoItemHolder) {
            Uri uri = mKeyProvider.getKey(position);
            ((DemoItemHolder) holder).update(uri, uri.getPathSegments().get(1),
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
    public DemoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_HEADER:
                return new DemoHeaderHolder(
                        inflateLayout(mContext, parent, R.layout.selection_demo_list_header));
            case TYPE_ITEM:
                return new DemoItemHolder(
                        inflateLayout(mContext, parent, R.layout.selection_demo_list_item));
        }
        throw new RuntimeException("Unsupported view type" + viewType);
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")  // Convenience to avoid clumsy cast.
    private static <V extends View> V inflateLayout(
            Context context, ViewGroup parent, int layout) {

        return (V) LayoutInflater.from(context).inflate(layout, parent, false);
    }

    // Creates a list of cheese Uris and section header Uris.
    private static List<Uri> createCheeseList(String authority) {
        List<Uri> cheeses = new ArrayList<>();
        char section = '-';  // any ol' value other than 'a' will do the trick here.

        for (String cheese : Cheeses.sCheeseStrings) {
            char leadingChar = cheese.toLowerCase().charAt(0);

            // When we find a new leading character insert an artificial
            // cheese header
            if (leadingChar != section) {
                section = leadingChar;
                Uri headerUri = new Uri.Builder()
                        .scheme("content")
                        .encodedAuthority(authority)
                        .appendPath(Character.toString(section))
                        .build();

                cheeses.add(headerUri);
            }

            Uri itemUri = new Uri.Builder()
                    .scheme("content")
                    .encodedAuthority(authority)
                    .appendPath(Character.toString(section))
                    .appendPath(cheese)
                    .build();
            cheeses.add(itemUri);
        }

        return cheeses;
    }

    private interface SelectionTest {
        boolean isSelected(Uri id);
    }

    public boolean removeItem(Uri key) {
        int position = mKeyProvider.getPosition(key);
        if (position == RecyclerView.NO_POSITION) {
            return false;
        }

        @Nullable Uri removed = mCheeses.remove(position);
        notifyItemRemoved(position);
        return removed != null;
    }

    /**
     * When ever possible provide the selection library with a
     * "SCOPED_MAPPED" ItemKeyProvider. This enables the selection
     * library to provide ChromeOS friendly features such as mouse-driven
     * band selection.
     *
     * Background: SCOPED_MAPPED providers allow the library to access
     * an item's key or position independently of how the data is
     * represented in the RecyclerView. This is useful in that it
     * allows the library to operate on items that are not currently laid
     * out in RecyclerView.
     */
    static final class KeyProvider extends ItemKeyProvider<Uri> {

        private final List<Uri> mData;

        KeyProvider(List<Uri> cheeses) {
            // Advise the world we can supply ids/position for any item at any time,
            // not just when visible in RecyclerView.
            // This enables fancy stuff especially helpful to users with pointy
            // devices like Chromebooks, or tablets with touch pads
            super(SCOPE_MAPPED);
            mData = cheeses;
        }

        @Override
        public @Nullable Uri getKey(int position) {
            return mData.get(position);
        }

        @Override
        public int getPosition(@NonNull Uri key) {
            int position = Collections.binarySearch(mData, key);
            // position is insertion point if key is missing.
            // Since the insertion point could be end of the list + 1
            // both verify the position is in bounds, and that the value
            // at position is the same as the key.
            return position >= 0 && position <= mData.size() - 1 && key.equals(mData.get(position))
                    ? position
                    : RecyclerView.NO_POSITION;
        }
    }
}
