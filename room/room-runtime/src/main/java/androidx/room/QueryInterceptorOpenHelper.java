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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import java.util.concurrent.Executor;

final class QueryInterceptorOpenHelper implements SupportSQLiteOpenHelper, DelegatingOpenHelper {

    private final SupportSQLiteOpenHelper mDelegate;
    private final RoomDatabase.QueryCallback mQueryCallback;
    private final Executor mQueryCallbackExecutor;

    QueryInterceptorOpenHelper(@NonNull SupportSQLiteOpenHelper supportSQLiteOpenHelper,
            @NonNull RoomDatabase.QueryCallback queryCallback, @NonNull Executor
            queryCallbackExecutor) {
        mDelegate = supportSQLiteOpenHelper;
        mQueryCallback = queryCallback;
        mQueryCallbackExecutor = queryCallbackExecutor;
    }

    @Nullable
    @Override
    public String getDatabaseName() {
        return mDelegate.getDatabaseName();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void setWriteAheadLoggingEnabled(boolean enabled) {
        mDelegate.setWriteAheadLoggingEnabled(enabled);
    }

    @Override
    public SupportSQLiteDatabase getWritableDatabase() {
        return new QueryInterceptorDatabase(mDelegate.getWritableDatabase(), mQueryCallback,
                mQueryCallbackExecutor);
    }

    @Override
    public SupportSQLiteDatabase getReadableDatabase() {
        return new QueryInterceptorDatabase(mDelegate.getReadableDatabase(), mQueryCallback,
                mQueryCallbackExecutor);
    }

    @Override
    public void close() {
        mDelegate.close();
    }

    @Override
    @NonNull
    public SupportSQLiteOpenHelper getDelegate() {
        return mDelegate;
    }

}
