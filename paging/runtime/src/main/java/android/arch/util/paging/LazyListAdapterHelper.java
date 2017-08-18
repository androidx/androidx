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

import android.arch.core.executor.AppToolkitTaskExecutor;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;

import java.util.concurrent.Executor;

/**
 * Helper object for mapping a {@link LazyList} into a
 * {@link android.support.v7.widget.RecyclerView.Adapter RecyclerView.Adapter} - both the internal
 * paging of the list as more data is loaded, and updates in the form of new LazyLists.
 * <p>
 * The LazyListAdapterHelper can take a {@link LiveData} of LazyList and present the data simply for
 * an adapter. It listens to LazyList loading callbacks, and uses DiffUtil on a background thread to
 * compute updates as new LazyLists are received.
 * <p>
 * It provides a simple list-like API with {@link #get(int)} and {@link #getItemCount()} for an
 * adapter to acquire and present data objects.
 * <p>
 * A complete usage pattern with Room would look like this:
 * <pre>
 * {@literal @}Dao
 * interface UserDao {
 *     {@literal @}Query("SELECT * FROM user ORDER BY lastName ASC")
 *     LiveLazyListProvider&lt;User> usersByLastName();
 * }
 *
 * class MyViewModel extends ViewModel {
 *     public final LiveData&lt;LazyList&lt;User>> usersList;
 *     public MyViewModel(UserDao userDao) {
 *         usersList = userDao.usersByLastName().create(
 *                        ListConfig.builder()
 *                                  .pageSize(50)
 *                                  .prefetchDistance(50)
 *                                  .create());
 *     }
 * }
 *
 * class MyActivity extends Activity implements LifecycleRegistryOwner {
 *     {@literal @}Override
 *     public void onCreate(Bundle savedState) {
 *         super.onCreate(savedState);
 *         MyViewModel viewModel = ViewModelProviders.of(this).get(MyViewModel.class);
 *         RecyclerView recyclerView = findViewById(R.id.user_list);
 *         UserAdapter&lt;User> adapter = new UserAdapter(
 *                  LazyListAdapterHelper.&lt;User>builder()
 *                                       .lifecycle(this)
 *                                       .diffCallback(User.DIFF_CALLBACK)
 *                                       .source(viewModel.usersList));
 *         recyclerView.setAdapter(adapter);
 *     }
 * }
 *
 * class UserAdapter extends RecyclerView.Adapter&lt;UserViewHolder> {
 *     private final LazyListAdapterHelper&lt;User> helper;
 *     public UserAdapter(LazyListAdapterHelper.Builder&lt;User> builder) {
 *         helper = builder.adapter(this).create();
 *     }
 *     {@literal @}Override
 *     public int getItemCount() {
 *         return mHelper.getItemCount();
 *     }
 *     {@literal @}Override
 *     public void onBindViewHolder(UserViewHolder holder, int position) {
 *         User user = mHelper.get(position);
 *         if (user == null) {
 *             // AdapterHelper will automatically call invalidate on this row when the actual
 *             // object is loaded from the database
 *             holder.clear();
 *         } else {
 *             holder.bindTo(user);
 *         }
 *     }
 * }
 * </pre>
 *
 * @param <Value> Type of the LazyLists this helper will receive.
 */
public class LazyListAdapterHelper<Value> extends PagerBaseAdapterHelper<Value> {
    /**
     * set to false if the helper is observing a LiveData.
     */
    private final boolean mCanSetList;
    private LazyList.ChangeCallback mChangeCallback = new LazyList.ChangeCallback() {
        @Override
        public void onLoaded(int start, int count) {
            mListUpdateCallback.onChanged(start, count, null);
        }
    };

    private LazyListAdapterHelper(
            @NonNull Executor mainThreadExecutor, @NonNull Executor backgroundThreadExecutor,
            @NonNull ListUpdateCallback listUpdateCallback,
            @NonNull DiffCallback<Value> diffCallback, boolean canSetList) {
        super(mainThreadExecutor, backgroundThreadExecutor, listUpdateCallback, diffCallback);
        mCanSetList = canSetList;
    }

