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

package android.support.v7.recyclerview.extensions;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Utility class for binding a {@link LiveData} of List or PagedList to an AdapterHelper or Adapter.
 * <pre>
 * class MyActivity extends Activity implements LifecycleRegistryOwner {
 *     {@literal @}Override
 *     public void onCreate(Bundle savedState) {
 *         ...
 *         RecyclerView recyclerView = findViewById(R.id.user_list);
 *         UserAdapter&lt;User> adapter = new UserAdapter();
 *         LiveData&lt;PagedList&lt;<User>> liveUserList = myViewModel.getLiveUsersPaged();
 *         LiveListAdapterUtil.bind(liveUserList, this, adapter);
 *         ...
 *     }
 * }</pre>
 */
public class LiveListAdapterUtil {
    private LiveListAdapterUtil() {
        // utility class, no instance
    }

    /**
     * Connect the {@code LiveData<List<T>>} as a data source for the adapter or adapter helper,
     * under the lifecycle of the provided LifecycleOwner.
     *
     * @param liveData Source of data, will be observed by the adapter while the lifecycleOwner
     *                 remains active.
     * @param lifecycleOwner LifecycleOwner, defining when the liveData should be observed.
     * @param adapterOrHelper Adapter, or AdapterHelper that will consume the Lists
     *                        from the LiveData.
     */
    public static <L extends List<T>, T> void bind(
            LiveData<L> liveData,
            LifecycleOwner lifecycleOwner,
            final ListReceiver<L> adapterOrHelper) {
        lifecycleOwner.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void clear() {
                adapterOrHelper.setList(null);
            }
        });
        liveData.observe(lifecycleOwner, new Observer<L>() {
            @Override
            public void onChanged(@Nullable L value) {
                adapterOrHelper.setList(value);
            }
        });
    }

}
