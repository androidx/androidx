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
import androidx.paging.LoadType;
import androidx.paging.PagingState;
import androidx.paging.rxjava2.RxRemoteMediator;
import androidx.paging.samples.shared.ExampleRxBackendService;
import androidx.paging.samples.shared.RoomDb;
import androidx.paging.samples.shared.SearchUserResponse;
import androidx.paging.samples.shared.User;
import androidx.paging.samples.shared.UserDao;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

@SuppressWarnings("unused")
@OptIn(markerClass = ExperimentalPagingApi.class)
class RemoteMediatorRxItemKeyedSample extends RxRemoteMediator<Integer, User> {
    private String mQuery;
    private ExampleRxBackendService mNetworkService;
    private RoomDb mDatabase;
    private UserDao mUserDao;

    RemoteMediatorRxItemKeyedSample(String query, ExampleRxBackendService networkService,
            RoomDb database) {
        mQuery = query;
        mNetworkService = networkService;
        mDatabase = database;
        mUserDao = database.userDao();
    }

    @NotNull
    @Override
    public Single<InitializeAction> initializeSingle() {
        long cacheTimeout = TimeUnit.HOURS.convert(1, TimeUnit.MILLISECONDS);
        return mUserDao.lastUpdatedSingle()
                .map(lastUpdatedMillis -> {
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
                });
    }

    @NotNull
    @Override
    public Single<MediatorResult> loadSingle(@NotNull LoadType loadType,
            @NotNull PagingState<Integer, User> state) {
        // The network load method takes an optional `after=<user.id>` parameter. For every
        // page after the first, we pass the last user ID to let it continue from where it
        // left off. For REFRESH, pass `null` to load the first page.
        String loadKey = null;
        switch (loadType) {
            case REFRESH:
                break;
            case PREPEND:
                // In this example, we never need to prepend, since REFRESH will always load the
                // first page in the list. Immediately return, reporting end of pagination.
                return Single.just(new MediatorResult.Success(true));
            case APPEND:
                User lastItem = state.lastItemOrNull();

                // We must explicitly check if the last item is `null` when appending,
                // since passing `null` to networkService is only valid for initial load.
                // If lastItem is `null` it means no items were loaded after the initial
                // REFRESH and there are no more items to load.
                if (lastItem == null) {
                    return Single.just(new MediatorResult.Success(true));
                }

                loadKey = lastItem.getId();
                break;
        }

        return mNetworkService.searchUsers(mQuery, loadKey)
                .subscribeOn(Schedulers.io())
                .map((Function<SearchUserResponse, MediatorResult>) response -> {
                    mDatabase.runInTransaction(() -> {
                        if (loadType == LoadType.REFRESH) {
                            mUserDao.deleteByQuery(mQuery);
                        }

                        // Insert new users into database, which invalidates the current
                        // PagingData, allowing Paging to present the updates in the DB.
                        mUserDao.insertAll(response.getUsers());
                    });

                    return new MediatorResult.Success(response.getNextKey() == null);
                })
                .onErrorResumeNext(e -> {
                    if (e instanceof IOException || e instanceof HttpException) {
                        return Single.just(new MediatorResult.Error(e));
                    }

                    return Single.error(e);
                });
    }
}
