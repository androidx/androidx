/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.room.DatabaseConfiguration.CopyFrom;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

/**
 * Implementation of {@link SupportSQLiteOpenHelper.Factory} that creates
 * {@link SQLiteCopyOpenHelper}.
 */
class SQLiteCopyOpenHelperFactory implements SupportSQLiteOpenHelper.Factory {

    @CopyFrom
    private final int mCopyFrom;
    @NonNull
    private final String mCopyFromFilePath;
    @NonNull
    private final SupportSQLiteOpenHelper.Factory mDelegate;

    SQLiteCopyOpenHelperFactory(
            @CopyFrom int copyFrom,
            @NonNull String copyFromFilePath,
            @NonNull SupportSQLiteOpenHelper.Factory factory) {
        mCopyFrom = copyFrom;
        mCopyFromFilePath = copyFromFilePath;
        mDelegate = factory;
    }

    @Override
    public SupportSQLiteOpenHelper create(SupportSQLiteOpenHelper.Configuration configuration) {
        return new SQLiteCopyOpenHelper(
                configuration.context,
                mCopyFrom,
                mCopyFromFilePath,
                configuration.callback.version,
                mDelegate.create(configuration));
    }
}
