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

package android.arch.util.paging;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import java.util.List;

public class LiveListAdapterUtil {
    /**
     * Connect the {@code LiveData<PagedList<T>>} as a data source for the adapter, under the
     * lifecycle of the provided LifecycleOwner.
     *
     * @param liveData Source of data, will be observed by the adapter while the lifecycleOwner
     *                 remains active.
     * @param lifecycleOwner LifecycleOwner, defining when the liveData should be observed.
     * @param adapter Adapter that will consume the PagedLists from the LiveData.
     * @param <T> Type of item in the paged list, to be provided to the adapter.
     * @param <VH> ViewHolder type of the Adapter.
     */
    public static <T, VH extends RecyclerView.ViewHolder> void observe(
            LiveData<PagedList<T>> liveData,
            LifecycleOwner lifecycleOwner,
            final PagedListAdapter<T, VH> adapter) {
        lifecycleOwner.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void clear() {
                adapter.setPagedList(null);
            }
        });
        liveData.observe(lifecycleOwner, new Observer<PagedList<T>>() {
            @Override
            public void onChanged(@Nullable PagedList<T> valuePagedList) {
                adapter.setPagedList(valuePagedList);
            }
        });
    }

    @SuppressWarnings("unused")
    public static <T> void observe(
            LiveData<PagedList<T>> liveData,
            LifecycleOwner lifecycleOwner,
            final PagedListAdapterHelper<T> helper) {
        lifecycleOwner.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void clear() {
                helper.setPagedList(null);
            }
        });
        liveData.observe(lifecycleOwner, new Observer<PagedList<T>>() {
            @Override
            public void onChanged(@Nullable PagedList<T> valuePagedList) {
                helper.setPagedList(valuePagedList);
            }
        });
    }

    @SuppressWarnings("unused")
    public static <T, VH extends RecyclerView.ViewHolder> void observe(
            LiveData<List<T>> liveData,
            LifecycleOwner lifecycleOwner,
            final ListAdapter<T, VH> adapter) {
        lifecycleOwner.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void clear() {
                adapter.setList(null);
            }
        });
        liveData.observe(lifecycleOwner, new Observer<List<T>>() {
            @Override
            public void onChanged(@Nullable List<T> valueList) {
                adapter.setList(valueList);
            }
        });
    }

    @SuppressWarnings("unused")
    public static <T> void observe(
            LiveData<List<T>> liveData,
            LifecycleOwner lifecycleOwner,
            final ListAdapterHelper<T> helper) {
        lifecycleOwner.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void clear() {
                helper.setList(null);
            }
        });
        liveData.observe(lifecycleOwner, new Observer<List<T>>() {
            @Override
            public void onChanged(@Nullable List<T> valueList) {
                helper.setList(valueList);
            }
        });
    }
}
