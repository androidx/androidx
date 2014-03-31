/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Adapter implemented with an {@link ArrayList}.
 */
public class ArrayObjectAdapter extends ObjectAdapter {

    private ArrayList<Object> mItems = new ArrayList<Object>();

    public ArrayObjectAdapter(PresenterSelector presenterSelector) {
        super(presenterSelector);
    }

    public ArrayObjectAdapter(Presenter presenter) {
        super(presenter);
    }

    public ArrayObjectAdapter() {
        super();
    }

    @Override
    public int size() {
        return mItems.size();
    }

    @Override
    public Object get(int index) {
        return mItems.get(index);
    }

    /**
     * Adds an item to the end of the list.
     *
     * @param item The item to add to the end of the list.
     */
    public void add(Object item) {
        add(mItems.size(), item);
    }

    /**
     * Inserts an item into this list at the specified index.
     *
     * @param index The index at which the item should be inserted.
     * @param item The item to insert into the list.
     */
    public void add(int index, Object item) {
        mItems.add(index, item);
        notifyItemRangeInserted(index, 1);
    }

    /**
     * Adds the objects in the given collection to the list, starting at the
     * given index.
     *
     * @param index The index at which the items should be inserted.
     * @param items A {@link Collection} of items to insert.
     */
    public void addAll(int index, Collection items) {
        int itemsCount = items.size();
        mItems.addAll(index, items);
        notifyItemRangeInserted(index, itemsCount);
    }

    /**
     * Removes the first occurrence of the given item from the list.
     *
     * @param item The item to remove from the list.
     * @return True if the item was found and thus removed from the list.
     */
    public boolean remove(Object item) {
        int index = mItems.indexOf(item);
        if (index >= 0) {
            mItems.remove(index);
            notifyItemRangeRemoved(index, 1);
        }
        return index >= 0;
    }

    /**
     * Removes a range of items from the list. The range is specified by giving
     * the starting position and the number of elements to remove.
     *
     * @param position The index of the first item to remove.
     * @param count The number of items to remove.
     * @return The number of items removed.
     */
    public int removeItems(int position, int count) {
        int itemsToRemove = Math.min(count, mItems.size() - position);

        for (int i = 0; i < itemsToRemove; i++) {
            mItems.remove(position);
        }
        notifyItemRangeRemoved(position, itemsToRemove);
        return itemsToRemove;
    }

    /**
     * Removes all items from this list, leaving it empty.
     */
    public void clear() {
        int itemCount = mItems.size();
        mItems.clear();
        notifyItemRangeRemoved(0, itemCount);
    }
}
