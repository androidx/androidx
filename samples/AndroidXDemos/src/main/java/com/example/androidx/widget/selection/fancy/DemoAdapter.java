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

package com.example.androidx.widget.selection.fancy;

import static androidx.core.util.Preconditions.checkArgument;

import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Predicate;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidx.Cheeses;

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
    private final List<Uri> mCheeses = new ArrayList<>();
    private boolean mSmallItemLayout;
    private boolean mAllCheesesEnabled;

    // This default implementation must be replaced
    // with a real implementation in #bindSelectionHelper.
    private Predicate<Uri> mIsSelectedTest = new Predicate<Uri>() {
        @Override
        public boolean test(Uri key) {
            throw new IllegalStateException(
                    "Adapter must be initialized with SelectionTracker");
        }
    };

    DemoAdapter(Context context) {
        mContext = context;
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
        mIsSelectedTest = new Predicate<Uri>() {
            @Override
            public boolean test(Uri key) {
                return tracker.isSelected(key);
            }
        };
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
        Uri uri = mKeyProvider.getKey(position);
        holder.update(uri);
        if (holder instanceof DemoItemHolder) {
            DemoItemHolder itemHolder = (DemoItemHolder) holder;
            itemHolder.setSelected(mIsSelectedTest.test(uri));
            itemHolder.setSmallLayoutMode(mSmallItemLayout);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Uri uri = mKeyProvider.getKey(position);
        if (Uris.isGroup(uri)) {
            return TYPE_HEADER;
        } else if (Uris.isCheese(uri)) {
            return TYPE_ITEM;
        }

        throw new RuntimeException("Unknown view type a position " + position);
    }

    @Override
    public DemoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_HEADER:
                return new DemoHeaderHolder(mContext, parent);
            case TYPE_ITEM:
                return new DemoItemHolder(mContext, parent);
        }
        throw new RuntimeException("Unsupported view type" + viewType);
    }

    // Creates a list of cheese Uris and section header Uris.
    private void populateCheeses(int maxItemsPerGroup) {
        String group = "-";  // any ol' value other than 'a' will do the trick here.
        int itemsInGroup = 0;

        for (String cheese : Cheeses.sCheeseStrings) {
            String leadingChar = Character.toString(cheese.toLowerCase().charAt(0));

            // When we find a new leading character insert an artificial
            // cheese header
            if (!leadingChar.equals(group)) {
                group = leadingChar;
                itemsInGroup = 0;
                mCheeses.add(Uris.forGroup(group));
            }
            if (++itemsInGroup <= maxItemsPerGroup) {
                mCheeses.add(Uris.forCheese(group, cheese));
            }
        }
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

    void enableSmallItemLayout(boolean enabled) {
        mSmallItemLayout = enabled;
    }

    void enableAllCheeses(boolean enabled) {
        mAllCheesesEnabled = enabled;
    }

    boolean smallItemLayoutEnabled() {
        return mSmallItemLayout;
    }

    boolean allCheesesEnabled() {
        return mAllCheesesEnabled;
    }



    void refresh() {
        mCheeses.clear();
        populateCheeses(mAllCheesesEnabled ? Integer.MAX_VALUE : 5);
        notifyDataSetChanged();
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

        KeyProvider(List<Uri> data) {
            // Advise the world we can supply ids/position for any item at any time,
            // not just when visible in RecyclerView.
            // This enables fancy stuff especially helpful to users with pointy
            // devices like Chromebooks, or tablets with touch pads
            super(SCOPE_MAPPED);
            mData = data;
        }

        @Override
        public @Nullable Uri getKey(int position) {
            return mData.get(position);
        }

        @Override
        public int getPosition(@NonNull Uri key) {
            int position = Collections.binarySearch(mData, key);
            return position >= 0 ? position : RecyclerView.NO_POSITION;
        }
    }
}
