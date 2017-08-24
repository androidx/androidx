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

import java.util.concurrent.Executor;

public class ListAdapterConfig<Value> {
    final Executor mMainThreadExecutor;
    final Executor mBackgroundThreadExecutor;
    final DiffCallback<Value> mDiffCallback;

    private ListAdapterConfig(Executor mainThreadExecutor, Executor backgroundThreadExecutor,
            DiffCallback<Value> diffCallback) {
        mMainThreadExecutor = mainThreadExecutor;
        mBackgroundThreadExecutor = backgroundThreadExecutor;
        mDiffCallback = diffCallback;
    }

    public static class Builder<Value> {
        private Executor mMainThreadExecutor;
        private Executor mBackgroundThreadExecutor;
        private DiffCallback<Value> mDiffCallback;

        /**
         * The {@link DiffCallback} to be used while diffing an old list with the updated one.
         * Must be provided.
         *
         * @param diffCallback The {@link DiffCallback} instance to compare items in the list.
         * @return this
         */
        @SuppressWarnings("WeakerAccess")
        public ListAdapterConfig.Builder<Value> setDiffCallback(DiffCallback<Value> diffCallback) {
            mDiffCallback = diffCallback;
            return this;
        }

        /**
         * If provided, {@link ListAdapterHelper} will use the given executor to execute
         * adapter update notifications on the main thread.
         * <p>
         * If not provided, it will default to the UI thread.
         *
         * @param executor The executor which can run tasks in the UI thread.
         * @return this
         */
        @SuppressWarnings("unused")
        public ListAdapterConfig.Builder<Value> setMainThreadExecutor(Executor executor) {
            mMainThreadExecutor = executor;
            return this;
        }

        /**
         * If provided, {@link ListAdapterHelper} will use the given executor to calculate the
         * diff between an old and a new list.
         * <p>
         * If not provided, defaults to the IO thread pool from Architecture Components.
         *
         * @param executor The background executor to run list diffing.
         * @return this
         */
        @SuppressWarnings("unused")
        public ListAdapterConfig.Builder<Value> setBackgroundThreadExecutor(Executor executor) {
            mBackgroundThreadExecutor = executor;
            return this;
        }

        /**
         * Creates a {@link ListAdapterHelper} with the given parameters.
         *
         * @return A new ListAdapterHelper.
         */
        public ListAdapterConfig<Value> build() {
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
