/*
 * Copyright 2020 The Android Open Source Project
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
// @exportToFramework:skipFile()
package androidx.appsearch.localstorage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.app.ReportSystemUsageRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.util.FutureUtil;
import androidx.appsearch.observer.AppSearchObserverCallback;
import androidx.appsearch.observer.ObserverSpec;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * An implementation of {@link GlobalSearchSession} which stores data locally
 * in the app's storage space using a bundled version of the search native library.
 *
 * <p>Queries are executed multi-threaded, but a single thread is used for mutate requests (put,
 * delete, etc..).
 */
class GlobalSearchSessionImpl implements GlobalSearchSession {
    private final AppSearchImpl mAppSearchImpl;
    private final Executor mExecutor;
    private final Features mFeatures;
    private final Context mContext;

    private boolean mIsClosed = false;

    @Nullable
    private final AppSearchLogger mLogger;

    GlobalSearchSessionImpl(
            @NonNull AppSearchImpl appSearchImpl,
            @NonNull Executor executor,
            @NonNull Features features,
            @NonNull Context context,
            @Nullable AppSearchLogger logger) {
        mAppSearchImpl = Preconditions.checkNotNull(appSearchImpl);
        mExecutor = Preconditions.checkNotNull(executor);
        mFeatures = Preconditions.checkNotNull(features);
        mContext = Preconditions.checkNotNull(context);
        mLogger = logger;
    }

    @NonNull
    @Override
    public SearchResults search(
            @NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        return new SearchResultsImpl(
                mAppSearchImpl,
                mExecutor,
                mContext.getPackageName(),
                /*databaseName=*/ null,
                queryExpression,
                searchSpec,
                mLogger);
    }

    /**
     * Reporting system usage is not supported in the local backend, so this method does nothing
     * and always completes the return value with an
     * {@link androidx.appsearch.exceptions.AppSearchException} having a result code of
     * {@link AppSearchResult#RESULT_SECURITY_ERROR}.
     */
    @NonNull
    @Override
    public ListenableFuture<Void> reportSystemUsage(@NonNull ReportSystemUsageRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        return FutureUtil.execute(mExecutor, () -> {
            throw new AppSearchException(
                    AppSearchResult.RESULT_SECURITY_ERROR,
                    mContext.getPackageName() + " does not have access to report system usage");
        });
    }

    @SuppressLint("KotlinPropertyAccess")
    @NonNull
    @Override
    public ListenableFuture<GetSchemaResponse> getSchema(@NonNull String packageName,
            @NonNull String databaseName) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        return FutureUtil.execute(mExecutor,
                () -> mAppSearchImpl.getSchema(
                        packageName,
                        databaseName,
                        /*callerPackageName=*/mContext.getPackageName(),
                        /*callerUid=*/Process.myUid(),
                        /*callerHasSystemAccess=*/false));
    }

    @NonNull
    @Override
    public Features getFeatures() {
        return mFeatures;
    }

    @Override
    public void addObserver(
            @NonNull String observedPackage,
            @NonNull ObserverSpec spec,
            @NonNull Executor executor,
            @NonNull AppSearchObserverCallback observer) {
        Preconditions.checkNotNull(observedPackage);
        Preconditions.checkNotNull(spec);
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(observer);
        // LocalStorage does not support observing data from other packages.
        if (!observedPackage.equals(mContext.getPackageName())) {
            throw new UnsupportedOperationException(
                    "Local storage implementation does not support receiving change notifications "
                            + "from other packages.");
        }
        mAppSearchImpl.addObserver(observedPackage, spec, executor, observer);
    }

    @Override
    public void removeObserver(
            @NonNull String observedPackage, @NonNull AppSearchObserverCallback observer) {
        Preconditions.checkNotNull(observedPackage);
        Preconditions.checkNotNull(observer);
        // LocalStorage does not support observing data from other packages.
        if (!observedPackage.equals(mContext.getPackageName())) {
            throw new UnsupportedOperationException(
                    "Local storage implementation does not support receiving change notifications "
                            + "from other packages.");
        }
        mAppSearchImpl.removeObserver(observedPackage, observer);
    }

    @Override
    public void close() {
        mIsClosed = true;
    }
}
