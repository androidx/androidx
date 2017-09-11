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

import android.arch.core.executor.AppToolkitTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration object for {@link ListAdapter}, {@link ListAdapterHelper}, and similar
 * background-thread list diffing adapter logic.
 * <p>
 * At minimum, defines item diffing behavior with a {@link DiffCallback}, used to compute item
 * differences to pass to a RecyclerView adapter.
 *
 * @param <T> Type of items in the lists, and being compared.
 */
public final class ListAdapterConfig<T> {
    private final Executor mMainThreadExecutor;
    private final Executor mBackgroundThreadExecutor;
    private final DiffCallback<T> mDiffCallback;

    private ListAdapterConfig(Executor mainThreadExecutor, Executor backgroundThreadExecutor,
            DiffCallback<T> diffCallback) {
        mMainThreadExecutor = mainThreadExecutor;
        mBackgroundThreadExecutor = backgroundThreadExecutor;
        mDiffCallback = diffCallback;
    }

    public Executor getMainThreadExecutor() {
        return mMainThreadExecutor;
    }

    public Executor getBackgroundThreadExecutor() {
        return mBackgroundThreadExecutor;
    }

    public DiffCallback<T> getDiffCallback() {
        return mDiffCallback;
    }

    /**
     * Builder class for {@link ListAdapterConfig}.
     * <p>
     * You must at minimum specify a DiffCallback with {@link #setDiffCallback(DiffCallback)}
     *
     * @param <T>
     */
    public static class Builder<T> {
        private Executor mMainThreadExecutor;
        private Executor mBackgroundThreadExecutor;
        private DiffCallback<T> mDiffCallback;

        /**
         * The {@link DiffCallback} to be used while diffing an old list with the updated one.
         * Must be provided.
         *
         * @param diffCallback The {@link DiffCallback} instance to compare items in the list.
         * @return this
         */
        @SuppressWarnings("WeakerAccess")
        public ListAdapterConfig.Builder<T> setDiffCallback(DiffCallback<T> diffCallback) {
            mDiffCallback = diffCallback;
            return this;
        }

        /**
         * If provided, defines the main thread executor used to dispatch adapter update
         * notifications on the main thread.
         * <p>
         * If not provided, it will default to the UI thread.
         *
         * @param executor The executor which can run tasks in the UI thread.
         * @return this
         */
        @SuppressWarnings("unused")
        public ListAdapterConfig.Builder<T> setMainThreadExecutor(Executor executor) {
            mMainThreadExecutor = executor;
            return this;
        }

        /**
         * If provided, defines the background executor used to calculate the diff between an old
         * and a new list.
         * <p>
         * If not provided, defaults to the IO thread pool from Architecture Components.
         *
         * @param executor The background executor to run list diffing.
         * @return this
         */
        @SuppressWarnings("unused")
        public ListAdapterConfig.Builder<T> setBackgroundThreadExecutor(Executor executor) {
            mBackgroundThreadExecutor = executor;
            return this;
        }

        /**
         * Creates a {@link ListAdapterHelper} with the given parameters.
         *
         * @return A new ListAdapterConfig.
         */
        public ListAdapterConfig<T> build() {
            if (mDiffCallback == null) {
                throw new IllegalArgumentException("Must provide a diffCallback");
            }
            if (mBackgroundThreadExecutor == null) {
                mBackgroundThreadExecutor = AppToolkitTaskExecutor.getIOThreadExecutor();
            }
            if (mMainThreadExecutor == null) {
                mMainThreadExecutor = AppToolkitTaskExecutor.getMainThreadExecutor();
            }
            return new ListAdapterConfig<>(
                    mMainThreadExecutor,
                    mBackgroundThreadExecutor,
                    mDiffCallback);
        }
    }
}
