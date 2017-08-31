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

package android.arch.paging.integration.testapp;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListProvider;
import android.arch.paging.PagedList;

/**
 * Sample ViewModel backed by an artificial data source
 */
@SuppressWarnings("WeakerAccess")
public class PagedListItemViewModel extends ViewModel {
    private LiveData<PagedList<Item>> mLivePagedList;
    private ItemDataSource mDataSource;
    private final Object mDataSourceLock = new Object();

    void invalidateList() {
        synchronized (mDataSourceLock) {
            if (mDataSource != null) {
                mDataSource.invalidate();
            }
        }
    }

    LiveData<PagedList<Item>> getLivePagedList() {
        if (mLivePagedList == null) {
            mLivePagedList = new LivePagedListProvider<Integer, Item>() {
                @Override
                protected DataSource<Integer, Item> createDataSource() {
                    ItemDataSource newDataSource = new ItemDataSource();
                    synchronized (mDataSourceLock) {
                        mDataSource = newDataSource;
                        return mDataSource;
                    }
                }
            }.create(0, 20);
        }

        return mLivePagedList;
    }
}
