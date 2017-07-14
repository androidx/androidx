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

package android.arch.paging.integration.testapp.lazylist;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.paging.integration.testapp.Item;
import android.arch.paging.integration.testapp.ItemCountedDataSource;
import android.arch.util.paging.CountedDataSource;
import android.arch.util.paging.LazyList;
import android.arch.util.paging.ListConfig;
import android.arch.util.paging.LiveLazyListProvider;

/**
 * Sample ViewModel backed by an artificial data source
 */
public class LazyListItemViewModel extends ViewModel {
    private final LiveData<LazyList<Item>> mLiveLazyList;
    private ItemCountedDataSource mDataSource;

    public LazyListItemViewModel() {
        mLiveLazyList = new LiveLazyListProvider<Item>() {
            @Override
            protected CountedDataSource<Item> createDataSource() {
                mDataSource = new ItemCountedDataSource();
                return mDataSource;
            }
        }.create(ListConfig.builder().pageSize(20).prefetchDistance(40).create());
    }

    void invalidateList() {
        if (mDataSource != null) {
            mDataSource.invalidate();
        }
    }

    LiveData<LazyList<Item>> getLazyList() {
        return mLiveLazyList;
    }
}
