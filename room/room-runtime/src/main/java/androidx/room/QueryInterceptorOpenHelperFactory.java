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

package androidx.room;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import java.util.concurrent.Executor;

/**
 * Implements {@link SupportSQLiteOpenHelper.Factory} to wrap QueryInterceptorOpenHelper.
 */
@SuppressWarnings("AcronymName")
final class QueryInterceptorOpenHelperFactory implements SupportSQLiteOpenHelper.Factory {

    private final SupportSQLiteOpenHelper.Factory mDelegate;
    private final RoomDatabase.QueryCallback mQueryCallback;
    private final Executor mQueryCallbackExecutor;

    @SuppressWarnings("LambdaLast")
    QueryInterceptorOpenHelperFactory(@NonNull SupportSQLiteOpenHelper.Factory factory,
            @NonNull RoomDatabase.QueryCallback queryCallback,
            @NonNull Executor queryCallbackExecutor) {
        mDelegate = factory;
        mQueryCallback = queryCallback;
        mQueryCallbackExecutor = queryCallbackExecutor;
    }

    @NonNull
    @Override
    public SupportSQLiteOpenHelper create(
            @NonNull SupportSQLiteOpenHelper.Configuration configuration) {
        return new QueryInterceptorOpenHelper(mDelegate.create(configuration), mQueryCallback,
                mQueryCallbackExecutor);
    }
}
