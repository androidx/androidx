/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.sample.githubbrowser.adapter;

import android.support.v7.util.DiffUtil;

import java.util.List;

/**
 * A DiffUtil callback implementation for lists which use equals for comparison.
 * @param <T>
 * @param <K>
 */
abstract class DiffUtilListCallback<T, K> extends DiffUtil.Callback {
    private final List<T> mOldList;
    private final List<T> mNewList;

    DiffUtilListCallback(List<T> oldList, List<T> newList) {
        mOldList = oldList;
        mNewList = newList;
    }

    @Override
    public int getOldListSize() {
        return mOldList.size();
    }

    @Override
    public int getNewListSize() {
        return mNewList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        K oldId = getId(mOldList.get(oldItemPosition));
        K newId = getId(mNewList.get(newItemPosition));
        return oldId.equals(newId);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldList.get(oldItemPosition).equals(
                mNewList.get(newItemPosition));
    }

    abstract K getId(T item);
}