    /**
     * Returns the item at the given index.
     * <p>
     * Might be null if the item is not loaded yet.
     *
     * @param index The position of the item
     * @return The item at the given index or {@code null} if it is not loaded into memory yet.
     */
    @Override
    @Nullable
    public Value get(int index) {
        return super.get(index);
    }

    /**
     * Sets the lazy list for this adapter helper. If you are manually observing the
     * {@link LazyList} for changes, you should call this method with the new
     * {@link LazyList} when the previous one is invalidated.
     * <p>
     * Adapter helper will calculate the diff between this list and the previous one on a background
     * thread then replace the data with this one, while unsubscribing from the previous list and
     * subscribing to the new one.
     * <p>
     * If you have already provided a {@link LiveData} source via {@link Builder#source(LiveData)},
     * calling this method will throw an {@link IllegalStateException}.
     *
     * @param newList The new LazyList to observe.
     */
    @MainThread
    public void setLazyList(@Nullable LazyList<Value> newList) {
        if (mCanSetList) {
            setPagerBase(newList);
        } else {
            throw new IllegalStateException("When an AdapterHelper is observing a LiveData, you"
                    + " cannot set the list on it because it will be overridden by the LiveData"
                    + " source");
        }
    }

    @SuppressWarnings("WeakerAccess")
    @MainThread
    void internalSetLazyList(@Nullable LazyList<Value> newList) {
        setPagerBase(newList);
    }

    @Override
    void addCallback(PagerBase<Value> list) {
        ((LazyList<Value>) list).addCallback(mChangeCallback);
    }

    @Override
    void removeCallback(PagerBase<Value> list) {
        ((LazyList<Value>) list).removeCallback(mChangeCallback);
    }

    /**
     * Creates a {@link Builder} that can be used to construct a {@link LazyListAdapterHelper}.
     *
     * @param <Value> The type parameter for the {@link LazyListAdapterHelper}.
     * @return a new {@link Builder}.
     */
    public static <Value> Builder<Value> builder() {
        return new Builder<>();
    }


    /**
     * Builder class for {@link LazyListAdapterHelper}.
     * <p>
     * You must at least provide an {@link DiffCallback} and also one of the
     * {@link Builder#adapter(RecyclerView.Adapter)} or
     * {@link Builder#updateCallback(ListUpdateCallback)}.
     *
     * @param <Value> Data type held by the adapter helper.
     */
    @SuppressWarnings("WeakerAccess")
    public static class Builder<Value> {
        private DiffCallback<Value> mDiffCallback;
        private ListUpdateCallback mUpdateCallback;
        private LifecycleOwner mLifecycle;
        private LiveData<LazyList<Value>> mLiveData;
        private Executor mMainThreadExecutor;
        private Executor mBackgroundThreadExecutor;

        /**
         * Sets the {@link android.support.v7.widget.RecyclerView.Adapter RecyclerView.Adapter}
         * instance that will receive the update events.
         * <p>
         * If you have a more complex case where your adapter has additional items from different
         * data sources, you can use the {@link #updateCallback(ListUpdateCallback)} to manually
         * dispatch changes to your adapter.
         *
         * @param adapter The adapter to receive change/move/insert/remove updates when the data
         *                provided by the helper changes.
         */
        public Builder<Value> adapter(RecyclerView.Adapter adapter) {
            return updateCallback(new PagerBaseAdapterHelper.AdapterCallback(adapter));
        }

        /**
         * Sets the ListUpdateCallback that will receive updates as the data maintained by the
         * helper is updated.
         * <p>
         * In simple cases, you can instead pass your Adapter to
         * {@link #adapter(RecyclerView.Adapter)} to receive updates in the form of e.g.
         * {@link android.support.v7.widget.RecyclerView.Adapter#notifyItemRangeChanged(int, int)}
         * or
         * {@link android.support.v7.widget.RecyclerView.Adapter#notifyItemRangeInserted(int, int)}
         * on your adapter.
         *
         * @param callback The callback to receive change/move/insert/remove updates when the data
         *                 provided by the helper changes.
         */
        public Builder<Value> updateCallback(ListUpdateCallback callback) {
            mUpdateCallback = callback;
            return this;
        }

