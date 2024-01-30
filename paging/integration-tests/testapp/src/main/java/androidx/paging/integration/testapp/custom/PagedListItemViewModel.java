/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.paging.integration.testapp.custom;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PagingSource;

import kotlin.jvm.functions.Function0;

/**
 * Sample ViewModel backed by an artificial data source
 */
public class PagedListItemViewModel extends ViewModel {
    private ItemDataSource mDataSource;
    private final Object mDataSourceLock = new Object();

    private final Function0<PagingSource<Integer, Item>> mFactory =
            new Function0<PagingSource<Integer, Item>>() {
                @NonNull
                @Override
                public PagingSource<Integer, Item> invoke() {
                    ItemDataSource newDataSource = new ItemDataSource();
                    synchronized (mDataSourceLock) {
                        mDataSource = newDataSource;
                        return mDataSource;
                    }
                }
            };

    @SuppressWarnings("deprecation")
    private LiveData<PagedList<Item>> mLivePagedList =
            new LivePagedListBuilder<>(mFactory, 10).build();

    void invalidateList() {
        synchronized (mDataSourceLock) {
            if (mDataSource != null) {
                mDataSource.invalidate();
            }
        }
    }

    LiveData<PagedList<Item>> getLivePagedList() {
        return mLivePagedList;
    }
}
