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

package androidx.paging.samples.java;

import androidx.annotation.OptIn;
import androidx.paging.ExperimentalPagingApi;
import androidx.paging.ListenableFutureRemoteMediator;
import androidx.paging.LoadType;
import androidx.paging.PagingState;
import androidx.paging.samples.shared.ExampleGuavaBackendService;
import androidx.paging.samples.shared.RemoteKey;
import androidx.paging.samples.shared.RemoteKeyDao;
import androidx.paging.samples.shared.RoomDb;
import androidx.paging.samples.shared.User;
import androidx.paging.samples.shared.UserDao;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import retrofit2.HttpException;

@SuppressWarnings("unused")
@OptIn(markerClass = ExperimentalPagingApi.class)
class RemoteMediatorGuavaPageKeyedSample extends ListenableFutureRemoteMediator<Integer, User> {
    private String mQuery;
    private ExampleGuavaBackendService mNetworkService;
    private RoomDb mDatabase;
    private UserDao mUserDao;
    private RemoteKeyDao mRemoteKeyDao;
    private Executor mBgExecutor;

    RemoteMediatorGuavaPageKeyedSample(String query, ExampleGuavaBackendService networkService,
            RoomDb database, Executor bgExecutor) {
        mQuery = query;
        mNetworkService = networkService;
        mDatabase = database;
        mUserDao = database.userDao();
        mRemoteKeyDao = database.remoteKeyDao();
        mBgExecutor = bgExecutor;
    }

    @NotNull
    @Override
    public ListenableFuture<InitializeAction> initializeFuture() {
        long cacheTimeout = TimeUnit.HOURS.convert(1, TimeUnit.MILLISECONDS);
        return Futures.transform(
                mUserDao.lastUpdatedFuture(),
                lastUpdatedMillis -> {
                    if (System.currentTimeMillis() - lastUpdatedMillis >= cacheTimeout) {
                        // Cached data is up-to-date, so there is no need to re-fetch
                        // from the network.
                        return InitializeAction.SKIP_INITIAL_REFRESH;
                    } else {
                        // Need to refresh cached data from network; returning
                        // LAUNCH_INITIAL_REFRESH here will also block RemoteMediator's
                        // APPEND and PREPEND from running until REFRESH succeeds.
                        return InitializeAction.LAUNCH_INITIAL_REFRESH;
                    }
                },
                mBgExecutor);
    }

    @NotNull
    @Override
    public ListenableFuture<MediatorResult> loadFuture(@NotNull LoadType loadType,
            @NotNull PagingState<Integer, User> state) {
        // The network load method takes an optional `after=<user.id>` parameter. For every
        // page after the first, we pass the last user ID to let it continue from where it
        // left off. For REFRESH, pass `null` to load the first page.
        ListenableFuture<RemoteKey> remoteKeyFuture = null;
        switch (loadType) {
            case REFRESH:
                remoteKeyFuture = Futures.immediateFuture(new RemoteKey(mQuery, null));
                break;
            case PREPEND:
                // In this example, we never need to prepend, since REFRESH will always load the
                // first page in the list. Immediately return, reporting end of pagination.
                return Futures.immediateFuture(new MediatorResult.Success(true));
            case APPEND:
                User lastItem = state.lastItemOrNull();

                // We must explicitly check if the last item is `null` when appending,
                // since passing `null` to networkService is only valid for initial load.
                // If lastItem is `null` it means no items were loaded after the initial
                // REFRESH and there are no more items to load.
                if (lastItem == null) {
                    return Futures.immediateFuture(new MediatorResult.Success(true));
                }

                // Query remoteKeyDao for the next RemoteKey.
                remoteKeyFuture = mRemoteKeyDao.remoteKeyByQueryFuture(mQuery);
                break;
        }

        //noinspection UnstableApiUsage
        return Futures.transformAsync(remoteKeyFuture, remoteKey -> {
            if (remoteKey == null) {
                throw new IllegalStateException("RemoteKey should never be null, since we would "
                        + "normally return MediatorResult.Success(true) immediately.");
            }

            // We must explicitly check if the page key is `null` when appending,
            // since `null` is only valid for initial load. If we receive `null`
            // for APPEND, that means we have reached the end of pagination and
            // there are no more items to load.
            if (loadType != LoadType.REFRESH && remoteKey.getNextKey() == null) {
                return Futures.immediateFuture(new MediatorResult.Success(true));
            }

            //noinspection UnstableApiUsage
            ListenableFuture<MediatorResult> networkResult = Futures.transform(
                    mNetworkService.searchUsers(mQuery, remoteKey.getNextKey()),
                    response -> {
                        if (response == null) {
                            throw new IllegalArgumentException("Response from network was null");
                        }

                        mDatabase.runInTransaction(() -> {
                            if (loadType == LoadType.REFRESH) {
                                mUserDao.deleteByQuery(mQuery);
                                mRemoteKeyDao.deleteByQuery(mQuery);
                            }

                            // Update RemoteKey for this query.
                            mRemoteKeyDao.insertOrReplace(
                                    new RemoteKey(mQuery, response.getNextKey()));

                            // Insert new users into database, which invalidates the current
                            // PagingData, allowing Paging to present the updates in the DB.
                            mUserDao.insertAll(response.getUsers());
                        });

                        return new MediatorResult.Success(response.getNextKey() == null);
                    }, mBgExecutor);

            @SuppressWarnings("UnstableApiUsage")
            ListenableFuture<MediatorResult> ioCatchingNetworkResult = Futures.catching(
                    networkResult, IOException.class, MediatorResult.Error::new,
                    mBgExecutor);

            //noinspection UnstableApiUsage
            return Futures.catching(ioCatchingNetworkResult, HttpException.class,
                    MediatorResult.Error::new, mBgExecutor);

        }, mBgExecutor);
    }
}