        /**
         * The {@link DiffCallback} to be used while diffing an old list with the updated one.
         * Must be provided.
         *
         * @param diffCallback The {@link DiffCallback} instance to compare items in the list.
         * @return this
         */
        public Builder<Value> diffCallback(DiffCallback<Value> diffCallback) {
            mDiffCallback = diffCallback;
            return this;
        }

        /**
         * Assigns a lifecycle to the {@link LazyListAdapterHelper} so that it can cancel the
         * observers automatically when the lifecycle is destroyed. This is especially useful if
         * the actual {@link LazyList} is owned by a
         * {@link android.arch.lifecycle.ViewModel ViewModel} or any other class that outlives
         * the {@link android.app.Activity Activity} or the
         * {@link android.support.v4.app.Fragment Fragment}.
         * <p>
         * Optional. If you don't provide this and the {@link LazyList} outlives the
         * {@link android.support.v7.widget.RecyclerView.Adapter Adapter}, you should call
         * {@link LazyListAdapterHelper#setLazyList(LazyList)} with {@code null} when the UI
         * element is destroyed.
         *
         * @param lifecycleOwner The {@link LifecycleOwner} where the adapter lives.
         * @return this
         */
        public Builder<Value> lifecycle(LifecycleOwner lifecycleOwner) {
            mLifecycle = lifecycleOwner;
            return this;
        }

        /**
         * If provided, the created {@link LazyListAdapterHelper} will observe the given
         * {@code source} automatically. If you provide a {@code source}, you must also provide a
         * {@link LifecycleOwner} via the {@link #lifecycle(LifecycleOwner)} method.
         *
         * @param source The LiveData source that should be observed.
         * @return this
         */
        public Builder<Value> source(LiveData<LazyList<Value>> source) {
            mLiveData = source;
            return this;
        }

        /**
         * If provided, {@link LazyListAdapterHelper} will use the given executor to execute adapter
         * update notifications on the main thread.
         * <p>
         * If not provided, it will default to the UI thread.
         *
         * @param executor The executor which can run tasks in the UI thread.
         * @return this
         */
        public Builder<Value> mainThreadExecutor(Executor executor) {
            mMainThreadExecutor = executor;
            return this;
        }

        /**
         * If provided, {@link LazyListAdapterHelper} will use the given executor to calculate the
         * diff between an old and a new list.
         * <p>
         * If not provided, defaults to the IO thread pool from Architecture Components.
         *
         * @param executor The background executor to run list diffing.
         * @return this
         */
        public Builder<Value> backgroundThreadExecutor(Executor executor) {
            mBackgroundThreadExecutor = executor;
            return this;
        }

        /**
         * Creates a {@link LazyListAdapterHelper} with the given parameters.
         *
         * @return A new LazyListAdapterHelper.
         */
        public LazyListAdapterHelper<Value> create() {
            if (mDiffCallback == null) {
                throw new IllegalArgumentException("Must provide a diffCallback");
            }
            if (mUpdateCallback == null) {
                throw new IllegalArgumentException(
                        "must provide either an adapter or update callback");
            }
            if (mBackgroundThreadExecutor == null) {
                mBackgroundThreadExecutor = AppToolkitTaskExecutor.getIOThreadExecutor();
            }
            if (mMainThreadExecutor == null) {
                mMainThreadExecutor = AppToolkitTaskExecutor.getMainThreadExecutor();
            }
            final LazyListAdapterHelper<Value> result =
                    new LazyListAdapterHelper<>(
                            mMainThreadExecutor,
                            mBackgroundThreadExecutor,
                            mUpdateCallback, mDiffCallback,
                            mLiveData == null);

            if (mLifecycle != null) {
                mLifecycle.getLifecycle().addObserver(new LifecycleObserver() {
                    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                    public void clear() {
                        result.internalSetLazyList(null);
                    }
                });
            }
            if (mLiveData != null) {
                if (mLifecycle == null) {
                    throw new IllegalArgumentException(
                            "If you provide a LiveData to be observed, you must also provide a"
                                    + " Livecycle via the lifecycle() method.");
                }
                mLiveData.observe(mLifecycle, new Observer<LazyList<Value>>() {
                    @Override
                    public void onChanged(@Nullable LazyList<Value> valueLazyList) {
                        result.internalSetLazyList(valueLazyList);
                    }
                });
            }
            return result;
        }
    }
}
