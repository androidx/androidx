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
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 * Implementation of {@link SupportSQLiteOpenHelper.Factory} that creates
 * {@link SQLiteCopyOpenHelper}.
 */
class SQLiteCopyOpenHelperFactory implements SupportSQLiteOpenHelper.Factory {

    @Nullable
    private final String mCopyFromAssetPath;
    @Nullable
    private final File mCopyFromFile;
    @Nullable
    private final Callable<InputStream> mCopyFromInputStream;
    @NonNull
    private final SupportSQLiteOpenHelper.Factory mDelegate;

    SQLiteCopyOpenHelperFactory(
            @Nullable String copyFromAssetPath,
            @Nullable File copyFromFile,
            @Nullable Callable<InputStream> copyFromInputStream,
            @NonNull SupportSQLiteOpenHelper.Factory factory) {
        mCopyFromAssetPath = copyFromAssetPath;
        mCopyFromFile = copyFromFile;
        mCopyFromInputStream = copyFromInputStream;
        mDelegate = factory;
    }

    @NonNull
    @Override
    public SupportSQLiteOpenHelper create(SupportSQLiteOpenHelper.Configuration configuration) {
        return new SQLiteCopyOpenHelper(
                configuration.context,
                mCopyFromAssetPath,
                mCopyFromFile,
                mCopyFromInputStream,
                configuration.callback.version,
                mDelegate.create(configuration));
    }
}
